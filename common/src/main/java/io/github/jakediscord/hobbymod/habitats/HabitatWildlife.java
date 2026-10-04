package io.github.jakediscord.hobbymod.habitats;

import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

/** Shared wildlife; biome injection and spawn placement hooks belong to the loader module. */
public final class HabitatWildlife {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    public static final RegistrySupplier<EntityType<WildCritter>> CRITTER=ENTITIES.register("wild_critter",()->EntityType.Builder.of(WildCritter::new,MobCategory.CREATURE).sized(.45f,.30f).clientTrackingRange(6).build("hobbymod:wild_critter"));
    public static final RegistrySupplier<EntityType<WildAquariumFish>> FISH=ENTITIES.register("wild_aquarium_fish",()->EntityType.Builder.of(WildAquariumFish::new,MobCategory.WATER_AMBIENT).sized(.45f,.3f).clientTrackingRange(6).build("hobbymod:wild_aquarium_fish"));
    public static final RegistrySupplier<Item> NET=ITEMS.register("habitat_net",HabitatNet::new);
    public static void register(){ENTITIES.register();ITEMS.register();}
    private HabitatWildlife(){}
}
