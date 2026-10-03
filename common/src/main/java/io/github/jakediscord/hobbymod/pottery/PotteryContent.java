package io.github.jakediscord.hobbymod.pottery;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import java.util.EnumMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class PotteryContent {
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.MENU);
    public static final RegistrySupplier<net.minecraft.world.inventory.MenuType<KilnMenu>> KILN_MENU=MENUS.register("pottery_kiln",()->new net.minecraft.world.inventory.MenuType<>(KilnMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<PotteryBlock> WHEEL=BLOCKS.register("pottery_wheel",()->new PotteryBlock(Block.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion(),true));
    public static final RegistrySupplier<PotteryBlock> POT=BLOCKS.register("thrown_pot",()->new PotteryBlock(Block.Properties.of().strength(.6F).sound(SoundType.DECORATED_POT).noOcclusion(),false));
    public static final RegistrySupplier<KilnBlock> KILN=BLOCKS.register("pottery_kiln",()->new KilnBlock(Block.Properties.of().strength(3).sound(SoundType.STONE).lightLevel(s->s.getValue(KilnBlock.LIT)?10:0)));
    public static final RegistrySupplier<BlockEntityType<PotteryBlockEntity>> PIECE=ENTITIES.register("pottery",()->BlockEntityType.Builder.of(PotteryBlockEntity::new,WHEEL.get(),POT.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<KilnBlockEntity>> KILN_ENTITY=ENTITIES.register("pottery_kiln",()->BlockEntityType.Builder.of(KilnBlockEntity::new,KILN.get()).build(null));
    public static final RegistrySupplier<Item> WHEEL_ITEM=ITEMS.register("pottery_wheel",()->new BlockItem(WHEEL.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> POT_ITEM=ITEMS.register("thrown_pot",PotteryPotItem::new);
    public static final RegistrySupplier<Item> KILN_ITEM=ITEMS.register("pottery_kiln",()->new BlockItem(KILN.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> CLAY=ITEMS.register("prepared_clay",()->new Item(new Item.Properties()));
    public static final RegistrySupplier<PotteryToolItem> RIB=ITEMS.register("pottery_rib",()->new PotteryToolItem(PotteryToolItem.Tool.RIB));
    public static final RegistrySupplier<PotteryToolItem> SPONGE=ITEMS.register("pottery_sponge",()->new PotteryToolItem(PotteryToolItem.Tool.SPONGE));
    public static final RegistrySupplier<PotteryToolItem> LOOP=ITEMS.register("pottery_loop",()->new PotteryToolItem(PotteryToolItem.Tool.LOOP));
    public static final RegistrySupplier<PotteryToolItem> WIRE=ITEMS.register("pottery_wire",()->new PotteryToolItem(PotteryToolItem.Tool.WIRE));
    public static final EnumMap<DyeColor,RegistrySupplier<GlazeItem>> GLAZES=new EnumMap<>(DyeColor.class);
    static {for(var color:DyeColor.values())GLAZES.put(color,ITEMS.register(color.getName()+"_pottery_glaze",()->new GlazeItem(color)));}
    public static final RegistrySupplier<CreativeModeTab> TAB=TABS.register("pottery",()->CreativeTabRegistry.create(b->b.title(Component.translatable("itemGroup.hobbymod.pottery")).icon(()->new ItemStack(WHEEL_ITEM.get())).displayItems((p,o)->{
        o.accept(WHEEL_ITEM.get());o.accept(CLAY.get());o.accept(RIB.get());o.accept(SPONGE.get());o.accept(LOOP.get());o.accept(WIRE.get());o.accept(KILN_ITEM.get());
        for(var color:DyeColor.values())o.accept(GLAZES.get(color).get());
    })));
    public static void register(){MENUS.register();BLOCKS.register();ITEMS.register();ENTITIES.register();TABS.register();}
    private PotteryContent(){}
}
