package io.github.jakediscord.hobbymod.winery.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jakediscord.hobbymod.winery.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** A single cached atlas sprite and 24 moving vertices; the rest of the press is a baked block model. */
public final class PressRenderer implements BlockEntityRenderer<WineryBlockEntity> {
    private static final ResourceLocation WOOD=ResourceLocation.withDefaultNamespace("block/oak_planks");
    private static final int[][][] FACES={{{0,0,0},{0,1,0},{1,1,0},{1,0,0}},{{1,0,1},{1,1,1},{0,1,1},{0,0,1}},{{0,0,1},{0,1,1},{0,1,0},{0,0,0}},{{1,0,0},{1,1,0},{1,1,1},{1,0,1}},{{0,1,0},{0,1,1},{1,1,1},{1,1,0}},{{0,0,1},{0,0,0},{1,0,0},{1,0,1}}};
    private static final int[][] NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    public PressRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(WineryBlockEntity b,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(b.getLevel()==null)return;if(b.machine()==WineryBlock.Machine.TUB){tub(b,pose,buffers,light,overlay);return;}if(b.machine()!=WineryBlock.Machine.PRESS)return;
        float elapsed=b.batch==null?0:Math.min(60,b.pressing+Math.clamp(b.getLevel().getGameTime()-b.pressSyncTime+partial,0,10));float y=11/16F-(float)Math.sin(elapsed/60*Math.PI)*3/16F;
        var s=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WOOD);var v=buffers.getBuffer(RenderType.cutout());var p=pose.last();float min=3/16F,max=13/16F,top=y+1/16F;
        for(int f=0;f<6;f++)for(int i=0;i<4;i++){int[] at=FACES[f][i];v.addVertex(p,min+at[0]*(max-min),y+at[1]*(top-y),min+at[2]*(max-min)).setColor(1F,1F,1F,1F).setUv(i==0 || i==3?s.getU0():s.getU1(),i<2?s.getV1():s.getV0()).setLight(light).setOverlay(overlay).setNormal(p,NORMALS[f][0],NORMALS[f][1],NORMALS[f][2]);}
    }
    private static final ResourceLocation[] MASH={ResourceLocation.fromNamespaceAndPath("hobbymod","block/grape_mash_red"),ResourceLocation.fromNamespaceAndPath("hobbymod","block/grape_mash_white"),ResourceLocation.fromNamespaceAndPath("hobbymod","block/grape_mash_rose")};
    private static final ResourceLocation[] JUICE={ResourceLocation.fromNamespaceAndPath("hobbymod","block/grape_juice_red"),ResourceLocation.fromNamespaceAndPath("hobbymod","block/grape_juice_white"),ResourceLocation.fromNamespaceAndPath("hobbymod","block/grape_juice_rose")};
    private static final ResourceLocation WHITE=ResourceLocation.withDefaultNamespace("block/white_concrete");
    private void tub(WineryBlockEntity b,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var batch=b.batch==null?MustBucketItem.batch(b.getItem(3)):b.batch;int red=0,total=0;
        if(batch==null){for(int i=0;i<2;i++){var fruit=b.getItem(i);if(b.fruit(fruit)){total+=fruit.getCount();if(GrapeItem.fruit(fruit).red())red+=fruit.getCount();}}if(total==0)return;}
        int color=batch!=null?batch.kind().ordinal():red*100/total>=75?0:red*100/total<=25?1:2;
        boolean ready=b.tubReady();float progress=ready?1:Math.clamp(b.pressing/120F,0,1);
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ready?JUICE[color]:MASH[color]);var v=buffers.getBuffer(RenderType.cutout());var p=pose.last();float y=(2.5F+progress*2)/16F;
        v.addVertex(p,1/16F,y,1/16F).setColor(1F,1F,1F,1F).setUv(sprite.getU0(),sprite.getV0()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);
        v.addVertex(p,1/16F,y,15/16F).setColor(1F,1F,1F,1F).setUv(sprite.getU0(),sprite.getV1()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);
        v.addVertex(p,15/16F,y,15/16F).setColor(1F,1F,1F,1F).setUv(sprite.getU1(),sprite.getV1()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);
        v.addVertex(p,15/16F,y,1/16F).setColor(1F,1F,1F,1F).setUv(sprite.getU1(),sprite.getV0()).setLight(light).setOverlay(overlay).setNormal(p,0,1,0);
        if(batch!=null){
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-b.getBlockState().getValue(WineryBlock.FACING).toYRot()+180));pose.translate(-.5,0,-.5);
            var white=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
            stripe(pose,v,white,2/16F,14/16F,.16F,.16F,.13F,light,overlay);
            if(progress>0)stripe(pose,v,white,2/16F,(2+12*progress)/16F,ready?.35F:.85F,ready?.85F:.59F,.2F,light,overlay);
            pose.popPose();
        }
    }
    private static void stripe(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer v,net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,float left,float right,float red,float green,float blue,int light,int overlay){
        var p=pose.last();float z=red==.16F?-.001F:-.002F;
        v.addVertex(p,left,2.5F/16,z).setColor(red,green,blue,1F).setUv(sprite.getU0(),sprite.getV1()).setLight(light).setOverlay(overlay).setNormal(p,0,0,-1);
        v.addVertex(p,left,4.5F/16,z).setColor(red,green,blue,1F).setUv(sprite.getU0(),sprite.getV0()).setLight(light).setOverlay(overlay).setNormal(p,0,0,-1);
        v.addVertex(p,right,4.5F/16,z).setColor(red,green,blue,1F).setUv(sprite.getU1(),sprite.getV0()).setLight(light).setOverlay(overlay).setNormal(p,0,0,-1);
        v.addVertex(p,right,2.5F/16,z).setColor(red,green,blue,1F).setUv(sprite.getU1(),sprite.getV1()).setLight(light).setOverlay(overlay).setNormal(p,0,0,-1);
    }
    @Override public int getViewDistance(){return 32;}
}
