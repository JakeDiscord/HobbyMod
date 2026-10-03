package io.github.jakediscord.hobbymod.bonsai;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class BonsaiPotBlock extends BaseEntityBlock {
    public static final MapCodec<BonsaiPotBlock> CODEC=simpleCodec(BonsaiPotBlock::new);
    public BonsaiPotBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,16,15);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,0,3,13,3,13);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BonsaiBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,BonsaiContent.TREE.get(),BonsaiBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof BonsaiBlockEntity tree))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(level.isClientSide)return ItemInteractionResult.SUCCESS;
        BonsaiGraph g=tree.graph; boolean changed=false;
        SoundEvent sound=SoundEvents.AZALEA_LEAVES_PLACE;
        if(!g.planted()){
            BonsaiGraph.Species species=stack.is(Items.OAK_SAPLING)?BonsaiGraph.Species.OAK:stack.is(Items.BIRCH_SAPLING)?BonsaiGraph.Species.BIRCH:stack.is(Items.CHERRY_SAPLING)?BonsaiGraph.Species.CHERRY:null;
            if(species!=null){g.plant(species,level.random.nextLong());consume(stack,player);changed=true;}
            else if(stack.isEmpty())player.displayClientMessage(Component.literal("Plant oak, birch or cherry."),true);
        }else if(stack.is(Items.WATER_BUCKET)){
            g.water();if(!player.getAbilities().instabuild)player.setItemInHand(hand,new ItemStack(Items.BUCKET));changed=true;sound=SoundEvents.BUCKET_EMPTY;
        }else if(stack.is(Items.SHEARS)){
            if(player.isShiftKeyDown()){
                if(g.rootPrune()){changed=true;player.displayClientMessage(Component.literal("Roots pruned."),true);}
                else player.displayClientMessage(Component.literal("Roots need more time."),true);
            }else{
                int id=target(g,player,pos);
                BonsaiGraph.ShearResult result=g.shear(id);changed=result.changed();
                player.displayClientMessage(Component.literal(result.leavesRemoved()?"Leaves removed.":result.branchesCut()>0?"Branch pruned.":id==1?"Base trunk protected.":"Aim at a branch."),true);
            }
            if(changed){stack.hurtAndBreak(1,player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);sound=SoundEvents.SHEEP_SHEAR;}
        }else if(stack.is(Items.COPPER_INGOT)){
            int id=target(g,player,pos);BonsaiGraph.Node n=g.node(id);
            if(n!=null && id>1 && n.health>0){boolean intact=g.wire(id,player.isShiftKeyDown());consume(stack,player);changed=true;sound=intact?SoundEvents.CHAIN_PLACE:SoundEvents.AZALEA_BREAK;
                player.displayClientMessage(Component.literal(intact?(player.isShiftKeyDown()?"Bent in reverse.":"Branch bent."):"Branch snapped."),true);}
            else player.displayClientMessage(Component.literal("Aim at a living branch."),true);
        }else if(stack.is(Items.DIRT)){
            if(g.repot()){consume(stack,player);changed=true;}
            else player.displayClientMessage(Component.literal("Soil is still fresh."),true);
        }
        if(changed){tree.changed();if(level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,.12,.12,.12,.01);level.playSound(null,pos,sound,SoundSource.BLOCKS,.6F,1.1F);}

        return ItemInteractionResult.CONSUME;
    }
    private static void consume(ItemStack s,Player p){if(!p.getAbilities().instabuild)s.shrink(1);}
    private static int target(BonsaiGraph g,Player player,BlockPos pos){
        var eye=player.getEyePosition().subtract(pos.getX(),pos.getY(),pos.getZ());
        var ray=player.getLookAngle(); int chosen=0; double best=.10*.10;
        for(BonsaiGraph.Node n:g.nodes()){
            if(g.visibleLength(n,0)==0)continue;
            BonsaiGraph.Point a=g.start(n),b=g.end(n);
            // A bounded sample gives reliable selection through the broad block outline.
            for(int i=0;i<=12;i++){
                double t=i/12.0;
                var point=new net.minecraft.world.phys.Vec3(a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t,a.z()+(b.z()-a.z())*t);
                double along=point.subtract(eye).dot(ray);if(along<0 || along>8)continue;
                double distance=point.distanceToSqr(eye.add(ray.scale(along)));
                if(distance<best){best=distance;chosen=n.id;}
            }
        }
        return chosen;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){
        if(!l.isClientSide && l.getBlockEntity(pos) instanceof BonsaiBlockEntity tree && !tree.graph.planted())
            player.displayClientMessage(Component.literal("Plant oak, birch or cherry."),true);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    private static ItemStack preserved(BlockEntity entity){
        ItemStack stack=new ItemStack(BonsaiContent.POT_ITEM.get());
        if(entity instanceof BonsaiBlockEntity t){var tag=t.saveWithId(t.getLevel().registryAccess());stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));}return stack;
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(preserved(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));}
}
