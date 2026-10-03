package io.github.jakediscord.hobbymod.pottery.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.pottery.*;
import io.github.jakediscord.hobbymod.sculpting.MarbleMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.inventory.InventoryMenu;

public final class PotteryRenderer implements BlockEntityRenderer<PotteryBlockEntity> {
    private record Key(PotteryShape shape,int detail){}
    private final io.github.jakediscord.hobbymod.rendering.client.GeometryRenderCache<Key> cache=new io.github.jakediscord.hobbymod.rendering.client.GeometryRenderCache<>();
    private static final float[] COS=new float[65],SIN=new float[65];
    static{for(int i=0;i<=64;i++){COS[i]=(float)Math.cos(i*Math.PI/32);SIN[i]=(float)Math.sin(i*Math.PI/32);}}
    public PotteryRenderer(BlockEntityRendererProvider.Context c){}
    @Override public int getViewDistance(){return 48;}
    @Override public void render(PotteryBlockEntity pot,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        boolean onWheel=pot.wheel();
        if(onWheel)wheel(pot,partial,poses,buffers,light,overlay);
        if(pot.piece==null)return;
        poses.pushPose();if(onWheel){poses.translate(.5,.55,.5);poses.mulPose(Axis.YP.rotationDegrees(wheelAngle(pot,partial)));poses.translate(-.5,0,-.5);}else poses.translate(pot.offsetX,pot.offsetY,pot.offsetZ);
        var piece=pot.piece;float r,g,b;
        switch(piece.stage) {
            case WET->{r=.86F;g=.78F;b=.70F;}case LEATHER_HARD->{r=.94F;g=.86F;b=.80F;}
            case DRY->{r=.98F;g=.94F;b=.85F;}case BISQUE->{r=.95F;g=.78F;b=.63F;}
            default->{int color=piece.glaze.getFireworkColor();r=(color>>16&255)/255F;g=(color>>8&255)/255F;b=(color&255)/255F;}
        }
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.withDefaultNamespace(piece.stage==PotteryPiece.Stage.FINISHED?"block/white_concrete":"block/white_terracotta"));
        var out=buffers.getBuffer(RenderType.solid());var pose=poses.last();
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance=camera.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pot.getBlockPos()));
        boolean editing=PotteryOrbit.active() && pot.getBlockPos().equals(PotteryOrbit.target());
        int detail=editing || distance<16?0:distance<144?1:2;
        cache.world(pot.getLevel());
        var data=cache.get(new Key(piece.shape,detail),piece.shape.mesh(detail),io.github.jakediscord.hobbymod.rendering.MeshVertexData::pottery).data();
        // View direction changes once per pot, not twice per vertex. Glaze remains animated.
        double vx=camera.x-pot.getBlockPos().getX()-.5-pot.offsetX,vy=camera.y-pot.getBlockPos().getY()-piece.shape.height()/2-(onWheel?.55:pot.offsetY),vz=camera.z-pot.getBlockPos().getZ()-.5-pot.offsetZ;
        double len=Math.max(1e-9,Math.sqrt(vx*vx+vy*vy+vz*vz));double hx=vx/len-.3,hy=vy/len+.8,hz=vz/len-.5,hl=Math.max(1e-9,Math.sqrt(hx*hx+hy*hy+hz*hz));
        float Hx=(float)(hx/hl),Hy=(float)(hy/hl),Hz=(float)(hz/hl);
        for(int i=0;i<data.length;i+=io.github.jakediscord.hobbymod.rendering.MeshVertexData.STRIDE){
            float shade=Math.clamp(data[i+8]*(onWheel?data[i+9]:1),.35F,1);
            float shine=piece.stage==PotteryPiece.Stage.FINISHED?io.github.jakediscord.hobbymod.rendering.MeshVertexData.shine(data[i+3]*Hx+data[i+4]*Hy+data[i+5]*Hz):0;
            out.addVertex(pose,data[i],data[i+1],data[i+2]).setColor(Math.min(1,r*shade+shine),Math.min(1,g*shade+shine),Math.min(1,b*shade+shine),1)
                    .setUv(sprite.getU(data[i+6]),sprite.getV(data[i+7])).setLight(light).setOverlay(overlay).setNormal(pose,data[i+3],data[i+4],data[i+5]);
        }
        if(pot.containsWater && piece.shape.open()) {
            var water=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.withDefaultNamespace("textures/block/water_still.png")));
            float y=(float)(piece.shape.height()*.87),radius=(float)Math.max(.01,piece.shape.radius(.87)-piece.shape.wall());
            var m=poses.last();
            for(int i=0;i<48;i++){
                double a=i*Math.PI/24,bAngle=(i+1)*Math.PI/24;
                float ax=.5F+(float)Math.cos(a)*radius,az=.5F+(float)Math.sin(a)*radius,bx=.5F+(float)Math.cos(bAngle)*radius,bz=.5F+(float)Math.sin(bAngle)*radius;
                waterVertex(water,m,ax,y,az,light,overlay);waterVertex(water,m,.5F,y,.5F,light,overlay);waterVertex(water,m,.5F,y,.5F,light,overlay);waterVertex(water,m,bx,y,bz,light,overlay);
            }
        }
        if(!pot.flower.isEmpty()) {
            if(pot.flower.getItem() instanceof net.minecraft.world.item.BlockItem plant){
                poses.pushPose();poses.translate(.15,piece.shape.height()-.10,.15);poses.scale(.7F,.7F,.7F);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(plant.getBlock().defaultBlockState(),poses,buffers,light,overlay);poses.popPose();
            }
        }
        poses.popPose();
    }
    private static void wheel(PotteryBlockEntity wheel,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        poses.pushPose();poses.translate(.5,.49,.5);
        // Clay and wheel head share the same angle, including the stopped orientation.
        poses.mulPose(Axis.YP.rotationDegrees(wheelAngle(wheel,partial)));
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var top=atlas.apply(ResourceLocation.withDefaultNamespace("block/smooth_stone"));var edge=atlas.apply(ResourceLocation.withDefaultNamespace("block/smooth_stone_slab_side"));
        var out=buffers.getBuffer(RenderType.solid());var pose=poses.last();
        for(int i=0;i<64;i++) {
            float ax=COS[i]*.43F,az=SIN[i]*.43F,bx=COS[i+1]*.43F,bz=SIN[i+1]*.43F;
            vertex(out,pose,ax,.05F,az,0,1,0,top,ax/.86F+.5F,az/.86F+.5F,light,overlay);
            vertex(out,pose,0,.05F,0,0,1,0,top,.5F,.5F,light,overlay);vertex(out,pose,0,.05F,0,0,1,0,top,.5F,.5F,light,overlay);
            vertex(out,pose,bx,.05F,bz,0,1,0,top,bx/.86F+.5F,bz/.86F+.5F,light,overlay);
            vertex(out,pose,ax,0,az,ax/.43F,0,az/.43F,edge,0,1,light,overlay);vertex(out,pose,ax,.05F,az,ax/.43F,0,az/.43F,edge,0,0,light,overlay);
            vertex(out,pose,bx,.05F,bz,bx/.43F,0,bz/.43F,edge,1,0,light,overlay);vertex(out,pose,bx,0,bz,bx/.43F,0,bz/.43F,edge,1,1,light,overlay);
        }
        poses.popPose();
    }
    private static float wheelAngle(PotteryBlockEntity wheel,float partial){return wheel.spinUntil>0 && wheel.getLevel()!=null?(float)(Math.min(wheel.getLevel().getGameTime()+partial,wheel.spinUntil)%40*9):0;}
    private static void waterVertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,int light,int overlay){out.addVertex(pose,x,y,z).setColor(72,146,210,180).setUv(x,(z/32)).setLight(light).setOverlay(overlay).setNormal(pose,0,1,0);}
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float nx,float ny,float nz,
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,float u,float v,int light,int overlay) {
        out.addVertex(pose,x,y,z).setColor(.88F,.88F,.88F,1).setUv(sprite.getU(u),sprite.getV(v)).setLight(light).setOverlay(overlay).setNormal(pose,nx,ny,nz);
    }
}
