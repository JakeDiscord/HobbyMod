package io.github.jakediscord.hobbymod.sculpting;

import io.github.jakediscord.hobbymod.registry.HobbyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Carves placed marble without consuming another block or touching other stone. */
public final class ChiselItem extends Item {
    public ChiselItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !level.getBlockState(pos).is(HobbyContent.MARBLE.get())
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockState statue = HobbyContent.MARBLE_STATUE.get().defaultBlockState()
                    .setValue(MarbleStatueBlock.FACING, player.getDirection().getOpposite());
            if (!level.setBlock(pos, statue, Block.UPDATE_ALL)) {
                return InteractionResult.FAIL;
            }
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7F, 1.3F);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
