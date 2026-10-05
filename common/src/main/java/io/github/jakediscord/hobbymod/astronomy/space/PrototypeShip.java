package io.github.jakediscord.hobbymod.astronomy.space;

import io.github.jakediscord.hobbymod.astronomy.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Temporary single-seat craft. Future Sable-built rockets can share the travel service and gates. */
public final class PrototypeShip extends Entity {
    private static final EntityDataAccessor<Integer> DESTINATION=SynchedEntityData.defineId(PrototypeShip.class,EntityDataSerializers.INT);
    public UUID owner;
    private BlockPos home;
    private int forward,strafe,vertical,lerpSteps;
    private float commandedYaw,commandedPitch;
    private long lastInput=Long.MIN_VALUE,transferAfter;
    private double lx,ly,lz;private float lyaw,lpitch;
    public PrototypeShip(EntityType<? extends PrototypeShip> type,Level level){super(type,level);blocksBuilding=true;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(DESTINATION,-1);}
    public int destination(){return entityData.get(DESTINATION);}
    public boolean select(Player player,int id){
        if(!player.isAlive() || player.getVehicle()!=this || !(player instanceof ServerPlayer p) || (id!=-1 && !ObservationAccess.discovered(AstronomyData.get(p.server).journal(p.getUUID()),id)) || (id!=-1 && CelestialBodies.target(id)==null))return false;
        try{if(id==-1)FlightPlanData.get(p.server).cancel(getUUID(),p.getUUID());else SpaceflightNavigation.chart(p,CelestialBodies.target(id).key());}catch(IllegalArgumentException e){return false;}entityData.set(DESTINATION,id);return true;
    }
    public boolean hasHome(){return home!=null;}
    public void setHome(BlockPos pos){home=pos.immutable();}
    public BlockPos home(){return home;}
    public void cooldown(long tick){transferAfter=tick+100;}
    public boolean readyToTransfer(){return level().getGameTime()>=transferAfter;}
    public void controls(ServerPlayer p,float yaw,float pitch,int f,int s,int v){
        if(!p.isAlive() || p.getVehicle()!=this || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(f)>1 || Math.abs(s)>1 || Math.abs(v)>1)return;
        long now=level().getGameTime();if(lastInput!=Long.MIN_VALUE && now>=lastInput && now-lastInput<2)return;
        lastInput=now;commandedYaw=net.minecraft.util.Mth.wrapDegrees(yaw);commandedPitch=Math.clamp(pitch,-85,85);forward=f;strafe=s;vertical=v;
    }
    @Override public void tick(){
        super.tick();fallDistance=0;
        if(level().isClientSide){if(lerpSteps>0){setPos(getX()+(lx-getX())/lerpSteps,getY()+(ly-getY())/lerpSteps,getZ()+(lz-getZ())/lerpSteps);setYRot(getYRot()+net.minecraft.util.Mth.wrapDegrees(lyaw-getYRot())/lerpSteps);setXRot(getXRot()+(lpitch-getXRot())/lerpSteps);lerpSteps--;}return;}
        boolean transfer=level().dimension().location().toString().equals(SolarMap.TRANSFER);
        var pilot=getControllingPassenger();var v=getDeltaMovement();
        if(pilot instanceof ServerPlayer p && p.isAlive() && lastInput!=Long.MIN_VALUE && level().getGameTime()-lastInput<20){
            boolean landed=!level().noCollision(this,getBoundingBox().move(0,-.15,0));setYRot(commandedYaw);setXRot(landed?0:commandedPitch);
            var next=FlightDynamics.advance(new SkyCatalog.Vector(v.x,v.y,v.z),commandedYaw,commandedPitch,forward,strafe,vertical,transfer);setDeltaMovement(next.x(),next.y(),next.z());if(!transfer && landed && forward==0 && vertical<=0)setDeltaMovement(getDeltaMovement().add(0,-.04,0));
            if(FlightTravel.advance(this,p,vertical))return;
        }else setDeltaMovement(v.scale(.8).add(0,SolarMap.voidSpace(level().dimension().location().toString())?0:-.04,0));
        hasImpulse=true;move(MoverType.SELF,getDeltaMovement());if(horizontalCollision || verticalCollision)setDeltaMovement(getDeltaMovement().scale(.25));
        if(getY()<level().getMinBuildHeight()+4 && pilot instanceof ServerPlayer p)FlightTravel.recover(this,p);
        if(level().getGameTime()%8==0 && getDeltaMovement().lengthSqr()>.06){((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,getX(),getY()+.25,getZ(),2,.2,.05,.2,.02);}
    }
    @Override public LivingEntity getControllingPassenger(){return getFirstPassenger() instanceof LivingEntity p?p:null;}
    @Override protected boolean canAddPassenger(Entity passenger){return getPassengers().isEmpty() && passenger instanceof Player;}
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){return position().add(0,.15,0);}
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
        double angle=Math.toRadians(getYRot());for(int side:new int[]{1,-1}){var candidate=position().add(Math.cos(angle)*side*2.7,.2,Math.sin(angle)*side*2.7);
            if(level().noCollision(passenger,passenger.getDimensions(passenger.getPose()).makeBoundingBox(candidate)))return candidate;
        }return super.getDismountLocationForPassenger(passenger);
    }
    @Override public InteractionResult interact(Player p,InteractionHand h){
        if(isRemoved())return InteractionResult.FAIL;
        if(owner!=null && !owner.equals(p.getUUID())){if(!level().isClientSide)p.displayClientMessage(net.minecraft.network.chat.Component.literal("This ship belongs to another pilot"),true);return InteractionResult.FAIL;}
        if(!level().isClientSide){
            if(p.isShiftKeyDown() && !isVehicle() && onGround() && getDeltaMovement().lengthSqr()<.01){if(p instanceof ServerPlayer pilot)FlightPlanData.get(pilot.server).cancel(getUUID(),p.getUUID());var item=ShipItem.packed(this);if(!p.getInventory().add(item))p.drop(item,false);discard();}
            else if(!isVehicle()){owner=p.getUUID();if(destination()!=-1 && !ObservationAccess.discovered(AstronomyData.get(((ServerPlayer)p).server).journal(p.getUUID()),destination()))entityData.set(DESTINATION,-1);p.startRiding(this);FlightNetworking.open((ServerPlayer)p);}
        }return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean isPickable(){return true;}
    @Override public boolean canBeCollidedWith(){return !isRemoved();}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isControlledByLocalInstance(){return false;}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){lx=x;ly=y;lz=z;lyaw=yaw;lpitch=pitch;lerpSteps=Math.max(1,steps);}
    public void writePortable(CompoundTag t){t.putUUID("Ship",getUUID());if(home!=null)t.putLong("Home",home.asLong());if(owner!=null)t.putUUID("Owner",owner);t.putInt("Destination",destination());}
    public void readPortable(CompoundTag t){if(t.hasUUID("Ship"))setUUID(t.getUUID("Ship"));home=t.contains("Home")?BlockPos.of(t.getLong("Home")):null;owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;int id=t.getInt("Destination");entityData.set(DESTINATION,id==-1 || CelestialBodies.target(id)!=null?id:-1);}
    @Override protected void addAdditionalSaveData(CompoundTag t){writePortable(t);}
    @Override protected void readAdditionalSaveData(CompoundTag t){readPortable(t);lastInput=Long.MIN_VALUE;forward=strafe=vertical=0;setDeltaMovement(Vec3.ZERO);cooldown(level().getGameTime());}
}
