package io.github.jakediscord.hobbymod.sculpting;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class SculptureNetworking {
    private SculptureNetworking() {}
    public record OpenEditor(BlockPos pos) implements CustomPacketPayload {
        public static final Type<OpenEditor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod", "open_sculpture"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenEditor> CODEC = StreamCodec.of(
                (buffer, packet) -> buffer.writeBlockPos(packet.pos), buffer -> new OpenEditor(buffer.readBlockPos()));
        @Override public Type<OpenEditor> type() { return TYPE; }
    }
    public record Stroke(BlockPos pos, int revision, double x, double y, double z, boolean mirror, boolean continuing) implements CustomPacketPayload {
        public static final Type<Stroke> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod", "carve"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stroke> CODEC = StreamCodec.of((buffer, packet) -> {
            buffer.writeBlockPos(packet.pos); buffer.writeVarInt(packet.revision); buffer.writeDouble(packet.x); buffer.writeDouble(packet.y); buffer.writeDouble(packet.z); buffer.writeBoolean(packet.mirror); buffer.writeBoolean(packet.continuing);
        }, buffer -> new Stroke(buffer.readBlockPos(), buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(), buffer.readBoolean()));
        @Override public Type<Stroke> type() { return TYPE; }
    }

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(OpenEditor.TYPE, OpenEditor.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, Stroke.TYPE, Stroke.CODEC,
                (packet, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, packet);
                }));
    }

    public static ItemStack heldTool(net.minecraft.world.entity.player.Player player) {
        if (player.getMainHandItem().getItem() instanceof ChiselItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof ChiselItem) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static boolean mayEdit(net.minecraft.world.entity.player.Player player,BlockPos pos,ItemStack tool) {
        return tool.getItem() instanceof ChiselItem && !player.isSpectator() && player.isAlive()
                && player.level().mayInteract(player,pos) && player.mayUseItemAt(pos,Direction.UP,tool);
    }
    public static boolean permitted(net.minecraft.world.entity.player.Player player, BlockPos pos, ItemStack tool) {
        return mayEdit(player,pos,tool)
                && player.distanceToSqr(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=36;
    }

    private static void handle(ServerPlayer player, Stroke packet) {
        if (!player.serverLevel().hasChunkAt(packet.pos)) return;
        if (!(player.level().getBlockEntity(packet.pos) instanceof SculptureBlockEntity sculpture)) return;
        ItemStack stack = heldTool(player);
        if (!permitted(player, packet.pos, stack)) return;
        // Limit editing rate per player across sculptures, while allowing ordinary server lag.
        long now = player.level().getGameTime();
        if (!allowStroke(player, now)) { player.connection.send(sculpture.getUpdatePacket()); return; }
        boolean polishes = stack.getItem() instanceof ChiselItem item && item.tool().polishes;
        var end=new net.minecraft.world.phys.Vec3(packet.pos.getX()+packet.x,packet.pos.getY()+packet.y,packet.pos.getZ()+packet.z);
        var last=LAST_POINT.get(player);
        var start=packet.continuing && last!=null && now-last.tick<=20 && last.tool==stack.getItem() && last.mirror==packet.mirror
                && last.dimension.equals(player.level().dimension()) && last.pos.getX()==packet.pos.getX() && last.pos.getZ()==packet.pos.getZ()
                ? last.point:null;
        int changed = sculpture.carvePath(player, stack, packet.x, packet.y, packet.z, packet.mirror, packet.revision,start);
        if (changed > 0) {
            LAST_POINT.put(player,new LastPoint(packet.pos,end,now,stack.getItem(),packet.mirror,player.level().dimension()));
            player.level().playSound(null, packet.pos, polishes
                    ? SoundEvents.GRINDSTONE_USE : SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.35F, 1.15F);
            player.serverLevel().sendParticles(new DustParticleOptions(new org.joml.Vector3f(0.88F,0.86F,0.82F),0.25F),
                    end.x,end.y,end.z,2,0.012,0.012,0.012,0.003);
        } else LAST_POINT.remove(player);
        player.connection.send(sculpture.getUpdatePacket());
    }
    private record LastPoint(BlockPos pos,net.minecraft.world.phys.Vec3 point,long tick,net.minecraft.world.item.Item tool,boolean mirror,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {}
    private static final java.util.Map<ServerPlayer,LastPoint> LAST_POINT=new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerPlayer, Long> LAST_STROKE = new java.util.WeakHashMap<>();
    private static boolean allowStroke(ServerPlayer player, long tick) {
        Long last = LAST_STROKE.get(player);
        if (last != null && tick - last < 2) return false;
        LAST_STROKE.put(player, tick);
        return true;
    }
}
