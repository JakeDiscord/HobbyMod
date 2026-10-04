package io.github.jakediscord.hobbymod.habitats;

import io.github.jakediscord.hobbymod.aquarium.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;

/** Vanilla schooling/swimming AI and textures with a persistent aquarium phenotype. */
public final class WildAquariumFish extends TropicalFish {
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(WildAquariumFish.class,EntityDataSerializers.INT),GENES=SynchedEntityData.defineId(WildAquariumFish.class,EntityDataSerializers.INT);
    public WildAquariumFish(EntityType<? extends WildAquariumFish> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(KIND,0);b.define(GENES,0);}
    public AquariumData.Fish resident(){var f=new AquariumData.Fish(getUUID(),AquariumData.Species.values()[Math.clamp(entityData.get(KIND),0,AquariumData.Species.values().length-1)]);int genes=entityData.get(GENES);f.colorA=genes&15;f.colorB=(genes>>4)&15;f.formA=(genes>>8)&3;f.formB=(genes>>10)&3;f.female=(genes&4096)!=0;f.age=12;f.health=Math.clamp((int)(getHealth()/getMaxHealth()*100),0,100);return f;}
    public void setResident(AquariumData.Fish f){entityData.set(KIND,f.species.ordinal());entityData.set(GENES,f.colorA|f.colorB<<4|f.formA<<8|f.formB<<10|(f.female?4096:0));var tag=new CompoundTag();super.addAdditionalSaveData(tag);tag.putInt("Variant",AquariumFishAppearance.variant(f));super.readAdditionalSaveData(tag);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){var result=super.finalizeSpawn(level,difficulty,reason,group);var data=new AquariumData();var species=speciesFor(level.getBiome(blockPosition()));var f=data.newFish(species.get(random.nextInt(species.size())));setResident(f);return result;}
    public static java.util.List<AquariumData.Species> speciesFor(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome){
        boolean warm=biome.value().getBaseTemperature()>.8f || biome.is(net.minecraft.world.level.biome.Biomes.WARM_OCEAN) || biome.is(net.minecraft.world.level.biome.Biomes.LUKEWARM_OCEAN) || biome.is(net.minecraft.world.level.biome.Biomes.DEEP_LUKEWARM_OCEAN);
        return java.util.Arrays.stream(AquariumData.Species.values()).filter(s->s.warm==warm).toList();
    }
    @Override public ItemStack getBucketItemStack(){return AquariumFishItem.capture(resident());}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.put("HabitatFish",AquariumNbt.fish(resident()));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("HabitatFish")){var fish=AquariumNbt.fish(tag.getCompound("HabitatFish"));if(fish!=null)setResident(fish);}}
    @Override protected Component getTypeName(){return Component.literal(resident().species.label);}
}
