package io.github.jakediscord.hobbymod.astronomy.space.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.astronomy.space.PrototypeShip;
import io.github.jakediscord.hobbymod.aquarium.client.AquariumRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** Compact voxel shuttle: stepped nose, cockpit, swept wings, twin engines and landing skids. */
public final class ShipRenderer extends EntityRenderer<PrototypeShip> {
    private static final ResourceLocation WHITE=ResourceLocation.parse("hobbymod:textures/entity/aquarium_white.png");
    public ShipRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=1.2f;}
    private static void box(VertexConsumer out,PoseStack p,double x,double y,double z,double X,double Y,double Z,int color,int light){AquariumRenderer.box(out,p.last(),null,x,y,z,X,Y,Z,color,light,OverlayTexture.NO_OVERLAY);}
    @Override public ResourceLocation getTextureLocation(PrototypeShip e){return WHITE;}
    @Override public void render(PrototypeShip e,float yaw,float delta,PoseStack p,MultiBufferSource b,int light){
        if(Minecraft.getInstance().player!=null && Minecraft.getInstance().player.getVehicle()==e && Minecraft.getInstance().options.getCameraType().isFirstPerson())return;
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.translate(0,.62,0);p.mulPose(Axis.XP.rotationDegrees(-e.getXRot()));
        var out=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));int hull=0xE0E7E5,trim=0x213A4B,accent=0x31ABB3;
        box(out,p,-.48,-.25,-1.2,.48,.32,1.05,hull,light);
        box(out,p,-.34,-.18,-1.6,.34,.23,-1.2,hull,light);box(out,p,-.22,-.1,-1.85,.22,.12,-1.6,trim,light);
        box(out,p,-.38,.3,-.8,.38,1.36,.4,trim,light);box(out,p,-.34,.35,-.81,.34,1.27,-.805,0x76CCD7,light);
        box(out,p,-.385,.35,-.7,-.38,1.2,.26,0x2B788D,light);box(out,p,.38,.35,-.7,.385,1.2,.26,0x2B788D,light);
        box(out,p,-.486,.18,-1.15,.486,.326,-.85,accent,light);
        for(int side:new int[]{-1,1}){
            p.pushPose();if(side<0)p.scale(-1,1,1);
            box(out,p,.45,-.18,-.25,1.05,-.04,.95,hull,light);box(out,p,1.05,-.16,.1,1.55,-.05,1.05,hull,light);
            box(out,p,1.4,-.18,.18,1.58,-.03,1.06,accent,light);
            box(out,p,.64,-.14,.65,1.06,.22,1.37,trim,light);box(out,p,.72,-.05,1.36,.98,.13,1.39,0xEEAE60,15728880);
            box(out,p,.63,-.5,-.9,.78,-.35,1.0,trim,light);box(out,p,.64,-.4,-.8,.76,-.2,-.6,trim,light);box(out,p,.64,-.4,.55,.76,-.2,.75,trim,light);
            box(out,p,.48,.12,.75,.65,.55,1.05,trim,light);box(out,p,.48,.55,.86,.65,.81,1.06,accent,light);
            p.popPose();
        }
        box(out,p,-.32,.33,.45,.32,.38,.98,0x8A9CA4,light);
        p.popPose();super.render(e,yaw,delta,p,b,light);
    }
}
