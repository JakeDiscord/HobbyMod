package io.github.jakediscord.hobbymod.painting;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;
public final class PaintingEaselTopBlock extends Block {
    public PaintingEaselTopBlock(Properties p){super(p);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,12,14);}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return l.getBlockState(p.below()).is(PaintingContent.EASEL.get());}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos other){return canSurvive(s,l,p)?s:Blocks.AIR.defaultBlockState();}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){return PaintingContent.EASEL.get().useItemOn(stack,l.getBlockState(pos.below()),l,pos.below(),p,hand,hit);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){return PaintingContent.EASEL.get().useWithoutItem(l.getBlockState(pos.below()),l,pos.below(),p,hit);}
    @Override public BlockState playerWillDestroy(Level l,BlockPos pos,BlockState s,Player p){if(!l.isClientSide && l.getBlockState(pos.below()).is(PaintingContent.EASEL.get()))l.destroyBlock(pos.below(),!p.getAbilities().instabuild,p);return super.playerWillDestroy(l,pos,s,p);}
    @Override protected List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return List.of();}
}
