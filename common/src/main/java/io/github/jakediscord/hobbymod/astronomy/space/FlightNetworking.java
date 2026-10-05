package io.github.jakediscord.hobbymod.astronomy.space;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import java.util.*;

public final class FlightNetworking {
    public record Input(int ship,float yaw,float pitch,int forward,int strafe,int vertical) implements CustomPacketPayload {
        public static final Type<Input> TYPE=new Type<>(ResourceLocation.parse("hobbymod:flight_input"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.ship);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeByte(p.forward);b.writeByte(p.strafe);b.writeByte(p.vertical);},b->new Input(b.readVarInt(),b.readFloat(),b.readFloat(),b.readByte(),b.readByte(),b.readByte()));
        public Type<Input> type(){return TYPE;}
    }
    public record Destination(int ship,int target) implements CustomPacketPayload {
        public static final Type<Destination> TYPE=new Type<>(ResourceLocation.parse("hobbymod:flight_destination"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Destination> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.ship);b.writeVarInt(p.target);},b->new Destination(b.readVarInt(),b.readVarInt()));
        public Type<Destination> type(){return TYPE;}
    }
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(ResourceLocation.parse("hobbymod:flight_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.unit(new Open());
        public Type<Open> type(){return TYPE;}
    }
    private record Pending(UUID player,UUID ship,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> level,long until){}
    private static final List<Pending> PENDING=new ArrayList<>();
    public static void remount(ServerPlayer p,PrototypeShip ship){PENDING.removeIf(x->x.player.equals(p.getUUID()));PENDING.add(new Pending(p.getUUID(),ship.getUUID(),ship.level().dimension(),ship.level().getGameTime()+10));}
    public static void open(ServerPlayer p){io.github.jakediscord.hobbymod.astronomy.AstronomyNetworking.send(p,io.github.jakediscord.hobbymod.astronomy.AstronomyNetworking.SYNC,null);NetworkManager.sendToPlayer(p,new Open());}
    public static void register(){
        if(Platform.getEnvironment()==Env.SERVER)NetworkManager.registerS2CPayloadType(Open.TYPE,Open.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Input.TYPE,Input.CODEC,(v,c)->c.queue(()->{if(c.getPlayer() instanceof ServerPlayer p && p.getVehicle() instanceof PrototypeShip ship && ship.getId()==v.ship)ship.controls(p,v.yaw,v.pitch,v.forward,v.strafe,v.vertical);}));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Destination.TYPE,Destination.CODEC,(v,c)->c.queue(()->{if(c.getPlayer() instanceof ServerPlayer p && p.getVehicle() instanceof PrototypeShip ship && ship.getId()==v.ship){if(v.target==-2)open(p);else if(!ship.select(p,v.target))p.displayClientMessage(net.minecraft.network.chat.Component.literal("Discover this planet before charting it"),true);}}));
        dev.architectury.event.events.common.TickEvent.SERVER_POST.register(server->{
            var iterator=PENDING.iterator();while(iterator.hasNext()){var v=iterator.next();var p=server.getPlayerList().getPlayer(v.player);var l=server.getLevel(v.level);if(p==null || l==null){iterator.remove();continue;}if(l.getGameTime()<v.until)continue;var entity=l.getEntity(v.ship);if(p.isAlive() && p.level()==l && entity instanceof PrototypeShip ship && !ship.isVehicle())p.startRiding(ship,true);iterator.remove();}
            for(var p:server.getPlayerList().getPlayers())if(p.isAlive() && SolarMap.voidSpace(p.level().dimension().location().toString()) && p.getY()<p.level().getMinBuildHeight()+8 && !p.isPassenger()){
                var earth=server.overworld();var spawn=earth.getSharedSpawnPos();earth.getChunkAt(spawn);var point=new Vec3(spawn.getX()+.5,earth.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn.getX(),spawn.getZ())+2,spawn.getZ()+.5);
                p.changeDimension(new DimensionTransition(earth,point,Vec3.ZERO,p.getYRot(),0,DimensionTransition.DO_NOTHING));p.fallDistance=0;p.displayClientMessage(net.minecraft.network.chat.Component.literal("Emergency recovery to Earth · your ship remains parked in space"),false);
            }
        });
    }
    private FlightNetworking(){}
}
