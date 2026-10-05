package io.github.jakediscord.hobbymod.neoforge.astronomy.space;

import io.github.jakediscord.hobbymod.astronomy.space.*;
import io.github.jakediscord.hobbymod.astronomy.space.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="hobbymod",value=Dist.CLIENT)
public final class FlightClientEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(FlightContent.SHIP.get(),ShipRenderer::new);}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){FlightClient.hud(e.getGuiGraphics());}
}
