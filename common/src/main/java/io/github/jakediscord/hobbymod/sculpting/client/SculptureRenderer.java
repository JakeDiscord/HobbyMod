package io.github.jakediscord.hobbymod.sculpting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jakediscord.hobbymod.sculpting.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

@Environment(EnvType.CLIENT)
public final class SculptureRenderer implements BlockEntityRenderer<SculptureBlockEntity> {
    public SculptureRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 48; }
    @Override public void render(SculptureBlockEntity sculpture,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        VertexConsumer vertices=buffers.getBuffer(RenderType.solid());var pose=poses.last();
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var raw=atlas.apply(ResourceLocation.withDefaultNamespace("block/calcite"));
        var polished=atlas.apply(ResourceLocation.withDefaultNamespace("block/quartz_block_side"));
        for (MarbleMesh.Quad face:sculpture.volume().mesh().quads()) {
            double nx=face.a().nx()+face.b().nx()+face.c().nx()+face.d().nx();
            double ny=face.a().ny()+face.b().ny()+face.c().ny()+face.d().ny();
            double nz=face.a().nz()+face.b().nz()+face.c().nz()+face.d().nz();
            int axis=Math.abs(ny)>Math.abs(nx)?1:0;if(Math.abs(nz)>Math.abs(axis==0?nx:ny))axis=2;
            int finished=(face.a().polished()?1:0)+(face.b().polished()?1:0)+(face.c().polished()?1:0)+(face.d().polished()?1:0);
            // A quad must use one atlas sprite: mixed sprite UVs sample unrelated atlas textures.
            var sprite=finished>=2?polished:raw;
            for (MarbleMesh.Vertex p:face.vertices()) {
                float shade=(float)(0.72+0.18*p.ny()+0.08*p.nx()+0.04*p.nz());
                float u=(float)(axis==0?p.z():p.x()),v=(float)(axis==1?p.z():1-p.y());
                vertices.addVertex(pose,(float)p.x(),(float)p.y(),(float)p.z()).setColor(shade,shade,shade,1)
                        .setUv(sprite.getU(u),sprite.getV(v)).setOverlay(overlay).setLight(light)
                        .setNormal(pose,(float)p.nx(),(float)p.ny(),(float)p.nz());
            }
        }
        SculptureOrbit.drawBrush(sculpture,poses,buffers);
    }
}
