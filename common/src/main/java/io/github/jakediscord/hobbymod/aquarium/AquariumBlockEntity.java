package io.github.jakediscord.hobbymod.aquarium;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AquariumBlockEntity extends BlockEntity {
    public enum Condition { READY,EMPTY,MISSING_WATER,BROKEN_GLASS,UNLOADED }
    public final AquariumData data=new AquariumData();
    public Condition condition=Condition.EMPTY;
    private final AquariumClock clock=new AquariumClock();
    public AquariumBlockEntity(BlockPos pos,BlockState state){super(AquariumContent.TANK_ENTITY.get(),pos,state);}
    public BlockPos maximum(){return worldPosition.offset(data.size.width-1,data.size.height-1,data.size.depth-1);}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public Condition inspect(){
        if(level==null || !level.hasChunksAt(worldPosition,maximum()))return condition=Condition.UNLOADED;
        int water=0,volume=data.size.volume();boolean broken=false;
        for(int y=0;y<data.size.height;y++)for(int x=0;x<data.size.width;x++)for(int z=0;z<data.size.depth;z++){
            if(x==0 && y==0 && z==0)continue;
            BlockPos p=worldPosition.offset(x,y,z);BlockState state=level.getBlockState(p);
            if(data.size.shell(x,y,z)){
                boolean glass=state.is(Blocks.GLASS)||state.is(BlockTags.IMPERMEABLE);
                if(!glass && !(y==0 && state.isSolidRender(level,p)))broken=true;
            }else if(y<data.size.height-1 && state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource())water++;
        }
        data.filled=water==volume;
        return condition=broken?Condition.BROKEN_GLASS:water==volume?Condition.READY:water==0?Condition.EMPTY:Condition.MISSING_WATER;
    }
    /** Preflight the entire fill/drain before changing any block. Never overwrite interior builds. */
    public boolean water(boolean fill){return water(fill,true);}
    public boolean restoreWater(){return water(true,false);}
    private boolean water(boolean fill,boolean care){
        if(level==null || !level.hasChunksAt(worldPosition,maximum()))return false;
        if(fill){
            for(int y=1;y<data.size.height-1;y++)for(int x=1;x<data.size.width-1;x++)for(int z=1;z<data.size.depth-1;z++){
                BlockState state=level.getBlockState(worldPosition.offset(x,y,z));
                if(!state.isAir() && !state.is(Blocks.WATER))return false;
            }
        }
        for(int y=1;y<data.size.height-1;y++)for(int x=1;x<data.size.width-1;x++)for(int z=1;z<data.size.depth-1;z++){
            BlockPos p=worldPosition.offset(x,y,z);BlockState state=level.getBlockState(p);
            if(fill)level.setBlock(p,Blocks.WATER.defaultBlockState(),3);
            else if(state.is(Blocks.WATER))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        }
        if(care){if(fill){if(!data.filled)clock.reset();data.waterChange();}else data.drain();}inspect();changed();return true;
    }
    public String warning(){
        return switch(condition){case UNLOADED->"Tank partly unloaded.";case BROKEN_GLASS->"Repair tank walls.";
            case MISSING_WATER->"Refill missing water.";case EMPTY->"Fill the tank.";case READY->data.issue(true);};
    }
    public static void tick(Level level,BlockPos pos,BlockState state,AquariumBlockEntity tank){
        if(level.isClientSide)return;
        if(level.getGameTime()%20==0){
            Condition previous=tank.condition;boolean filled=tank.data.filled;
            tank.inspect();if(previous!=tank.condition || filled!=tank.data.filled)tank.changed();
        }
        if(tank.condition==Condition.UNLOADED){tank.clock.reset();return;}
        if(!tank.clock.poll(level.getGameTime()))return;
        tank.inspect();if(tank.condition==Condition.UNLOADED){tank.clock.reset();return;}
        tank.data.advance(tank.condition==Condition.READY,level.getMaxLocalRawBrightness(pos.offset(2,2,2))>=9);
        if(tank.data.filter && tank.condition==Condition.READY && level instanceof net.minecraft.server.level.ServerLevel server)
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,pos.getX()+1.3,pos.getY()+1.7,pos.getZ()+1.3,3,.05,.2,.05,.01);
        tank.changed();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.put("Aquarium",AquariumNbt.save(data));tag.putString("Condition",condition.name());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);clock.reset();
        if(tag.contains("Aquarium"))AquariumNbt.load(data,tag.getCompound("Aquarium"));
        try{condition=Condition.valueOf(tag.getString("Condition"));}catch(IllegalArgumentException e){condition=Condition.EMPTY;}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
