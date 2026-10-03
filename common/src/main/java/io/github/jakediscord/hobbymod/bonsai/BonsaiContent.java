package io.github.jakediscord.hobbymod.bonsai;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.jakediscord.hobbymod.HobbyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BonsaiContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<BonsaiPotBlock> POT=BLOCKS.register("bonsai_pot",()->new BonsaiPotBlock(Block.Properties.of().strength(.8F).sound(SoundType.DECORATED_POT).noOcclusion()));
    public static final RegistrySupplier<Item> POT_ITEM=ITEMS.register("bonsai_pot",BonsaiPotItem::new);
    public static final RegistrySupplier<BlockEntityType<BonsaiBlockEntity>> TREE=ENTITIES.register("bonsai",()->BlockEntityType.Builder.of(BonsaiBlockEntity::new,POT.get()).build(null));
    public static final RegistrySupplier<CreativeModeTab> TAB=TABS.register("bonsai",()->CreativeTabRegistry.create(b->b.title(Component.literal("HobbyMod: Bonsai")).icon(()->new ItemStack(POT_ITEM.get())).displayItems((p,o)->o.accept(POT_ITEM.get()))));
    public static void register(){BLOCKS.register();ITEMS.register();ENTITIES.register();TABS.register();}
    private BonsaiContent(){}
}
