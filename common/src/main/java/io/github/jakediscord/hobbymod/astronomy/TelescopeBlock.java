package io.github.jakediscord.hobbymod.astronomy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class TelescopeBlock extends BaseEntityBlock {
    public static final MapCodec<TelescopeBlock> CODEC=simpleCodec(TelescopeBlock::new);
    public TelescopeBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced() && (this!=AstronomyContent.OBSERVATORY.get() || c.getLevel().getBlockState(c.getClickedPos().above(2)).canBeReplaced())?defaultBlockState():null;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TelescopeBlockEntity(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.or(Block.box(2,0,2,14,3,14),Block.box(6,3,6,10,s.is(AstronomyContent.OBSERVATORY.get())?25:19,10));}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity e,ItemStack stack){if(l.getBlockEntity(p) instanceof TelescopeBlockEntity b){b.yaw=e.getYRot();b.magnification=b.large()?16:8;b.changed();}}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(p instanceof net.minecraft.server.level.ServerPlayer server)AstronomyNetworking.openTelescope(server,pos);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){if(p instanceof net.minecraft.server.level.ServerPlayer server)AstronomyNetworking.openTelescope(server,pos);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
}
