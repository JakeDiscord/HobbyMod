package io.github.jakediscord.hobbymod.pottery.client;
import dev.architectury.event.events.client.*;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import io.github.jakediscord.hobbymod.pottery.*;
public final class PotteryClient {
    public static void init(){
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,PotteryNetworking.OpenWheel.TYPE,PotteryNetworking.OpenWheel.CODEC,(p,c)->c.queue(()->PotteryOrbit.begin(p.pos())));
        PotteryContent.KILN_MENU.listen(type->dev.architectury.registry.menu.MenuRegistry.registerScreenFactory(type,KilnScreen::new));
        ClientTickEvent.CLIENT_POST.register(PotteryOrbit::tick);
        ClientLifecycleEvent.CLIENT_SETUP.register(c->{BlockEntityRendererRegistry.register(PotteryContent.PIECE.get(),PotteryRenderer::new);});
    }
    private PotteryClient(){}
}
