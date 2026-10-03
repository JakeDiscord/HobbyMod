package io.github.jakediscord.hobbymod.aquarium;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class AquariumContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<AquariumControllerBlock> CONTROLLER=BLOCKS.register("aquarium_controller",()->new AquariumControllerBlock(Block.Properties.of().strength(1.5F).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistrySupplier<Block> PART=BLOCKS.register("aquarium_part",()->new AquariumPartBlock(Block.Properties.of().strength(1.5F).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistrySupplier<BlockEntityType<AquariumBlockEntity>> TANK_ENTITY=ENTITIES.register("aquarium",()->BlockEntityType.Builder.of(AquariumBlockEntity::new,CONTROLLER.get()).build(null));
    public static final Map<AquariumData.Size,RegistrySupplier<Item>> KITS=new EnumMap<>(AquariumData.Size.class);
    public static final Map<AquariumData.Species,RegistrySupplier<Item>> FISH=new EnumMap<>(AquariumData.Species.class);
    public static final RegistrySupplier<Item> FOOD=ITEMS.register("fish_food",()->named("Fish Food",new Item.Properties()));
    public static final RegistrySupplier<Item> FILTER=ITEMS.register("aquarium_filter",()->named("Aquarium Filter",new Item.Properties()));
    static{
        for(var size:AquariumData.Size.values())KITS.put(size,ITEMS.register(size.name().toLowerCase(Locale.ROOT)+"_aquarium",()->new AquariumKitItem(size)));
        for(var species:AquariumData.Species.values())FISH.put(species,ITEMS.register(species.name().toLowerCase(Locale.ROOT)+"_fish",()->new AquariumFishItem(species)));
    }
    private static Item named(String name,Item.Properties properties){return new Item(properties){@Override public Component getName(ItemStack stack){return Component.literal(name);}};}
    public static final RegistrySupplier<CreativeModeTab> TAB=TABS.register("aquariums",()->CreativeTabRegistry.create(b->b.title(Component.literal("HobbyMod: Aquariums"))
            .icon(()->new ItemStack(KITS.get(AquariumData.Size.SMALL).get())).displayItems((p,o)->{
                KITS.values().forEach(item->o.accept(item.get()));o.accept(FOOD.get());o.accept(FILTER.get());FISH.values().forEach(item->o.accept(item.get()));
            })));
    public static void register(){BLOCKS.register();ITEMS.register();ENTITIES.register();TABS.register();}
    private AquariumContent(){}
}
