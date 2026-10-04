package io.github.jakediscord.hobbymod.dj;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class DjContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(HobbyMod.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<DjBlock> WORKSTATION=BLOCKS.register("dj_workstation",()->new DjBlock(Block.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistrySupplier<Block> SPEAKER=BLOCKS.register("dj_speaker",()->new DjSpeakerBlock(Block.Properties.of().strength(2).sound(SoundType.WOOD)));
    public static final RegistrySupplier<BlockEntityType<DjBlockEntity>> ENTITY=ENTITIES.register("dj_workstation",()->BlockEntityType.Builder.of(DjBlockEntity::new,WORKSTATION.get()).build(null));
    public static final RegistrySupplier<Item> TABLE=ITEMS.register("dj_workstation",()->namedBlock(WORKSTATION.get(),"DJ Workstation"));
    public static final RegistrySupplier<Item> SPEAKER_ITEM=ITEMS.register("dj_speaker",()->namedBlock(SPEAKER.get(),"DJ Speaker"));
    public static final RegistrySupplier<Item> BLANK=ITEMS.register("blank_music_disc",()->new Item(new Item.Properties()){@Override public Component getName(ItemStack s){return Component.literal("Blank Music Disc");}});
    public static final RegistrySupplier<Item> HEADPHONES=ITEMS.register("dj_headphones",()->new Item(new Item.Properties().stacksTo(1)){@Override public Component getName(ItemStack s){return Component.literal("Studio Headphones");}@Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> text,TooltipFlag f){text.add(Component.literal("Carry to privately monitor either deck in the DJ tab."));}});
    public static final RegistrySupplier<Item> DISC=ITEMS.register("music_project_disc",DjDiscItem::new);
    public static final RegistrySupplier<CreativeModeTab> TAB=TABS.register("dj",()->CreativeTabRegistry.create(b->b.title(Component.literal("HobbyMod: DJ")).icon(()->new ItemStack(TABLE.get())).displayItems((p,o)->{o.accept(TABLE.get());o.accept(SPEAKER_ITEM.get());o.accept(BLANK.get());o.accept(HEADPHONES.get());o.accept(DjDiscItem.create(MusicProject.demo()));})));
    private static Item namedBlock(Block b,String name){return new BlockItem(b,new Item.Properties()){@Override public Component getName(ItemStack s){return Component.literal(name);}};}
    public static void register(){BLOCKS.register();ITEMS.register();ENTITIES.register();TABS.register();}
    private DjContent(){}
}
