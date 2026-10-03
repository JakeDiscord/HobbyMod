package io.github.jakediscord.hobbymod.sculpting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.sculpting.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** World camera session. It never moves the player or edits local stone speculatively. */
@Environment(EnvType.CLIENT)
public final class SculptureOrbit {
    private SculptureOrbit() {}
    private static BlockPos target;
    private static ClientLevel world;
    public static float yaw,pitch;
    public static double distance;
    public static boolean mirror;
    private static int pending=-1,waitTicks,ticks,lastStroke=-10;
    private static CameraPose lastClearPose;
    public static MarbleMesh.Hit hit;
    public record CameraPose(Vec3 position,float yaw,float pitch) {}
    public static boolean active() { return target!=null; }
    public static BlockPos target() { return target; }
    public static SculptureBlockEntity sculpture() {
        var mc=Minecraft.getInstance();
        return active() && mc.level==world && world.getBlockEntity(target) instanceof SculptureBlockEntity s ? s : null;
    }
    public static void begin(BlockPos pos) {
        var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null)return;
        target=pos.immutable();world=mc.level;mirror=false;pending=-1;hit=null;lastClearPose=null;frame();
        mc.setScreen(new OrbitControls());
    }
    public static void frame() { var p=Minecraft.getInstance().player;yaw=p==null?0:p.getYRot();pitch=20;distance=2.6; }
    public static void close() {
        target=null;world=null;hit=null;pending=-1;lastClearPose=null;
        var mc=Minecraft.getInstance();if(mc.screen instanceof OrbitControls)mc.setScreen(null);
    }
    public static void tick(Minecraft mc) {
        if(!active())return;
        ticks++;
        var s=sculpture();
        if(s==null || mc.player==null || !mc.player.isAlive() || mc.player.isSpectator()
                || mc.player.hurtTime>0 || mc.player.distanceToSqr(Vec3.atCenterOf(target))>36) { close();return; }
        if(pending>=0 && (s.revision()!=pending || ++waitTicks>20))pending=-1;
        // E opens the real inventory; resume the same camera when it closes.
        if(mc.screen==null)mc.setScreen(new OrbitControls());
    }
    public static void stroke(boolean undo) {
        var mc=Minecraft.getInstance();var s=sculpture();
        if(s==null || pending>=0 || ticks-lastStroke<4 || SculptureNetworking.heldTool(mc.player).isEmpty() || (!undo && hit==null))return;
        pending=s.revision();waitTicks=0;lastStroke=ticks;
        NetworkManager.sendToServer(new SculptureNetworking.Stroke(target,pending,undo?0:hit.x(),undo?0:hit.y(),undo?0:hit.z(),mirror,undo));
    }
    public static CameraPose cameraPose() {
        var mc=Minecraft.getInstance();if(sculptureMissing(mc))return null;
        Vec3 center=Vec3.atCenterOf(target);
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
        Vec3 desired=center.add(Math.sin(y)*Math.cos(p)*distance,Math.sin(p)*distance,-Math.cos(y)*Math.cos(p)*distance);
        // Ignore only the sculpture itself; walls and surrounding blocks still stop the orbit camera.
        BlockGetter surrounding=new BlockGetter() {
            public BlockEntity getBlockEntity(BlockPos pos) { return pos.equals(target)?null:world.getBlockEntity(pos); }
            public BlockState getBlockState(BlockPos pos) { return pos.equals(target)?Blocks.AIR.defaultBlockState():world.getBlockState(pos); }
            public FluidState getFluidState(BlockPos pos) { return pos.equals(target)?Fluids.EMPTY.defaultFluidState():world.getFluidState(pos); }
            public int getHeight() { return world.getHeight(); }
            public int getMinBuildHeight() { return world.getMinBuildHeight(); }
        };
        var obstacle=surrounding.clip(new ClipContext(center,desired,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
        if(obstacle.getType()!=HitResult.Type.MISS) {
            // A camera squeezed against a floor/wall must not end up inside the marble.
            if(center.distanceTo(obstacle.getLocation())<1.05) {
                if(lastClearPose!=null && surrounding.clip(new ClipContext(center,lastClearPose.position(),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS)
                    return lastClearPose;
                close();return null;
            }
            Vec3 direction=desired.subtract(center).normalize();
            desired=center.add(direction.scale(Math.max(0.15,center.distanceTo(obstacle.getLocation())-0.15)));
        }
        CameraPose pose=new CameraPose(desired,yaw,pitch);
        if(obstacle.getType()==HitResult.Type.MISS)lastClearPose=pose;
        return pose;
    }
    private static boolean sculptureMissing(Minecraft mc) { return !active() || mc.level!=world || mc.player==null; }
    public static void pick(double mouseX,double mouseY,float partialTick) {
        var mc=Minecraft.getInstance();var s=sculpture();hit=null;if(s==null || mc.screen==null)return;
        var camera=mc.gameRenderer.getMainCamera();
        double fov=((GameRendererAccess)mc.gameRenderer).hobby$getFov(camera,partialTick,true);
        double tan=Math.tan(Math.toRadians(fov)*0.5),aspect=mc.getWindow().getWidth()/(double)mc.getWindow().getHeight();
        double x=mouseX/mc.getWindow().getGuiScaledWidth()*2-1,y=1-mouseY/mc.getWindow().getGuiScaledHeight()*2;
        Vector3f d=new Vector3f((float)(x*aspect*tan),(float)(y*tan),-1).rotate(camera.rotation()).normalize();
        Vec3 origin=camera.getPosition().subtract(target.getX(),target.getY(),target.getZ());
        hit=s.volume().pick(new double[]{origin.x,origin.y,origin.z},new double[]{d.x,d.y,d.z});
        if(hit!=null) {
            Vec3 end=camera.getPosition().add(d.x*hit.distance(),d.y*hit.distance(),d.z*hit.distance());
            var blocked=world.clip(new ClipContext(camera.getPosition(),end,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
            if(blocked.getType()!=HitResult.Type.MISS && !blocked.getBlockPos().equals(target))hit=null;
        }
    }
    public static void drawBrush(SculptureBlockEntity s,PoseStack poses,MultiBufferSource buffers) {
        var mc=Minecraft.getInstance();
        if(!active() || !(mc.screen instanceof OrbitControls) || !s.getBlockPos().equals(target) || hit==null)return;
        var stack=SculptureNetworking.heldTool(mc.player);if(!(stack.getItem() instanceof ChiselItem item))return;
        drawRing(s,poses,buffers,hit.x(),hit.y(),hit.z(),item.tool().cutRadius,1,0.8F,0.35F);
        if(mirror)drawRing(s,poses,buffers,1-hit.x(),hit.y(),hit.z(),item.tool().cutRadius,0.3F,0.9F,1);
    }
    private static void drawRing(SculptureBlockEntity s,PoseStack poses,MultiBufferSource buffers,double x,double y,double z,double radius,float r,float g,float b) {
        double[] n=s.volume().normal(x,y,z);
        Vec3 normal=new Vec3(n[0],n[1],n[2]);Vec3 u=normal.cross(Math.abs(n[1])>0.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();Vec3 v=normal.cross(u);
        Vec3 center=new Vec3(x,y,z).add(normal.scale(0.004));
        var lines=buffers.getBuffer(RenderType.lines());var pose=poses.last();
        for(int i=0;i<32;i++) {
            double a=i*Math.PI/16,c=(i+1)*Math.PI/16;
            Vec3 p=center.add(u.scale(Math.cos(a)*radius)).add(v.scale(Math.sin(a)*radius));
            Vec3 q=center.add(u.scale(Math.cos(c)*radius)).add(v.scale(Math.sin(c)*radius));Vec3 dir=q.subtract(p).normalize();
            lines.addVertex(pose,(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,1).setNormal(pose,(float)dir.x,(float)dir.y,(float)dir.z);
            lines.addVertex(pose,(float)q.x,(float)q.y,(float)q.z).setColor(r,g,b,1).setNormal(pose,(float)dir.x,(float)dir.y,(float)dir.z);
        }
    }
}
