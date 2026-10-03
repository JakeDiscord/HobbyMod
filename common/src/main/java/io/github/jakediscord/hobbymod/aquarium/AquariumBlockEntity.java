package io.github.jakediscord.hobbymod.aquarium;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AquariumBlockEntity extends BlockEntity {
    public enum Condition { READY,EMPTY,MISSING_WATER,BROKEN_GLASS,UNLOADED }
    public final AquariumData data=new AquariumData();
    public long fedAt=-1000;
    public Condition condition=Condition.EMPTY;
    private final AquariumClock clock=new AquariumClock();
    public AquariumBlockEntity(BlockPos pos,BlockState state){super(AquariumContent.TANK_ENTITY.get(),pos,state);}
    // Full model bounds supplied by the platform renderer; the shared entity stays loader-neutral.
    public net.minecraft.world.phys.AABB getRenderBoundingBox(){
        return new net.minecraft.world.phys.AABB(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),
                worldPosition.getX()+data.size.blocksWide(),worldPosition.getY()+data.size.blocksHigh(),worldPosition.getZ()+data.size.blocksDeep());
    }
    public BlockPos maximum(){return worldPosition.offset(data.size.blocksWide()-1,data.size.blocksHigh()-1,data.size.blocksDeep()-1);}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public Condition inspect(){
        if(level==null || !level.hasChunksAt(worldPosition,maximum()))return condition=Condition.UNLOADED;
        for(int y=0;y<data.size.blocksHigh();y++)for(int x=0;x<data.size.blocksWide();x++)for(int z=0;z<data.size.blocksDeep();z++){
            if(x==0 && y==0 && z==0)continue;
            if(!level.getBlockState(worldPosition.offset(x,y,z)).is(AquariumContent.PART.get()))return condition=Condition.BROKEN_GLASS;
        }
        return condition=data.filled?Condition.READY:Condition.EMPTY;
    }
    public boolean water(boolean fill){
        if(inspect()==Condition.UNLOADED || condition==Condition.BROKEN_GLASS)return false;
        if(fill){if(!data.filled)clock.reset();data.waterChange();}else data.drain();
        inspect();changed();return true;
    }
    public boolean restoreWater(){inspect();changed();return true;}
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
        tank.data.advance(tank.condition==Condition.READY,level.getMaxLocalRawBrightness(pos.above())>=9);
        tank.changed();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putLong("FedAt",fedAt);tag.put("Aquarium",AquariumNbt.save(data));tag.putString("Condition",condition.name());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);clock.reset();fedAt=tag.contains("FedAt")?tag.getLong("FedAt"):-1000;
        if(tag.contains("Aquarium"))AquariumNbt.load(data,tag.getCompound("Aquarium"));
        try{condition=Condition.valueOf(tag.getString("Condition"));}catch(IllegalArgumentException e){condition=Condition.EMPTY;}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
