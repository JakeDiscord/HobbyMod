package io.github.jakediscord.hobbymod.winery;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.shapes.*;

public final class WineryBlock extends BaseEntityBlock {
    public enum Machine implements StringRepresentable { PRESS,BARREL; public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);} }
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<WineryBlock> CODEC=RecordCodecBuilder.mapCodec(i->i.group(propertiesCodec(),StringRepresentable.fromEnum(Machine::values).fieldOf("machine").forGetter(b->b.machine)).apply(i,WineryBlock::new));
    public final Machine machine;
    public WineryBlock(Properties p,Machine machine){super(p);this.machine=machine;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WineryBlockEntity(p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return machine==Machine.PRESS?Shapes.or(Block.box(1,0,1,15,3,15),Block.box(2,3,2,14,10,14),Block.box(1,0,6,3,16,10),Block.box(13,0,6,15,16,10),Block.box(1,14,6,15,16,10)):Block.box(1,0,1,15,16,15);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,WineryContent.MACHINE.get(),WineryBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){open(l,p,player);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){open(l,p,player);return InteractionResult.sidedSuccess(l.isClientSide);}
    private void open(Level l,BlockPos p,Player player){if(!l.isClientSide && GrapeTrellisBlock.permitted(player,p) && player instanceof ServerPlayer server && l.getBlockEntity(p) instanceof WineryBlockEntity b){b.advance();server.openMenu(new SimpleMenuProvider((id,inv,who)->new WineryMenu(id,inv,b),Component.literal(machine==Machine.PRESS?"Grape press":"Fermentation barrel")));}}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock()) && !l.isClientSide && l.getBlockEntity(p) instanceof WineryBlockEntity b){b.advance();for(var item:b.contentsForDrop())popResource(l,p,item);}super.onRemove(s,l,p,next,moving);}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){if(machine==Machine.BARREL && l.getBlockEntity(p) instanceof WineryBlockEntity b && b.batch!=null && b.batch.stage==WineBatch.Stage.FERMENTING && r.nextInt(10)==0){l.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP,p.getX()+.5,p.getY()+1.02,p.getZ()+.5,0,.01,0);if(r.nextInt(3)==0)l.playLocalSound(p,net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,net.minecraft.sounds.SoundSource.BLOCKS,.15F,.8F,false);}}
}
