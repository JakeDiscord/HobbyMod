package io.github.jakediscord.hobbymod.winery;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

public final class GrapeItem extends Item {
    public final boolean red;
    public GrapeItem(boolean red){super(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.2F).build()));this.red=red;}
    public static ItemStack harvest(boolean red,int ripeness,boolean sunlit,int count){var s=new ItemStack(red?WineryContent.RED.get():WineryContent.WHITE.get(),count);var tag=new CompoundTag();tag.putInt("Ripeness",Math.clamp(ripeness,3,6));tag.putBoolean("Sunlit",sunlit);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return s;}
    public static WineBatch.Fruit fruit(ItemStack s){var data=s.get(DataComponents.CUSTOM_DATA);var tag=data==null?new CompoundTag():data.copyTag();boolean white=s.is(WineryContent.WHITE.get()) || s.is(WineryContent.WHITE_GRAPES);return new WineBatch.Fruit(!white,tag.contains("Ripeness")?Math.clamp(tag.getInt("Ripeness"),3,6):4,!tag.contains("Sunlit") || tag.getBoolean("Sunlit"));}
    public static String ripeness(int age){return switch(age){case 3->"Firm / tart";case 4->"Ripe";case 5->"Late harvest / sweet";default->"Overripe";};}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){var fruit=fruit(s);lines.add(Component.literal(ripeness(fruit.ripeness())));if(fruit.sunlit())lines.add(Component.literal("Sun-grown"));lines.add(Component.literal("Press 8 grapes with an empty bucket"));}
}
