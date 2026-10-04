package io.github.jakediscord.hobbymod.winery.client;

import com.mojang.blaze3d.vertex.*;
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
    @Override public void render(WineryBlockEntity b,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(b.machine()!=WineryBlock.Machine.PRESS || b.getLevel()==null)return;
        float elapsed=b.batch==null?0:Math.min(60,b.pressing+Math.clamp(b.getLevel().getGameTime()-b.pressSyncTime+partial,0,10));float y=11/16F-(float)Math.sin(elapsed/60*Math.PI)*3/16F;
        var s=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WOOD);var v=buffers.getBuffer(RenderType.cutout());var p=pose.last();float min=3/16F,max=13/16F,top=y+1/16F;
        for(int f=0;f<6;f++)for(int i=0;i<4;i++){int[] at=FACES[f][i];v.addVertex(p,min+at[0]*(max-min),y+at[1]*(top-y),min+at[2]*(max-min)).setColor(1F,1F,1F,1F).setUv(i==0 || i==3?s.getU0():s.getU1(),i<2?s.getV1():s.getV0()).setLight(light).setOverlay(overlay).setNormal(p,NORMALS[f][0],NORMALS[f][1],NORMALS[f][2]);}
    }
    @Override public int getViewDistance(){return 32;}
}
