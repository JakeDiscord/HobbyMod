package io.github.jakediscord.hobbymod.sculpting.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import io.github.jakediscord.hobbymod.registry.HobbyContent;
import io.github.jakediscord.hobbymod.sculpting.SculptureNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import dev.architectury.event.events.client.ClientTickEvent;

@Environment(EnvType.CLIENT)
public final class SculptureClient {
    private SculptureClient() {}
    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SculptureNetworking.OpenEditor.TYPE, SculptureNetworking.OpenEditor.CODEC,
                (packet, context) -> context.queue(() -> SculptureOrbit.begin(packet.pos())));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,io.github.jakediscord.hobbymod.sculpting.BlueprintNetworking.OpenFiles.TYPE,io.github.jakediscord.hobbymod.sculpting.BlueprintNetworking.OpenFiles.CODEC,(p,c)->c.queue(()->net.minecraft.client.Minecraft.getInstance().setScreen(new BlueprintFilesScreen(p.hand()))));
        ClientTickEvent.CLIENT_POST.register(SculptureOrbit::tick);
        ClientLifecycleEvent.CLIENT_SETUP.register(client ->
                BlockEntityRendererRegistry.register(HobbyContent.SCULPTURE_ENTITY.get(), SculptureRenderer::new));
    }
}
