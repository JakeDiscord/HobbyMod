package io.github.jakediscord.hobbymod.astronomy.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.astronomy.*;
import io.github.jakediscord.hobbymod.aquarium.client.AquariumRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
public final class TelescopeRenderer implements BlockEntityRenderer<TelescopeBlockEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("hobbymod","textures/entity/aquarium_white.png");
    public TelescopeRenderer(BlockEntityRendererProvider.Context c){}
    private void box(VertexConsumer o,PoseStack p,double x,double y,double z,double X,double Y,double Z,int c,int light,int overlay){AquariumRenderer.box(o,p.last(),null,x,y,z,X,Y,Z,c,light,overlay);}
    private void tube(VertexConsumer o,PoseStack p,double radius,double z,double Z,int color,int light,int overlay){
        for(int i=0;i<12;i++){double a=i*Math.PI/6,b=(i+1)*Math.PI/6;double x=Math.cos(a)*radius,y=Math.sin(a)*radius,X=Math.cos(b)*radius,Y=Math.sin(b)*radius;
            vertex(o,p,x,y,z,color,light,overlay);vertex(o,p,X,Y,z,color,light,overlay);vertex(o,p,X,Y,Z,color,light,overlay);vertex(o,p,x,y,Z,color,light,overlay);
        }
    }
    private void vertex(VertexConsumer o,PoseStack p,double x,double y,double z,int color,int light,int overlay){o.addVertex(p.last(),(float)x,(float)y,(float)z).setColor(0xFF000000|color).setUv((float)(x+.5),(float)(z+.5)).setOverlay(overlay).setLight(light).setNormal(p.last(),0,1,0);}
    @Override public void render(TelescopeBlockEntity b,float partial,PoseStack p,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));p.pushPose();p.translate(.5,b.large()?1.55:1.12,.5);
        float yaw=b.yaw,pitch=b.pitch;var scope=AstronomyClient.scope();if(scope!=null && scope.pos.equals(b.getBlockPos())){yaw=scope.yaw;pitch=scope.pitch;}
        p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.mulPose(Axis.XP.rotationDegrees(-pitch));if(b.large())p.scale(1.35f,1.35f,1.35f);
        tube(out,p,.145,-.65,.18,0xD6DFE4,light,overlay);tube(out,p,.155,-.68,-.60,0x27343C,light,overlay);tube(out,p,.15,.15,.22,0x27343C,light,overlay);
        tube(out,p,.058,.20,.37,0x1B2732,light,overlay);box(out,p,-.045,-.045,.36,.045,.045,.38,0x4C839D,light,overlay);
        box(out,p,-.11,-.11,-.671,.11,.11,-.666,0x153C50,light,overlay);
        tube(out,p,.157,-.26,-.21,0x4D6472,light,overlay);tube(out,p,.157,-.10,-.05,0x4D6472,light,overlay);
        box(out,p,.11,-.025,.21,.20,.025,.27,0x8DB9CA,light,overlay);
        p.pushPose();p.translate(.08,.19,-.03);tube(out,p,.034,-.32,.15,0x293C49,light,overlay);box(out,p,-.026,-.026,-.322,.026,.026,-.32,0x78BAD1,light,overlay);p.popPose();
        p.popPose();
    }
    @Override public boolean shouldRenderOffScreen(TelescopeBlockEntity b){return true;}
}
