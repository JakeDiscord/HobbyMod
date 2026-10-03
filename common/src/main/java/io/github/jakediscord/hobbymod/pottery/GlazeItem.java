package io.github.jakediscord.hobbymod.pottery;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
public final class GlazeItem extends Item {
    public final DyeColor color;
    public GlazeItem(DyeColor color){super(new Properties());this.color=color;}
}
