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
    private final io.github.jakediscord.hobbymod.rendering.client.GeometryRenderCache<SculptureBlockEntity> cache=new io.github.jakediscord.hobbymod.rendering.client.GeometryRenderCache<>();
    public SculptureRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 48; }
    @Override public void render(SculptureBlockEntity sculpture,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        VertexConsumer vertices=buffers.getBuffer(RenderType.solid());var pose=poses.last();
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var raw=atlas.apply(ResourceLocation.withDefaultNamespace("block/calcite"));
        var polished=atlas.apply(ResourceLocation.withDefaultNamespace("block/quartz_block_side"));
        cache.world(sculpture.getLevel());
        var data=cache.get(sculpture,sculpture.mesh(),io.github.jakediscord.hobbymod.rendering.MeshVertexData::marble).data();
        for(int i=0;i<data.length;i+=io.github.jakediscord.hobbymod.rendering.MeshVertexData.STRIDE){
            var sprite=data[i+10]>0?polished:raw;float shade=data[i+8];
            vertices.addVertex(pose,data[i],data[i+1],data[i+2]).setColor(shade,shade,shade,1)
                    .setUv(sprite.getU(data[i+6]),sprite.getV(data[i+7])).setOverlay(overlay).setLight(light)
                    .setNormal(pose,data[i+3],data[i+4],data[i+5]);
        }
        SculptureOrbit.drawBrush(sculpture,poses,buffers);
    }
}
