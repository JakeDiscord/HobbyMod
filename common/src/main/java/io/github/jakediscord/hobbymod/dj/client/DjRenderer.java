package io.github.jakediscord.hobbymod.dj.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.dj.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
/** Two tiny textured quads animate the vinyl; no per-frame song decoding or generated mesh. */
public final class DjRenderer implements BlockEntityRenderer<DjBlockEntity> {
    private static final ResourceLocation VINYL=ResourceLocation.fromNamespaceAndPath("hobbymod","block/dj_vinyl");
    public DjRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(DjBlockEntity b,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){if(b.getLevel()==null)return;var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(VINYL);var consumer=buffers.getBuffer(RenderType.cutout());
        poses.pushPose();poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotationDegrees(-b.getBlockState().getValue(DjBlock.FACING).toYRot()+180));poses.translate(-.5,0,-.5);
        for(int d=0;d<2;d++){if(!b.playing[d])continue;poses.pushPose();poses.translate(d==0?.25:.75,15.03/16,.5625);poses.mulPose(Axis.YP.rotationDegrees((float)((b.getLevel().getGameTime()+partial-b.started[d])*b.projects[d].bpm/20.0)));var p=poses.last();float s=3.1F/16;
            consumer.addVertex(p,-s,0,-s).setColor(1F,1F,1F,1F).setUv(sprite.getU0(),sprite.getV0()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);
            consumer.addVertex(p,-s,0,s).setColor(1F,1F,1F,1F).setUv(sprite.getU0(),sprite.getV1()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);
            consumer.addVertex(p,s,0,s).setColor(1F,1F,1F,1F).setUv(sprite.getU1(),sprite.getV1()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);
            consumer.addVertex(p,s,0,-s).setColor(1F,1F,1F,1F).setUv(sprite.getU1(),sprite.getV0()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);poses.popPose();}
        poses.popPose();
    }
}
