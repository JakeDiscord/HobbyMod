package io.github.jakediscord.hobbymod.habitats;

import io.github.jakediscord.hobbymod.terrarium.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;

/** Actual naturally spawned small fauna; closed enclosures still use cheap saved visual residents. */
public final class WildCritter extends PathfinderMob {
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(WildCritter.class,EntityDataSerializers.INT),VARIANT=SynchedEntityData.defineId(WildCritter.class,EntityDataSerializers.INT);
    public WildCritter(EntityType<? extends WildCritter> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(KIND,0);builder.define(VARIANT,0);}
    public TerrariumData.Species species(){return TerrariumData.Species.values()[Math.clamp(entityData.get(KIND),0,TerrariumData.Species.values().length-1)];}
    public TerrariumData.Resident resident(){var r=new TerrariumData.Resident(getUUID(),species(),Math.clamp(entityData.get(VARIANT),0,3));r.vigor=Math.clamp((int)(getHealth()/getMaxHealth()*100),0,100);return r;}
    public void setSpecies(TerrariumData.Species species,int variant){entityData.set(KIND,species.ordinal());entityData.set(VARIANT,Math.clamp(variant,0,3));getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(species==TerrariumData.Species.SNAIL?.065:species.vertebrate()?.15:.10);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,6).add(Attributes.MOVEMENT_SPEED,.10).add(Attributes.FOLLOW_RANGE,12);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,1.3));goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.7,.001F));goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,4));goalSelector.addGoal(5,new RandomLookAroundGoal(this));}
    @Override protected void customServerAiStep(){super.customServerAiStep();if(species()==TerrariumData.Species.TREE_FROG && onGround() && !getNavigation().isDone() && Math.floorMod(tickCount+getUUID().hashCode(),36)==0)getJumpControl().jump();}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){var result=super.finalizeSpawn(level,difficulty,reason,group);int roll=random.nextInt(100);setSpecies(roll<35?TerrariumData.Species.SPRINGTAIL:roll<65?TerrariumData.Species.ISOPOD:roll<80?TerrariumData.Species.SNAIL:roll<90?TerrariumData.Species.TREE_FROG:TerrariumData.Species.GECKO,random.nextInt(4));return result;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putString("HabitatSpecies",species().name());tag.putInt("HabitatVariant",entityData.get(VARIANT));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);try{setSpecies(TerrariumData.Species.valueOf(tag.getString("HabitatSpecies")),tag.getInt("HabitatVariant"));}catch(IllegalArgumentException ignored){}}
    @Override protected Component getTypeName(){return Component.literal(switch(species()){case SPRINGTAIL->"Springtail";case ISOPOD->"Isopod";case SNAIL->"Snail";case TREE_FROG->"Tree Frog";case GECKO->"Gecko";});}
}
