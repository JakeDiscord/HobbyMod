package io.github.jakediscord.hobbymod.aquarium;

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

public final class AquariumControllerBlock extends BaseEntityBlock {
    public static final MapCodec<AquariumControllerBlock> CODEC=simpleCodec(AquariumControllerBlock::new);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public AquariumControllerBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(LIT,false));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,LIT);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AquariumBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,AquariumContent.TANK_ENTITY.get(),AquariumBlockEntity::tick);}
    private static void message(Player player,String message){
        player.displayClientMessage(Component.literal(message),true);
        if(player instanceof net.minecraft.server.level.ServerPlayer server)
            dev.architectury.networking.NetworkManager.sendToPlayer(server,new AquariumNetworking.Notice(message));
    }
    private static void consume(ItemStack stack,Player player){if(!player.getAbilities().instabuild)stack.shrink(1);}
    private static void refund(Player player,Item item){
        if(player.getAbilities().instabuild)return;
        ItemStack returned=new ItemStack(item);if(!player.addItem(returned))player.drop(returned,false);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        open(level,pos,player);return level.isClientSide?ItemInteractionResult.SUCCESS:ItemInteractionResult.CONSUME;
    }
    public static void open(Level level,BlockPos pos,Player player){
        if(player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof AquariumBlockEntity tank){
            server.connection.send(tank.getUpdatePacket());
            dev.architectury.networking.NetworkManager.sendToPlayer(server,new AquariumNetworking.Open(pos));
        }
    }
    public ItemInteractionResult applyItem(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof AquariumBlockEntity tank))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(level.isClientSide)return ItemInteractionResult.SUCCESS;
        AquariumData d=tank.data;tank.inspect();
        if(d.terrarium!=null)return io.github.jakediscord.hobbymod.terrarium.TerrariumActions.apply(stack,tank,player,hand);
        if(tank.condition==AquariumBlockEntity.Condition.UNLOADED){message(player,"Load the whole tank first.");return ItemInteractionResult.CONSUME;}
        SoundEvent sound=SoundEvents.AZALEA_PLACE;boolean changed=false;
        if(stack.isEmpty()){
            d.selectNext();changed=true;
            if(d.selected()==null)message(player,d.cycle<5?"Cycle water before adding fish.":"Add a compatible fish.");
        }else if(stack.is(Items.WATER_BUCKET)){
            if(tank.condition==AquariumBlockEntity.Condition.BROKEN_GLASS){message(player,"Repair tank walls first.");return ItemInteractionResult.CONSUME;}
            if(tank.water(true)){
                if(!player.getAbilities().instabuild)player.setItemInHand(hand,new ItemStack(Items.BUCKET));
                changed=true;sound=SoundEvents.BUCKET_EMPTY;
            }else message(player,"Clear the interior before filling.");
        }else if(stack.is(Items.BUCKET)){
            if(player.isShiftKeyDown() || d.fish().isEmpty()){
                if(!d.fish().isEmpty()){message(player,"Move your fish into buckets before draining.");return ItemInteractionResult.CONSUME;}
                if(!d.filled){message(player,"No full tank to drain.");return ItemInteractionResult.CONSUME;}
                if(tank.water(false)){if(!player.getAbilities().instabuild)player.setItemInHand(hand,new ItemStack(Items.WATER_BUCKET));changed=true;sound=SoundEvents.BUCKET_FILL;}
            }else{
                if(d.selected()!=null && !d.selected().alive()){message(player,"Remove the body from the tank.");return ItemInteractionResult.CONSUME;}
                var fish=d.capture();if(fish!=null){
                    ItemStack caught=AquariumFishItem.capture(fish);
                    if(!player.getAbilities().instabuild){stack.shrink(1);if(stack.isEmpty())player.setItemInHand(hand,caught);else if(!player.addItem(caught))player.drop(caught,false);}
                    else if(!player.addItem(caught))player.drop(caught,false);
                    changed=true;sound=SoundEvents.BUCKET_FILL_FISH;
                }
            }
        }else if(stack.getItem() instanceof AquariumFishItem item){
            if(tank.condition!=AquariumBlockEntity.Condition.READY){message(player,tank.warning());return ItemInteractionResult.CONSUME;}
            var fish=item.resident(stack,d);
            if(fish==null){message(player,"Invalid fish data.");return ItemInteractionResult.CONSUME;}
            String refusal=d.canAdd(fish);
            if(!refusal.isEmpty()){message(player,refusal);return ItemInteractionResult.CONSUME;}
            d.add(fish);
            var saved=stack.get(DataComponents.CUSTOM_DATA);
            boolean bucket=saved!=null && saved.copyTag().getBoolean("CapturedBucket");
            if(!player.getAbilities().instabuild){if(bucket)player.setItemInHand(hand,new ItemStack(Items.BUCKET));else stack.shrink(1);}
            changed=true;sound=SoundEvents.FISH_SWIM;message(player,"Acclimating: 2 min.");
        }else if(stack.is(AquariumContent.FOOD.get())){
            if(!d.filled){message(player,"Fill the tank first.");return ItemInteractionResult.CONSUME;}
            d.feed();tank.fedAt=level.getGameTime();consume(stack,player);changed=true;sound=SoundEvents.GENERIC_EAT;
        }else if(stack.is(AquariumContent.STARTER.get())){
            if(d.seedFilter()){consume(stack,player);refund(player,Items.GLASS_BOTTLE);changed=true;message(player,"Established bacteria: your filter is cycled.");}
            else message(player,!d.filled?"Fill the tank first.":!d.filter?"Install a filter first.":"Your filter is already cycled.");
        }else if(stack.is(Items.SHEARS)){
            d.clean();stack.hurtAndBreak(1,player,hand==InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);changed=true;sound=SoundEvents.SHEEP_SHEAR;
        }else if(stack.is(Items.SAND) || stack.is(Items.GRAVEL)){
            if(player.isShiftKeyDown()){
                if(d.plants>0)message(player,"Remove plants first.");
                else if(d.substrate){d.substrate=false;refund(player,d.gravel?Items.GRAVEL:Items.SAND);changed=true;}
            }else if(!d.substrate){d.substrate=true;d.gravel=stack.is(Items.GRAVEL);consume(stack,player);changed=true;}
            else message(player,"Substrate already placed.");
        }else if(material(stack)!=null){
            d.ensureScape();var kind=material(stack);
            if(player.isShiftKeyDown()){
                for(int i=d.scape.pieces().size()-1;i>=0;i--)if(d.scape.pieces().get(i).material()==kind){removeDecor(player,tank,i);break;}
            }else{int n=d.scape.pieces().size();addDecor(player,tank,stack,.12+(n*.618%.76),.12+(n*.414%.76),0);}
        }else if(stack.is(AquariumContent.FILTER.get())){
            if(player.isShiftKeyDown()){if(d.filter){d.filter=false;refund(player,AquariumContent.FILTER.get());changed=true;}}
            else if(!d.filter){d.filter=true;consume(stack,player);changed=true;}else message(player,"Filter already fitted.");
        }else if(stack.is(Items.MAGMA_CREAM) || stack.is(Items.SNOWBALL)){
            boolean warm=stack.is(Items.MAGMA_CREAM);
            if(d.warm!=warm){d.warm=warm;consume(stack,player);changed=true;message(player,warm?"Warm water.":"Cool water.");}
        }else if(stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME) && d.selected()!=null){
            String name=stack.getHoverName().getString();d.selected().name=name.substring(0,Math.min(32,name.length()));consume(stack,player);changed=true;
        }
        if(changed){tank.changed();level.playSound(null,pos,sound,SoundSource.BLOCKS,.5F,1F);}
        return ItemInteractionResult.CONSUME;
    }
    public static AquariumScape.Material material(ItemStack s){
        if(s.is(Items.SEAGRASS))return AquariumScape.Material.SEAGRASS;if(s.is(Items.KELP))return AquariumScape.Material.KELP;
        if(s.is(Items.COBBLESTONE))return AquariumScape.Material.ROCK;if(s.is(Items.STICK))return AquariumScape.Material.WOOD;
        if(s.getItem() instanceof BlockItem && !(s.getItem() instanceof AquariumKitItem))return AquariumScape.Material.BLOCK;return null;
    }
    public static Item decorItem(AquariumScape.Material m){return switch(m){case SEAGRASS->Items.SEAGRASS;case KELP->Items.KELP;case ROCK->Items.COBBLESTONE;case WOOD->Items.STICK;case BLOCK->Items.STONE;};}
    public static Item decorItem(AquariumScape.Piece piece){
        if(piece.material()!=AquariumScape.Material.BLOCK)return decorItem(piece.material());
        var id=net.minecraft.resources.ResourceLocation.tryParse(piece.block());
        return id==null?Items.STONE:net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).asItem();
    }
    public static boolean addDecor(Player player,AquariumBlockEntity tank,ItemStack stack,double x,double z,int rotation){
        var kind=material(stack);var d=tank.data;d.ensureScape();
        if(kind==null)return false;
        if((kind==AquariumScape.Material.SEAGRASS || kind==AquariumScape.Material.KELP) && !d.substrate){message(player,"Lay sand or gravel before planting.");return false;}
        String block=kind==AquariumScape.Material.BLOCK?net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(((BlockItem)stack.getItem()).getBlock()).toString():"";
        if(!d.scape.add(new AquariumScape.Piece(kind,x,z,Math.floorMod(rotation,4),0,1,1,1,java.util.UUID.randomUUID(),block))){message(player,"Decoration limit reached, or position outside the tank.");return false;}
        String label=stack.getHoverName().getString();d.scape.fitAll(d.size);d.syncScape();consume(stack,player);tank.changed();message(player,"Placed "+label+".");return true;
    }
    public static boolean removeDecor(Player player,AquariumBlockEntity tank,int index){
        tank.data.ensureScape();var piece=tank.data.scape.remove(index);if(piece==null)return false;
        tank.data.syncScape();refund(player,decorItem(piece));tank.changed();message(player,"Decoration returned to your inventory.");return true;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        open(level,pos,player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof AquariumBlockEntity tank && level.hasChunksAt(pos,tank.maximum())){
            var size=tank.data.size;
            for(int y=0;y<size.blocksHigh();y++)for(int x=0;x<tank.blocksWide();x++)for(int z=0;z<tank.blocksDeep();z++){
                if(x==0 && y==0 && z==0)continue;
                BlockPos cell=pos.offset(x,y,z);
                if(level.getBlockState(cell).is(AquariumContent.PART.get()))level.setBlock(cell,Blocks.AIR.defaultBlockState(),3);
            }
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        var entity=builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        var size=entity instanceof AquariumBlockEntity tank?tank.data.size:AquariumData.Size.SMALL;
        ItemStack stack=new ItemStack(entity instanceof AquariumBlockEntity t && t.data.terrarium!=null?io.github.jakediscord.hobbymod.terrarium.TerrariumContent.KIT.get():AquariumContent.KITS.get(size).get());
        if(entity instanceof AquariumBlockEntity tank)stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tank.saveWithId(tank.getLevel().registryAccess())));
        return List.of(stack);
    }
}
