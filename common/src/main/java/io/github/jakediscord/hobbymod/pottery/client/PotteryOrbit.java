package io.github.jakediscord.hobbymod.pottery.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.pottery.*;
import io.github.jakediscord.hobbymod.sculpting.MarbleMesh;
import io.github.jakediscord.hobbymod.sculpting.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** Transparent in-world wheel session, using the same camera hooks and mesh picking as marble. */
public final class PotteryOrbit {
    private static BlockPos target;
    private static ClientLevel world;
    public static float yaw,pitch;
    public static double distance;
    private static int pending=-1,pendingSnapshot,waiting,ticks,lastSend=-10,lastPedal=-30;
    private static SculptureOrbit.CameraPose lastClear;
    public static MarbleMesh.Hit hit;
    public static boolean active(){return target!=null;}
    public static BlockPos target(){return target;}
    public static PotteryBlockEntity wheel(){var mc=Minecraft.getInstance();return active() && mc.level==world && world.getBlockEntity(target) instanceof PotteryBlockEntity w && w.wheel()?w:null;}
    public static void begin(BlockPos pos){
        var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null)return;
        SculptureOrbit.close();target=pos.immutable();world=mc.level;pending=-1;hit=null;lastClear=null;
        yaw=mc.player.getYRot();pitch=30;distance=1.8;lastPedal=ticks;mc.setScreen(new PotteryControls());
    }
    public static void close(){target=null;world=null;hit=null;pending=-1;lastClear=null;var mc=Minecraft.getInstance();if(mc.screen instanceof PotteryControls)mc.setScreen(null);}
    public static void tick(Minecraft mc){
        if(!active())return;ticks++;var w=wheel();
        if(w==null || w.piece==null || mc.player==null || !mc.player.isAlive() || mc.player.isSpectator() || mc.player.hurtTime>0
                || !PotteryNetworking.permitted(mc.player,target) || (w.piece.stage!=PotteryPiece.Stage.WET && w.piece.stage!=PotteryPiece.Stage.LEATHER_HARD)){close();return;}
        if(pending>=0 && (w.revision!=pending || w.snapshots!=pendingSnapshot || ++waiting>20))pending=-1;
        if(mc.screen==null)mc.setScreen(new PotteryControls());
        if(mc.screen instanceof PotteryControls && pending<0 && ticks-lastPedal>=20 && ticks-lastSend>=2){send(0,0,0,true);lastPedal=ticks;}
    }
    public static boolean send(double t,double push,double lift,boolean pedal){
        var w=wheel();if(w==null || pending>=0 || ticks-lastSend<2)return false;
        pending=w.revision;pendingSnapshot=w.snapshots;waiting=0;lastSend=ticks;
        NetworkManager.sendToServer(new PotteryNetworking.Shape(target,pending,t,push,lift,pedal));return true;
    }
    public static SculptureOrbit.CameraPose cameraPose(){
        var w=wheel();if(w==null || w.piece==null)return null;var mc=Minecraft.getInstance();
        Vec3 center=new Vec3(target.getX()+.5,target.getY()+.55+w.piece.shape.height()*.5,target.getZ()+.5);
        double a=Math.toRadians(yaw),p=Math.toRadians(pitch);
        Vec3 desired=center.add(Math.sin(a)*Math.cos(p)*distance,Math.sin(p)*distance,-Math.cos(a)*Math.cos(p)*distance);
        BlockGetter surrounding=new BlockGetter(){
            public BlockEntity getBlockEntity(BlockPos p){return p.equals(target)?null:world.getBlockEntity(p);}
            public BlockState getBlockState(BlockPos p){return p.equals(target)?Blocks.AIR.defaultBlockState():world.getBlockState(p);}
            public FluidState getFluidState(BlockPos p){return p.equals(target)?Fluids.EMPTY.defaultFluidState():world.getFluidState(p);}
            public int getHeight(){return world.getHeight();}public int getMinBuildHeight(){return world.getMinBuildHeight();}
        };
        var obstacle=surrounding.clip(new ClipContext(center,desired,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
        if(obstacle.getType()!=HitResult.Type.MISS){
            if(center.distanceTo(obstacle.getLocation())<.65){
                if(lastClear!=null && surrounding.clip(new ClipContext(center,lastClear.position(),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS)return lastClear;
                close();return null;
            }
            desired=center.add(desired.subtract(center).normalize().scale(center.distanceTo(obstacle.getLocation())-.15));
        }
        var pose=new SculptureOrbit.CameraPose(desired,yaw,pitch);if(obstacle.getType()==HitResult.Type.MISS)lastClear=pose;return pose;
    }
    public static void pick(double mouseX,double mouseY,float partial){
        hit=null;var w=wheel();var mc=Minecraft.getInstance();if(w==null || w.piece==null)return;
        var camera=mc.gameRenderer.getMainCamera();double fov=((GameRendererAccess)mc.gameRenderer).hobby$getFov(camera,partial,true);
        double tan=Math.tan(Math.toRadians(fov)/2),aspect=mc.getWindow().getWidth()/(double)mc.getWindow().getHeight();
        double x=mouseX/mc.getWindow().getGuiScaledWidth()*2-1,y=1-mouseY/mc.getWindow().getGuiScaledHeight()*2;
        var d=new Vector3f((float)(x*aspect*tan),(float)(y*tan),-1).rotate(camera.rotation()).normalize();
        Vec3 o=camera.getPosition().subtract(target.getX(),target.getY()+.55,target.getZ());
        hit=w.piece.shape.mesh().pick(new double[]{o.x,o.y,o.z},new double[]{d.x,d.y,d.z});
        if(hit!=null){var blocked=world.clip(new ClipContext(camera.getPosition(),camera.getPosition().add(d.x*hit.distance(),d.y*hit.distance(),d.z*hit.distance()),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
            if(blocked.getType()!=HitResult.Type.MISS && !blocked.getBlockPos().equals(target))hit=null;}
    }
}
