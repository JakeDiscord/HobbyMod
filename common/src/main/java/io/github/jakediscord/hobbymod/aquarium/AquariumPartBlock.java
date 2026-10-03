package io.github.jakediscord.hobbymod.aquarium;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Invisible occupied cells belonging to one custom aquarium model. */
public final class AquariumPartBlock extends Block {
    public AquariumPartBlock(Properties properties){super(properties);}
    public static AquariumBlockEntity find(Level level,BlockPos cell){
        for(int y=0;y<2;y++)for(int x=0;x<4;x++)for(int z=0;z<4;z++){
            BlockPos origin=cell.offset(-x,-y,-z);
            if(!level.hasChunkAt(origin))continue;
            if(level.getBlockEntity(origin) instanceof AquariumBlockEntity tank && x<tank.blocksWide()
                    && y<tank.data.size.blocksHigh() && z<tank.blocksDeep())return tank;
        }
        return null;
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var tank=find(level,pos);if(tank!=null)AquariumControllerBlock.open(level,tank.getBlockPos(),player);
        return level.isClientSide?ItemInteractionResult.SUCCESS:ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        var tank=find(level,pos);if(tank!=null)AquariumControllerBlock.open(level,tank.getBlockPos(),player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock()) && !level.isClientSide){
            var tank=find(level,pos);if(tank!=null)level.destroyBlock(tank.getBlockPos(),true);
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
}
