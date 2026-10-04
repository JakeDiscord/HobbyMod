package io.github.jakediscord.hobbymod.dj;
import com.mojang.serialization.MapCodec;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.*;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.*;
public final class DjBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<DjBlock> CODEC=simpleCodec(DjBlock::new);
    public DjBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DjBlockEntity(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){var monitor=switch(s.getValue(FACING)){case NORTH->Block.box(3,15,13,13,21,14);case SOUTH->Block.box(3,15,2,13,21,3);case EAST->Block.box(2,15,3,3,21,13);default->Block.box(13,15,3,14,21,13);};return Shapes.or(Block.box(0,11,0,16,16,16),Block.box(1,0,1,4,11,4),Block.box(12,0,12,15,11,15),Block.box(1,0,12,4,11,15),Block.box(12,0,1,15,11,4),monitor);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,DjContent.ENTITY.get(),l.isClientSide?io.github.jakediscord.hobbymod.dj.client.DjClient::tickBlock:DjBlockEntity::tickServer);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){open(l,pos,p);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){open(l,pos,p);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    private void open(Level l,BlockPos pos,Player p){if(!l.isClientSide && io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,pos) && l.getBlockEntity(pos) instanceof DjBlockEntity b && p instanceof ServerPlayer server){if(!b.claim(p)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Another DJ is editing this workstation."),true);return;}server.connection.send(b.getUpdatePacket());NetworkManager.sendToPlayer(server,new DjNetworking.State(pos,b.getUpdateTag(l.registryAccess()),"",true));}}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity e,ItemStack stack){var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);if(data!=null && l.getBlockEntity(p) instanceof DjBlockEntity b){var tag=data.copyTag();if(tag.contains("Studio")){b.loadWithComponents(tag.getCompound("Studio"),l.registryAccess());b.changed();}}}
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder params){var item=new ItemStack(DjContent.TABLE.get());if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DjBlockEntity b){var tag=new net.minecraft.nbt.CompoundTag();tag.put("Studio",b.saveWithoutMetadata(params.getLevel().registryAccess()));item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));}return List.of(item);}
}
