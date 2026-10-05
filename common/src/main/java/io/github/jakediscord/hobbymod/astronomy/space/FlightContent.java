package io.github.jakediscord.hobbymod.astronomy.space;

import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

public final class FlightContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    public static final RegistrySupplier<EntityType<PrototypeShip>> SHIP=ENTITIES.register("prototype_ship",()->EntityType.Builder.of(PrototypeShip::new,MobCategory.MISC).sized(3.2f,2.0f).clientTrackingRange(12).updateInterval(2).fireImmune().build("hobbymod:prototype_ship"));
    public static final RegistrySupplier<Item> ITEM=ITEMS.register("prototype_ship",ShipItem::new);
    public static void register(){ENTITIES.register();ITEMS.register();FlightNetworking.register();}
    private FlightContent(){}
}
