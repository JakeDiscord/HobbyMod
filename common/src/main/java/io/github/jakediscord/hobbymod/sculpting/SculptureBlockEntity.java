package io.github.jakediscord.hobbymod.sculpting;

import io.github.jakediscord.hobbymod.registry.HobbyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SculptureBlockEntity extends BlockEntity {
    private MarbleVolume volume = new MarbleVolume();
    private int revision;
    private int snapshots;
    public int snapshots(){return snapshots;}
    public void applyBlueprint(MarbleVolume design){volume=design.copy();update();}
    private VoxelShape shape;
    private MarbleMesh joinedMesh;
    private MarbleVolume meshOwn,meshBelow,meshAbove;
    private int meshRevision=-1,belowRevision=-1,aboveRevision=-1;

    public SculptureBlockEntity(BlockPos pos, BlockState state) {
        super(HobbyContent.SCULPTURE_ENTITY.get(), pos, state);
    }
    public MarbleVolume volume() { return volume; }
    public int revision() { return revision; }

    /** Server-side transaction. Stale operations cannot overwrite another artist's work. */
    public int carve(Player player, ItemStack toolStack, double x, double y, double z, boolean mirror, int expectedRevision) {
        if (level == null || level.isClientSide || expectedRevision != revision
                || !(toolStack.getItem() instanceof ChiselItem item) || !volume.exposed(x,y,z)) return 0;
        return carvePath(player,toolStack,x,y,z,mirror,expectedRevision,null);
    }

    public int carvePath(Player player,ItemStack toolStack,double x,double y,double z,boolean mirror,int expectedRevision,net.minecraft.world.phys.Vec3 previous) {
        if(level==null || level.isClientSide || revision!=expectedRevision || !(toolStack.getItem() instanceof ChiselItem item)
                || !volume.exposed(x,y,z) || !SculptureNetworking.permitted(player,worldPosition,toolStack))return 0;
        var sections=MarbleColumn.sculptures(level,worldPosition);
        if(sections.isEmpty())return 0;
        for(var s:sections)if(!SculptureNetworking.mayEdit(player,s.worldPosition,toolStack))return 0;
        var volumes=sections.stream().map(SculptureBlockEntity::volume).toList();
        var before=volumes.stream().map(MarbleVolume::copy).toList();
        var end=new net.minecraft.world.phys.Vec3(worldPosition.getX()+x,worldPosition.getY()+y,worldPosition.getZ()+z);
        double length=previous==null?0:previous.distanceTo(end);
        int steps=length>0 && length<=1.25?Math.clamp((int)Math.ceil(length/(item.tool().cutRadius*0.5)),1,64):1;
        int changed=0;
        for(int step=1;step<=steps;step++) {
            var point=steps==1?end:previous.lerp(end,step/(double)steps);
            var section=sections.stream().filter(s->point.y>=s.worldPosition.getY() && point.y<=s.worldPosition.getY()+1).findFirst().orElse(this);
            double px=point.x-section.worldPosition.getX(),py=point.y-section.worldPosition.getY(),pz=point.z-section.worldPosition.getZ();
            int index=sections.indexOf(section);
            // Earlier samples in this packet must not erase the surface needed by later samples.
            var surface=before.get(index);
            if(!surface.exposed(px,py,pz))continue;
            var field=MarbleColumn.field(surface,index>0?before.get(index-1):null,index+1<before.size()?before.get(index+1):null);
            double[] normal=field.normal(px,py,pz);
            boolean mirrored=mirror && Math.abs(px-0.5)>0.001 && surface.exposed(1-px,py,pz);
            double[] reflected=mirrored?field.normal(1-px,py,pz):null;
            for(var s:sections) {
                double localY=point.y-s.worldPosition.getY();
                if(localY < -item.tool().cutRadius || localY>1+item.tool().cutRadius)continue;
                changed+=s.volume.applyBrush(px,localY,pz,item.tool(),normal);
                if(mirrored)changed+=s.volume.applyBrush(1-px,localY,pz,item.tool(),reflected);
            }
        }
        if(changed==0)return 0;
        MarbleColumn.prune(volumes);
        if(volumes.stream().mapToInt(MarbleVolume::count).sum()==0) {
            for(int i=0;i<sections.size();i++)sections.get(i).volume=before.get(i);
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            toolStack.hurtAndBreak(1, player, toolStack == player.getOffhandItem()
                    ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        for(var s:sections) {
            if(s.volume.count()==0)level.removeBlock(s.worldPosition,false);
            else s.update();
        }
        return changed;
    }

    void update() {
        revision++;
        shape = null;
        joinedMesh=null;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public MarbleMesh mesh() {
        var below=level!=null && level.getBlockEntity(worldPosition.below()) instanceof SculptureBlockEntity s?s:null;
        var above=level!=null && level.getBlockEntity(worldPosition.above()) instanceof SculptureBlockEntity s?s:null;
        var bv=below==null?null:below.volume;var av=above==null?null:above.volume;
        int br=below==null?-1:below.revision,ar=above==null?-1:above.revision;
        if(joinedMesh==null || meshOwn!=volume || meshBelow!=bv || meshAbove!=av || meshRevision!=revision || belowRevision!=br || aboveRevision!=ar) {
            joinedMesh=MarbleMesh.build(MarbleColumn.field(volume,bv,av),below!=null,above!=null);
            meshOwn=volume;meshBelow=bv;meshAbove=av;meshRevision=revision;belowRevision=br;aboveRevision=ar;
        }
        return joinedMesh;
    }

    /** Cached 8^3 collision sampling bounds work per update, even for intricate designs. */
    public VoxelShape shape() {
        if (shape != null) return shape;
        if (volume.count() == MarbleVolume.CELLS) return shape = Shapes.block();
        VoxelShape result = Shapes.empty();
        for (int z = 0; z < 8; z++) for (int y = 0; y < 8; y++) {
            int start = -1;
            for (int x = 0; x <= 8; x++) {
                boolean occupied = false;
                if (x < 8) outer: for (int dx = 0; dx < 4; dx++) for (int dy = 0; dy < 4; dy++) for (int dz = 0; dz < 4; dz++) {
                    if (volume.has(x * 4 + dx, y * 4 + dy, z * 4 + dz)) { occupied = true; break outer; }
                }
                if (occupied && start < 0) start = x;
                if (!occupied && start >= 0) {
                    result = Shapes.or(result, Shapes.box(start / 8.0, y / 8.0, z / 8.0, x / 8.0, (y + 1) / 8.0, (z + 1) / 8.0));
                    start = -1;
                }
            }
        }
        return shape = result.optimize();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("sculpture_format", 3);
        tag.putByteArray("density", volume.densityBytes());
        tag.putLongArray("polish", volume.polishBits());
        tag.putInt("revision", revision);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        snapshots++;
        volume = tag.contains("density", net.minecraft.nbt.Tag.TAG_BYTE_ARRAY)
                ? MarbleVolume.read(tag.getByteArray("density"), tag.getLongArray("polish"),tag.getInt("sculpture_format")<3)
                : tag.contains("marble") ? MarbleVolume.read(tag.getLongArray("marble"), tag.getLongArray("polish")) : new MarbleVolume();
        revision = tag.getInt("revision");
        joinedMesh=null;
        shape = null;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
