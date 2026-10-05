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
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var below=c.getLevel().getBlockState(c.getClickedPos().below());var above=c.getLevel().getBlockState(c.getClickedPos().above());return defaultBlockState().setValue(FACING,below.getBlock()==this?below.getValue(FACING):above.getBlock()==this?above.getValue(FACING):c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WineRackBlockEntity(p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,2,15,16,14);}
    public static int slot(BlockState state,BlockPos pos,BlockHitResult hit){double dx=hit.getLocation().x-pos.getX()-.5,dz=hit.getLocation().z-pos.getZ()-.5;double x=switch(state.getValue(FACING)){case NORTH->dx;case SOUTH->-dx;case WEST->-dz;default->dz;};return (hit.getLocation().y-pos.getY()>=.5?2:0)+(x>=0?1:0);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!stack.is(WineryContent.BOTTLE.get()))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide && GrapeTrellisBlock.permitted(player,pos) && level.getBlockEntity(pos) instanceof WineRackBlockEntity rack && rack.insert(stack,slot(state,pos,hit))){if(!player.getAbilities().instabuild)stack.shrink(1);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1);}
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!player.getMainHandItem().isEmpty())return InteractionResult.PASS;
        if(!level.isClientSide && GrapeTrellisBlock.permitted(player,pos) && level.getBlockEntity(pos) instanceof WineRackBlockEntity rack){var bottle=rack.take(slot(state,pos,hit));if(!bottle.isEmpty() && !player.addItem(bottle))player.drop(bottle,false);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock()) && !l.isClientSide && l.getBlockEntity(p) instanceof WineRackBlockEntity rack)for(var bottle:rack.bottles)if(!bottle.isEmpty())popResource(l,p,bottle);super.onRemove(s,l,p,next,moving);}
}
