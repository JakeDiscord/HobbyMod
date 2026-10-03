package io.github.jakediscord.hobbymod.aquarium.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.aquarium.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Custom glass enclosure, contained water, aquascape and independently lit residents. */
public class AquariumRenderer implements BlockEntityRenderer<AquariumBlockEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("hobbymod","textures/entity/aquarium_white.png");
    private final AquariumFishRenderer fishModels=new AquariumFishRenderer();
    public AquariumRenderer(BlockEntityRendererProvider.Context context){}
    @Override public int getViewDistance(){return 48;}
    @Override public boolean shouldRenderOffScreen(AquariumBlockEntity tank){return true;}
    @Override public boolean shouldRender(AquariumBlockEntity tank,Vec3 camera){
        var s=tank.data.size;return camera.distanceToSqr(Vec3.atLowerCornerOf(tank.getBlockPos()).add(s.blocksWide()/2.0,s.blocksHigh()/2.0,s.blocksDeep()/2.0))<48*48;
    }
    private static TextureAtlasSprite sprite(String name){return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/"+name));}
    @Override public void render(AquariumBlockEntity tank,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        var d=tank.data;var s=d.size;
        boolean distant=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(Vec3.atLowerCornerOf(tank.getBlockPos()).add(s.blocksWide()/2.0,s.blocksHigh()/2.0,s.blocksDeep()/2.0))>24*24;
        double W=s.blocksWide(),H=s.blocksHigh()*.88,D=s.blocksDeep();
        var solid=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));var trim=sprite("dark_oak_planks");
        box(solid,poses.last(),trim,.01,.02,.01,W-.01,.10,D-.01,0xFFFFFF,light,overlay);
        box(solid,poses.last(),trim,.01,H-.07,.01,W-.01,H,D-.01,0xFFFFFF,light,overlay);
        for(double x:new double[]{.01,W-.05})for(double z:new double[]{.01,D-.05})
            box(solid,poses.last(),trim,x,.10,z,x+.04,H-.07,z+.04,0xCCCCCC,light,overlay);
        poses.pushPose();poses.translate(.08,.10,.08);
        poses.scale((float)((W-.16)/(s.width-2)),(float)((H-.24)/(s.height-2)),(float)((D-.16)/(s.depth-2)));
        poses.translate(-1,-1,-1);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        var sand=sprite(d.gravel?"gravel":"sand");
        if(d.substrate)for(int x=1;x<s.width-1;x++)for(int z=1;z<s.depth-1;z++)box(out,poses.last(),sand,x,1.005,z,x+1,1.08,z+1,0xFFFFFF,light,overlay);
        d.ensureScape();
        for(var piece:d.scape.pieces()){
            double x=1+piece.x()*(s.width-2),z=1+piece.z()*(s.depth-2);
            poses.pushPose();poses.translate(x,1.08+piece.y()*(s.height-2),z);poses.mulPose(Axis.YP.rotationDegrees(piece.rotation()*90));poses.scale((float)piece.scaleX(),(float)piece.scaleY(),(float)piece.scaleZ());
            if(piece.material()==AquariumScape.Material.ROCK){
                box(out,poses.last(),sprite("cobblestone"),-.25,0,-.23,.25,.28,.23,0xFFFFFF,light,overlay);
                box(out,poses.last(),sprite("stone"),-.18,.28,-.17,.14,.45,.17,0xFFFFFF,light,overlay);
            }else if(piece.material()==AquariumScape.Material.WOOD){
                box(out,poses.last(),sprite("oak_log"),-.4,0,-.10,.35,.17,.10,0xFFFFFF,light,overlay);
                box(out,poses.last(),sprite("oak_log"),-.10,.10,-.08,.08,.5,.08,0xFFFFFF,light,overlay);
            }else if(piece.material()==AquariumScape.Material.BLOCK){
                var id=ResourceLocation.tryParse(piece.block());
                if(id!=null){
                    poses.translate(-.25,0,-.25);poses.scale(.5f,.5f,.5f);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState(),poses,buffers,light,overlay);
                    out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                }
            }else{
                var plant=sprite(piece.material()==AquariumScape.Material.KELP?"kelp":"seagrass");double h=(piece.material()==AquariumScape.Material.KELP?.7:.35)*(s.height-2);
                double sway=Math.sin((tank.getLevel().getGameTime()+partial)*.035+x*3+z)*.07;
                quad(out,poses.last(),plant,new Vec3(-.22,0,0),new Vec3(.22,0,0),new Vec3(.22+sway,h,0),new Vec3(-.22+sway,h,0),0xA9CF75,light,overlay);
                quad(out,poses.last(),plant,new Vec3(0,0,-.22),new Vec3(0,0,.22),new Vec3(sway,h,.22),new Vec3(sway,h,-.22),0xA9CF75,light,overlay);
            }
            poses.popPose();
        }
        if(d.filter)box(out,poses.last(),sprite("iron_block"),1.05,1.15,1.05,1.25,s.height-1.1,1.35,0x808C96,light,overlay);
        if(d.algae>15){
            var moss=sprite("moss_block");double amount=d.algae/100.0;
            // Bounded patches on the inside front glass, rather than another transparent shell.
            for(int i=0;i<4;i++){
                double x=1.1+i*(s.width-2.3)/4,z=1.005;
                quad(out,poses.last(),moss,new Vec3(x,1.08,z),new Vec3(x+.25*amount,1.08,z),new Vec3(x+.25*amount,1.08+.7*amount,z),new Vec3(x,1.08+.7*amount,z),0x6C883D,light,overlay);
            }
        }
        poses.popPose();
        TextureAtlasSprite texture=null;
        out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        // Internal aquarium lamp provides a minimum light level, without full-bright fish.
        light=Math.max(light&0xFFFF,10<<4) | (Math.max((light>>>16)&0xFFFF,10<<4)<<16);
        double time=tank.getLevel()==null?0:tank.getLevel().getGameTime()+partial;
        if(distant)time=Math.floor(time/20)*20;
        for(var fish:d.fish()){
            var p=AquariumMotion.pose(fish,s,time,d.filled);
            poses.pushPose();double fx=Math.clamp(.08+(p.x()-1)/(s.width-2)*(W-.16),.3,W-.3);
            double fz=Math.clamp(.08+(p.z()-1)/(s.depth-2)*(D-.16),.28,D-.28);
            double fy=.10+(p.y()-1)/(s.height-2)*(H-.24);
            double feeding=Math.clamp((time-tank.fedAt)/160.0,0,1),attraction= d.filled && feeding>0 && feeding<1?Math.sin(feeding*Math.PI):0;
            fx=fx*(1-attraction*.75)+W*.5*attraction*.75;fz=fz*(1-attraction*.75)+D*.5*attraction*.75;fy=fy*(1-attraction)+Math.max(.3,H-.32)*attraction;
            poses.translate(fx,fy-.10*fish.size(),fz);
            if(!d.filled)poses.mulPose(Axis.XP.rotationDegrees(65));
            fishModels.render(fish,tank.getLevel(),time,partial,(float)(-90-Math.toDegrees(p.yaw())),poses,buffers,light);
            poses.popPose();
        }

        if(AquariumOrbit.active() && AquariumOrbit.tank()==tank && AquariumOrbit.selected!=null){
            for(var piece:d.scape.pieces())if(piece.id().equals(AquariumOrbit.selected)){
                var b=AquariumScape.bounds(piece,s);LevelRenderer.renderLineBox(poses,buffers.getBuffer(RenderType.lines()),b.x(),b.y(),b.z(),b.X(),b.Y(),b.Z(),1,.8f,.2f,1);
            }
        }
        if(d.filled && time-tank.fedAt>=0 && time-tank.fedAt<160){out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));for(int i=0;i<7;i++){double px=W*.5+Math.sin(i*2.4)*.12,pz=D*.5+Math.cos(i*2.4)*.1,py=H-.14-(time-tank.fedAt)*.001;box(out,poses.last(),null,px,py,pz,px+.018,py+.012,pz+.018,0xAD7541,light,overlay);}}
        // Draw transparent shells after the opaque contents: their depth writes
        // must not hide the substrate, decorations or residents.
        if(d.filled && d.filter){var bubble=buffers.getBuffer(RenderType.entityTranslucent(WHITE));for(int i=0;i<5;i++){double by=.16+((time*.008+i*.19)%(H-.28));double bx=.15+Math.sin(time*.04+i)*.015;box(bubble,poses.last(),null,bx,by,.16,bx+.018,by+.018,.178,0x779ACEE5,light,overlay);}}
        if(d.filled)box(buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),poses.last(),sprite("water_still"),.05,.105,.05,W-.05,H-.13,D-.05,d.quality<50?0x88577748:0x804878CD,light,overlay);
        var glass=buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));var glassTexture=sprite("glass");
        box(glass,poses.last(),glassTexture,.04,.10,.04,W-.04,H-.07,.047,0x99FFFFFF,light,overlay);
        box(glass,poses.last(),glassTexture,.04,.10,D-.047,W-.04,H-.07,D-.04,0x99FFFFFF,light,overlay);
        box(glass,poses.last(),glassTexture,.04,.10,.047,.047,H-.07,D-.047,0x99FFFFFF,light,overlay);
        box(glass,poses.last(),glassTexture,W-.047,.10,.047,W-.04,H-.07,D-.047,0x99FFFFFF,light,overlay);

    }
    private static void box(VertexConsumer out,PoseStack.Pose pose,TextureAtlasSprite sprite,double x,double y,double z,double X,double Y,double Z,int color,int light,int overlay){
        Vec3 a=new Vec3(x,y,z),b=new Vec3(X,y,z),c=new Vec3(X,Y,z),d=new Vec3(x,Y,z);
        Vec3 e=new Vec3(x,y,Z),f=new Vec3(X,y,Z),g=new Vec3(X,Y,Z),h=new Vec3(x,Y,Z);
        quad(out,pose,sprite,a,d,c,b,color,light,overlay);quad(out,pose,sprite,e,f,g,h,color,light,overlay);
        quad(out,pose,sprite,a,e,h,d,color,light,overlay);quad(out,pose,sprite,b,c,g,f,color,light,overlay);
        quad(out,pose,sprite,d,h,g,c,color,light,overlay);quad(out,pose,sprite,a,b,f,e,color,light,overlay);
    }
    private static void quad(VertexConsumer out,PoseStack.Pose pose,TextureAtlasSprite sprite,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,int light,int overlay){
        Vec3 normal=b.subtract(a).cross(c.subtract(a)).normalize();
        vertex(out,pose,a,(sprite==null?0:sprite.getU0()),(sprite==null?1:sprite.getV1()),color,normal,light,overlay);
        vertex(out,pose,b,(sprite==null?1:sprite.getU1()),(sprite==null?1:sprite.getV1()),color,normal,light,overlay);
        vertex(out,pose,c,(sprite==null?1:sprite.getU1()),(sprite==null?0:sprite.getV0()),color,normal,light,overlay);
        vertex(out,pose,d,(sprite==null?0:sprite.getU0()),(sprite==null?0:sprite.getV0()),color,normal,light,overlay);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,float u,float v,int color,Vec3 normal,int light,int overlay){
        out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z).setColor((color>>16)&255,(color>>8)&255,color&255,(color>>>24)==0?255:color>>>24).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
    }
}
