package io.github.jakediscord.hobbymod.neoforge.painting;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jakediscord.hobbymod.painting.*;
import io.github.jakediscord.hobbymod.painting.client.PaintingRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import java.util.*;
/** The actual raster is visible in inventory, in hand and in item frames. */
public final class CanvasItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final PaintingRenderer renderer=new PaintingRenderer(null);
    private final Map<PaintingData.Shape,PaintingData> blanks=new EnumMap<>(PaintingData.Shape.class);
    private final Map<CustomData,PaintingData> carried=new WeakHashMap<>();
    public CanvasItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        if(!(stack.getItem() instanceof CanvasItem item))return;var saved=stack.get(DataComponents.CUSTOM_DATA);var data=saved==null?blanks.computeIfAbsent(item.shape,s->new PaintingData(s,32)):carried.computeIfAbsent(saved,t->CanvasItem.read(stack));
        var canvas=new PaintingBlockEntity(BlockPos.ZERO,PaintingContent.DISPLAY.get().defaultBlockState());canvas.painting=data;
        poses.pushPose();poses.translate(0,0,-.43);renderer.render(canvas,0,poses,buffers,light,overlay);poses.popPose();
    }
}
