package io.github.jakediscord.hobbymod.sculpting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MarbleStatueBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MarbleStatueBlock> CODEC = simpleCodec(MarbleStatueBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14), Block.box(4, 2, 4, 12, 4, 12),
            Block.box(5, 4, 6, 7, 8, 10), Block.box(9, 4, 6, 11, 8, 10),
            Block.box(4, 8, 5, 12, 12, 11), Block.box(2, 8, 6, 4, 12, 10),
            Block.box(12, 8, 6, 14, 12, 10), Block.box(5, 12, 5, 11, 16, 11));
    private static final VoxelShape SIDE_SHAPE = sidewaysShape();

    private static VoxelShape sidewaysShape() {
        VoxelShape result = Shapes.empty();
        for (AABB box : SHAPE.toAabbs()) {
            result = Shapes.or(result, Shapes.create(new AABB(box.minZ, box.minY, box.minX, box.maxZ, box.maxY, box.maxX)));
        }
        return result;
    }

    public MarbleStatueBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return state.getValue(FACING).getAxis() == Direction.Axis.X ? SIDE_SHAPE : SHAPE; }
}
