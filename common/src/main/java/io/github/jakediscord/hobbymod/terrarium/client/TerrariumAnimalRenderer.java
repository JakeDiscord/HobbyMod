package io.github.jakediscord.hobbymod.terrarium.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.terrarium.*;
import io.github.jakediscord.hobbymod.aquarium.client.AquariumRenderer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.Level;

/** Small pixel-shaped animals with distance-driven feet, feelers, breathing and native frog animation. */
public final class TerrariumAnimalRenderer {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("hobbymod","textures/entity/aquarium_white.png");
    private record FrogKey(Object enclosure,UUID resident){}
    private final Map<FrogKey,Frog> frogs=new LinkedHashMap<>(64,.75f,true){protected boolean removeEldestEntry(Map.Entry<FrogKey,Frog> e){if(size()>128){frogTicks.remove(e.getKey());return true;}return false;}};
    private Level world;
    private final Map<FrogKey,Integer> frogTicks=new HashMap<>();
    private static void cube(VertexConsumer out,PoseStack poses,double x,double y,double z,double X,double Y,double Z,int color,int light,int overlay){AquariumRenderer.box(out,poses.last(),null,x,y,z,X,Y,Z,color,light,overlay);}
    public void render(TerrariumData.Resident r,Object enclosure,TerrariumMotion.Pose p,Level level,double time,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay,boolean distant){
        if(world!=level){world=level;frogs.clear();frogTicks.clear();}
        if(r.species==TerrariumData.Species.TREE_FROG){
            var key=new FrogKey(enclosure,r.id);var frog=frogs.computeIfAbsent(key,id->new Frog(EntityType.FROG,level));frog.setNoAi(true);frog.tickCount=(int)time;
            var variants=level.registryAccess().registryOrThrow(Registries.FROG_VARIANT);
            frog.setVariant(variants.getHolder(ResourceLocation.withDefaultNamespace(new String[]{"temperate","warm","cold","temperate"}[r.variant])).orElseThrow());
            float yaw=(float)(Math.toDegrees(p.yaw())-90);frog.yBodyRot=frog.yBodyRotO=yaw;frog.yHeadRot=frog.yHeadRotO=yaw+(float)Math.toDegrees(p.head());frog.setYRot(yaw);frog.yRotO=yaw;
            int tick=(int)Math.floor(time);Integer previous=frogTicks.put(key,tick);
            if(previous==null || previous!=tick){
                boolean jumping=r.alive() && p.jump()>=0;
                if(jumping){int start=tick-(int)Math.round(p.jump()*TerrariumMotion.HOP_TICKS);if(!frog.jumpAnimationState.isStarted())frog.jumpAnimationState.start(start);}else frog.jumpAnimationState.stop();
                frog.croakAnimationState.animateWhen(r.alive() && p.activity()==TerrariumMotion.Activity.GROOM,tick);
                // One update per game tick. Hopping and walking never play on top of each other.
                frog.walkAnimation.update(0,1);
            }
            frog.deathTime=r.alive()?0:20;
            poses.pushPose();if(!r.alive())poses.translate(0,.045,0);poses.scale(.28f,.28f,.28f);Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(frog).render(frog,yaw,partial,poses,buffers,light);poses.popPose();return;
        }
        poses.pushPose();poses.mulPose(Axis.YP.rotationDegrees((float)TerrariumMotion.modelYaw(p.yaw())));
        if(!r.alive()){poses.translate(0,.025,0);poses.mulPose(Axis.ZP.rotationDegrees(90));}
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));int color;
        if(r.species==TerrariumData.Species.GECKO){
            color=new int[]{0x7C9B43,0xD6B05D,0x8A9F91,0xC47950}[r.variant];
            cube(out,poses,-.023,.016,-.043,.023,.047,.039,color,light,overlay);
            for(int i=0;i<4;i++){double half=.018-i*.003,z=.034+i*.019;cube(out,poses,-half,.012,z,half,.03-i*.003,z+.021,color,light,overlay);}
            for(int side:new int[]{-1,1})for(int i=0;i<2;i++){double a=Math.sin(p.gait()+(i+ (side<0?0:1))*Math.PI)*.010,z=i==0?-.030:.024;double x=side<0?-.044:.023;cube(out,poses,x,.006,z+a,x+.021,.018,z+.018+a,color,light,overlay);cube(out,poses,side<0?-.052:.040,.001,z+a,side<0?-.035:.057,.007,z+.018+a,0xB1BC77,light,overlay);}
            for(int i=0;i<3;i++)cube(out,poses,-.008,.047,-.025+i*.024,.009,.049,-.017+i*.024,0x465E31,light,overlay);
            poses.pushPose();poses.translate(0,0,-.042);poses.mulPose(Axis.YP.rotation((float)p.head()));
            cube(out,poses,-.028,.023,-.039,.028,.048,.010,color,light,overlay);
            for(int side:new int[]{-1,1})cube(out,poses,side<0?-.030:.022,.040,-.024,side<0?-.022:.030,.051,-.012,0x201D17,light,overlay);poses.popPose();
        }else if(r.species==TerrariumData.Species.SNAIL){
            color=new int[]{0xA28A58,0xDED1AE,0x758B8F,0xBC7655}[r.variant];
            cube(out,poses,-.024,.002,-.055,.024,.014,.045,0xA4A482,light,overlay);
            cube(out,poses,-.025,.012,-.010,.025,.061,.039,color,light,overlay);cube(out,poses,-.018,.061,-.003,.018,.073,.031,color,light,overlay);
            cube(out,poses,-.027,.027,.001,-.025,.049,.025,0x574D35,light,overlay);cube(out,poses,.025,.027,.001,.027,.049,.025,0x574D35,light,overlay);
            cube(out,poses,-.027,.035,.008,-.025,.040,.018,color,light,overlay);cube(out,poses,.025,.035,.008,.027,.040,.018,color,light,overlay);
            for(int side:new int[]{-1,1}){poses.pushPose();poses.translate(side*.012,.010,-.047);poses.mulPose(Axis.ZP.rotation((float)(side*.22+p.head()*.2)));cube(out,poses,-.002,0,-.003,.002,.027,.002,0xABB69A,light,overlay);cube(out,poses,-.003,.025,-.004,.003,.030,.003,0x262C21,light,overlay);poses.popPose();}
        }else{
            boolean iso=r.species==TerrariumData.Species.ISOPOD;double scale=iso?1:.45;poses.scale((float)scale,(float)scale,(float)scale);
            color=iso?new int[]{0x899BA1,0xD8CBA4,0x5F789B,0x9F8678}[r.variant]:new int[]{0xE0E4D3,0xF0EADB,0xD4DFE5,0xE8D6D0}[r.variant];
            for(int i=0;i<(iso?7:4);i++){double z=-.049+i*(iso?.014:.022),half=i==0 || i==(iso?6:3)?.020:.032;
                cube(out,poses,-half,.009,z,half,.029,z+.012,color,light,overlay);
                if(!distant)for(int side:new int[]{-1,1}){double foot=Math.sin(p.gait()+i*.9+(side<0?0:Math.PI))*.008;cube(out,poses,side<0?-half-.014:half-.003,.001,z+foot,side<0?-half+.003:half+.014,.006,z+.005+foot,0x535A48,light,overlay);}
            }
            for(int side:new int[]{-1,1}){cube(out,poses,side<0?-.017:.011,.018,-.052,side<0?-.011:.017,.024,-.045,0x20251F,light,overlay);poses.pushPose();poses.translate(side*.011,.017,-.049);poses.mulPose(Axis.YP.rotation((float)(side*.4+p.head())));cube(out,poses,-.002,0,-.032,.002,.004,0,color,light,overlay);poses.popPose();}
        }
        poses.popPose();
    }
}
