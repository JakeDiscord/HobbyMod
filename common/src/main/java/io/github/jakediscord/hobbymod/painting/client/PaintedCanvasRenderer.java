package io.github.jakediscord.hobbymod.painting.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.painting.PaintedCanvasEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class PaintedCanvasRenderer extends EntityRenderer<PaintedCanvasEntity> {
    public PaintedCanvasRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public ResourceLocation getTextureLocation(PaintedCanvasEntity e){return PaintingTextures.texture(e.painting());}
    @Override public void render(PaintedCanvasEntity e,float yaw,float delta,PoseStack p,MultiBufferSource b,int light){p.pushPose();p.mulPose(Axis.YP.rotationDegrees(e.getDirection().toYRot()+180));PaintingRenderer.canvas(e.painting(),p,b,e.canvasWidth()-.05,e.canvasHeight()-.05,0,0,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);p.popPose();super.render(e,yaw,delta,p,b,light);}
}
