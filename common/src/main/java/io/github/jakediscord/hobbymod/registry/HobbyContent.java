package io.github.jakediscord.hobbymod.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.sculpting.ChiselItem;
import io.github.jakediscord.hobbymod.sculpting.MarbleStatueBlock;
import io.github.jakediscord.hobbymod.sculpting.CarvingTool;
import io.github.jakediscord.hobbymod.sculpting.SculptureBlock;
import io.github.jakediscord.hobbymod.sculpting.SculptureBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class HobbyContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(HobbyMod.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(HobbyMod.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(HobbyMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<Block> MARBLE = BLOCKS.register("marble", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ)
                    .strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<MarbleStatueBlock> MARBLE_STATUE = BLOCKS.register("marble_statue", () ->
            new MarbleStatueBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ)
                    .strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(HobbyMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<SculptureBlock> SCULPTURE = BLOCKS.register("marble_sculpture", () ->
            new SculptureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5F, 6.0F)
                    .sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().isSuffocating((s,l,p) -> false).isViewBlocking((s,l,p) -> false)));
    public static final RegistrySupplier<BlockEntityType<SculptureBlockEntity>> SCULPTURE_ENTITY = ENTITIES.register("marble_sculpture", () ->
            BlockEntityType.Builder.of(SculptureBlockEntity::new, SCULPTURE.get()).build(null));
    public static final RegistrySupplier<Item> SCULPTURE_ITEM = ITEMS.register("marble_sculpture", () -> new net.minecraft.world.item.BlockItem(SCULPTURE.get(), new Item.Properties()));
    public static final RegistrySupplier<ChiselItem> POINT_CHISEL = ITEMS.register("point_chisel", () -> new ChiselItem(new Item.Properties().durability(192), CarvingTool.POINT));
    public static final RegistrySupplier<ChiselItem> MALLET = ITEMS.register("roughing_mallet", () -> new ChiselItem(new Item.Properties().durability(256), CarvingTool.ROUGH));
    public static final RegistrySupplier<ChiselItem> RASP = ITEMS.register("polishing_rasp", () -> new ChiselItem(new Item.Properties().durability(256), CarvingTool.POLISH));
    public static final RegistrySupplier<Item> BLUEPRINT=ITEMS.register("marble_blueprint",io.github.jakediscord.hobbymod.sculpting.MarbleBlueprintItem::new);
    public static final RegistrySupplier<Item> MARBLE_ITEM = ITEMS.register("marble", () -> new BlockItem(MARBLE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> MARBLE_STATUE_ITEM = ITEMS.register("marble_statue", () -> new BlockItem(MARBLE_STATUE.get(), new Item.Properties()));
    public static final RegistrySupplier<ChiselItem> CHISEL = ITEMS.register("chisel", () -> new ChiselItem(new Item.Properties().durability(128)));
    public static final RegistrySupplier<CreativeModeTab> HOBBIES_TAB = TABS.register("hobbies", () ->
            CreativeTabRegistry.create(builder -> builder.title(Component.translatable("itemGroup.hobbymod.hobbies"))
                    .icon(() -> new ItemStack(SCULPTURE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(MARBLE_ITEM.get());
                        output.accept(CHISEL.get());
                        output.accept(POINT_CHISEL.get());
                        output.accept(MALLET.get());
                        output.accept(RASP.get());
                        output.accept(BLUEPRINT.get());
                        output.accept(SCULPTURE_ITEM.get());
                    })));

    private HobbyContent() {}

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        ENTITIES.register();
        TABS.register();
    }
}
