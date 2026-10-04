package io.github.jakediscord.hobbymod.aquarium;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;

public final class AquariumNetworking {
    public record Open(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","aquarium_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->b.writeBlockPos(p.pos),b->new Open(b.readBlockPos()));
        public Type<Open> type(){return TYPE;}
    }
    public record Notice(String text) implements CustomPacketPayload {
        public static final Type<Notice> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","aquarium_notice"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Notice> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.text,256),b->new Notice(b.readUtf(256)));
        public Type<Notice> type(){return TYPE;}
    }
    /** Slot -1 selects a resident; otherwise uses an actual inventory stack. */
    public record Action(BlockPos pos,int slot,int resident,boolean remove,java.util.UUID fishId) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","aquarium_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeInt(p.slot);b.writeInt(p.resident);b.writeBoolean(p.remove);b.writeBoolean(p.fishId!=null);if(p.fishId!=null)b.writeUUID(p.fishId);},b->new Action(b.readBlockPos(),b.readInt(),b.readInt(),b.readBoolean(),b.readBoolean()?b.readUUID():null));
        public Type<Action> type(){return TYPE;}
    }
    public record Decor(BlockPos pos,int slot,int index,double x,double z,int rotation) implements CustomPacketPayload {
        public static final Type<Decor> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","aquarium_scape"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Decor> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeInt(p.slot);b.writeInt(p.index);b.writeDouble(p.x);b.writeDouble(p.z);b.writeInt(p.rotation);},b->new Decor(b.readBlockPos(),b.readInt(),b.readInt(),b.readDouble(),b.readDouble(),b.readInt()));
        public Type<Decor> type(){return TYPE;}
    }
    public record Transform(BlockPos pos,java.util.UUID id,double x,double y,double z,int rotation,double sx,double sy,double sz) implements CustomPacketPayload {
        public static final Type<Transform> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","aquarium_transform"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Transform> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeUUID(p.id);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeInt(p.rotation);b.writeDouble(p.sx);b.writeDouble(p.sy);b.writeDouble(p.sz);},b->new Transform(b.readBlockPos(),b.readUUID(),b.readDouble(),b.readDouble(),b.readDouble(),b.readInt(),b.readDouble(),b.readDouble(),b.readDouble()));
        public Type<Transform> type(){return TYPE;}
    }
    public record Angles(BlockPos pos,java.util.UUID id,double yaw,double pitch,double roll) implements CustomPacketPayload {
        public static final Type<Angles> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","habitat_angles"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Angles> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeUUID(p.id);b.writeDouble(p.yaw);b.writeDouble(p.pitch);b.writeDouble(p.roll);},b->new Angles(b.readBlockPos(),b.readUUID(),b.readDouble(),b.readDouble(),b.readDouble()));
        public Type<Angles> type(){return TYPE;}
    }
    public record Terrain(BlockPos pos,double x,double z,double radius,int mode) implements CustomPacketPayload {
        public static final Type<Terrain> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","terrarium_terrain"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Terrain> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeDouble(p.x);b.writeDouble(p.z);b.writeDouble(p.radius);b.writeInt(p.mode);},b->new Terrain(b.readBlockPos(),b.readDouble(),b.readDouble(),b.readDouble(),b.readInt()));
        public Type<Terrain> type(){return TYPE;}
    }
    private static final java.util.Map<ServerPlayer,Long> LAST=new java.util.WeakHashMap<>();
    public static void register(){
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Terrain.TYPE,Terrain.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer player) || !player.level().hasChunkAt(a.pos)
                    || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(player,a.pos)
                    || !(player.level().getBlockEntity(a.pos) instanceof AquariumBlockEntity tank) || tank.data.terrarium==null
                    || tank.inspect()!=AquariumBlockEntity.Condition.READY)return;
            long now=player.level().getGameTime();Long last=LAST.get(player);if(last!=null && now>=last && now-last<2)return;LAST.put(player,now);
            if(tank.data.terrarium.terrain.brush(tank.data.terrarium,a.x,a.z,a.radius,a.mode)){
                io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(tank.data);tank.changed();
            }
            player.connection.send(tank.getUpdatePacket());
        }));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Angles.TYPE,Angles.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer player) || !player.level().hasChunkAt(a.pos)
                    || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(player,a.pos)
                    || !(player.level().getBlockEntity(a.pos) instanceof AquariumBlockEntity tank) || tank.data.terrarium==null)return;
            long now=player.level().getGameTime();Long last=LAST.get(player);if(last!=null && now>=last && now-last<2)return;LAST.put(player,now);
            if(tank.data.scape.angles(a.id,tank.data.size,a.yaw,a.pitch,a.roll)){io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(tank.data);tank.changed();}
            else NetworkManager.sendToPlayer(player,new Notice("Rotation is invalid or the selected piece moved."));
            player.connection.send(tank.getUpdatePacket());
        }));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Transform.TYPE,Transform.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer player) || !player.level().hasChunkAt(a.pos)
                    || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(player,a.pos)
                    || !(player.level().getBlockEntity(a.pos) instanceof AquariumBlockEntity tank))return;
            long now=player.level().getGameTime();Long last=LAST.get(player);if(last!=null && now>=last && now-last<2)return;LAST.put(player,now);
            if((tank.data.terrarium!=null || a.y>=0) && tank.data.scape.transform(a.id,tank.data.size,a.x,a.y,a.z,a.rotation,a.sx,a.sy,a.sz)){io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(tank.data);tank.changed();}
            else NetworkManager.sendToPlayer(player,new Notice("Decoration changed or transform is invalid. Select it again."));
            player.connection.send(tank.getUpdatePacket());
        }));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Decor.TYPE,Decor.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer player) || !player.level().hasChunkAt(a.pos) || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(player,a.pos) || !(player.level().getBlockEntity(a.pos) instanceof AquariumBlockEntity tank))return;
            long now=player.level().getGameTime();Long last=LAST.get(player);if(last!=null && now>=last && now-last<2)return;LAST.put(player,now);
            if(!Double.isFinite(a.x) || !Double.isFinite(a.z))return;
            tank.data.ensureScape();
            if(a.slot>=0 && a.slot<36){if(tank.data.terrarium!=null)io.github.jakediscord.hobbymod.terrarium.TerrariumActions.add(player,tank,player.getInventory().getItem(a.slot),a.x,a.z,a.rotation);
                else AquariumContent.CONTROLLER.get().addDecor(player,tank,player.getInventory().getItem(a.slot),a.x,a.z,a.rotation);}
            else if(a.slot==-1 && a.index>=0 && a.index<tank.data.scape.pieces().size()){
                var p=tank.data.scape.pieces().get(a.index);if(Math.abs(p.x()-a.x)<.000001 && Math.abs(p.z()-a.z)<.000001)AquariumContent.CONTROLLER.get().removeDecor(player,tank,a.index);
            }
            player.inventoryMenu.broadcastChanges();player.connection.send(tank.getUpdatePacket());
        }));
        if(Platform.getEnvironment()==Env.SERVER){
            NetworkManager.registerS2CPayloadType(Open.TYPE,Open.CODEC);
            NetworkManager.registerS2CPayloadType(Notice.TYPE,Notice.CODEC);
        }
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Action.TYPE,Action.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer player) || !player.level().hasChunkAt(a.pos)
                    || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(player,a.pos)
                    || !(player.level().getBlockEntity(a.pos) instanceof AquariumBlockEntity tank))return;
            long now=player.level().getGameTime();Long last=LAST.get(player);if(last!=null && now>=last && now-last<2)return;LAST.put(player,now);
            NetworkManager.sendToPlayer(player,new Notice(""));
            if(tank.data.terrarium!=null && a.slot==-2){tank.data.terrarium.open=!tank.data.terrarium.open;tank.changed();player.connection.send(tank.getUpdatePacket());return;}
            if(a.fishId!=null){int found=-1;for(int i=0;i<tank.data.fish().size();i++)if(tank.data.fish().get(i).id.equals(a.fishId)){found=i;break;}if(found<0){NetworkManager.sendToPlayer(player,new Notice("That fish has moved. Select a resident again."));return;}tank.data.selected=found;}
            else if(a.resident>=0 && a.resident<tank.data.fish().size())tank.data.selected=a.resident;
            if(a.slot>=0 && a.slot<36){
                var inventory=player.getInventory();int previous=inventory.selected;boolean sneaking=player.isShiftKeyDown();
                boolean swap=a.slot!=previous;
                if(swap){var held=inventory.getItem(previous);inventory.setItem(previous,inventory.getItem(a.slot));inventory.setItem(a.slot,held);}
                try{
                    player.setShiftKeyDown(a.remove);
                    AquariumContent.CONTROLLER.get().applyItem(inventory.getItem(previous),tank.getBlockState(),player.level(),a.pos,player,InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(a.pos),Direction.UP,a.pos,false));
                }finally{if(swap){var used=inventory.getItem(previous);inventory.setItem(previous,inventory.getItem(a.slot));inventory.setItem(a.slot,used);}player.setShiftKeyDown(sneaking);}
                player.inventoryMenu.broadcastChanges();
            }
            tank.changed();player.connection.send(tank.getUpdatePacket());
        }));
    }
    private AquariumNetworking(){}
}
