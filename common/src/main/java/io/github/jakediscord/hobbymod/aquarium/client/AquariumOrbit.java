package io.github.jakediscord.hobbymod.aquarium.client;

import io.github.jakediscord.hobbymod.aquarium.*;
import io.github.jakediscord.hobbymod.sculpting.client.*;
import io.github.jakediscord.hobbymod.pottery.client.PotteryOrbit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** Aquarium orbit uses the same camera hooks as sculpting and pottery. */
public final class AquariumOrbit {
    private static BlockPos target;
    private static boolean previousHud;
    public static java.util.UUID selected;
    public static float yaw,pitch;
    public static double distance;
    public static boolean active(){return target!=null;}
    public static AquariumBlockEntity tank(){var mc=Minecraft.getInstance();return active() && mc.level!=null && mc.level.getBlockEntity(target) instanceof AquariumBlockEntity t?t:null;}
    public static void begin(BlockPos pos){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        SculptureOrbit.close();PotteryOrbit.close();target=pos.immutable();selected=null;
        previousHud=mc.options.hideGui;mc.options.hideGui=true;
        yaw=mc.player.getYRot();pitch=20;var t=tank();distance=t==null?3:Math.max(2,t.data.size.blocksWide()*1.25);
        mc.setScreen(new AquariumEditScreen());
    }
    public static void close(){if(active())Minecraft.getInstance().options.hideGui=previousHud;target=null;selected=null;}
    public static SculptureOrbit.CameraPose cameraPose(){
        var t=tank();var mc=Minecraft.getInstance();if(t==null || mc.player==null)return null;
        var s=t.data.size;Vec3 center=t.toWorld(new Vec3(s.blocksWide()*.5,s.blocksHigh()*.44,s.blocksDeep()*.5));
        double a=Math.toRadians(yaw),p=Math.toRadians(pitch);
        Vec3 desired=center.add(Math.sin(a)*Math.cos(p)*distance,Math.sin(p)*distance,-Math.cos(a)*Math.cos(p)*distance);
        BlockGetter surrounding=new BlockGetter(){
            private boolean inside(BlockPos p){return p.getX()>=target.getX() && p.getX()<target.getX()+t.blocksWide() && p.getY()>=target.getY() && p.getY()<target.getY()+s.blocksHigh() && p.getZ()>=target.getZ() && p.getZ()<target.getZ()+t.blocksDeep();}
            public BlockEntity getBlockEntity(BlockPos p){return inside(p)?null:mc.level.getBlockEntity(p);}
            public BlockState getBlockState(BlockPos p){return inside(p)?Blocks.AIR.defaultBlockState():mc.level.getBlockState(p);}
            public FluidState getFluidState(BlockPos p){return inside(p)?Fluids.EMPTY.defaultFluidState():mc.level.getFluidState(p);}
            public int getHeight(){return mc.level.getHeight();}public int getMinBuildHeight(){return mc.level.getMinBuildHeight();}
        };
        var obstacle=surrounding.clip(new ClipContext(center,desired,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
        if(obstacle.getType()!=HitResult.Type.MISS)desired=center.add(desired.subtract(center).normalize().scale(Math.max(.5,center.distanceTo(obstacle.getLocation())-.15)));
        return new SculptureOrbit.CameraPose(desired,yaw,pitch);
    }
    public static Vec3 ray(double mx,double my,float partial){
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.getMainCamera();
        double fov=((GameRendererAccess)mc.gameRenderer).hobby$getFov(camera,partial,true),tan=Math.tan(Math.toRadians(fov)/2),aspect=mc.getWindow().getWidth()/(double)mc.getWindow().getHeight();
        double x=mx/mc.getWindow().getGuiScaledWidth()*2-1,y=1-my/mc.getWindow().getGuiScaledHeight()*2;
        var v=new Vector3f((float)(x*aspect*tan),(float)(y*tan),-1).rotate(camera.rotation()).normalize();return new Vec3(v.x,v.y,v.z);
    }
    public static java.util.UUID pick(double mx,double my,float partial){
        var t=tank();if(t==null)return null;var mc=Minecraft.getInstance();Vec3 world=mc.gameRenderer.getMainCamera().getPosition();Vec3 o=t.toLocal(world);Vec3 end=t.toLocal(world.add(ray(mx,my,partial).scale(48)));
        double nearest=Double.MAX_VALUE;java.util.UUID found=null;
        for(var piece:t.data.scape.pieces()){
            var b=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.bounds(piece,t.data);var box=new AABB(b.x(),b.y(),b.z(),b.X(),b.Y(),b.Z());
            var hit=box.clip(o,end);double dist=box.contains(o)?0:hit.map(v->v.distanceToSqr(o)).orElse(Double.MAX_VALUE);
            if(dist<nearest){nearest=dist;found=piece.id();}
        }
        return found;
    }
    public static Vec3 terrain(double mx,double my,float partial){
        var t=tank();if(t==null || t.data.terrarium==null || !t.data.substrate)return null;var s=t.data.size;var land=t.data.terrarium;var world=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 o=t.toLocal(world),dir=t.toLocal(world.add(ray(mx,my,partial))).subtract(o);double best=Double.POSITIVE_INFINITY;
        int nx=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.X,nz=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.Z;double ay=(s.blocksHigh()*.88-.24)/(s.height-2);
        for(int x=0;x<nx;x++)for(int z=0;z<nz;z++){
            double X=.08+x*(s.blocksWide()-.16)/nx,XX=.08+(x+1)*(s.blocksWide()-.16)/nx,Z=.08+z*(s.blocksDeep()-.16)/nz,ZZ=.08+(z+1)*(s.blocksDeep()-.16)/nz;
            var a=new Vec3(X,.1+land.terrain.vertex(land,x,z)*ay,Z);var b=new Vec3(X,.1+land.terrain.vertex(land,x,z+1)*ay,ZZ);var c=new Vec3(XX,.1+land.terrain.vertex(land,x+1,z+1)*ay,ZZ);var d=new Vec3(XX,.1+land.terrain.vertex(land,x+1,z)*ay,Z);
            best=Math.min(best,Math.min(triangle(o,dir,a,b,c),triangle(o,dir,a,c,d)));
        }return Double.isFinite(best)?o.add(dir.scale(best)):null;
    }
    private static double triangle(Vec3 o,Vec3 dir,Vec3 a,Vec3 b,Vec3 c){
        var edge=b.subtract(a);var other=c.subtract(a);var cross=dir.cross(other);double det=edge.dot(cross);if(Math.abs(det)<1e-8)return Double.POSITIVE_INFINITY;
        var offset=o.subtract(a);double u=offset.dot(cross)/det;if(u<0 || u>1)return Double.POSITIVE_INFINITY;var q=offset.cross(edge);double v=dir.dot(q)/det;if(v<0 || u+v>1)return Double.POSITIVE_INFINITY;double distance=other.dot(q)/det;return distance>=0?distance:Double.POSITIVE_INFINITY;
    }
    public static Vec3 plane(double mx,double my,float partial,double y){
        Vec3 o=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),d=ray(mx,my,partial);
        if(Math.abs(d.y)<.001)return null;double t=(y-o.y)/d.y;return t<0?null:tank().toLocal(o.add(d.scale(t)));
    }
    private AquariumOrbit(){}
}
