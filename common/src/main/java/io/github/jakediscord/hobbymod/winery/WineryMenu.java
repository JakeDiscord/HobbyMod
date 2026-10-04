package io.github.jakediscord.hobbymod.winery;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class WineryMenu extends AbstractContainerMenu {
    public final WineryBlock.Machine machine;
    public final ContainerData data;
    public final WineryBlockEntity entity;
    private final Container inventory;
    public WineryMenu(int id,Inventory player,WineryBlock.Machine machine){this(id,player,machine,new SimpleContainer(4),new SimpleContainerData(10),null);}
    public WineryMenu(int id,Inventory player,WineryBlockEntity entity){this(id,player,entity.machine(),entity,entity.progress(),entity);}
    private WineryMenu(int id,Inventory player,WineryBlock.Machine machine,Container inventory,ContainerData data,WineryBlockEntity entity){
        super(machine==WineryBlock.Machine.PRESS?WineryContent.PRESS_MENU.get():WineryContent.BARREL_MENU.get(),id);this.machine=machine;this.inventory=inventory;this.data=data;this.entity=entity;
        int[] xs=machine==WineryBlock.Machine.PRESS?new int[]{26,50,90,138}:new int[]{18,58,100,142};for(int i=0;i<4;i++){final int index=i;addSlot(new Slot(inventory,i,xs[i],40){@Override public boolean mayPlace(ItemStack s){if(index==3)return false;if(entity!=null)return entity.canPlaceItem(index,s);if(machine==WineryBlock.Machine.PRESS)return index<2?s.is(WineryContent.GRAPES) || s.is(WineryContent.RED.get()) || s.is(WineryContent.WHITE.get()):s.is(Items.BUCKET);return index==0?MustBucketItem.batch(s)!=null || s.is(Items.WATER_BUCKET):index==1?s.is(WineryContent.YEAST.get()):s.is(Items.GLASS_BOTTLE);}});}
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,col+row*9+9,8+col*18,140+row*18));for(int col=0;col<9;col++)addSlot(new Slot(player,col,8+col*18,198));addDataSlots(data);
    }
    @Override public boolean stillValid(Player p){return inventory.stillValid(p);}
    @Override public boolean clickMenuButton(Player p,int action){if(entity==null || !stillValid(p))return false;if(machine==WineryBlock.Machine.PRESS)return action==0 && entity.startPress();return switch(action){case 0->entity.getItem(0).is(Items.WATER_BUCKET)?entity.rinse():entity.startBarrel();case 1->entity.rack();case 2->entity.bottle();default->false;};}
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<0 || index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<4){if(!moveItemStackTo(stack,4,40,true))return ItemStack.EMPTY;}else{boolean moved=false;for(int i=0;i<3;i++)if(slots.get(i).mayPlace(stack)){moved=moveItemStackTo(stack,i,i+1,false);if(moved)break;}if(!moved && !(index<31?moveItemStackTo(stack,31,40,false):moveItemStackTo(stack,4,31,false)))return ItemStack.EMPTY;}
        if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;slot.setByPlayer(stack.isEmpty()?ItemStack.EMPTY:stack);slot.onTake(p,stack);return copy;
    }
}
