package io.github.jakediscord.hobbymod.astronomy.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.astronomy.*;
import io.github.jakediscord.hobbymod.aquarium.client.AquariumRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

/** Faceted optical tube, rotating cradle, finder, focuser and inset objective. */
public final class TelescopeRenderer implements BlockEntityRenderer<TelescopeBlockEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("hobbymod","textures/entity/aquarium_white.png");
    public TelescopeRenderer(BlockEntityRendererProvider.Context c){}
    private void box(VertexConsumer o,PoseStack p,double x,double y,double z,double X,double Y,double Z,int c,int light,int overlay){AquariumRenderer.box(o,p.last(),null,x,y,z,X,Y,Z,c,light,overlay);}
    private void tube(VertexConsumer o,PoseStack p,double radius,double z,double Z,int color,int light,int overlay){
        for(int i=0;i<8;i++){
            double a=(i+.5)*Math.PI/4,b=(i+1.5)*Math.PI/4;
            float nx=(float)Math.cos((a+b)/2),ny=(float)Math.sin((a+b)/2);
            double x=Math.cos(a)*radius,y=Math.sin(a)*radius,X=Math.cos(b)*radius,Y=Math.sin(b)*radius;
            vertex(o,p,x,y,z,color,light,overlay,nx,ny);vertex(o,p,X,Y,z,color,light,overlay,nx,ny);
            vertex(o,p,X,Y,Z,color,light,overlay,nx,ny);vertex(o,p,x,y,Z,color,light,overlay,nx,ny);
        }
    }
    private void vertex(VertexConsumer o,PoseStack p,double x,double y,double z,int color,int light,int overlay,float nx,float ny){o.addVertex(p.last(),(float)x,(float)y,(float)z).setColor(0xFF000000|color).setUv((float)(x+.5),(float)(z+.5)).setOverlay(overlay).setLight(light).setNormal(p.last(),nx,ny,0);}
    @Override public void render(TelescopeBlockEntity b,float partial,PoseStack p,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));p.pushPose();p.translate(.5,b.large()?1.55:1.12,.5);
        float yaw=b.yaw,pitch=b.pitch;var scope=AstronomyClient.scope();if(scope!=null && scope.pos.equals(b.getBlockPos())){yaw=scope.yaw;pitch=scope.pitch;}
        p.mulPose(Axis.YP.rotationDegrees(180-yaw));if(b.large())p.scale(1.35f,1.35f,1.35f);
        double radius=b.large()?.205:.155;
        box(out,p,-radius-.11,-.25,-.075,-radius-.045,.045,.075,0x223749,light,overlay);
        box(out,p,radius+.045,-.25,-.075,radius+.11,.045,.075,0x223749,light,overlay);
        box(out,p,-radius-.11,-.25,-.075,radius+.11,-.19,.075,0x304B61,light,overlay);
        box(out,p,radius+.11,-.05,-.047,radius+.14,.05,.047,0xBE9970,light,overlay);
        p.mulPose(Axis.XP.rotationDegrees(-pitch));
        tube(out,p,radius,-.60,.16,0xDEE6E8,light,overlay);
        tube(out,p,radius+.018,-.68,-.59,0x263C50,light,overlay);
        tube(out,p,radius+.012,.12,.22,0x263C50,light,overlay);
        tube(out,p,radius+.01,-.37,-.33,0xC6A178,light,overlay);
        tube(out,p,radius+.01,-.05,-.015,0x57758A,light,overlay);
        // Objective sits behind the raised hood; the telescope camera is outside it.
        double lens=radius*.77;
        box(out,p,-lens,-lens,-.64,lens,lens,-.635,0x0D233C,light,overlay);
        box(out,p,-lens+.024,-lens+.025,-.642,-lens+.056,lens-.02,-.64,0x43778B,light,overlay);
        box(out,p,-lens+.065,lens-.045,-.643,lens-.025,lens-.026,-.641,0x82C3CE,light,overlay);
        box(out,p,-radius,-radius,.19,radius,radius,.215,0x203349,light,overlay);
        tube(out,p,.065,.21,.34,0x506879,light,overlay);tube(out,p,.073,.31,.39,0x172838,light,overlay);
        box(out,p,-.044,-.044,.389,.044,.044,.395,0x477C8F,light,overlay);
        box(out,p,-.065,-radius-.045,-.25,.065,-radius+.014,.07,0x30495C,light,overlay);
        box(out,p,radius-.02,-.032,.24,radius+.10,.032,.285,0xB99873,light,overlay);
        for(double z:new double[]{-.35,-.035})for(double x:new double[]{-radius-.014,radius+.009})box(out,p,x,-.025,z,x+.012,.025,z+.025,0xADC0CB,light,overlay);
        // Finder is on a raised two-point bracket rather than floating above the tube.
        box(out,p,-.035,radius-.015,-.32,.035,radius+.065,-.275,0x263C50,light,overlay);
        box(out,p,-.035,radius-.015,-.07,.035,radius+.065,-.025,0x263C50,light,overlay);
        p.pushPose();p.translate(0,radius+.10,-.12);tube(out,p,.042,-.28,.12,0x314B60,light,overlay);tube(out,p,.048,-.30,-.25,0xBC9871,light,overlay);box(out,p,-.03,-.03,-.295,.03,.03,-.29,0x345D72,light,overlay);p.popPose();
        p.popPose();
    }
    @Override public boolean shouldRenderOffScreen(TelescopeBlockEntity b){return true;}
}
