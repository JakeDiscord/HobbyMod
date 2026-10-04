package io.github.jakediscord.hobbymod.winery;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class WineRackBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<WineRackBlock> CODEC=simpleCodec(WineRackBlock::new);
    public WineRackBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WineRackBlockEntity(p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,11,15);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult h){if(!l.isClientSide && GrapeTrellisBlock.permitted(player,p) && l.getBlockEntity(p) instanceof WineRackBlockEntity rack){if(rack.insert(stack)){if(!player.getAbilities().instabuild)stack.shrink(1);l.playSound(null,p,net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1);}else if(stack.isEmpty())take(rack,player);}return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){if(!l.isClientSide && GrapeTrellisBlock.permitted(player,p) && l.getBlockEntity(p) instanceof WineRackBlockEntity rack)take(rack,player);return InteractionResult.sidedSuccess(l.isClientSide);}
    private void take(WineRackBlockEntity rack,Player p){var s=rack.take();if(!s.isEmpty() && !p.addItem(s))p.drop(s,false);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock()) && !l.isClientSide && l.getBlockEntity(p) instanceof WineRackBlockEntity rack)for(var bottle:rack.bottles)if(!bottle.isEmpty())popResource(l,p,bottle);super.onRemove(s,l,p,next,moving);}
}
