package io.github.jakediscord.hobbymod.astronomy;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class AstronomyContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<TelescopeBlock> TELESCOPE=BLOCKS.register("telescope",()->new TelescopeBlock(Block.Properties.of().strength(2).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<TelescopeBlock> OBSERVATORY=BLOCKS.register("observatory_telescope",()->new TelescopeBlock(Block.Properties.of().strength(3).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<BlockEntityType<TelescopeBlockEntity>> ENTITY=ENTITIES.register("telescope",()->BlockEntityType.Builder.of(TelescopeBlockEntity::new,TELESCOPE.get(),OBSERVATORY.get()).build(null));
    public static final RegistrySupplier<Item> SMALL_ITEM=ITEMS.register("telescope",()->new BlockItem(TELESCOPE.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> LARGE_ITEM=ITEMS.register("observatory_telescope",()->new BlockItem(OBSERVATORY.get(),new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL=ITEMS.register("astronomy_journal",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,InteractionHand h){if(p instanceof net.minecraft.server.level.ServerPlayer s)AstronomyNetworking.openJournal(s);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistrySupplier<CreativeModeTab> TAB=TABS.register("astronomy",()->CreativeTabRegistry.create(b->b.title(Component.literal("HobbyMod: Astronomy")).icon(()->new ItemStack(SMALL_ITEM.get())).displayItems((p,o)->{o.accept(JOURNAL.get());o.accept(Items.SPYGLASS);o.accept(SMALL_ITEM.get());o.accept(LARGE_ITEM.get());})));
    public static void register(){BLOCKS.register();ITEMS.register();ENTITIES.register();TABS.register();}
    private AstronomyContent(){}
}
