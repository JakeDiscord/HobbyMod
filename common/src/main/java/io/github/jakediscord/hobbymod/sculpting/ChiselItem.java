package io.github.jakediscord.hobbymod.sculpting;

import io.github.jakediscord.hobbymod.registry.HobbyContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Opens a solid editable blank. Marble is removed only by explicit editor strokes. */
public final class ChiselItem extends Item {
    private final CarvingTool tool;
    public ChiselItem(Properties properties) { this(properties, CarvingTool.DETAIL); }
    public ChiselItem(Properties properties, CarvingTool tool) { super(properties); this.tool = tool; }
    public CarvingTool tool() { return tool; }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || player.isSpectator() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.PASS;
        boolean blank = level.getBlockState(pos).is(HobbyContent.MARBLE.get());
        if (!blank && !level.getBlockState(pos).is(HobbyContent.SCULPTURE.get())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (blank && !level.setBlock(pos, HobbyContent.SCULPTURE.get().defaultBlockState(), Block.UPDATE_ALL)) return InteractionResult.FAIL;
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof SculptureBlockEntity sculpture) {
                serverPlayer.connection.send(sculpture.getUpdatePacket());
                dev.architectury.networking.NetworkManager.sendToPlayer(serverPlayer, new SculptureNetworking.OpenEditor(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hobbymod.carving_tool").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(tool.polishes ? "tooltip.hobbymod.polish" : "tooltip.hobbymod.chip", Math.round(tool.cutRadius * 200))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
