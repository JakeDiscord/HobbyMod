package io.github.jakediscord.hobbymod.painting;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Vanilla hanging-entity placement, collision, support checks, damage and drops. */
public final class PaintedCanvasEntity extends HangingEntity {
    private static final EntityDataAccessor<CompoundTag> ART=SynchedEntityData.defineId(PaintedCanvasEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private PaintingData painting;
    public PaintedCanvasEntity(EntityType<? extends PaintedCanvasEntity> type,Level level){super(type,level);}
    public PaintedCanvasEntity(Level level,BlockPos pos,Direction face,PaintingData data){super(PaintingContent.HANGING.get(),level,pos);setPainting(data);setDirection(face);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(ART,new CompoundTag());}
    public PaintingData painting(){if(painting==null)painting=PaintingNbt.read(entityData.get(ART),PaintingData.Shape.SQUARE);return painting;}
    private void setPainting(PaintingData d){painting=d.copy();entityData.set(ART,PaintingNbt.save(d));}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key.equals(ART)){painting=null;if(pos!=null && direction!=null)recalculateBoundingBox();}}
    public int canvasWidth(){return switch(painting().shape){case LANDSCAPE->3;case PORTRAIT->2;case PANORAMA->4;default->1;};}
    public int canvasHeight(){return switch(painting().shape){case LANDSCAPE,PANORAMA->2;case PORTRAIT->3;default->1;};}
    @Override protected AABB calculateBoundingBox(BlockPos anchor,Direction face){
        double w=canvasWidth(),h=canvasHeight();Direction across=face.getCounterClockWise();
        Vec3 center=Vec3.atCenterOf(anchor).add(-face.getStepX()*.46875,h%2==0?.5:0,-face.getStepZ()*.46875).add(across.getStepX()*(w%2==0?.5:0),0,across.getStepZ()*(w%2==0?.5:0));
        double x=face.getAxis()==Direction.Axis.X?1.0/16:w,z=face.getAxis()==Direction.Axis.Z?1.0/16:w;
        return AABB.ofSize(center,x,h,z);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.put("Painting",PaintingNbt.save(painting()));tag.putByte("Facing",(byte)direction.get3DDataValue());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setPainting(PaintingNbt.read(tag.getCompound("Painting"),PaintingData.Shape.SQUARE));Direction face=Direction.from3DDataValue(tag.getByte("Facing"));setDirection(face.getAxis().isHorizontal()?face:Direction.NORTH);}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity e){return new ClientboundAddEntityPacket(this,direction.get3DDataValue(),getPos());}
    @Override public void recreateFromPacket(ClientboundAddEntityPacket p){super.recreateFromPacket(p);Direction face=Direction.from3DDataValue(p.getData());setDirection(face.getAxis().isHorizontal()?face:Direction.NORTH);}
    @Override public void playPlacementSound(){playSound(SoundEvents.PAINTING_PLACE,1,1);}
    @Override public void dropItem(Entity cause){if(!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))return;playSound(SoundEvents.PAINTING_BREAK,1,1);if(cause instanceof Player p && p.getAbilities().instabuild)return;spawnAtLocation(CanvasItem.create(painting()));}
    @Override public ItemStack getPickResult(){return CanvasItem.create(painting());}
    @Override public InteractionResult interact(Player p,InteractionHand hand){
        if(!io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,getPos()))return InteractionResult.FAIL;
        if(!level().isClientSide){if(p.isShiftKeyDown() && p.getItemInHand(hand).isEmpty()){PaintingBlock.give(p,CanvasItem.create(painting()));discard();}else p.displayClientMessage(net.minecraft.network.chat.Component.literal((painting().title.isBlank()?painting().shape.label+" painting":painting().title)+(painting().author.isBlank()?"":" · "+painting().author)),true);}
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
