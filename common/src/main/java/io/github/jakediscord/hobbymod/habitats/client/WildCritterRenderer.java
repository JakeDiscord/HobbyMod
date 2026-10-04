package io.github.jakediscord.hobbymod.habitats.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jakediscord.hobbymod.habitats.WildCritter;
import io.github.jakediscord.hobbymod.terrarium.TerrariumMotion;
import io.github.jakediscord.hobbymod.terrarium.client.TerrariumAnimalRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class WildCritterRenderer extends EntityRenderer<WildCritter> {
    private final TerrariumAnimalRenderer models=new TerrariumAnimalRenderer();
    public WildCritterRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.12f;}
    @Override public ResourceLocation getTextureLocation(WildCritter e){return ResourceLocation.fromNamespaceAndPath("hobbymod","textures/entity/aquarium_white.png");}
    @Override public void render(WildCritter e,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        var r=e.resident();double time=e.tickCount+partial,gait=e.walkAnimation.position(partial)*4;
        var p=new TerrariumMotion.Pose(0,0,Math.toRadians(e.yBodyRot+90),e.onGround()?0:.018,gait,Math.toRadians(e.yHeadRot-e.yBodyRot),e.getDeltaMovement().horizontalDistanceSqr()>.00001?TerrariumMotion.Activity.EXPLORE:TerrariumMotion.Activity.REST,e.onGround()?-1:.5);
        poses.pushPose();float scale=r.species.vertebrate()?2:3;poses.scale(scale,scale,scale);models.render(r,e.getUUID(),p,e.level(),time,partial,poses,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,false);poses.popPose();super.render(e,yaw,partial,poses,buffers,light);
    }
}
