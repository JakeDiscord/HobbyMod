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
    public record Tasting(java.util.List<String> notes) implements CustomPacketPayload {
        public Tasting{notes=java.util.List.copyOf(notes.stream().limit(8).map(WineBatch::clean).toList());}
        public static final Type<Tasting> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","wine_tasting"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Tasting> CODEC=StreamCodec.of((buf,p)->{buf.writeVarInt(p.notes.size());for(var note:p.notes)buf.writeUtf(note,32);},buf->{int n=buf.readVarInt();if(n<0 || n>8)throw new IllegalArgumentException("Invalid tasting sequence");var notes=new java.util.ArrayList<String>();for(int i=0;i<n;i++)notes.add(buf.readUtf(32));return new Tasting(notes);});
        public Type<Tasting> type(){return TYPE;}
    }
    public static void register(){if(dev.architectury.platform.Platform.getEnvironment()==dev.architectury.utils.Env.SERVER)NetworkManager.registerS2CPayloadType(Tasting.TYPE,Tasting.CODEC);NetworkManager.registerReceiver(NetworkManager.Side.C2S,Label.TYPE,Label.CODEC,(packet,context)->context.queue(()->{
        var player=context.getPlayer();if(player.containerMenu instanceof WineryMenu menu && menu.containerId==packet.menu && menu.entity!=null && menu.machine==WineryBlock.Machine.BARREL && menu.stillValid(player) && (menu.entity.batch==null || menu.entity.batch.stage!=WineBatch.Stage.BOTTLED)){menu.entity.bottleLabel=WineBatch.clean(packet.text);menu.entity.setChanged();}
    }));}
    private WineryNetworking(){}
}
