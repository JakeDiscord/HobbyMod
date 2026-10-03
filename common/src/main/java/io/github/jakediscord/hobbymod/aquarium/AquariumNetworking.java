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
    public record Action(BlockPos pos,int slot,int resident,boolean remove) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","aquarium_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeInt(p.slot);b.writeInt(p.resident);b.writeBoolean(p.remove);},b->new Action(b.readBlockPos(),b.readInt(),b.readInt(),b.readBoolean()));
        public Type<Action> type(){return TYPE;}
    }
    private static final java.util.Map<ServerPlayer,Long> LAST=new java.util.WeakHashMap<>();
    public static void register(){
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
            if(a.resident>=0 && a.resident<tank.data.fish().size())tank.data.selected=a.resident;
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
