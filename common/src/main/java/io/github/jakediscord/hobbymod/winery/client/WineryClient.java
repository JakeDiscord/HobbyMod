package io.github.jakediscord.hobbymod.winery.client;

import io.github.jakediscord.hobbymod.winery.*;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class WineryClient {
    public static void init(){
        WineryContent.PRESS_MENU.listen(type->MenuRegistry.registerScreenFactory(type,WineryScreen::new));WineryContent.BARREL_MENU.listen(type->MenuRegistry.registerScreenFactory(type,WineryScreen::new));
        ClientLifecycleEvent.CLIENT_SETUP.register(client->{
            dev.architectury.registry.client.rendering.RenderTypeRegistry.register(RenderType.cutout(),WineryContent.TRELLIS.get(),WineryContent.PRESS.get(),WineryContent.BARREL.get(),WineryContent.RACK.get());
            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(WineryContent.MACHINE.get(),PressRenderer::new);dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(WineryContent.RACK_ENTITY.get(),WineRackRenderer::new);
            dev.architectury.registry.item.ItemPropertiesRegistry.register(WineryContent.BOTTLE.get(),ResourceLocation.fromNamespaceAndPath("hobbymod","wine_color"),(s,l,e,seed)->WineBottleItem.color(s)/2F);
        });
    }
    private WineryClient(){}
}
