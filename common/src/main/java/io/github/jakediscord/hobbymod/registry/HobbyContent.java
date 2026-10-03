package io.github.jakediscord.hobbymod.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.sculpting.ChiselItem;
import io.github.jakediscord.hobbymod.sculpting.MarbleStatueBlock;
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
    public static final RegistrySupplier<Item> MARBLE_ITEM = ITEMS.register("marble", () -> new BlockItem(MARBLE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> MARBLE_STATUE_ITEM = ITEMS.register("marble_statue", () -> new BlockItem(MARBLE_STATUE.get(), new Item.Properties()));
    public static final RegistrySupplier<ChiselItem> CHISEL = ITEMS.register("chisel", () -> new ChiselItem(new Item.Properties().durability(128)));
    public static final RegistrySupplier<CreativeModeTab> HOBBIES_TAB = TABS.register("hobbies", () ->
            CreativeTabRegistry.create(builder -> builder.title(Component.translatable("itemGroup.hobbymod.hobbies"))
                    .icon(() -> new ItemStack(MARBLE_STATUE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(MARBLE_ITEM.get());
                        output.accept(CHISEL.get());
                        output.accept(MARBLE_STATUE_ITEM.get());
                    })));

    private HobbyContent() {}

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        TABS.register();
    }
}
