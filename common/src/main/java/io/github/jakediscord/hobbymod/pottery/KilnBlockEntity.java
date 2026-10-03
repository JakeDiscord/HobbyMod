package io.github.jakediscord.hobbymod.pottery;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fuel-powered firing, followed by cooling before extraction. All state survives a chunk reload. */
public final class KilnBlockEntity extends BlockEntity {
    public static final int FIRING_TICKS=600,COOLING_TICKS=200,FUEL_TICKS=1600;
    public ItemStack vessel=ItemStack.EMPTY;
    public int fuel,coalFuel,burn,firing,cooling;
    public boolean completed;
    public KilnBlockEntity(BlockPos pos,BlockState state){super(PotteryContent.KILN_ENTITY.get(),pos,state);}
    public boolean insert(ItemStack stack) {
        if(!vessel.isEmpty() || !stack.is(PotteryContent.POT_ITEM.get()) || !PotteryPotItem.piece(stack).canFire())return false;
        vessel=stack.copyWithCount(1);firing=0;cooling=0;completed=false;changed();return true;
    }
    public boolean addFuel(){return addFuel(false);}
    public boolean addFuel(boolean charcoal){if(fuel>=64)return false;fuel++;if(!charcoal)coalFuel++;changed();return true;}
    public ItemStack extract(){
        if(vessel.isEmpty() || cooling>0 || (!completed && firing>0))return ItemStack.EMPTY;
        ItemStack out=vessel;vessel=ItemStack.EMPTY;completed=false;firing=0;changed();return out;
    }
    public net.minecraft.world.inventory.ContainerData progress(){return new net.minecraft.world.inventory.ContainerData(){
        public int get(int i){return switch(i){case 0->burn;case 1->firing;case 2->cooling;case 3->completed?1:0;case 4->vessel.isEmpty()?0:1;case 5->fuel;default->0;};}
        public void set(int i,int value){} public int getCount(){return 6;}
    };}
    public net.minecraft.world.Container inventory(){return new net.minecraft.world.Container(){
        public int getContainerSize(){return 3;}
        public boolean isEmpty(){return vessel.isEmpty() && fuel==0;}
        public ItemStack getItem(int i){return switch(i){case 0->completed?ItemStack.EMPTY:vessel;case 1->fuel==0?ItemStack.EMPTY:new ItemStack(coalFuel>0?net.minecraft.world.item.Items.COAL:net.minecraft.world.item.Items.CHARCOAL,coalFuel>0?coalFuel:fuel);case 2->completed && cooling==0?vessel:ItemStack.EMPTY;default->ItemStack.EMPTY;};}
        public ItemStack removeItem(int i,int count){
            ItemStack s=getItem(i);if(s.isEmpty() || count<=0 || i!=1 && (cooling>0 || firing>0 && !completed))return ItemStack.EMPTY;
            ItemStack out=s.copyWithCount(Math.min(count,s.getCount()));s=s.copy();s.shrink(out.getCount());setItem(i,s);return out;
        }
        public ItemStack removeItemNoUpdate(int i){return removeItem(i,getItem(i).getCount());}
        public void setItem(int i,ItemStack stack){
            if(i==1){int charcoal=fuel-coalFuel;if(coalFuel>0)coalFuel=0;else charcoal=0;
                if(stack.is(net.minecraft.world.item.Items.COAL))coalFuel=Math.min(stack.getCount(),64-charcoal);
                else if(stack.is(net.minecraft.world.item.Items.CHARCOAL))charcoal=Math.min(stack.getCount(),64-coalFuel);
                fuel=coalFuel+charcoal;
            }else if(i==0 && firing==0 && !completed){if(stack.isEmpty()){vessel=ItemStack.EMPTY;}else if(stack.is(PotteryContent.POT_ITEM.get()) && PotteryPotItem.piece(stack).canFire())vessel=stack.copyWithCount(1);}
            else if(i==2 && cooling==0 && completed && stack.isEmpty()){vessel=ItemStack.EMPTY;completed=false;firing=0;}
            changed();
        }
        public void setChanged(){changed();}
        public boolean stillValid(net.minecraft.world.entity.player.Player player){return level!=null && level.getBlockEntity(worldPosition)==KilnBlockEntity.this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
        public void clearContent(){if(cooling==0 && firing==0){vessel=ItemStack.EMPTY;fuel=coalFuel=0;changed();}}
        public int getMaxStackSize(){return 64;}
    };}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,KilnBlockEntity kiln) {
        if(level.isClientSide)return;
        boolean lit=false,update=false;
        if(kiln.cooling>0){kiln.cooling--;if(kiln.cooling==0)update=true;}
        else if(!kiln.vessel.isEmpty() && !kiln.completed) {
            if(kiln.burn==0 && kiln.fuel>0){kiln.fuel--;if(kiln.coalFuel>0)kiln.coalFuel--;kiln.burn=FUEL_TICKS;update=true;}
            if(kiln.burn>0){
                kiln.burn--;kiln.firing++;lit=true;
                if(kiln.firing>=FIRING_TICKS) {
                    var piece=PotteryPotItem.piece(kiln.vessel);
                    if(piece.fire())kiln.vessel=PotteryPotItem.create(piece);
                    kiln.completed=true;kiln.cooling=COOLING_TICKS;lit=false;update=true;
                }
            }
        }
        if(state.getValue(KilnBlock.LIT)!=lit)level.setBlock(pos,state.setValue(KilnBlock.LIT,lit),3);
        if(update || level.getGameTime()%20==0)kiln.changed();else kiln.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);if(!vessel.isEmpty())tag.put("vessel",vessel.save(registries));
        tag.putInt("fuel",fuel);tag.putInt("coal_fuel",coalFuel);tag.putInt("burn",burn);tag.putInt("firing",firing);tag.putInt("cooling",cooling);tag.putBoolean("completed",completed);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);vessel=ItemStack.parseOptional(registries,tag.getCompound("vessel"));
        if(!vessel.isEmpty() && !vessel.is(PotteryContent.POT_ITEM.get()))vessel=ItemStack.EMPTY;
        fuel=Math.clamp(tag.getInt("fuel"),0,64);coalFuel=Math.clamp(tag.getInt("coal_fuel"),0,fuel);burn=Math.clamp(tag.getInt("burn"),0,FUEL_TICKS);
        firing=Math.clamp(tag.getInt("firing"),0,FIRING_TICKS);cooling=Math.clamp(tag.getInt("cooling"),0,COOLING_TICKS);completed=tag.getBoolean("completed");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
