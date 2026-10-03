package io.github.jakediscord.hobbymod.painting.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.painting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
public final class PaintingRenderer implements BlockEntityRenderer<PaintingBlockEntity> {
    public PaintingRenderer(BlockEntityRendererProvider.Context c){}
    @Override public boolean shouldRenderOffScreen(PaintingBlockEntity b){return true;}
    @Override public int getViewDistance(){return 48;}
    private static TextureAtlasSprite wood(){return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/oak_planks"));}
    @Override public void render(PaintingBlockEntity b,float delta,PoseStack p,MultiBufferSource buffers,int light,int overlay){p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(b.getBlockState().getValue(PaintingBlock.FACING).toYRot()+180));var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));var wood=wood();
        if(b.easel()){
            p.pushPose();p.translate(-.30,0,0);p.mulPose(Axis.ZP.rotationDegrees(-8));box(out,p.last(),wood,-.035,0,-.03,.035,1.72,.03,light,overlay);p.popPose();
            p.pushPose();p.translate(.30,0,0);p.mulPose(Axis.ZP.rotationDegrees(8));box(out,p.last(),wood,-.035,0,-.03,.035,1.72,.03,light,overlay);p.popPose();
            p.pushPose();p.translate(0,0,.30);p.mulPose(Axis.XP.rotationDegrees(12));box(out,p.last(),wood,-.035,0,-.03,.035,1.60,.03,light,overlay);p.popPose();
            box(out,p.last(),wood,-.38,.63,-.11,.38,.70,.06,light,overlay);box(out,p.last(),wood,-.25,.35,-.03,.25,.40,.03,light,overlay);
        }
        var d=b.painting;if(d!=null){double ratio=d.width()/(double)d.height(),h=ratio>=1?Math.min(.90,1.28/ratio):1.05,w=h*ratio,cy=b.easel()?.72+h/2:.5,z=b.easel()?-.07:.43;
            if(d.shape==PaintingData.Shape.ROUND){int n=40;for(int i=0;i<n;i++){double a=i*Math.PI*2/n,c=(i+1)*Math.PI*2/n;quad(out,p.last(),wood,new Vec3(Math.cos(a)*(w/2+.035),cy+Math.sin(a)*(h/2+.035),z),new Vec3(Math.cos(c)*(w/2+.035),cy+Math.sin(c)*(h/2+.035),z),new Vec3(Math.cos(c)*w/2,cy+Math.sin(c)*h/2,z-.002),new Vec3(Math.cos(a)*w/2,cy+Math.sin(a)*h/2,z-.002),light,overlay);}}
            else{box(out,p.last(),wood,-w/2-.035,cy-h/2-.035,z-.015,w/2+.035,cy-h/2,z+.035,light,overlay);box(out,p.last(),wood,-w/2-.035,cy+h/2,z-.015,w/2+.035,cy+h/2+.035,z+.035,light,overlay);box(out,p.last(),wood,-w/2-.035,cy-h/2,z-.015,-w/2,cy+h/2,z+.035,light,overlay);box(out,p.last(),wood,w/2,cy-h/2,z-.015,w/2+.035,cy+h/2,z+.035,light,overlay);}
            var canvas=buffers.getBuffer(RenderType.entityCutoutNoCull(PaintingTextures.texture(d)));var pose=p.last();
            vertex(canvas,pose,-w/2,cy-h/2,z-.018,1,1,light,overlay);vertex(canvas,pose,w/2,cy-h/2,z-.018,0,1,light,overlay);vertex(canvas,pose,w/2,cy+h/2,z-.018,0,0,light,overlay);vertex(canvas,pose,-w/2,cy+h/2,z-.018,1,0,light,overlay);
            var back=buffers.getBuffer(RenderType.entityCutoutNoCull(PaintingTextures.texture(PaintingTextures.blank(d.shape))));
            vertex(back,pose,-w/2,cy-h/2,z+.032,0,1,light,overlay);vertex(back,pose,w/2,cy-h/2,z+.032,1,1,light,overlay);vertex(back,pose,w/2,cy+h/2,z+.032,1,0,light,overlay);vertex(back,pose,-w/2,cy+h/2,z+.032,0,0,light,overlay);

        }p.popPose();
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose p,double x,double y,double z,float u,float v,int l,int o){out.addVertex(p,(float)x,(float)y,(float)z).setColor(255,255,255,255).setUv(u,v).setOverlay(o).setLight(l).setNormal(p,0,0,-1);}
    private static void quad(VertexConsumer out,PoseStack.Pose p,TextureAtlasSprite t,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int l,int o){vertex(out,p,a.x,a.y,a.z,t.getU0(),t.getV1(),l,o);vertex(out,p,b.x,b.y,b.z,t.getU1(),t.getV1(),l,o);vertex(out,p,c.x,c.y,c.z,t.getU1(),t.getV0(),l,o);vertex(out,p,d.x,d.y,d.z,t.getU0(),t.getV0(),l,o);}
    private static void box(VertexConsumer out,PoseStack.Pose p,TextureAtlasSprite t,double x,double y,double z,double X,double Y,double Z,int l,int o){Vec3 a=new Vec3(x,y,z),b=new Vec3(X,y,z),c=new Vec3(X,Y,z),d=new Vec3(x,Y,z),e=new Vec3(x,y,Z),f=new Vec3(X,y,Z),g=new Vec3(X,Y,Z),h=new Vec3(x,Y,Z);quad(out,p,t,a,b,c,d,l,o);quad(out,p,t,e,h,g,f,l,o);quad(out,p,t,a,d,h,e,l,o);quad(out,p,t,b,f,g,c,l,o);quad(out,p,t,d,c,g,h,l,o);quad(out,p,t,a,e,f,b,l,o);}
}
