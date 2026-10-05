package io.github.jakediscord.hobbymod.winery;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class GrapeTrellisBlock extends Block implements BonemealableBlock {
    public enum Vine implements StringRepresentable { EMPTY,RED,WHITE;public String getSerializedName(){return name().toLowerCase(Locale.ROOT);} }
    public static final EnumProperty<Vine> VINE=EnumProperty.create("vine",Vine.class);
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,6);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<GrapeTrellisBlock> CODEC=simpleCodec(GrapeTrellisBlock::new);
    public GrapeTrellisBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(VINE,Vine.EMPTY).setValue(AGE,0).setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(VINE,AGE,FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var neighbor=c.getLevel().getBlockState(c.getClickedPos().below());if(!(neighbor.getBlock() instanceof GrapeTrellisBlock))neighbor=c.getLevel().getBlockState(c.getClickedPos().above());return neighbor.getBlock() instanceof GrapeTrellisBlock?defaultBlockState().setValue(FACING,neighbor.getValue(FACING)):defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING).getAxis()==Direction.Axis.Z?Block.box(0,0,6,16,16,10):Block.box(6,0,0,10,16,16);}
    public static boolean soil(LevelReader l,BlockPos p){var cursor=p.below();for(int i=0;i<32 && l.getBlockState(cursor).getBlock() instanceof GrapeTrellisBlock;i++)cursor=cursor.below();var s=l.getBlockState(cursor);return s.is(BlockTags.DIRT) || s.is(Blocks.FARMLAND);}
    private static boolean rooted(LevelReader level,BlockPos pos,Vine cultivar){
        var below=pos.below();
        for(int i=0;i<32;i++){
            var state=level.getBlockState(below);
            if(!(state.getBlock() instanceof GrapeTrellisBlock))return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
            if(state.getValue(VINE)!=cultivar)return false;
            below=below.below();
        }
        return false;
    }
    private static boolean canClimb(LevelReader level,BlockPos pos,BlockState state){
        var next=level.getBlockState(pos.above());
        return state.getValue(AGE)>=2 && next.getBlock() instanceof GrapeTrellisBlock && next.getValue(VINE)==Vine.EMPTY && soil(level,pos.above());
    }
    private static void climb(ServerLevel level,BlockPos pos,BlockState state){
        var above=pos.above();var next=level.getBlockState(above);
        level.setBlock(above,next.setValue(VINE,state.getValue(VINE)).setValue(AGE,0).setValue(FACING,state.getValue(FACING)),3);
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(state.getValue(VINE)==Vine.EMPTY || !rooted(level,pos,state.getValue(VINE)) || level.getRawBrightness(pos,0)<9)return;
        if(random.nextInt(state.getValue(AGE)>=4?10:3)!=0)return;
        if(canClimb(level,pos,state) && level.getRawBrightness(pos.above(),0)>=9)climb(level,pos,state);
        if(state.getValue(AGE)<6)level.setBlock(pos,state.setValue(AGE,state.getValue(AGE)+1),3);
    }
    /** Feeding the base reaches the developing tip, then ripens the clicked section. */
    private static BlockPos growthTip(LevelReader level,BlockPos pos,BlockState state){
        var at=pos;
        for(int i=0;i<32;i++){
            var current=level.getBlockState(at);
            if(current.getValue(AGE)<2 || canClimb(level,at,current))return at;
            var above=level.getBlockState(at.above());
            if(!(above.getBlock() instanceof GrapeTrellisBlock) || above.getValue(VINE)!=state.getValue(VINE))break;
            at=at.above();
        }
        return pos;
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state){
        if(state.getValue(VINE)==Vine.EMPTY || !rooted(level,pos,state.getValue(VINE)))return false;
        var tip=growthTip(level,pos,state);var target=level.getBlockState(tip);
        return target.getValue(AGE)<4 || canClimb(level,tip,target);
    }
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state){return true;}
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state){
        if(!isValidBonemealTarget(level,pos,state))return;
        var tip=growthTip(level,pos,state);var target=level.getBlockState(tip);
        if(canClimb(level,tip,target))climb(level,tip,target);
        else level.setBlock(tip,target.setValue(AGE,Math.min(4,target.getValue(AGE)+1)),3);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        boolean red=stack.is(WineryContent.RED_CUTTING.get()),white=stack.is(WineryContent.WHITE_CUTTING.get());
        if(s.getValue(VINE)==Vine.EMPTY && (red || white)){
            var ground=l.getBlockState(pos.below());
            if(!ground.is(BlockTags.DIRT) && !ground.is(Blocks.FARMLAND))return ItemInteractionResult.sidedSuccess(l.isClientSide);
            if(!l.isClientSide && permitted(player,pos)){l.setBlock(pos,s.setValue(VINE,red?Vine.RED:Vine.WHITE).setValue(AGE,0),3);if(!player.getAbilities().instabuild)stack.shrink(1);l.playSound(null,pos,net.minecraft.sounds.SoundEvents.CROP_PLANTED,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1);}
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        if(stack.is(Items.BONE_MEAL) && isValidBonemealTarget(l,pos,s))return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        harvest(s,l,pos,player);return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult h){harvest(s,l,pos,p);return InteractionResult.sidedSuccess(l.isClientSide);}
    private void harvest(BlockState s,Level l,BlockPos pos,Player p){if(l.isClientSide || !permitted(p,pos))return;
        if(s.getValue(VINE)==Vine.EMPTY){return;}
        int age=s.getValue(AGE);if(age<3){return;}
        var grapes=GrapeItem.harvest(s.getValue(VINE)==Vine.RED,age,l.canSeeSky(pos.above()),age==3?3:4);if(!p.addItem(grapes))p.drop(grapes,false);l.setBlock(pos,s.setValue(AGE,2),3);l.playSound(null,pos,net.minecraft.sounds.SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,net.minecraft.sounds.SoundSource.BLOCKS,.8F,1);

    }
    public static boolean permitted(Player p,BlockPos pos){return io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,pos);}
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder params){var out=new ArrayList<ItemStack>();out.add(new ItemStack(WineryContent.TRELLIS_ITEM.get()));if(s.getValue(VINE)!=Vine.EMPTY)out.add(new ItemStack(s.getValue(VINE)==Vine.RED?WineryContent.RED_CUTTING.get():WineryContent.WHITE_CUTTING.get()));return out;}
}
