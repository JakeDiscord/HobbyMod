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
public final class AquariumRenderer implements BlockEntityRenderer<AquariumBlockEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("hobbymod","textures/entity/aquarium_white.png");
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
        var solid=buffers.getBuffer(RenderType.entitySolid(WHITE));
        box(solid,poses.last(),null,.01,.02,.01,W-.01,.10,D-.01,0x303D46,light,overlay);
        box(solid,poses.last(),null,.01,H-.07,.01,W-.01,H,D-.01,0x303D46,light,overlay);
        for(double x:new double[]{.01,W-.05})for(double z:new double[]{.01,D-.05})
            box(solid,poses.last(),null,x,.10,z,x+.04,H-.07,z+.04,0x435763,light,overlay);
        var glass=buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        box(glass,poses.last(),null,.04,.10,.04,W-.04,H-.07,.047,0x287AD1DD,light,overlay);
        box(glass,poses.last(),null,.04,.10,D-.047,W-.04,H-.07,D-.04,0x287AD1DD,light,overlay);
        box(glass,poses.last(),null,.04,.10,.047,.047,H-.07,D-.047,0x287AD1DD,light,overlay);
        box(glass,poses.last(),null,W-.047,.10,.047,W-.04,H-.07,D-.047,0x287AD1DD,light,overlay);
        if(d.filled)box(glass,poses.last(),null,.05,.105,.05,W-.05,H-.13,D-.05,0x263696C4,light,overlay);
        poses.pushPose();poses.translate(.08,.10,.08);
        poses.scale((float)((W-.16)/(s.width-2)),(float)((H-.24)/(s.height-2)),(float)((D-.16)/(s.depth-2)));
        poses.translate(-1,-1,-1);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        var sand=sprite("sand");
        if(d.substrate)for(int x=1;x<s.width-1;x++)for(int z=1;z<s.depth-1;z++)box(out,poses.last(),sand,x,1.005,z,x+1,1.08,z+1,0xFFFFFF,light,overlay);
        var rock=sprite("cobblestone");var wood=sprite("oak_log");
        for(int i=0;i<d.rocks;i++){
            double x=1.3+((i*1.71+.4)%(s.width-2.6)),z=1.3+((i*1.19+.7)%(s.depth-2.6));
            box(out,poses.last(),rock,x-.22,1.08,z-.22,x+.22,1.38+(i%3)*.15,z+.22,0xFFFFFF,light,overlay);
        }
        for(int i=0;i<d.wood;i++){
            double x=1.4+((i*1.37+.2)%(s.width-2.8)),z=1.4+((i*1.73+.1)%(s.depth-2.8));
            box(out,poses.last(),wood,x-.32,1.1,z-.10,x+.32,1.28,z+.10,0xFFFFFF,light,overlay);
        }
        var plants=sprite("seagrass");
        for(int i=0;i<d.plants;i++){
            double x=1.3+((i*1.618+.8)%(s.width-2.6)),z=1.3+((i*1.414+.3)%(s.depth-2.6)),h=.35+(i%4)*.15;
            quad(out,poses.last(),plants,new Vec3(x-.20,1.08,z),new Vec3(x+.20,1.08,z),new Vec3(x+.20,1.08+h,z),new Vec3(x-.20,1.08+h,z),0x9CD976,light,overlay);
            if(!distant)quad(out,poses.last(),plants,new Vec3(x,1.08,z-.20),new Vec3(x,1.08,z+.20),new Vec3(x,1.08+h,z+.20),new Vec3(x,1.08+h,z-.20),0x9CD976,light,overlay);
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
        TextureAtlasSprite texture=null;
        out=buffers.getBuffer(RenderType.entitySolid(WHITE));
        // Internal aquarium lamp provides a minimum light level, without full-bright fish.
        light=Math.max(light&0xFFFF,10<<4) | (Math.max((light>>>16)&0xFFFF,10<<4)<<16);
        double time=tank.getLevel()==null?0:tank.getLevel().getGameTime()+partial;
        if(distant)time=Math.floor(time/20)*20;
        for(var fish:d.fish()){
            var p=AquariumMotion.pose(fish,s,time,d.filled);
            poses.pushPose();poses.translate(p.x(),p.y(),p.z());poses.mulPose(Axis.YP.rotation((float)p.yaw()));
            if(!d.filled)poses.mulPose(Axis.XP.rotationDegrees(65));
            float scale=(float)fish.size();poses.scale(scale,scale,scale);
            double length=fish.species==AquariumData.Species.GOLDFISH?.25:.20;
            double height=switch(fish.species){case ANGELFISH->.19;case BETTA->.12;case NEON_TETRA,ZEBRA_DANIO->.065;default->.09;};
            height*=1+(fish.formA+fish.formB-3)*.08;
            int color=fish.health<35?0xA58F7A:fish.color();
            body(out,poses.last(),length,height,color,light,overlay);
            int stripe=fish.species==AquariumData.Species.NEON_TETRA?0x39DDF5:fish.species==AquariumData.Species.ZEBRA_DANIO?0x344668:fish.species==AquariumData.Species.ANGELFISH?0x484851:color;
            if(stripe!=color)for(int side:new int[]{-1,1})quad(out,poses.last(),texture,new Vec3(-length,0,side*.076),new Vec3(length,0,side*.076),new Vec3(length,.035,side*.076),new Vec3(-length,.035,side*.076),stripe,light,overlay);
            double fan=fish.species==AquariumData.Species.BETTA || fish.species==AquariumData.Species.GUPPY?.17:.10;
            fan*=1+(fish.formA+fish.formB)*.05;
            quad(out,poses.last(),texture,new Vec3(-length,0,0),new Vec3(-length-.14,-fan,p.tail()),new Vec3(-length-.14,fan,p.tail()),new Vec3(-length,0,0),color,light,overlay);
            if(!distant){
                quad(out,poses.last(),texture,new Vec3(-.1,height,0),new Vec3(.1,height,0),new Vec3(-.02,height+.1,0),new Vec3(-.1,height,0),color,light,overlay);
                for(int side:new int[]{-1,1})box(out,poses.last(),texture,length-.07,.02,side*.076-.005,length-.035,.052,side*.076+.005,0x101010,light,overlay);
            }
            poses.popPose();
        }
        poses.popPose();
    }
    private static void body(VertexConsumer out,PoseStack.Pose pose,double length,double height,int color,int light,int overlay){
        for(int ring=0;ring<6;ring++)for(int side=0;side<8;side++){
            Vec3[] v=new Vec3[4];int[][] points={{ring,side},{ring+1,side},{ring+1,side+1},{ring,side+1}};
            for(int i=0;i<4;i++){
                double t=Math.PI*(.04+.92*points[i][0]/6.0),angle=2*Math.PI*points[i][1]/8;
                v[i]=new Vec3(-Math.cos(t)*length,Math.sin(t)*Math.sin(angle)*height,Math.sin(t)*Math.cos(angle)*.075);
            }
            quad(out,pose,null,v[0],v[1],v[2],v[3],color,light,overlay);
        }
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
