package io.github.jakediscord.hobbymod.sculpting;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;

public final class BlueprintNetworking {
    public record OpenFiles(InteractionHand hand) implements CustomPacketPayload {
        public static final Type<OpenFiles> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","blueprint_files"));
        public static final StreamCodec<RegistryFriendlyByteBuf,OpenFiles> CODEC=StreamCodec.of((b,p)->b.writeEnum(p.hand),b->new OpenFiles(b.readEnum(InteractionHand.class)));
        public Type<OpenFiles> type(){return TYPE;}
    }
    public record Import(InteractionHand hand,byte[] data) implements CustomPacketPayload {
        public static final Type<Import> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","blueprint_import"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Import> CODEC=StreamCodec.of((b,p)->{b.writeEnum(p.hand);b.writeByteArray(p.data);},b->new Import(b.readEnum(InteractionHand.class),b.readByteArray(MarbleBlueprint.MAX_COMPRESSED)));
        public Type<Import> type(){return TYPE;}
    }
    private static final java.util.Map<net.minecraft.world.entity.player.Player,Long> LAST_IMPORT=new java.util.WeakHashMap<>();
    public static void register(){
        if(Platform.getEnvironment()==Env.SERVER)NetworkManager.registerS2CPayloadType(OpenFiles.TYPE,OpenFiles.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Import.TYPE,Import.CODEC,(p,c)->c.queue(()->{
            var player=c.getPlayer();var stack=player.getItemInHand(p.hand);
            if(!(stack.getItem() instanceof MarbleBlueprintItem) || !player.isAlive() || player.isSpectator())return;
            long now=player.level().getGameTime();Long previous=LAST_IMPORT.get(player);if(previous!=null && now-previous<20)return;LAST_IMPORT.put(player,now);
            try{MarbleBlueprintItem.store(stack,MarbleBlueprint.decode(p.data));player.displayClientMessage(Component.literal("Blueprint imported. Use it on matching marble blocks."),false);}
            catch(IllegalArgumentException e){player.displayClientMessage(Component.literal(e.getMessage()),false);}
        }));
    }
    private BlueprintNetworking(){}
}
