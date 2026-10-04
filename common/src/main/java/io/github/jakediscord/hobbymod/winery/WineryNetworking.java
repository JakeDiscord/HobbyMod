package io.github.jakediscord.hobbymod.winery;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class WineryNetworking {
    public record Label(int menu,String text) implements CustomPacketPayload {
        public static final Type<Label> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","wine_label"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Label> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeUtf(p.text,32);},b->new Label(b.readVarInt(),b.readUtf(32)));
        public Type<Label> type(){return TYPE;}
    }
    public static void register(){NetworkManager.registerReceiver(NetworkManager.Side.C2S,Label.TYPE,Label.CODEC,(packet,context)->context.queue(()->{
        var player=context.getPlayer();if(player.containerMenu instanceof WineryMenu menu && menu.containerId==packet.menu && menu.entity!=null && menu.machine==WineryBlock.Machine.BARREL && menu.stillValid(player) && (menu.entity.batch==null || menu.entity.batch.stage!=WineBatch.Stage.BOTTLED)){menu.entity.bottleLabel=WineBatch.clean(packet.text);menu.entity.setChanged();}
    }));}
    private WineryNetworking(){}
}
