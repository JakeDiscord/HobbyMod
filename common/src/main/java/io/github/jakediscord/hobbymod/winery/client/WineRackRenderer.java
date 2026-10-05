package io.github.jakediscord.hobbymod.winery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.winery.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;

public final class WineRackRenderer implements BlockEntityRenderer<WineRackBlockEntity> {
    public WineRackRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(WineRackBlockEntity b,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){if(b.getLevel()==null)return;poses.pushPose();poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotationDegrees(-b.getBlockState().getValue(WineRackBlock.FACING).toYRot()+180));poses.translate(-.5,0,-.5);
        for(int i=0;i<4;i++){var bottle=b.bottles.get(i);if(bottle.isEmpty())continue;poses.pushPose();poses.translate(i%2==0?.28:.72,i<2?4.5/16D:12/16D,.48);poses.mulPose(Axis.XP.rotationDegrees(-90));poses.scale(.65F,.65F,.65F);Minecraft.getInstance().getItemRenderer().renderStatic(bottle,ItemDisplayContext.FIXED,light,overlay,poses,buffers,b.getLevel(),i);poses.popPose();}poses.popPose();
    }
    @Override public int getViewDistance(){return 32;}
}
