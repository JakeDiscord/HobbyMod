package io.github.jakediscord.hobbymod.sculpting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jakediscord.hobbymod.sculpting.MarbleVolume;
import io.github.jakediscord.hobbymod.sculpting.SculptureBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

@Environment(EnvType.CLIENT)
public final class SculptureRenderer implements BlockEntityRenderer<SculptureBlockEntity> {
    public SculptureRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 48; }
    @Override public void render(SculptureBlockEntity sculpture, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.solid());
        var pose = poses.last();
        TextureAtlasSprite raw = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/calcite"));
        TextureAtlasSprite smooth = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/quartz_block_side"));
        for (MarbleVolume.Face face : sculpture.volume().faces()) {
            int axis = face.side() / 2;
            float sign = face.side() % 2 == 1 ? 1 : -1;
            float nx = axis == 0 ? sign : 0, ny = axis == 1 ? sign : 0, nz = axis == 2 ? sign : 0;
            float shade = axis == 1 ? (sign > 0 ? 1 : 0.6F) : (axis == 0 ? 0.8F : 0.9F);
            float inset = (sign > 0 ? MarbleVolume.SIZE - face.plane() : face.plane()) / (float) MarbleVolume.SIZE;
            shade *= 1 - inset * 0.45F;
            TextureAtlasSprite sprite = face.polished() ? smooth : raw;
            for (double[] p : face.vertices()) {
                vertices.addVertex(pose, (float) p[0], (float) p[1], (float) p[2])
                        .setColor(shade, shade, shade, 1)
                        .setUv(sprite.getU((float) p[(axis + 1) % 3]), sprite.getV((float) p[(axis + 2) % 3]))
                        .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
            }
        }
    }
}
