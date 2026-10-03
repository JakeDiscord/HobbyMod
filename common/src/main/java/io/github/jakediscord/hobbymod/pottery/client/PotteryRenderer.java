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
    public PotteryRenderer(BlockEntityRendererProvider.Context c){}
    @Override public int getViewDistance(){return 48;}
    @Override public void render(PotteryBlockEntity pot,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        if(pot.wheel())wheel(pot,partial,poses,buffers,light,overlay);
        if(pot.piece==null)return;
        poses.pushPose();if(pot.wheel())poses.translate(0,.55,0);else poses.translate(pot.offsetX,pot.offsetY,pot.offsetZ);
        var piece=pot.piece;float r,g,b;
        switch(piece.stage) {
            case WET->{r=.61F;g=.43F;b=.34F;}case LEATHER_HARD->{r=.73F;g=.53F;b=.42F;}
            case DRY->{r=.83F;g=.65F;b=.51F;}case BISQUE->{r=.78F;g=.47F;b=.31F;}
            default->{int color=piece.glaze.getFireworkColor();r=(color>>16&255)/255F;g=(color>>8&255)/255F;b=(color&255)/255F;}
        }
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
        var out=buffers.getBuffer(RenderType.solid());var pose=poses.last();
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for(var q:piece.shape.mesh().quads())for(MarbleMesh.Vertex v:q.vertices()) {
            float shade=(float)Math.clamp(.72+.23*v.ny()+.12*v.nx()-.06*v.nz(),.35,1);
            float shine=0;
            if(piece.stage==PotteryPiece.Stage.FINISHED) {
                double vx=camera.x-pot.getBlockPos().getX()-v.x()-pot.offsetX,vy=camera.y-pot.getBlockPos().getY()-v.y()-(pot.wheel()?.55:0),vz=camera.z-pot.getBlockPos().getZ()-v.z()-pot.offsetZ;
                double len=Math.sqrt(vx*vx+vy*vy+vz*vz);
                double hx=vx/len-.3,hy=vy/len+.8,hz=vz/len-.5,hl=Math.sqrt(hx*hx+hy*hy+hz*hz);
                shine=(float)(.38*Math.pow(Math.max(0,(v.nx()*hx+v.ny()*hy+v.nz()*hz)/hl),24));
            }
            out.addVertex(pose,(float)v.x(),(float)v.y(),(float)v.z()).setColor(Math.min(1,r*shade+shine),Math.min(1,g*shade+shine),Math.min(1,b*shade+shine),1)
                    .setUv(sprite.getU((float)v.x()),sprite.getV((float)v.z())).setLight(light).setOverlay(overlay).setNormal(pose,(float)v.nx(),(float)v.ny(),(float)v.nz());
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
        // Rotate the head and its wood grain; the pot remains rotationally symmetric.
        if(wheel.spinUntil>0)poses.mulPose(Axis.YP.rotationDegrees((float)(Math.min(wheel.getLevel().getGameTime()+partial,wheel.spinUntil)%40*9)));
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var top=atlas.apply(ResourceLocation.withDefaultNamespace("block/oak_planks"));var edge=atlas.apply(ResourceLocation.withDefaultNamespace("block/iron_block"));
        var out=buffers.getBuffer(RenderType.solid());var pose=poses.last();
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32,b=(i+1)*Math.PI/32;
            float ax=(float)(Math.cos(a)*.43),az=(float)(Math.sin(a)*.43),bx=(float)(Math.cos(b)*.43),bz=(float)(Math.sin(b)*.43);
            vertex(out,pose,ax,.05F,az,0,1,0,top,ax/.86F+.5F,az/.86F+.5F,light,overlay);
            vertex(out,pose,0,.05F,0,0,1,0,top,.5F,.5F,light,overlay);vertex(out,pose,0,.05F,0,0,1,0,top,.5F,.5F,light,overlay);
            vertex(out,pose,bx,.05F,bz,0,1,0,top,bx/.86F+.5F,bz/.86F+.5F,light,overlay);
            vertex(out,pose,ax,0,az,ax/.43F,0,az/.43F,edge,0,1,light,overlay);vertex(out,pose,ax,.05F,az,ax/.43F,0,az/.43F,edge,0,0,light,overlay);
            vertex(out,pose,bx,.05F,bz,bx/.43F,0,bz/.43F,edge,1,0,light,overlay);vertex(out,pose,bx,0,bz,bx/.43F,0,bz/.43F,edge,1,1,light,overlay);
        }
        poses.popPose();
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float nx,float ny,float nz,
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,float u,float v,int light,int overlay) {
        out.addVertex(pose,x,y,z).setColor(.88F,.88F,.88F,1).setUv(sprite.getU(u),sprite.getV(v)).setLight(light).setOverlay(overlay).setNormal(pose,nx,ny,nz);
    }
}
