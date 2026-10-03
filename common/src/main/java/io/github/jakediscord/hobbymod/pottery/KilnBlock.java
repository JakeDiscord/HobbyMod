package io.github.jakediscord.hobbymod.pottery;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class KilnBlock extends BaseEntityBlock {
    public static final MapCodec<KilnBlock> CODEC=simpleCodec(KilnBlock::new);
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public KilnBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new KilnBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,PotteryContent.KILN_ENTITY.get(),KilnBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        open(level,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){
        open(level,pos,player);return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static void open(Level level,BlockPos pos,Player player){
        if(!level.isClientSide && PotteryNetworking.permitted(player,pos) && player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof KilnBlockEntity kiln)
            server.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new KilnMenu(id,inv,kiln),Component.translatable("block.hobbymod.pottery_kiln")));
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof KilnBlockEntity kiln && !level.isClientSide) {
            if(!kiln.vessel.isEmpty())popResource(level,pos,kiln.vessel);
            if(kiln.coalFuel>0)popResource(level,pos,new ItemStack(Items.COAL,kiln.coalFuel));
            if(kiln.fuel>kiln.coalFuel)popResource(level,pos,new ItemStack(Items.CHARCOAL,kiln.fuel-kiln.coalFuel));
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
