package io.github.jakediscord.hobbymod.pottery;

import com.mojang.serialization.MapCodec;
import dev.architectury.networking.NetworkManager;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class PotteryBlock extends BaseEntityBlock {
    public static final MapCodec<PotteryBlock> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i->i.group(
            propertiesCodec(),com.mojang.serialization.Codec.BOOL.fieldOf("wheel").forGetter(b->b.wheel)).apply(i,PotteryBlock::new));
    private final boolean wheel;
    public PotteryBlock(Properties properties,boolean wheel){super(properties);this.wheel=wheel;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public RenderShape getRenderShape(BlockState state){return wheel?RenderShape.MODEL:RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PotteryBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,PotteryContent.PIECE.get(),PotteryBlockEntity::tick);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(wheel)return Block.box(1,0,1,15,9,15);
        if(level.getBlockEntity(pos) instanceof PotteryBlockEntity pot && pot.piece!=null) {
            double radius=0;for(int i=0;i<32;i++)radius=Math.max(radius,pot.piece.shape.radius(i/31.0));
            return Shapes.box(.5-radius+pot.offsetX,pot.offsetY,.5-radius+pot.offsetZ,.5+radius+pot.offsetX,pot.piece.shape.height()+pot.piece.shape.wall()/2+pot.offsetY,.5+radius+pot.offsetZ);
        }
        return Block.box(4,0,4,12,8,12);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(stack.isEmpty())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pot) || !PotteryNetworking.permitted(player,pos))return ItemInteractionResult.FAIL;
        if(level.isClientSide)return ItemInteractionResult.SUCCESS;
        boolean success=false;
        if(wheel && pot.piece==null && stack.is(PotteryContent.CLAY.get())) {
            pot.piece=new PotteryPiece();consume(stack,player);pot.changed();success=true;
        }else if(wheel && pot.piece==null && stack.is(PotteryContent.POT_ITEM.get())) {
            var piece=PotteryPotItem.piece(stack);
            if(piece.stage==PotteryPiece.Stage.WET || piece.stage==PotteryPiece.Stage.LEATHER_HARD || piece.stage==PotteryPiece.Stage.DRY){pot.piece=piece;consume(stack,player);pot.changed();success=true;}
        }else if(wheel && pot.piece!=null && stack.getItem() instanceof PotteryToolItem tool && tool.tool==PotteryToolItem.Tool.WIRE) {
            give(player,preserved(pot));pot.piece=null;pot.spinUntil=0;pot.changed();
            if(!player.getAbilities().instabuild)stack.hurtAndBreak(1,player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            success=true;
        }else if(pot.piece!=null && stack.is(Items.WATER_BUCKET) && pot.piece.rewet()) {
            if(!player.getAbilities().instabuild)player.setItemInHand(hand,new ItemStack(Items.BUCKET));pot.changed();success=true;
        }else if(!wheel && pot.piece!=null && stack.getItem() instanceof GlazeItem glaze && pot.piece.glaze(glaze.color)) {
            consume(stack,player);pot.changed();success=true;
        }else if(!wheel && pot.piece!=null && pot.piece.stage==PotteryPiece.Stage.FINISHED && pot.flower.isEmpty() && stack.is(ItemTags.SMALL_FLOWERS)) {
            pot.flower=stack.copyWithCount(1);consume(stack,player);pot.changed();success=true;
        }else if(!wheel && !pot.flower.isEmpty() && stack.is(Items.SHEARS)) {
            give(player,pot.flower);pot.flower=ItemStack.EMPTY;pot.changed();success=true;
        }else if(wheel && pot.piece!=null && (stack.getItem() instanceof PotteryToolItem || stack.isEmpty())) {
            open(player,pot);return ItemInteractionResult.CONSUME;
        }
        if(!success)status(player,pot);
        return ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!PotteryNetworking.permitted(player,pos))return InteractionResult.FAIL;
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof PotteryBlockEntity pot) {
            if(wheel && pot.piece!=null)open(player,pot);
            else if(!wheel && !player.isShiftKeyDown() && !pot.flower.isEmpty()){give(player,pot.flower);pot.flower=ItemStack.EMPTY;pot.changed();}
            else if(!wheel && player.isShiftKeyDown()){give(player,preserved(pot));level.removeBlock(pos,false);}
            else status(player,pot);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static void open(Player player,PotteryBlockEntity pot) {
        if(pot.piece.stage!=PotteryPiece.Stage.WET && pot.piece.stage!=PotteryPiece.Stage.LEATHER_HARD){status(player,pot);return;}
        pot.spin();if(player instanceof ServerPlayer server){server.connection.send(pot.getUpdatePacket());NetworkManager.sendToPlayer(server,new PotteryNetworking.OpenWheel(pot.getBlockPos()));}
    }
    public static void give(Player player,ItemStack stack){if(!player.getInventory().add(stack))player.drop(stack,false);}
    private static void consume(ItemStack stack,Player player){if(!player.getAbilities().instabuild)stack.shrink(1);}
    private static void status(Player p,PotteryBlockEntity pot){p.displayClientMessage(Component.literal(pot.piece==null?"Empty pottery wheel":pot.piece.description()+(pot.piece.stage==PotteryPiece.Stage.WET?" · "+pot.piece.moisture+"% moisture":"")),true);}
    public static ItemStack preserved(PotteryBlockEntity pot) {
        var stack=PotteryPotItem.create(pot.piece);pot.saveToItem(stack,pot.getLevel().registryAccess());return stack;
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        var drops=new ArrayList<ItemStack>();if(wheel)drops.add(new ItemStack(PotteryContent.WHEEL_ITEM.get()));
        if(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PotteryBlockEntity pot && pot.piece!=null)drops.add(preserved(pot));
        return drops;
    }
}
