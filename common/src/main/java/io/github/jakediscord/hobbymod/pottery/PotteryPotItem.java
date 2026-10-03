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
    @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context){
        var result=super.place(context);
        if(result.consumesAction() && !context.getLevel().isClientSide && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PotteryBlockEntity pot && !pot.wheel()){
            double edge=.5-pot.piece.shape.maxRadius();var point=context.getClickLocation();var pos=context.getClickedPos();
            pot.offsetX=Math.clamp(point.x-pos.getX()-.5,-edge,edge);pot.offsetZ=Math.clamp(point.z-pos.getZ()-.5,-edge,edge);
            pot.offsetY=Math.clamp(point.y-pos.getY(),-.5,0);pot.changed();
        }
        return result;
    }
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
