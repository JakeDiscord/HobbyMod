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
    private static TextureAtlasSprite wood(){return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/stripped_oak_log"));}
    @Override public void render(PaintingBlockEntity b,float delta,PoseStack p,MultiBufferSource buffers,int light,int overlay){
        p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(b.getBlockState().getValue(PaintingBlock.FACING).toYRot()+180));
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));var wood=wood();
        if(b.easel()){
            // Front legs meet at the head; the rear leg meets that same hinge.
            for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*.38,0,-.03);p.mulPose(Axis.ZP.rotationDegrees(side*11));box(out,p.last(),wood,-.04,0,-.035,.04,1.75,.035,light,overlay);p.popPose();}
            p.pushPose();p.translate(0,0,.40);p.mulPose(Axis.XP.rotationDegrees(-14));box(out,p.last(),wood,-.04,0,-.035,.04,1.72,.035,light,overlay);p.popPose();
            box(out,p.last(),wood,-.30,.34,-.065,.30,.40,.005,light,overlay);
            p.pushPose();p.translate(0,.64,-.17);p.mulPose(Axis.XP.rotationDegrees(8));
            box(out,p.last(),wood,-.045,0,-.015,.045,1.12,.065,light,overlay);
            box(out,p.last(),wood,-.42,0,-.14,.42,.07,.08,light,overlay);
            box(out,p.last(),wood,-.10,.94,-.06,.10,1.02,.06,light,overlay);
            if(b.painting!=null){var d=b.painting;double ratio=d.width()/(double)d.height(),h=ratio>=1?Math.min(.86,1.20/ratio):1.02;canvas(d,p,buffers,h*ratio,h,.09+h/2,-.055,light,overlay);}
            p.popPose();
        }else if(b.painting!=null){var d=b.painting;double ratio=d.width()/(double)d.height(),h=ratio>=1?Math.min(.9,1.28/ratio):1.05;canvas(d,p,buffers,h*ratio,h,.5,.43,light,overlay);}
        p.popPose();
    }
    public static void canvas(PaintingData d,PoseStack p,MultiBufferSource buffers,double w,double h,double cy,double z,int light,int overlay){
        var wood=wood();var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        double rim=.025;
        if(d.shape==PaintingData.Shape.ROUND){for(int i=0;i<64;i++){double a=i*Math.PI/32,c=(i+1)*Math.PI/32;quad(out,p.last(),wood,new Vec3(Math.cos(a)*(w/2+rim),cy+Math.sin(a)*(h/2+rim),z),new Vec3(Math.cos(c)*(w/2+rim),cy+Math.sin(c)*(h/2+rim),z),new Vec3(Math.cos(c)*w/2,cy+Math.sin(c)*h/2,z-.002),new Vec3(Math.cos(a)*w/2,cy+Math.sin(a)*h/2,z-.002),.10,.04,0,0,-1,light,overlay);}}
        else{
            box(out,p.last(),wood,-w/2-rim,cy-h/2-rim,z-.012,w/2+rim,cy-h/2,z+.03,light,overlay);
            box(out,p.last(),wood,-w/2-rim,cy+h/2,z-.012,w/2+rim,cy+h/2+rim,z+.03,light,overlay);
            box(out,p.last(),wood,-w/2-rim,cy-h/2,z-.012,-w/2,cy+h/2,z+.03,light,overlay);
            box(out,p.last(),wood,w/2,cy-h/2,z-.012,w/2+rim,cy+h/2,z+.03,light,overlay);
        }
        var canvas=buffers.getBuffer(RenderType.entityCutoutNoCull(PaintingTextures.texture(d)));var pose=p.last();
        vertex(canvas,pose,-w/2,cy-h/2,z-.014,1,1,0,0,-1,light,overlay);vertex(canvas,pose,w/2,cy-h/2,z-.014,0,1,0,0,-1,light,overlay);vertex(canvas,pose,w/2,cy+h/2,z-.014,0,0,0,0,-1,light,overlay);vertex(canvas,pose,-w/2,cy+h/2,z-.014,1,0,0,0,-1,light,overlay);
        var back=buffers.getBuffer(RenderType.entityCutoutNoCull(PaintingTextures.texture(PaintingTextures.blank(d.shape))));
        vertex(back,pose,-w/2,cy-h/2,z+.028,0,1,0,0,1,light,overlay);vertex(back,pose,w/2,cy-h/2,z+.028,1,1,0,0,1,light,overlay);vertex(back,pose,w/2,cy+h/2,z+.028,1,0,0,0,1,light,overlay);vertex(back,pose,-w/2,cy+h/2,z+.028,0,0,0,0,1,light,overlay);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose p,double x,double y,double z,float u,float v,float nx,float ny,float nz,int l,int o){out.addVertex(p,(float)x,(float)y,(float)z).setColor(255,255,255,255).setUv(u,v).setOverlay(o).setLight(l).setNormal(p,nx,ny,nz);}
    private static void quad(VertexConsumer out,PoseStack.Pose p,TextureAtlasSprite t,Vec3 a,Vec3 b,Vec3 c,Vec3 d,double w,double h,float nx,float ny,float nz,int l,int o){float u0=t.getU(.25F),u1=t.getU(.25F+(float)Math.min(.7,w)),v0=t.getV(0),v1=t.getV((float)Math.min(1,h));vertex(out,p,a.x,a.y,a.z,u0,v1,nx,ny,nz,l,o);vertex(out,p,b.x,b.y,b.z,u1,v1,nx,ny,nz,l,o);vertex(out,p,c.x,c.y,c.z,u1,v0,nx,ny,nz,l,o);vertex(out,p,d.x,d.y,d.z,u0,v0,nx,ny,nz,l,o);}
    private static void box(VertexConsumer out,PoseStack.Pose p,TextureAtlasSprite t,double x,double y,double z,double X,double Y,double Z,int l,int o){
        // Repeat long boards at block scale; never stretch a whole plank texture onto a thin edge.
        if(Y-y>1){box(out,p,t,x,y,z,X,y+1,Z,l,o);box(out,p,t,x,y+1,z,X,Y,Z,l,o);return;}
        if(X-x>1){box(out,p,t,x,y,z,x+1,Y,Z,l,o);box(out,p,t,x+1,y,z,X,Y,Z,l,o);return;}
        Vec3 a=new Vec3(x,y,z),b=new Vec3(X,y,z),c=new Vec3(X,Y,z),d=new Vec3(x,Y,z),e=new Vec3(x,y,Z),f=new Vec3(X,y,Z),g=new Vec3(X,Y,Z),h=new Vec3(x,Y,Z);
        quad(out,p,t,a,b,c,d,X-x,Y-y,0,0,-1,l,o);quad(out,p,t,e,f,g,h,X-x,Y-y,0,0,1,l,o);
        quad(out,p,t,a,e,h,d,Z-z,Y-y,-1,0,0,l,o);quad(out,p,t,b,f,g,c,Z-z,Y-y,1,0,0,l,o);
        quad(out,p,t,d,c,g,h,X-x,Z-z,0,1,0,l,o);quad(out,p,t,a,b,f,e,X-x,Z-z,0,-1,0,l,o);
    }
}
