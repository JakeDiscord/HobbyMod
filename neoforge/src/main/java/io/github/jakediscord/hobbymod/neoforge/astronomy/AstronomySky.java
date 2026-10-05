package io.github.jakediscord.hobbymod.neoforge.astronomy;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.astronomy.*;
import io.github.jakediscord.hobbymod.astronomy.client.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
@EventBusSubscriber(modid=HobbyMod.MOD_ID,value=Dist.CLIENT)
public final class AstronomySky {
    private static void quad(BufferBuilder b,Matrix4f matrix,SkyCatalog.Vector center,SkyCatalog.Vector right,SkyCatalog.Vector up,double radius,int color,int alpha){
        for(int[] corner:new int[][]{{-1,-1},{-1,1},{1,1},{1,-1}}){double x=(center.x()+radius*(corner[0]*right.x()+corner[1]*up.x()))*100,y=(center.y()+radius*(corner[0]*right.y()+corner[1]*up.y()))*100,z=(center.z()+radius*(corner[0]*right.z()+corner[1]*up.z()))*100;b.addVertex(matrix,(float)x,(float)y,(float)z).setColor((color>>16)&255,(color>>8)&255,color&255,alpha);}
    }
    private static final net.minecraft.resources.ResourceLocation PLANETS=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hobbymod","textures/environment/planet_cubes.png");
    private static void starCube(BufferBuilder b,Matrix4f m,SkyCatalog.Object star,SkyCatalog.Vector d,double size,int alpha,double time){
        var frame=CelestialGeometry.frame(star.id(),time);var vertices=CelestialGeometry.cube(d,size,frame);
        for(int face=0;face<6;face++)if(CelestialGeometry.front(face,frame,d)){
            double shade=.72+.28*(-CelestialGeometry.normal(face,frame).dot(d));
            for(int index:CelestialGeometry.FACES[face]){var p=vertices[index];b.addVertex(m,(float)(p.x()*100),(float)(p.y()*100),(float)(p.z()*100)).setColor((int)(((star.color()>>16)&255)*shade),(int)(((star.color()>>8)&255)*shade),(int)((star.color()&255)*shade),alpha);}
        }
    }
    private static void texturedFace(BufferBuilder b,Matrix4f m,SkyCatalog.Vector[] vertices,int[] indices,int sprite,int face,int shade,int alpha){
        float u0=sprite/8f,u1=(sprite+1)/8f,v0=face/6f,v1=(face+1)/6f;float[][] uv={{u0,v1},{u0,v0},{u1,v0},{u1,v1}};
        for(int i=0;i<4;i++){var p=vertices[indices[i]];b.addVertex(m,(float)(p.x()*100),(float)(p.y()*100),(float)(p.z()*100)).setUv(uv[i][0],uv[i][1]).setColor(shade,shade,shade,alpha);}
    }
    private static void rings(BufferBuilder b,Matrix4f m,SkyCatalog.Vector d,CelestialGeometry.Frame frame,double size,int alpha,boolean front){
        for(int side=0;side<4;side++){
            double[][] corners=switch(side){case 0->new double[][]{{-2.1,-2.1},{-1.35,-1.35},{1.35,-1.35},{2.1,-2.1}};case 1->new double[][]{{2.1,-2.1},{1.35,-1.35},{1.35,1.35},{2.1,2.1}};case 2->new double[][]{{2.1,2.1},{1.35,1.35},{-1.35,1.35},{-2.1,2.1}};default->new double[][]{{-2.1,2.1},{-1.35,1.35},{-1.35,-1.35},{-2.1,-2.1}};};
            var center=frame.local((corners[0][0]+corners[3][0])*.5,0,(corners[0][1]+corners[3][1])*.5);if((center.dot(d)<0)!=front)continue;
            var vertices=new SkyCatalog.Vector[4];for(int i=0;i<4;i++){var p=frame.local(corners[i][0]*size,0,corners[i][1]*size);vertices[i]=new SkyCatalog.Vector(d.x()+p.x(),d.y()+p.y(),d.z()+p.z());}
            texturedFace(b,m,vertices,new int[]{0,1,2,3},7,0,200,alpha);
        }
    }
    public static void cube(BufferBuilder b,Matrix4f m,SkyCatalog.Object planet,SkyCatalog.Vector d,double size,int alpha,double time){
        var frame=CelestialGeometry.frame(planet.name().equals("Earth")?-1:planet.id(),time);var vertices=CelestialGeometry.cube(d,size,frame);
        if(planet.name().equals("Saturn"))rings(b,m,d,frame,size,alpha,false);
        var light=CelestialGeometry.observer(new SkyCatalog.Vector(.4,.5,-.7).unit(),time);
        for(int face=0;face<6;face++)if(CelestialGeometry.front(face,frame,d)){
            int shade=(int)(255*(.38+.62*Math.max(0,CelestialGeometry.normal(face,frame).dot(light))));texturedFace(b,m,vertices,CelestialGeometry.FACES[face],planet.id()-22,face,shade,alpha);
        }
        if(planet.name().equals("Saturn"))rings(b,m,d,frame,size,alpha,true);
    }
    @SubscribeEvent public static void sky(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();var catalog=AstronomyClient.catalog;if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY || catalog==null || mc.level==null || mc.level.dimension()!=Level.OVERWORLD)return;
        double time=AstronomyClient.time(e.getPartialTick().getGameTimeDeltaPartialTick(false)),night=SkyCatalog.night(time)*(1-mc.level.getRainLevel(1));if(night<.01)return;
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);var m=e.getModelViewMatrix();
        for(var star:catalog.stars){var d=star.direction(time);if(d.y()<-.05)continue;var right=d.cross(new SkyCatalog.Vector(0,1,0));if(right.dot(right)<.0001)right=d.cross(new SkyCatalog.Vector(1,0,0));right=right.unit();var up=right.cross(d).unit();
            double size=star.kind()==SkyCatalog.Kind.PLANET?.006:star.deep()?.003:star.id()>=0?.0018+Math.max(0,4-star.magnitude())*.00035:.0007+Math.max(0,4-star.magnitude())*.00018;
            int alpha=(int)(Math.clamp((8-star.magnitude())/5,.18,1)*night*255);if(star.deep())alpha=(int)(night*(AstronomyClient.active()?90:22));
            var scope=AstronomyClient.scope();double blur=1;
            if(star.deep() && scope!=null){
                var random=new java.util.Random(AstronomyClient.seed^star.id()*7919L);double extent=.004*star.radius();
                if(star.kind()!=SkyCatalog.Kind.CLUSTER)for(int layer=5;layer>=1;layer--)quad(b,m,d,right.scale(star.kind()==SkyCatalog.Kind.GALAXY?1.6:1),up.scale(star.kind()==SkyCatalog.Kind.GALAXY?.55:1),extent*layer/5,star.color(),(int)(night*10));
                for(int i=0;i<60;i++){double angle=random.nextDouble()*Math.PI*2,dist=Math.sqrt(random.nextDouble())*extent;double xx=Math.cos(angle)*dist*(star.kind()==SkyCatalog.Kind.GALAXY?1.5:1),yy=Math.sin(angle)*dist*(star.kind()==SkyCatalog.Kind.GALAXY?.5:1);var point=new SkyCatalog.Vector(d.x()+right.x()*xx+up.x()*yy,d.y()+right.y()*xx+up.y()*yy,d.z()+right.z()*xx+up.z()*yy);quad(b,m,point,right,up,star.kind()==SkyCatalog.Kind.CLUSTER?.00004:.0002*blur,star.color(),(int)(night*(star.kind()==SkyCatalog.Kind.CLUSTER?190:15)));}
            }else if(star.kind()==SkyCatalog.Kind.PLANET){}else if(star.id()>=0 && !star.deep()){quad(b,m,d,right,up,size*1.7,star.color(),(int)(alpha*.08));starCube(b,m,star,d,size,alpha,time);}else quad(b,m,d,right,up,size,star.color(),alpha);
        }
        double meteor=SkyCatalog.meteor(AstronomyClient.seed,time);if(meteor>=0){var d=SkyCatalog.aim(45+meteor*28,-45+meteor*20);var right=d.cross(new SkyCatalog.Vector(0,1,0)).unit();var up=right.cross(d).unit();quad(b,m,d,right.scale(10),up,.0007,0xD9E8FF,(int)(220*Math.sin(meteor*Math.PI)));}
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,PLANETS);
        b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(var planet:catalog.targets)if(planet.kind()==SkyCatalog.Kind.PLANET){var d=planet.direction(time);if(d.y()<-.05)continue;cube(b,m,planet,d,.006,(int)(night*255),time);}
        BufferUploader.drawWithShader(b.buildOrThrow());RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();
    }
}
