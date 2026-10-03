package io.github.jakediscord.hobbymod.pottery;
import net.minecraft.world.item.Item;
public final class PotteryToolItem extends Item {
    public enum Tool { HAND, RIB, SPONGE, LOOP, WIRE }
    public final Tool tool;
    public PotteryToolItem(Tool tool){super(new Properties().durability(256));this.tool=tool;}
}
