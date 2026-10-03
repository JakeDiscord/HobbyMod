package io.github.jakediscord.hobbymod.pottery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Furnace layout, with server-owned slots and a locked hot vessel. */
public final class KilnMenu extends AbstractContainerMenu {
    public final ContainerData progress;
    private final Container kiln;
    public KilnMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(3),new SimpleContainerData(6));}
    public KilnMenu(int id,Inventory inventory,KilnBlockEntity entity){this(id,inventory,entity.inventory(),entity.progress());}
    private KilnMenu(int id,Inventory inventory,Container kiln,ContainerData data){
        super(PotteryContent.KILN_MENU.get(),id);this.kiln=kiln;this.progress=data;
        addSlot(new Slot(kiln,0,56,17){
            @Override public boolean mayPlace(ItemStack s){return s.is(PotteryContent.POT_ITEM.get()) && PotteryPotItem.piece(s).canFire() && progress.get(3)==0 && progress.get(4)==0;}
            @Override public boolean mayPickup(Player p){return progress.get(1)==0 && progress.get(2)==0 && progress.get(3)==0;}
            @Override public int getMaxStackSize(){return 1;}
        });
        addSlot(new Slot(kiln,1,56,53){@Override public int getMaxStackSize(){return 64-Math.max(0,progress.get(5)-getItem().getCount());}@Override public boolean mayPlace(ItemStack s){return (s.is(Items.COAL)||s.is(Items.CHARCOAL)) && (getItem().isEmpty() || s.is(getItem().getItem()));}});
        addSlot(new Slot(kiln,2,116,35){@Override public boolean mayPlace(ItemStack s){return false;} @Override public boolean mayPickup(Player p){return progress.get(2)==0 && progress.get(3)==1;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
        addDataSlots(data);
    }
    @Override public boolean stillValid(Player player){return kiln.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        Slot slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<3){if(!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
        else if(slots.get(0).mayPlace(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(slots.get(1).mayPlace(stack)){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(index<30){if(!moveItemStackTo(stack,30,39,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,3,30,false))return ItemStack.EMPTY;
        slot.setByPlayer(stack.isEmpty()?ItemStack.EMPTY:stack);
        if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return copy;
    }
}
