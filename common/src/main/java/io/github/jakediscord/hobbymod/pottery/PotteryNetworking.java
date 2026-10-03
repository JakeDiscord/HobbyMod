package io.github.jakediscord.hobbymod.pottery;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PotteryNetworking {
    public record OpenWheel(BlockPos pos) implements CustomPacketPayload {
        public static final Type<OpenWheel> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","open_wheel"));
        public static final StreamCodec<RegistryFriendlyByteBuf,OpenWheel> CODEC=StreamCodec.of((b,p)->b.writeBlockPos(p.pos),b->new OpenWheel(b.readBlockPos()));
        public Type<OpenWheel> type(){return TYPE;}
    }
    public record Shape(BlockPos pos,int revision,double height,double push,double lift,boolean pedal) implements CustomPacketPayload {
        public static final Type<Shape> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","throw_clay"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Shape> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.revision);b.writeDouble(p.height);b.writeDouble(p.push);b.writeDouble(p.lift);b.writeBoolean(p.pedal);},
                b->new Shape(b.readBlockPos(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean()));
        public Type<Shape> type(){return TYPE;}
    }
    public static boolean permitted(Player player,BlockPos pos) {
        return player.isAlive() && !player.isSpectator() && player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=36
                && player.level().mayInteract(player,pos) && player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem());
    }
    private static final java.util.Map<ServerPlayer,Long> LAST=new java.util.WeakHashMap<>();
    public static void register() {
        if(Platform.getEnvironment()==Env.SERVER)NetworkManager.registerS2CPayloadType(OpenWheel.TYPE,OpenWheel.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Shape.TYPE,Shape.CODEC,(p,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer player) || !player.level().hasChunkAt(p.pos) || !permitted(player,p.pos)
                    || !(player.level().getBlockEntity(p.pos) instanceof PotteryBlockEntity wheel) || !wheel.wheel())return;
            long now=player.level().getGameTime();Long last=LAST.get(player);if(last!=null && now-last<2)return;LAST.put(player,now);
            if(p.pedal){if(wheel.piece!=null && wheel.revision==p.revision)wheel.spin();}
            else wheel.work(player,p.height,p.push,p.lift,p.revision);
            player.connection.send(wheel.getUpdatePacket());
        }));
    }
    private PotteryNetworking(){}
}
