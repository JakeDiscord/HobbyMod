package io.github.jakediscord.hobbymod.winery;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;

public final class WineryContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.MENU);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final TagKey<Item> GRAPES=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","fruits/grapes"));
    public static final TagKey<Item> WHITE_GRAPES=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","fruits/white_grapes"));
    public static final RegistrySupplier<GrapeTrellisBlock> TRELLIS=BLOCKS.register("grape_trellis",()->new GrapeTrellisBlock(Block.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion().randomTicks()));
    public static final RegistrySupplier<WineryBlock> PRESS=BLOCKS.register("grape_press",()->new WineryBlock(Block.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion(),WineryBlock.Machine.PRESS));
    public static final RegistrySupplier<WineryBlock> TUB=BLOCKS.register("grape_treading_tub",()->new WineryBlock(Block.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion(),WineryBlock.Machine.TUB));
    public static final RegistrySupplier<Item> TUB_ITEM=ITEMS.register("grape_treading_tub",()->new BlockItem(TUB.get(),new Item.Properties()));
    public static final RegistrySupplier<WineryBlock> BARREL=BLOCKS.register("fermentation_barrel",()->new WineryBlock(Block.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion(),WineryBlock.Machine.BARREL));
    public static final RegistrySupplier<WineRackBlock> RACK=BLOCKS.register("wine_rack",()->new WineRackBlock(Block.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistrySupplier<BlockEntityType<WineryBlockEntity>> MACHINE=ENTITIES.register("winery_machine",()->BlockEntityType.Builder.of(WineryBlockEntity::new,PRESS.get(),BARREL.get(),TUB.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<WineRackBlockEntity>> RACK_ENTITY=ENTITIES.register("wine_rack",()->BlockEntityType.Builder.of(WineRackBlockEntity::new,RACK.get()).build(null));
    public static final RegistrySupplier<MenuType<WineryMenu>> PRESS_MENU=MENUS.register("grape_press",()->new MenuType<>((id,inv)->new WineryMenu(id,inv,WineryBlock.Machine.PRESS),FeatureFlags.DEFAULT_FLAGS));
    public static final RegistrySupplier<MenuType<WineryMenu>> BARREL_MENU=MENUS.register("fermentation_barrel",()->new MenuType<>((id,inv)->new WineryMenu(id,inv,WineryBlock.Machine.BARREL),FeatureFlags.DEFAULT_FLAGS));
    public static final RegistrySupplier<Item> TRELLIS_ITEM=ITEMS.register("grape_trellis",()->new BlockItem(TRELLIS.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> PRESS_ITEM=ITEMS.register("grape_press",()->new BlockItem(PRESS.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> BARREL_ITEM=ITEMS.register("fermentation_barrel",()->new BlockItem(BARREL.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> RACK_ITEM=ITEMS.register("wine_rack",()->new BlockItem(RACK.get(),new Item.Properties()));
    public static final RegistrySupplier<GrapeItem> RED=ITEMS.register("red_grapes",()->new GrapeItem(true));
    public static final RegistrySupplier<GrapeItem> WHITE=ITEMS.register("white_grapes",()->new GrapeItem(false));
    public static final RegistrySupplier<Item> RED_CUTTING=ITEMS.register("red_grape_cutting",()->new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WHITE_CUTTING=ITEMS.register("white_grape_cutting",()->new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> YEAST=ITEMS.register("wine_yeast",()->new Item(new Item.Properties()));
    public static final RegistrySupplier<MustBucketItem> MUST=ITEMS.register("grape_must",MustBucketItem::new);
    public static final RegistrySupplier<MustBucketItem> CASK=ITEMS.register("portable_wine_cask",MustBucketItem::new);
    public static final RegistrySupplier<WineBottleItem> BOTTLE=ITEMS.register("wine_bottle",WineBottleItem::new);
    public static final RegistrySupplier<CreativeModeTab> TAB=TABS.register("winery",()->CreativeTabRegistry.create(b->b.title(Component.literal("HobbyMod: Winery")).icon(()->new ItemStack(RED.get())).displayItems((p,o)->{
        for(var i:new RegistrySupplier<?>[]{TRELLIS_ITEM,RED_CUTTING,WHITE_CUTTING,RED,WHITE,TUB_ITEM,PRESS_ITEM,YEAST,BARREL_ITEM,RACK_ITEM,BOTTLE})o.accept(new ItemStack((Item)i.get()));o.accept(Items.BUCKET);o.accept(Items.GLASS_BOTTLE);
    })));
    public static void register(){BLOCKS.register();ITEMS.register();ENTITIES.register();MENUS.register();TABS.register();}
    private WineryContent(){}
}
