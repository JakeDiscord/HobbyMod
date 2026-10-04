package io.github.jakediscord.hobbymod.winery;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WineRackBlockEntity extends BlockEntity {
    public final NonNullList<ItemStack> bottles=NonNullList.withSize(4,ItemStack.EMPTY);
    public WineRackBlockEntity(BlockPos p,BlockState s){super(WineryContent.RACK_ENTITY.get(),p,s);}
    public boolean insert(ItemStack s){if(!s.is(WineryContent.BOTTLE.get()) || WineBottleItem.batchId(s)==null)return false;for(int i=0;i<4;i++)if(bottles.get(i).isEmpty()){bottles.set(i,s.copyWithCount(1));changed();return true;}return false;}
    public ItemStack take(){for(int i=3;i>=0;i--)if(!bottles.get(i).isEmpty()){var s=bottles.set(i,ItemStack.EMPTY);changed();return s;}return ItemStack.EMPTY;}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);ContainerHelper.saveAllItems(t,bottles,r);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);ContainerHelper.loadAllItems(t,bottles,r);for(int i=0;i<4;i++)if(!bottles.get(i).isEmpty()){if(!bottles.get(i).is(WineryContent.BOTTLE.get()) || WineBottleItem.batchId(bottles.get(i))==null)bottles.set(i,ItemStack.EMPTY);else bottles.get(i).setCount(1);}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
