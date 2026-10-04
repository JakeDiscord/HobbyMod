package io.github.jakediscord.hobbymod.dj;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class DjDiscItem extends Item {
    public DjDiscItem(){super(new Item.Properties().stacksTo(1));}
    public static MusicProject read(ItemStack stack){if(!stack.is(DjContent.DISC.get()))return null;var c=stack.get(DataComponents.CUSTOM_DATA);if(c==null)return null;try{return MusicProject.decode(c.copyTag().getByteArray("Music"));}catch(java.io.IOException e){return null;}}
    public static ItemStack create(MusicProject p){var s=new ItemStack(DjContent.DISC.get());var tag=new net.minecraft.nbt.CompoundTag();tag.putByteArray("Music",p.encode());s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return s;}
    @Override public Component getName(ItemStack s){var p=read(s);return Component.literal(p==null?"Music Project Disc":p.title);}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){var p=read(s);text.add(Component.literal(p==null?"Save a set at a DJ workstation":p.bpm+" BPM · "+p.bars+" bars · "+p.author));text.add(Component.literal("Load in a workstation deck; contains editable music."));}
}
