package io.github.jakediscord.hobbymod.winery;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

public final class MustBucketItem extends Item {
    public MustBucketItem(){super(new Item.Properties().stacksTo(1));}
    public static ItemStack create(WineBatch b){var s=new ItemStack(WineryContent.MUST.get());var tag=new CompoundTag();tag.putByteArray("WineBatch",b.encode());s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return s;}
    public static ItemStack portable(WineBatch b){var s=new ItemStack(WineryContent.CASK.get());var tag=new CompoundTag();tag.putByteArray("WineBatch",b.encode());s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return s;}
    public static WineBatch batch(ItemStack s){if(!s.is(WineryContent.MUST.get()) && !s.is(WineryContent.CASK.get()))return null;var data=s.get(DataComponents.CUSTOM_DATA);if(data==null)return null;try{return WineBatch.decode(data.copyTag().getByteArray("WineBatch"));}catch(java.io.IOException e){return null;}}
    @Override public Component getName(ItemStack s){var b=batch(s);return Component.literal(b==null?"Empty must bucket":b.stage==WineBatch.Stage.MUST?b.kind().label()+" grape must":"Portable "+b.kind().label().toLowerCase(java.util.Locale.ROOT)+" wine batch");}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){var b=batch(s);if(b==null){return;}lines.add(Component.literal("Vintage "+b.vintage+" · "+b.remaining+" bottles"));lines.add(Component.literal("Sugar "+b.sugar+" · Acidity "+b.acidity));lines.add(Component.literal(b.stage==WineBatch.Stage.MUST?"Must":b.stage.name().replace('_',' ')+" · quality "+b.quality()));}
}
