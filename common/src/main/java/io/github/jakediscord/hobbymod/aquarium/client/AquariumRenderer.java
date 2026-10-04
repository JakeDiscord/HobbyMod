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
        var s=tank.data.size;return camera.distanceToSqr(Vec3.atLowerCornerOf(tank.getBlockPos()).add(tank.blocksWide()/2.0,s.blocksHigh()/2.0,tank.blocksDeep()/2.0))<48*48;
    }
    private static TextureAtlasSprite sprite(String name){return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/"+name));}
    @Override public void render(AquariumBlockEntity tank,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        var d=tank.data;var s=d.size;var land=d.terrarium;
        boolean distant=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(Vec3.atLowerCornerOf(tank.getBlockPos()).add(tank.blocksWide()/2.0,s.blocksHigh()/2.0,tank.blocksDeep()/2.0))>24*24;
        double W=s.blocksWide(),H=s.blocksHigh()*.88,D=s.blocksDeep();
        poses.pushPose();
        switch(tank.facing()){
            case NORTH->{poses.translate(W,0,D);poses.mulPose(Axis.YP.rotationDegrees(180));}
            case EAST->{poses.translate(0,0,W);poses.mulPose(Axis.YP.rotationDegrees(90));}
            case WEST->{poses.translate(D,0,0);poses.mulPose(Axis.YP.rotationDegrees(-90));}
            default->{}
        }
        var solid=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));var trim=sprite("dark_oak_planks");
        box(solid,poses.last(),trim,.01,.02,.01,W-.01,.10,D-.01,0xFFFFFF,light,overlay);
        if(land==null)box(solid,poses.last(),trim,.01,H-.07,.01,W-.01,H,D-.01,0xFFFFFF,light,overlay);
        else{
            box(solid,poses.last(),trim,.01,H-.07,.01,W-.01,H,.06,0xFFFFFF,light,overlay);box(solid,poses.last(),trim,.01,H-.07,D-.06,W-.01,H,D-.01,0xFFFFFF,light,overlay);
            box(solid,poses.last(),trim,.01,H-.07,.06,.06,H,D-.06,0xFFFFFF,light,overlay);box(solid,poses.last(),trim,W-.06,H-.07,.06,W-.01,H,D-.06,0xFFFFFF,light,overlay);
            if(!land.open){var mesh=sprite("iron_block");for(double x=.15;x<W-.1;x+=.15)box(solid,poses.last(),mesh,x,H-.02,.06,x+.009,H-.012,D-.06,0x6D786D,light,overlay);}
        }
        for(double x:new double[]{.01,W-.05})for(double z:new double[]{.01,D-.05})
            box(solid,poses.last(),trim,x,.10,z,x+.04,H-.07,z+.04,0xCCCCCC,light,overlay);
        poses.pushPose();poses.translate(.08,.10,.08);
        poses.scale((float)((W-.16)/(s.width-2)),(float)((H-.24)/(s.height-2)),(float)((D-.16)/(s.depth-2)));
        poses.translate(-1,-1,-1);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        var sand=sprite(land==null?(d.gravel?"gravel":"sand"):switch(land.substrate){case SOIL->"dirt";case SAND->"sand";case MOSS->"moss_block";case NONE->"dirt";});
        if(land!=null){
            double drainage=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.drainage(land);
            if(land.drainage)box(out,poses.last(),sprite("gravel"),1,1.005,1,s.width-1,1+drainage,s.depth-1,0xD5D7CF,light,overlay);
            if(d.substrate){
                int nx=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.X,nz=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.Z;
                int tint=land.moisture>60?0xBAC2AA:0xFFFFFF;
                for(int ix=0;ix<nx;ix++)for(int iz=0;iz<nz;iz++){
                    double x=1+ix*(s.width-2)/(double)nx,X=1+(ix+1)*(s.width-2)/(double)nx,z=1+iz*(s.depth-2)/(double)nz,Z=1+(iz+1)*(s.depth-2)/(double)nz;
                    double a=1+land.terrain.vertex(land,ix,iz),b=1+land.terrain.vertex(land,ix,iz+1),c=1+land.terrain.vertex(land,ix+1,iz+1),e=1+land.terrain.vertex(land,ix+1,iz);
                    quad(out,poses.last(),sand,new Vec3(x,a,z),new Vec3(x,b,Z),new Vec3(X,c,Z),new Vec3(X,e,z),tint,light,overlay);
                    double bottom=1+drainage;
                    if(iz==0)quad(out,poses.last(),sand,new Vec3(x,bottom,z),new Vec3(x,a,z),new Vec3(X,e,z),new Vec3(X,bottom,z),tint,light,overlay);
                    if(iz==nz-1)quad(out,poses.last(),sand,new Vec3(x,bottom,Z),new Vec3(X,bottom,Z),new Vec3(X,c,Z),new Vec3(x,b,Z),tint,light,overlay);
                    if(ix==0)quad(out,poses.last(),sand,new Vec3(x,bottom,z),new Vec3(x,bottom,Z),new Vec3(x,b,Z),new Vec3(x,a,z),tint,light,overlay);
                    if(ix==nx-1)quad(out,poses.last(),sand,new Vec3(X,bottom,z),new Vec3(X,e,z),new Vec3(X,c,Z),new Vec3(X,bottom,Z),tint,light,overlay);
                }
            }
        }else if(d.substrate)for(int x=1;x<s.width-1;x++)for(int z=1;z<s.depth-1;z++)box(out,poses.last(),sand,x,1.005,z,x+1,1.08,z+1,0xFFFFFF,light,overlay);
        d.ensureScape();
        for(var piece:d.scape.pieces()){
            double x=1+piece.x()*(s.width-2),z=1+piece.z()*(s.depth-2);
            poses.pushPose();double lift=AquariumScape.precise(piece)?-AquariumScape.extents(piece,s).y():0;
            poses.translate(x,1+(land==null?.08:land.terrain.sample(land,piece.x(),piece.z()))+piece.y()*(s.height-2)+lift,z);poses.mulPose(Axis.YP.rotationDegrees((float)(piece.rotation()*90+piece.yaw())));poses.mulPose(Axis.XP.rotationDegrees((float)piece.pitch()));poses.mulPose(Axis.ZP.rotationDegrees((float)piece.roll()));poses.scale((float)piece.scaleX(),(float)piece.scaleY(),(float)piece.scaleZ());
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
                    // Native block-sheet buffers otherwise flush after the water shell and fail its depth test.
                    // Share the aquascape passes, preserving separate textures for special block renderers.
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState(),poses,type->buffers.getBuffer(
                            type==Sheets.cutoutBlockSheet()?RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS):
                            type==Sheets.translucentCullBlockSheet() || type==Sheets.translucentItemSheet()?
                                    RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS):type),light,overlay);
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
        if(land==null && d.filter)box(out,poses.last(),sprite("iron_block"),1.05,1.15,1.05,1.25,s.height-1.1,1.35,0x808C96,light,overlay);
        if(land==null && d.algae>15){
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
        if(land==null)light=Math.max(light&0xFFFF,10<<4) | (Math.max((light>>>16)&0xFFFF,10<<4)<<16);
        else light=Math.max(light&0xFFFF,(land.heatLamp?14:land.lamp?12:0)<<4) | (light&0xFFFF0000);
        double time=tank.getLevel()==null?0:tank.getLevel().getGameTime()+partial;
        if(distant)time=Math.floor(time/20)*20;
        if(land==null)for(var fish:d.fish()){
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

        if(land!=null){
            if(land.lamp)box(buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)),poses.last(),null,W*.5-.08,H-.05,D*.5-.035,W*.5+.08,H-.025,D*.5+.035,0xEBDD9D,0xF000F0,overlay);
            if(land.heatLamp){
                var hood=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                box(hood,poses.last(),sprite("iron_block"),W*.72-.10,H-.055,D*.5-.085,W*.72+.10,H-.02,D*.5+.085,0x60656B,light,overlay);
                box(hood,poses.last(),sprite("iron_block"),W*.72-.07,H-.12,D*.5-.065,W*.72+.07,H-.055,D*.5+.065,0xCDD0CB,light,overlay);
                box(buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)),poses.last(),null,W*.72-.055,H-.125,D*.5-.05,W*.72+.055,H-.11,D*.5+.05,0xFFCE78,0xF000F0,overlay);
            }
            var agents=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            int[] colors={0x899BA1,0xD8CBA4,0x5F789B,0x9F8678};int[] springColors={0xE0E4D3,0xF0EADB,0xD4DFE5,0xE8D6D0};
            for(var resident:land.residents()){
                var p=io.github.jakediscord.hobbymod.terrarium.TerrariumMotion.pose(resident,distant?Math.floor(time/20)*20:time);
                poses.pushPose();poses.translate(.08+p.x()*(W-.16),.10+land.terrain.sample(land,p.x(),p.z())*(H-.24)/(s.height-2)+.003+p.bob(),.08+p.z()*(D-.16));poses.mulPose(Axis.YP.rotationDegrees((float)io.github.jakediscord.hobbymod.terrarium.TerrariumMotion.modelYaw(p.yaw())));
                float size=resident.age<8?.6f:1;poses.scale(size,size,size);
                boolean isopod=resident.species==io.github.jakediscord.hobbymod.terrarium.TerrariumData.Species.ISOPOD;
                if(isopod){
                    for(int segment=0;segment<5;segment++){double z=-.045+segment*.018,half=segment==0 || segment==4?.025:.035;
                        box(agents,poses.last(),null,-half,.005,z,half,.028,z+.016,colors[resident.variant],light,overlay);
                        if(!distant){box(agents,poses.last(),null,-half-.012,0,z,-half,.007,z+.004,0x343C36,light,overlay);box(agents,poses.last(),null,half,0,z,half+.012,.007,z+.004,0x343C36,light,overlay);}
                    }
                    box(agents,poses.last(),null,-.017,.016,-.052,-.012,.022,-.045,0x20251F,light,overlay);box(agents,poses.last(),null,.012,.016,-.052,.017,.022,-.045,0x20251F,light,overlay);
                }else{box(agents,poses.last(),null,-.009,.002,-.022,.009,.012,.022,springColors[resident.variant],light,overlay);box(agents,poses.last(),null,-.012,.006,-.027,.012,.017,-.009,0xCFD8BD,light,overlay);}
                poses.popPose();
            }
            if(land.humidity>75 && !land.open){
                var droplets=buffers.getBuffer(RenderType.entityTranslucent(WHITE));
                for(int i=0;i<16;i++){double x=.13+(i*.173%(W-.26)),y=.12+(i*.113%(H-.25));double z=i%2==0?.048:D-.048;
                    quad(droplets,poses.last(),null,new Vec3(x,y,z),new Vec3(x+.012,y,z),new Vec3(x+.012,y+.025,z),new Vec3(x,y+.025,z),0x557AC1C3,light,overlay);}
            }
            if(time-tank.fedAt>=0 && time-tank.fedAt<35){var mist=buffers.getBuffer(RenderType.entityTranslucent(WHITE));for(int i=0;i<12;i++){double x=.1+(i*.213%(W-.2)),z=.1+(i*.157%(D-.2)),y=H-.16-(time-tank.fedAt)*.01;box(mist,poses.last(),null,x,y,z,x+.009,y+.009,z+.009,0x6698C7CC,light,overlay);}}
        }

        if(AquariumOrbit.active() && AquariumOrbit.tank()==tank && AquariumOrbit.selected!=null){
            for(var piece:d.scape.pieces())if(piece.id().equals(AquariumOrbit.selected)){
                var b=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.bounds(piece,d);LevelRenderer.renderLineBox(poses,buffers.getBuffer(RenderType.lines()),b.x(),b.y(),b.z(),b.X(),b.Y(),b.Z(),1,.8f,.2f,1);
            }
        }
        if(land==null && d.filled && time-tank.fedAt>=0 && time-tank.fedAt<160){out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));for(int i=0;i<7;i++){double px=W*.5+Math.sin(i*2.4)*.12,pz=D*.5+Math.cos(i*2.4)*.1,py=H-.14-(time-tank.fedAt)*.001;box(out,poses.last(),null,px,py,pz,px+.018,py+.012,pz+.018,0xAD7541,light,overlay);}}
        // Draw transparent shells after the opaque contents: their depth writes
        // must not hide the substrate, decorations or residents.
        if(d.filled && d.filter){var bubble=buffers.getBuffer(RenderType.entityTranslucent(WHITE));for(int i=0;i<5;i++){double by=.16+((time*.008+i*.19)%(H-.28));double bx=.15+Math.sin(time*.04+i)*.015;box(bubble,poses.last(),null,bx,by,.16,bx+.018,by+.018,.178,0x779ACEE5,light,overlay);}}
        if(d.filled)box(buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),poses.last(),sprite("water_still"),.05,.105,.05,W-.05,H-.13,D-.05,d.quality<50?0x88577748:0x804878CD,light,overlay);
        var glass=buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));var glassTexture=sprite("glass");
        box(glass,poses.last(),glassTexture,.04,.10,.04,W-.04,H-.07,.047,0x99FFFFFF,light,overlay);
        box(glass,poses.last(),glassTexture,.04,.10,D-.047,W-.04,H-.07,D-.04,0x99FFFFFF,light,overlay);
        box(glass,poses.last(),glassTexture,.04,.10,.047,.047,H-.07,D-.047,0x99FFFFFF,light,overlay);
        box(glass,poses.last(),glassTexture,W-.047,.10,.047,W-.04,H-.07,D-.047,0x99FFFFFF,light,overlay);

        poses.popPose();
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
