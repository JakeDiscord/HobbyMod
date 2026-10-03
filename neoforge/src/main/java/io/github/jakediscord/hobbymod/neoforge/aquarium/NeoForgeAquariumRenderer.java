package io.github.jakediscord.hobbymod.neoforge.aquarium;

import io.github.jakediscord.hobbymod.aquarium.AquariumBlockEntity;
import io.github.jakediscord.hobbymod.aquarium.client.AquariumRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** NeoForge's frustum checks query the renderer extension, not the block entity. */
public final class NeoForgeAquariumRenderer extends AquariumRenderer {
    public NeoForgeAquariumRenderer(BlockEntityRendererProvider.Context context){super(context);}
    @Override public AABB getRenderBoundingBox(AquariumBlockEntity tank){return tank.getRenderBoundingBox();}
}
