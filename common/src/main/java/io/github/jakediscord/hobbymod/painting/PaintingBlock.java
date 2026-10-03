package io.github.jakediscord.hobbymod.painting;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.*;
public final class PaintingBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<PaintingBlock> CODEC=RecordCodecBuilder.mapCodec(i->i.group(propertiesCodec(),com.mojang.serialization.Codec.BOOL.fieldOf("easel").forGetter(b->b.easel)).apply(i,PaintingBlock::new));
    private final boolean easel;
    public PaintingBlock(Properties p,boolean easel){super(p);this.easel=easel;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){if(!easel && !c.getClickedFace().getAxis().isHorizontal())return null;if(easel && !c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced())return null;return defaultBlockState().setValue(FACING,easel?c.getHorizontalDirection().getOpposite():c.getClickedFace());}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){Direction support=easel?Direction.DOWN:s.getValue(FACING).getOpposite();return l.getBlockState(p.relative(support)).isFaceSturdy(l,p.relative(support),support.getOpposite());}
    @Override protected BlockState updateShape(BlockState s,Direction direction,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos n){return !canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,direction,neighbor,l,p,n);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PaintingBlockEntity(p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(easel)return Block.box(2,0,2,14,16,14);return switch(s.getValue(FACING)){case NORTH->Block.box(0,0,14,16,16,16);case SOUTH->Block.box(0,0,0,16,16,2);case WEST->Block.box(14,0,0,16,16,16);default->Block.box(0,0,0,2,16,16);};}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity entity,ItemStack stack){super.setPlacedBy(l,p,s,entity,stack);if(easel)l.setBlock(p.above(),PaintingContent.EASEL_TOP.get().defaultBlockState(),3);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(easel && !s.is(next.getBlock()) && l.getBlockState(p.above()).is(PaintingContent.EASEL_TOP.get()))l.removeBlock(p.above(),false);super.onRemove(s,l,p,next,moving);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,pos))return ItemInteractionResult.FAIL;
        if(level.isClientSide)return ItemInteractionResult.SUCCESS;
        if(level.getBlockEntity(pos) instanceof PaintingBlockEntity b){if(easel && b.painting==null && stack.getItem() instanceof CanvasItem){b.painting=CanvasItem.read(stack);if(!p.getAbilities().instabuild)stack.shrink(1);b.changed();open(p,b);}else if(b.painting!=null)open(p,b);else p.displayClientMessage(Component.literal("Mount a canvas on the easel."),true);}return ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,pos))return InteractionResult.FAIL;if(!l.isClientSide && l.getBlockEntity(pos) instanceof PaintingBlockEntity b){if(!easel && p.isShiftKeyDown()){if(b.painting!=null)give(p,CanvasItem.create(b.painting));b.painting=null;l.removeBlock(pos,false);}else if(b.painting!=null)open(p,b);else p.displayClientMessage(Component.literal("Mount a canvas on the easel."),true);}return InteractionResult.sidedSuccess(l.isClientSide);}
    public static void open(Player p,PaintingBlockEntity b){if(p instanceof ServerPlayer server){server.connection.send(b.getUpdatePacket());NetworkManager.sendToPlayer(server,new PaintingNetworking.Open(b.getBlockPos()));}}
    public static void give(Player p,ItemStack s){if(!p.addItem(s))p.drop(s,false);}
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){var drops=new ArrayList<ItemStack>();if(easel)drops.add(new ItemStack(PaintingContent.EASEL_ITEM.get()));if(b.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PaintingBlockEntity e && e.painting!=null)drops.add(CanvasItem.create(e.painting));return drops;}
}
