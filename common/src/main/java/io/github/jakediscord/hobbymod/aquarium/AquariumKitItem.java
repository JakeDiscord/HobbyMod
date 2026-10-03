package io.github.jakediscord.hobbymod.aquarium;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;

/** A compact aquarium model. Reject an obstructed footprint before placing anything. */
public final class AquariumKitItem extends BlockItem {
    private final AquariumData.Size size;
    public AquariumKitItem(AquariumData.Size size){super(AquariumContent.CONTROLLER.get(),new Item.Properties().stacksTo(1));this.size=size;}
    private AquariumData.Size size(ItemStack stack){
        var saved=stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if(saved!=null)try{return AquariumData.Size.valueOf(saved.copyTag().getCompound("Aquarium").getString("Size"));}catch(IllegalArgumentException ignored){}
        return size;
    }
    @Override public Component getName(ItemStack stack){return Component.literal(size(stack).name().charAt(0)+size(stack).name().substring(1).toLowerCase(java.util.Locale.ROOT)+" Aquarium");}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag){
        var s=size(stack);lines.add(Component.literal(s.blocksWide()+" × "+s.blocksDeep()+" Aquarium"));
        lines.add(Component.literal("Right-click to manage fish and setup"));
    }
    @Override public InteractionResult place(BlockPlaceContext context){
        var level=context.getLevel();var player=context.getPlayer();var origin=context.getClickedPos();var size=size(context.getItemInHand());
        int width=context.getHorizontalDirection().getAxis()==net.minecraft.core.Direction.Axis.X?size.blocksDeep():size.blocksWide();
        int depth=context.getHorizontalDirection().getAxis()==net.minecraft.core.Direction.Axis.X?size.blocksWide():size.blocksDeep();
        if(player==null || !level.hasChunksAt(origin,origin.offset(width-1,size.blocksHigh()-1,depth-1)))return InteractionResult.FAIL;
        for(int y=0;y<size.blocksHigh();y++)for(int x=0;x<width;x++)for(int z=0;z<depth;z++){
            BlockPos p=origin.offset(x,y,z);
            if(level.isOutsideBuildHeight(p) || !level.getWorldBorder().isWithinBounds(p) || !level.getBlockState(p).canBeReplaced()
                    || !level.getFluidState(p).isEmpty() || !player.mayUseItemAt(p,context.getClickedFace(),context.getItemInHand())){
                if(!level.isClientSide)player.displayClientMessage(Component.literal("Tank needs clear "+size.blocksWide()+" × "+size.blocksHigh()+" × "+size.blocksDeep()+" space."),true);
                return InteractionResult.FAIL;
            }
        }
        boolean carried=context.getItemInHand().has(DataComponents.BLOCK_ENTITY_DATA);
        var box=new net.minecraft.world.phys.AABB(origin.getX(),origin.getY(),origin.getZ(),
                origin.getX()+width,origin.getY()+size.blocksHigh(),origin.getZ()+depth);
        if(level.getEntities(null,box).stream().anyMatch(entity->entity instanceof net.minecraft.world.entity.LivingEntity && !entity.isSpectator()))return InteractionResult.FAIL;
        InteractionResult result=super.place(context);
        if(result.consumesAction() && !level.isClientSide && level.getBlockEntity(origin) instanceof AquariumBlockEntity tank){
            tank.data.size=size;if(!carried)tank.data.seed=level.random.nextLong();
            for(int y=0;y<size.blocksHigh();y++)for(int x=0;x<width;x++)for(int z=0;z<depth;z++){
                if(x==0 && y==0 && z==0)continue;
                BlockPos p=origin.offset(x,y,z);
                level.setBlock(p,AquariumContent.PART.get().defaultBlockState(),3);
            }
            if(tank.data.filled)tank.restoreWater();else{tank.inspect();tank.changed();}
        }
        return result;
    }
}
