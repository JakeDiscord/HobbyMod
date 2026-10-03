package io.github.jakediscord.hobbymod.neoforge.bonsai;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.bonsai.BonsaiContent;
import io.github.jakediscord.hobbymod.bonsai.client.BonsaiRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=HobbyMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BonsaiClientEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(BonsaiContent.TREE.get(),BonsaiRenderer::new);}
}
