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
    private static void disk(BufferBuilder b,Matrix4f matrix,SkyCatalog.Vector d,SkyCatalog.Vector right,SkyCatalog.Vector up,double radius,int color,int alpha,String name){
        for(int row=-6;row<=6;row++){double yy=row/6.0,half=Math.sqrt(Math.max(0,1-yy*yy));int shaded=color;if(name.equals("Jupiter") || name.equals("Saturn")){double shade=(row%3==0?.75:1);shaded=((int)(((color>>16)&255)*shade)<<16)|((int)(((color>>8)&255)*shade)<<8)|(int)((color&255)*shade);}var center=new SkyCatalog.Vector(d.x()+up.x()*radius*yy,d.y()+up.y()*radius*yy,d.z()+up.z()*radius*yy);quad(b,matrix,center,right.scale(half),up.scale(.09),radius,shaded,alpha);}
        if(name.equals("Saturn"))for(int side:new int[]{-1,1}){var center=new SkyCatalog.Vector(d.x()+right.x()*radius*side*1.2,d.y()+right.y()*radius*side*1.2,d.z()+right.z()*radius*side*1.2);quad(b,matrix,center,right.scale(.75),up.scale(.11),radius,0xC8B98A,alpha);}
    }
    @SubscribeEvent public static void sky(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();var catalog=AstronomyClient.catalog;if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY || catalog==null || mc.level==null || mc.level.dimension()!=Level.OVERWORLD)return;
        double time=AstronomyClient.time(e.getPartialTick().getGameTimeDeltaPartialTick(false)),night=SkyCatalog.night(time)*(1-mc.level.getRainLevel(1));if(night<.01)return;
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);var m=e.getModelViewMatrix();
        for(var star:catalog.stars){var d=star.direction(time);if(d.y()<-.05)continue;var right=d.cross(new SkyCatalog.Vector(0,1,0));if(right.dot(right)<.0001)right=d.cross(new SkyCatalog.Vector(1,0,0));right=right.unit();var up=right.cross(d).unit();
            double size=star.kind()==SkyCatalog.Kind.PLANET?.0018:star.deep()?.003:.0005+Math.max(0,4-star.magnitude())*.00016;
            int alpha=(int)(Math.clamp((7-star.magnitude())/5,.08,1)*night*220);if(star.deep())alpha=(int)(night*(AstronomyClient.active()?90:22));
            var scope=AstronomyClient.scope();double blur=scope==null || scope.naked?1:1+Math.abs(scope.focus-.72)*5;
            if(star.deep() && scope!=null){
                var random=new java.util.Random(AstronomyClient.seed^star.id()*7919L);double extent=.004*star.radius();
                if(star.kind()!=SkyCatalog.Kind.CLUSTER)for(int layer=5;layer>=1;layer--)quad(b,m,d,right.scale(star.kind()==SkyCatalog.Kind.GALAXY?1.6:1),up.scale(star.kind()==SkyCatalog.Kind.GALAXY?.55:1),extent*layer/5,star.color(),(int)(night*10));
                for(int i=0;i<60;i++){double angle=random.nextDouble()*Math.PI*2,dist=Math.sqrt(random.nextDouble())*extent;double xx=Math.cos(angle)*dist*(star.kind()==SkyCatalog.Kind.GALAXY?1.5:1),yy=Math.sin(angle)*dist*(star.kind()==SkyCatalog.Kind.GALAXY?.5:1);var point=new SkyCatalog.Vector(d.x()+right.x()*xx+up.x()*yy,d.y()+right.y()*xx+up.y()*yy,d.z()+right.z()*xx+up.z()*yy);quad(b,m,point,right,up,star.kind()==SkyCatalog.Kind.CLUSTER?.00004:.0002*blur,star.color(),(int)(night*(star.kind()==SkyCatalog.Kind.CLUSTER?190:15)));}
            }else if(star.kind()==SkyCatalog.Kind.PLANET)disk(b,m,d,right,up,size*blur,star.color(),(int)(alpha/Math.sqrt(blur)),star.name());else quad(b,m,d,right,up,size*blur,star.color(),(int)(alpha/Math.sqrt(blur)));
        }
        double meteor=SkyCatalog.meteor(AstronomyClient.seed,time);if(meteor>=0){var d=SkyCatalog.aim(45+meteor*28,-45+meteor*20);var right=d.cross(new SkyCatalog.Vector(0,1,0)).unit();var up=right.cross(d).unit();quad(b,m,d,right.scale(10),up,.0007,0xD9E8FF,(int)(220*Math.sin(meteor*Math.PI)));}
        BufferUploader.drawWithShader(b.buildOrThrow());RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();
    }
}
