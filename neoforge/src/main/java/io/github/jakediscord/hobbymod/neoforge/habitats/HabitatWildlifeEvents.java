package io.github.jakediscord.hobbymod.neoforge.habitats;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.habitats.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.*;

@EventBusSubscriber(modid=HobbyMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class HabitatWildlifeEvents {
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){event.put(HabitatWildlife.CRITTER.get(),WildCritter.attributes().build());event.put(HabitatWildlife.FISH.get(),AbstractFish.createAttributes().build());}
    @SubscribeEvent public static void spawns(RegisterSpawnPlacementsEvent event){
        event.register(HabitatWildlife.CRITTER.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Mob::checkMobSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HabitatWildlife.FISH.get(),SpawnPlacementTypes.IN_WATER,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WaterAnimal::checkSurfaceWaterAnimalSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
