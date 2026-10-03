package io.github.jakediscord.hobbymod.painting;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.*;
public final class PaintingContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> HANGING_ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ENTITY_TYPE);
    public static final RegistrySupplier<net.minecraft.world.entity.EntityType<PaintedCanvasEntity>> HANGING=HANGING_ENTITIES.register("painted_canvas",()->net.minecraft.world.entity.EntityType.Builder.<PaintedCanvasEntity>of(PaintedCanvasEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).build("hobbymod:painted_canvas"));
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<PaintingBlock> EASEL=BLOCKS.register("painting_easel",()->new PaintingBlock(Block.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion(),true));
    public static final RegistrySupplier<Block> EASEL_TOP=BLOCKS.register("painting_easel_top",()->new PaintingEaselTopBlock(Block.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistrySupplier<PaintingBlock> DISPLAY=BLOCKS.register("painted_canvas",()->new PaintingBlock(Block.Properties.of().strength(.5F).sound(SoundType.WOOD).noOcclusion(),false));
    public static final RegistrySupplier<BlockEntityType<PaintingBlockEntity>> ENTITY=ENTITIES.register("painting",()->BlockEntityType.Builder.of(PaintingBlockEntity::new,EASEL.get(),DISPLAY.get()).build(null));
    public static final RegistrySupplier<Item> EASEL_ITEM=ITEMS.register("painting_easel",()->new BlockItem(EASEL.get(),new Item.Properties()){@Override public Component getName(ItemStack stack){return Component.literal("Painting Easel");}});
    public static final RegistrySupplier<Item> BRUSH=ITEMS.register("paint_brush",()->named("Paintbrush"));
    public static final RegistrySupplier<Item> PALETTE=ITEMS.register("paint_palette",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public Component getName(ItemStack stack){return Component.literal("Painter's Palette");}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> out,TooltipFlag f){int sum=Arrays.stream(PaintPalette.amounts(stack)).sum();out.add(Component.literal(sum+" paint · Load dyes at an easel"));}
    });
    public static final Map<PaintingData.Shape,RegistrySupplier<CanvasItem>> CANVASES=new EnumMap<>(PaintingData.Shape.class);
    static{for(var shape:PaintingData.Shape.values())CANVASES.put(shape,ITEMS.register(shape.name().toLowerCase(Locale.ROOT)+"_canvas",()->new CanvasItem(shape)));}
    private static Item named(String title){return new Item(new Item.Properties().stacksTo(1)){@Override public Component getName(ItemStack s){return Component.literal(title);}};}
    public static final RegistrySupplier<CreativeModeTab> TAB=TABS.register("painting",()->CreativeTabRegistry.create(b->b.title(Component.literal("HobbyMod: Painting")).icon(()->new ItemStack(PALETTE.get())).displayItems((p,o)->{o.accept(EASEL_ITEM.get());o.accept(PALETTE.get());o.accept(BRUSH.get());CANVASES.values().forEach(v->o.accept(v.get()));})));
    public static void register(){BLOCKS.register();ITEMS.register();ENTITIES.register();HANGING_ENTITIES.register();TABS.register();}
    private PaintingContent(){}
}
