package io.github.jakediscord.hobbymod.sculpting;

import io.github.jakediscord.hobbymod.registry.HobbyContent;
import java.util.ArrayDeque;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SculptureBlockEntity extends BlockEntity {
    private MarbleVolume volume = new MarbleVolume();
    private int revision;
    private final ArrayDeque<History> history = new ArrayDeque<>();
    private VoxelShape shape;
    private record History(UUID artist, MarbleVolume before) {}

    public SculptureBlockEntity(BlockPos pos, BlockState state) {
        super(HobbyContent.SCULPTURE_ENTITY.get(), pos, state);
    }
    public MarbleVolume volume() { return volume; }
    public int revision() { return revision; }

    /** Server-side transaction. Stale operations cannot overwrite another artist's work. */
    public int carve(Player player, ItemStack toolStack, int cell, boolean mirror, int expectedRevision) {
        if (level == null || level.isClientSide || expectedRevision != revision
                || !(toolStack.getItem() instanceof ChiselItem item) || cell < 0 || cell >= MarbleVolume.CELLS
                || !volume.surface(MarbleVolume.x(cell), MarbleVolume.y(cell), MarbleVolume.z(cell))) return 0;
        MarbleVolume before = volume.copy();
        int changed = volume.stroke(cell, item.tool());
        if (mirror && cell >= 0 && cell < MarbleVolume.CELLS) {
            int opposite = MarbleVolume.index(MarbleVolume.SIZE - 1 - MarbleVolume.x(cell), MarbleVolume.y(cell), MarbleVolume.z(cell));
            if (opposite != cell) changed += volume.stroke(opposite, item.tool());
        }
        if (changed == 0) return 0;
        history.addLast(new History(player.getUUID(), before));
        while (history.size() > 12) history.removeFirst();
        if (!player.getAbilities().instabuild) {
            toolStack.hurtAndBreak(1, player, toolStack == player.getOffhandItem()
                    ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        update();
        return changed;
    }

    public boolean undo(Player player, int expectedRevision) {
        if (level == null || level.isClientSide || expectedRevision != revision || history.isEmpty()
                || !history.getLast().artist.equals(player.getUUID())) return false;
        volume = history.removeLast().before;
        update(); // Undo restores the stone, but does not repair worn tools.
        return true;
    }

    private void update() {
        revision++;
        shape = null;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Cached 8^3 collision sampling bounds work per update, even for intricate designs. */
    public VoxelShape shape() {
        if (shape != null) return shape;
        if (volume.count() == MarbleVolume.CELLS) return shape = Shapes.block();
        VoxelShape result = Shapes.empty();
        for (int z = 0; z < 8; z++) for (int y = 0; y < 8; y++) {
            int start = -1;
            for (int x = 0; x <= 8; x++) {
                boolean occupied = false;
                if (x < 8) outer: for (int dx = 0; dx < 4; dx++) for (int dy = 0; dy < 4; dy++) for (int dz = 0; dz < 4; dz++) {
                    if (volume.has(x * 4 + dx, y * 4 + dy, z * 4 + dz)) { occupied = true; break outer; }
                }
                if (occupied && start < 0) start = x;
                if (!occupied && start >= 0) {
                    result = Shapes.or(result, Shapes.box(start / 8.0, y / 8.0, z / 8.0, x / 8.0, (y + 1) / 8.0, (z + 1) / 8.0));
                    start = -1;
                }
            }
        }
        return shape = result.optimize();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray("marble", volume.marbleBits());
        tag.putLongArray("polish", volume.polishBits());
        tag.putInt("revision", revision);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        volume = tag.contains("marble") ? MarbleVolume.read(tag.getLongArray("marble"), tag.getLongArray("polish")) : new MarbleVolume();
        revision = tag.getInt("revision");
        history.clear();
        shape = null;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
