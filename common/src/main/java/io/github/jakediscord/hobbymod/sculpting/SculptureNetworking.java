package io.github.jakediscord.hobbymod.sculpting;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
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
    public record Stroke(BlockPos pos, int revision, int cell, int tool, boolean mirror, boolean undo) implements CustomPacketPayload {
        public static final Type<Stroke> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod", "carve"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stroke> CODEC = StreamCodec.of((buffer, packet) -> {
            buffer.writeBlockPos(packet.pos); buffer.writeVarInt(packet.revision); buffer.writeVarInt(packet.cell);
            buffer.writeVarInt(packet.tool); buffer.writeBoolean(packet.mirror); buffer.writeBoolean(packet.undo);
        }, buffer -> new Stroke(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean()));
        @Override public Type<Stroke> type() { return TYPE; }
    }

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(OpenEditor.TYPE, OpenEditor.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, Stroke.TYPE, Stroke.CODEC,
                (packet, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, packet);
                }));
    }

    public static ItemStack findTool(net.minecraft.world.entity.player.Player player, int id) {
        if (id < 0 || id >= CarvingTool.values().length) return ItemStack.EMPTY;
        CarvingTool tool = CarvingTool.values()[id];
        if (player.getMainHandItem().getItem() instanceof ChiselItem item && item.tool() == tool) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof ChiselItem item && item.tool() == tool) return player.getOffhandItem();
        for (ItemStack stack : player.getInventory().items) if (stack.getItem() instanceof ChiselItem item && item.tool() == tool) return stack;
        return ItemStack.EMPTY;
    }

    public static boolean permitted(net.minecraft.world.entity.player.Player player, BlockPos pos, ItemStack tool) {
        return !tool.isEmpty() && !player.isSpectator() && player.isAlive()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 36
                && player.level().mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, tool);
    }

    private static void handle(ServerPlayer player, Stroke packet) {
        if (!player.serverLevel().hasChunkAt(packet.pos)) return;
        if (!(player.level().getBlockEntity(packet.pos) instanceof SculptureBlockEntity sculpture)) return;
        ItemStack stack = findTool(player, packet.tool);
        if (!permitted(player, packet.pos, stack)) return;
        // Limit editing rate per player across sculptures, while allowing ordinary server lag.
        long now = player.level().getGameTime();
        if (!allowStroke(player, now)) { player.connection.send(sculpture.getUpdatePacket()); return; }
        if (packet.undo) {
            sculpture.undo(player, packet.revision);
        } else {
            boolean polishes = stack.getItem() instanceof ChiselItem item && item.tool().polishes;
            int changed = sculpture.carve(player, stack, packet.cell, packet.mirror, packet.revision);
            if (changed > 0) {
                player.level().playSound(null, packet.pos, polishes
                        ? SoundEvents.GRINDSTONE_USE : SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5F, 1.15F);
                double x = packet.pos.getX() + (MarbleVolume.x(packet.cell) + 0.5) / MarbleVolume.SIZE;
                double y = packet.pos.getY() + (MarbleVolume.y(packet.cell) + 0.5) / MarbleVolume.SIZE;
                double z = packet.pos.getZ() + (MarbleVolume.z(packet.cell) + 0.5) / MarbleVolume.SIZE;
                player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, io.github.jakediscord.hobbymod.registry.HobbyContent.MARBLE.get().defaultBlockState()),
                        x, y, z, 5, 0.04, 0.04, 0.04, 0.02);
            }
        }
        player.connection.send(sculpture.getUpdatePacket());
    }
    private static final java.util.Map<ServerPlayer, Long> LAST_STROKE = new java.util.WeakHashMap<>();
    private static boolean allowStroke(ServerPlayer player, long tick) {
        Long last = LAST_STROKE.get(player);
        if (last != null && tick - last < 2) return false;
        LAST_STROKE.put(player, tick);
        return true;
    }
}
