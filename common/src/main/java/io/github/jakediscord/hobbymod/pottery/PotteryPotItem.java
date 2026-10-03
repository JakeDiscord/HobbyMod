package io.github.jakediscord.hobbymod.pottery;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

public final class PotteryPotItem extends BlockItem {
    public PotteryPotItem(){super(PotteryContent.POT.get(),new Properties().stacksTo(1));}
    public static PotteryPiece piece(ItemStack stack) {
        var data=stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data==null?new PotteryPiece():PotteryPiece.read(data.copyTag().getCompound("piece"));
    }
    public static ItemStack create(PotteryPiece piece) {
        var stack=new ItemStack(PotteryContent.POT_ITEM.get());var tag=new CompoundTag();
        tag.putString("id","hobbymod:pottery");tag.put("piece",piece.save());
        stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));return stack;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);var p=piece(stack);
        tooltip.add(Component.literal(p.description()).withStyle(ChatFormatting.GRAY));
        if(p.stage==PotteryPiece.Stage.GLAZED || p.stage==PotteryPiece.Stage.FINISHED)
            tooltip.add(Component.translatable("color.minecraft."+p.glaze.getName()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
