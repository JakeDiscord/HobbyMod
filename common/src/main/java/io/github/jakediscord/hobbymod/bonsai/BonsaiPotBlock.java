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
        boolean careReadout=!stack.is(Items.SHEARS) && !stack.is(Items.COPPER_INGOT); SoundEvent sound=SoundEvents.AZALEA_LEAVES_PLACE;
        if(!g.planted()){
            BonsaiGraph.Species species=stack.is(Items.OAK_SAPLING)?BonsaiGraph.Species.OAK:stack.is(Items.BIRCH_SAPLING)?BonsaiGraph.Species.BIRCH:stack.is(Items.CHERRY_SAPLING)?BonsaiGraph.Species.CHERRY:null;
            if(species!=null){g.plant(species,level.random.nextLong());consume(stack,player);changed=true;}
        }else if(stack.is(Items.WATER_BUCKET)){
            g.water();if(!player.getAbilities().instabuild)player.setItemInHand(hand,new ItemStack(Items.BUCKET));changed=true;sound=SoundEvents.BUCKET_EMPTY;
        }else if(stack.is(Items.SHEARS)){
            if(player.isShiftKeyDown()){
                if(g.rootPrune()){changed=true;player.displayClientMessage(Component.literal("Roots pruned. Let the tree recover before pruning again."),true);}
                else player.displayClientMessage(Component.literal("Let the roots recover: root pruning needs 12 growth intervals."),true);
            }else{
                int id=target(g,player,pos);int cut=g.prune(id);changed=cut>0;
                player.displayClientMessage(Component.literal(changed?"Pruned "+cut+" branch segments.":"Aim near a branch; the base trunk is protected."),true);
            }
            if(changed){stack.hurtAndBreak(1,player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);sound=SoundEvents.SHEEP_SHEAR;}
        }else if(stack.is(Items.COPPER_INGOT)){
            int id=target(g,player,pos);BonsaiGraph.Node n=g.node(id);
            if(n!=null && id>1 && n.health>0){boolean intact=g.wire(id,player.isShiftKeyDown());consume(stack,player);changed=true;sound=intact?SoundEvents.CHAIN_PLACE:SoundEvents.AZALEA_BREAK;
                player.displayClientMessage(Component.literal(intact?(player.isShiftKeyDown()?"Reverse bend applied. Repeated bending can snap the branch.":"Forward bend applied. Sneak to reverse; repeated bending can snap the branch."):"The branch snapped under excessive bending."),true);}
            else player.displayClientMessage(Component.literal("Aim near a living branch to wire it."),true);
        }else if(stack.is(Items.DIRT)){
            if(g.repot()){consume(stack,player);changed=true;}
            else player.displayClientMessage(Component.literal("Soil is still fresh; repot after 12 minutes."),true);
        }
        if(changed){tree.changed();if(careReadout)status(player,tree);if(level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,.12,.12,.12,.01);level.playSound(null,pos,sound,SoundSource.BLOCKS,.6F,1.1F);}
        else if(!stack.is(Items.SHEARS) && !stack.is(Items.COPPER_INGOT) && !stack.is(Items.DIRT))status(player,tree);
        return ItemInteractionResult.CONSUME;
    }
    private static void consume(ItemStack s,Player p){if(!p.getAbilities().instabuild)s.shrink(1);}
    private static int target(BonsaiGraph g,Player player,BlockPos pos){
        var eye=player.getEyePosition().subtract(pos.getX(),pos.getY(),pos.getZ());
        var ray=player.getLookAngle(); int chosen=0; double best=.10*.10;
        for(BonsaiGraph.Node n:g.nodes()){
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
    public static Component statusText(BonsaiBlockEntity tree){
        BonsaiGraph g=tree.graph;
        boolean light=tree.getLevel()!=null && tree.getLevel().getMaxLocalRawBrightness(tree.getBlockPos().above())>=9;
        return Component.literal(g.planted()?g.species+" | Age "+g.age+" min | Water "+g.water+"% | Health "+g.health+"% | Soil "+g.soilAge+" min | Roots "+g.rootAge+" min | "+g.careStatus(light):"Plant oak, birch or cherry. Water: bucket; prune: shears; wire: copper; repot: dirt.");
    }
    private static void status(Player player,BonsaiBlockEntity tree){player.displayClientMessage(statusText(tree),true);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide && l.getBlockEntity(pos) instanceof BonsaiBlockEntity t)status(p,t);return InteractionResult.sidedSuccess(l.isClientSide);}
    private static ItemStack preserved(BlockEntity entity){
        ItemStack stack=new ItemStack(BonsaiContent.POT_ITEM.get());
        if(entity instanceof BonsaiBlockEntity t){var tag=t.saveWithId(t.getLevel().registryAccess());stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));}return stack;
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of(preserved(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));}
}
