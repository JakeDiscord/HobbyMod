package io.github.jakediscord.hobbymod.painting;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
public final class CanvasItem extends BlockItem {
    public final PaintingData.Shape shape;
    public CanvasItem(PaintingData.Shape shape){super(PaintingContent.DISPLAY.get(),new Properties().stacksTo(1));this.shape=shape;}
    public static PaintingData read(ItemStack s){var data=s.get(DataComponents.CUSTOM_DATA);var shape=s.getItem() instanceof CanvasItem c?c.shape:PaintingData.Shape.SQUARE;return data==null?new PaintingData(shape,32):PaintingNbt.read(data.copyTag().getCompound("Painting"),shape);}
    public static ItemStack create(PaintingData d){var stack=new ItemStack(PaintingContent.CANVASES.get(d.shape).get());var tag=new net.minecraft.nbt.CompoundTag();tag.put("Painting",PaintingNbt.save(d));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
    @Override public Component getName(ItemStack s){var t=s.get(DataComponents.CUSTOM_DATA);String title=t==null?"":t.copyTag().getCompound("Painting").getString("Title");return Component.literal(title.isBlank()?shape.label+" Canvas":PaintingNbt.clean(title,48));}
    @Override public net.minecraft.world.InteractionResult place(BlockPlaceContext c){
        var face=c.getClickedFace();var player=c.getPlayer();var anchor=c.getClickedPos();
        if(!face.getAxis().isHorizontal() || player==null || !player.mayUseItemAt(anchor,face,c.getItemInHand()) || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(player,anchor))return net.minecraft.world.InteractionResult.FAIL;
        var canvas=new PaintedCanvasEntity(c.getLevel(),anchor,face,read(c.getItemInHand()));
        if(!canvas.survives())return net.minecraft.world.InteractionResult.FAIL;
        if(!c.getLevel().isClientSide){canvas.playPlacementSound();c.getLevel().addFreshEntity(canvas);if(!player.getAbilities().instabuild)c.getItemInHand().shrink(1);}
        return net.minecraft.world.InteractionResult.sidedSuccess(c.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> out,TooltipFlag flag){var t=s.get(DataComponents.CUSTOM_DATA);if(t==null){out.add(Component.literal("Mount on an easel; choose your resolution."));return;}var d=read(s);out.add(Component.literal(d.shape.label+" · "+d.width()+" × "+d.height()+" pixels"));if(!d.author.isBlank())out.add(Component.literal("Painted by "+d.author));out.add(Component.literal("Place against a wall to display."));}
}
