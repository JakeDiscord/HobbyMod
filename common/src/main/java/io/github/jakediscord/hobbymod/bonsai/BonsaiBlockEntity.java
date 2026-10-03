package io.github.jakediscord.hobbymod.bonsai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BonsaiBlockEntity extends BlockEntity {
    public final BonsaiGraph graph=new BonsaiGraph();
    private final BonsaiGrowthClock clock=new BonsaiGrowthClock();
    public BonsaiBlockEntity(BlockPos pos,BlockState state){super(BonsaiContent.TREE.get(),pos,state);}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,BonsaiBlockEntity tree){
        if(level.isClientSide){tree.graph.grow(1);return;}
        if(tree.graph.grow(1) && level.getGameTime()%20==0)tree.setChanged();
        if(!tree.clock.poll(level.getGameTime()))return;
        if(!tree.graph.planted())return;
        tree.graph.advance(level.getMaxLocalRawBrightness(pos.above())>=9,level.isRainingAt(pos.above()));
        tree.changed();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);
        tag.putString("Species",graph.species.name());tag.putLong("Seed",graph.seed);
        tag.putInt("Age",graph.age);tag.putInt("Water",graph.water);tag.putInt("Health",graph.health);tag.putInt("SoilAge",graph.soilAge);tag.putInt("RootAge",graph.rootAge);tag.putInt("NextId",graph.nextId);
        ListTag nodes=new ListTag();
        for(BonsaiGraph.Node n:graph.nodes()){
            CompoundTag t=new CompoundTag();t.putInt("Id",n.id);t.putInt("Parent",n.parent);
            t.putDouble("Length",n.length);t.putDouble("Radius",n.radius);t.putDouble("Yaw",n.yaw);t.putDouble("Pitch",n.pitch);
            t.putInt("Age",n.age);t.putInt("Health",n.health);t.putInt("Bend",n.bend);t.putInt("GrowthTicks",n.growthTicks);t.putBoolean("Bud",n.bud);t.putBoolean("Wired",n.wired);nodes.add(t);
        }tag.put("Branches",nodes);
    }
    private static int bounded(CompoundTag t,String key,int max){return Math.max(0,Math.min(max,t.getInt(key)));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);graph.clearForLoad();graph.nextId=1;clock.reset();
        try{graph.species=BonsaiGraph.Species.valueOf(tag.getString("Species"));}catch(IllegalArgumentException e){graph.species=BonsaiGraph.Species.OAK;}
        graph.seed=tag.getLong("Seed");graph.age=bounded(tag,"Age",1_000_000);graph.water=bounded(tag,"Water",100);graph.health=bounded(tag,"Health",100);graph.soilAge=bounded(tag,"SoilAge",1_000_000);graph.rootAge=bounded(tag,"RootAge",1_000_000);
        ListTag nodes=tag.getList("Branches",Tag.TAG_COMPOUND);
        // Read at most 28 records even if external NBT has thousands of nodes.
        for(int i=0;i<Math.min(nodes.size(),BonsaiGraph.MAX_NODES);i++){
            CompoundTag t=nodes.getCompound(i);
            BonsaiGraph.Node n=new BonsaiGraph.Node(t.getInt("Id"),t.getInt("Parent"),t.getDouble("Length"),t.getDouble("Radius"),t.getDouble("Yaw"),t.getDouble("Pitch"));
            n.growthTicks=t.contains("GrowthTicks",Tag.TAG_INT)?t.getInt("GrowthTicks"):BonsaiGraph.GROWTH_TICKS;
            n.age=t.getInt("Age");n.health=t.getInt("Health");n.bend=t.getInt("Bend");n.bud=t.getBoolean("Bud");n.wired=t.getBoolean("Wired");graph.acceptLoaded(n);
        }
        graph.nextId=Math.max(graph.nextId,Math.max(1,Math.min(1_000_001,tag.getInt("NextId"))));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
