package io.github.jakediscord.hobbymod.neoforge.painting;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.painting.PaintingContent;
import io.github.jakediscord.hobbymod.painting.client.PaintingRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=HobbyMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PaintingClientEvents {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(PaintingContent.ENTITY.get(),PaintingRenderer::new);e.registerEntityRenderer(PaintingContent.HANGING.get(),io.github.jakediscord.hobbymod.painting.client.PaintedCanvasRenderer::new);}
    @SubscribeEvent public static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){var extension=new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){private CanvasItemRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new CanvasItemRenderer();return renderer;}};for(var item:PaintingContent.CANVASES.values())e.registerItem(extension,item.get());e.registerItem(extension,PaintingContent.EASEL_ITEM.get());}

}
