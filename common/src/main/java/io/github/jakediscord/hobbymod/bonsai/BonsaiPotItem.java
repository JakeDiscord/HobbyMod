package io.github.jakediscord.hobbymod.bonsai;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class BonsaiPotItem extends BlockItem {
    public BonsaiPotItem(){super(BonsaiContent.POT.get(),new Item.Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return Component.literal("Bonsai Pot");}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);
        var saved=stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if(saved!=null){var tag=saved.copyTag();if(!tag.getList("Branches",10).isEmpty())tooltip.add(Component.literal(tag.getString("Species")+" | Age "+tag.getInt("Age")+" | Health "+tag.getInt("Health")+"%").withStyle(ChatFormatting.GREEN));}
        tooltip.add(Component.literal("Oak, birch or cherry · Water bucket · Dirt").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shears: leaves, then branch · Copper: bend").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak: reverse bend / prune roots").withStyle(ChatFormatting.GRAY));
    }
}
