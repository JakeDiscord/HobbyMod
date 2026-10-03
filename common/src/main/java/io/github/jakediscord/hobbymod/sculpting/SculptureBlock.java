package io.github.jakediscord.hobbymod.sculpting;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SculptureBlock extends BaseEntityBlock {
    public static final MapCodec<SculptureBlock> CODEC = simpleCodec(SculptureBlock::new);
    public SculptureBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SculptureBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof SculptureBlockEntity sculpture ? sculpture.shape() : net.minecraft.world.phys.shapes.Shapes.block();
    }
    @Override protected void onPlace(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState oldState,boolean moving) {
        super.onPlace(state,level,pos,oldState,moving);
        if(!level.isClientSide)level.scheduleTick(pos,this,1);
    }
    @Override protected void neighborChanged(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,
            net.minecraft.world.level.block.Block neighbor,BlockPos neighborPos,boolean moving) {
        super.neighborChanged(state,level,pos,neighbor,neighborPos,moving);
        if(!level.isClientSide && neighborPos.getX()==pos.getX() && neighborPos.getZ()==pos.getZ())level.scheduleTick(pos,this,1);
    }
    @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
        // Placement, mining, and support changes must obey the same connected-stone rule as a cut.
        for(BlockPos p:MarbleColumn.positions(level,pos))if(level.getBlockState(p).is(io.github.jakediscord.hobbymod.registry.HobbyContent.MARBLE.get()))
            level.setBlock(p,io.github.jakediscord.hobbymod.registry.HobbyContent.SCULPTURE.get().defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_ALL);
        var sections=MarbleColumn.sculptures(level,pos);
        var before=sections.stream().map(s->s.volume().densityBytes()).toList();
        MarbleColumn.prune(sections.stream().map(SculptureBlockEntity::volume).toList());
        for(int i=0;i<sections.size();i++) {
            var s=sections.get(i);
            if(s.volume().count()==0)level.removeBlock(s.getBlockPos(),false);
            else if(!java.util.Arrays.equals(before.get(i),s.volume().densityBytes()))s.update();
        }
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SculptureBlockEntity sculpture) {
            for (ItemStack stack : drops) sculpture.saveToItem(stack, builder.getLevel().registryAccess());
        }
        return drops;
    }
}
