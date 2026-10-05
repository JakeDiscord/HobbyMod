package io.github.jakediscord.hobbymod.winery;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** Standard hopper inventory. Fermentation samples once a second and advances by elapsed game time. */
public final class WineryBlockEntity extends BlockEntity implements WorldlyContainer {
    private final NonNullList<ItemStack> items=NonNullList.withSize(4,ItemStack.EMPTY);
    private final NonNullList<ItemStack> reserved=NonNullList.withSize(3,ItemStack.EMPTY);
    public WineBatch batch;
    public int pressing;
    public long pressSyncTime;
    public boolean clean=true,hot,dark;
    public String bottleLabel="";
    public WineryBlockEntity(BlockPos p,BlockState s){super(WineryContent.MACHINE.get(),p,s);}
    public WineryBlock.Machine machine(){return ((WineryBlock)getBlockState().getBlock()).machine;}
    public boolean fruit(ItemStack s){return s.is(WineryContent.RED.get()) || s.is(WineryContent.WHITE.get()) || s.is(WineryContent.GRAPES);}
    private boolean pressSupplies(){return items.get(3).isEmpty() && (machine()==WineryBlock.Machine.TUB || items.get(2).is(Items.BUCKET)) && (fruit(items.get(0))?items.get(0).getCount():0)+(fruit(items.get(1))?items.get(1).getCount():0)>=8;}
    public boolean startPress(){
        if(machine()==WineryBlock.Machine.BARREL || batch!=null || !items.get(3).isEmpty() || machine()!=WineryBlock.Machine.TUB && !items.get(2).is(Items.BUCKET))return false;
        int available=(fruit(items.get(0))?items.get(0).getCount():0)+(fruit(items.get(1))?items.get(1).getCount():0);if(available<8)return false;
        var grapes=new ArrayList<WineBatch.Fruit>();int left=8;
        for(int i=0;i<2;i++){var s=items.get(i);int n=fruit(s)?Math.min(left,s.getCount()):0;for(int j=0;j<n;j++)grapes.add(GrapeItem.fruit(s));reserved.set(i,n>0?s.split(n):ItemStack.EMPTY);left-=n;}
        reserved.set(2,machine()==WineryBlock.Machine.TUB?ItemStack.EMPTY:items.get(2).split(1));batch=WineBatch.press(grapes,(int)Math.min(Integer.MAX_VALUE,Math.max(0,level.getDayTime()/24000)));pressing=0;changed();return true;
    }
    public boolean startBarrel(){
        if(machine()!=WineryBlock.Machine.BARREL || batch!=null)return false;var incoming=MustBucketItem.batch(items.get(0));if(incoming==null)return false;
        if(level instanceof ServerLevel server){var existing=WineCellarData.get(server).ledger.get(incoming.id);if(existing!=null && incoming.stage!=WineBatch.Stage.BOTTLED)return false;}
        if(incoming.stage==WineBatch.Stage.MUST && !items.get(1).is(WineryContent.YEAST.get()))return false;
        boolean returnBucket=items.get(0).is(WineryContent.MUST.get());items.set(0,returnBucket?new ItemStack(Items.BUCKET):ItemStack.EMPTY);sample();if(incoming.stage==WineBatch.Stage.MUST){items.get(1).shrink(1);incoming.start(level.getGameTime(),clean);}else incoming.lastClock=level.getGameTime();
        batch=incoming;clean=false;changed();return true;
    }
    public boolean rinse(){if(batch!=null || !items.get(0).is(Items.WATER_BUCKET))return false;items.set(0,new ItemStack(Items.BUCKET));clean=true;changed();return true;}
    public boolean rack(){advance();if(batch==null || !batch.rack(level.getGameTime()))return false;changed();sound(net.minecraft.sounds.SoundEvents.BOTTLE_FILL);return true;}
    public boolean bottle(){
        if(!(level instanceof ServerLevel server) || batch==null || batch.stage!=WineBatch.Stage.AGING && batch.stage!=WineBatch.Stage.BOTTLED || !items.get(2).is(Items.GLASS_BOTTLE))return false;
        advance();var data=WineCellarData.get(server);var ledger=data.ledger;
        var wine=batch.stage==WineBatch.Stage.BOTTLED?batch.copy():batch.bottled(bottleLabel);
        var candidate=WineBottleItem.create(wine,1);var output=items.get(3);if(!output.isEmpty() && !ItemStack.isSameItemSameComponents(output,candidate))return false;
        int room=16-output.getCount();if(room<=0)return false;
        if(batch.stage!=WineBatch.Stage.BOTTLED){if(!ledger.finish(wine))return false;data.setDirty();batch=wine;}
        else if(ledger.get(batch.id)==null){ledger.finish(wine);data.setDirty();}
        // Use the central record even if an old container copy still has a different display snapshot.
        wine=ledger.get(batch.id).wine();candidate=WineBottleItem.create(wine,1);if(!output.isEmpty() && !ItemStack.isSameItemSameComponents(output,candidate))return false;
        int n=ledger.issue(batch.id,Math.min(room,items.get(2).getCount()));if(n==0){batch=null;changed();return false;}
        items.get(2).shrink(n);if(output.isEmpty())items.set(3,WineBottleItem.create(wine,n));else output.grow(n);batch.remaining=ledger.remaining(batch.id);data.setDirty();if(batch.remaining==0)batch=null;
        changed();sound(net.minecraft.sounds.SoundEvents.BOTTLE_FILL);return true;
    }
    /** Direct hand loading; the small tub needs no screen or piston. */
    public boolean useTub(Player player,ItemStack held){
        if(machine()!=WineryBlock.Machine.TUB)return false;
        // Previously completed tubs already contain their bucket; do not charge another.
        if(!items.get(3).isEmpty() && (held.isEmpty() || held.is(Items.BUCKET))){
            var out=removeItem(3,1);if(!player.addItem(out))player.drop(out,false);return true;
        }
        if(held.is(Items.BUCKET)){
            if(!tubReady())return false;
            var out=MustBucketItem.create(batch);
            // Older in-progress saves reserved a bucket before treading.
            if(!reserved.get(2).is(Items.BUCKET) && !player.getAbilities().instabuild)held.shrink(1);
            clearPress();if(!player.addItem(out))player.drop(out,false);sound(net.minecraft.sounds.SoundEvents.BUCKET_FILL);return true;
        }
        if(held.isEmpty()){
            if(player.isShiftKeyDown() && batch==null){for(int i=0;i<3;i++){var out=removeItem(i,64);if(!out.isEmpty() && !player.addItem(out))player.drop(out,false);}return true;}
            return false;
        }
        if(batch!=null)return false;
        if(!fruit(held))return false;int total=(fruit(items.get(0))?items.get(0).getCount():0)+(fruit(items.get(1))?items.get(1).getCount():0);
        int n=Math.min(8-total,held.getCount());if(n<=0)return false;
        for(int i=0;i<2;i++){var slot=items.get(i);if(slot.isEmpty() || ItemStack.isSameItemSameComponents(slot,held)){if(slot.isEmpty())items.set(i,held.copyWithCount(n));else slot.grow(n);if(!player.getAbilities().instabuild)held.shrink(n);changed();return true;}}return false;
    }
    public Player treader(){
        if(level==null)return null;
        for(var p:level.getEntitiesOfClass(Player.class,new net.minecraft.world.phys.AABB(worldPosition).inflate(0,.25,0),p->!p.isSpectator() && p.isAlive())){
            double x=p.getX()-worldPosition.getX(),z=p.getZ()-worldPosition.getZ(),y=p.getY()-worldPosition.getY();
            if(x>.15 && x<.85 && z>.15 && z<.85 && y>=.1 && y<.5 && GrapeTrellisBlock.permitted(p,worldPosition))return p;
        }return null;
    }
    private void sound(net.minecraft.sounds.SoundEvent sound){if(level!=null)level.playSound(null,worldPosition,sound,net.minecraft.sounds.SoundSource.BLOCKS,.6F,1);}
    public void advance(){if(batch!=null && machine()==WineryBlock.Machine.BARREL && level!=null && !level.isClientSide){batch.advance(level.getGameTime(),hot,dark);sample();}}
    private void sample(){dark=level.getMaxLocalRawBrightness(worldPosition.above())<=7;hot=false;for(var direction:Direction.values()){var s=level.getBlockState(worldPosition.relative(direction));if(s.is(Blocks.LAVA) || s.is(Blocks.FIRE) || s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)){hot=true;break;}}}
    public List<ItemStack> contentsForDrop(){var out=new ArrayList<ItemStack>();for(var item:items)if(!item.isEmpty())out.add(item.copy());if(machine()!=WineryBlock.Machine.BARREL){for(var item:reserved)if(!item.isEmpty())out.add(item.copy());}else if(batch!=null)out.add(MustBucketItem.portable(batch));return out;}
    public boolean tubReady(){return machine()==WineryBlock.Machine.TUB && (batch!=null && pressing>=120 || !items.get(3).isEmpty());}
    private void clearPress(){batch=null;pressing=0;reserved.clear();changed();}
    private void finishIntoOutput(){items.set(3,MustBucketItem.create(batch));clearPress();}
    private void tickTub(){
        if(tubReady()){
            if(batch!=null && items.get(3).isEmpty() && (reserved.get(2).is(Items.BUCKET) || items.get(2).is(Items.BUCKET))){
                if(!reserved.get(2).is(Items.BUCKET))items.get(2).shrink(1);
                finishIntoOutput();
            }
            return;
        }
        if(batch==null && !pressSupplies() || treader()==null)return;
        if(batch==null){startPress();return;}
        pressing++;
        if(pressing%20==0)sound(net.minecraft.sounds.SoundEvents.MUD_STEP);
        if(pressing>=120)sound(net.minecraft.sounds.SoundEvents.BOTTLE_FILL);
        if(pressing%10==0 || pressing>=120)changed();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,WineryBlockEntity b){
        if(b.machine()==WineryBlock.Machine.TUB){b.tickTub();return;}
        if(b.machine()==WineryBlock.Machine.PRESS){
            if(b.batch!=null){
                b.pressing++;
                if(b.pressing>=60 && b.items.get(3).isEmpty()){b.finishIntoOutput();b.sound(net.minecraft.sounds.SoundEvents.BOTTLE_FILL);}
                else if(b.pressing%10==0)b.changed();
            }else if(level.getGameTime()%20==0 && level.hasNeighborSignal(pos))b.startPress();
        }else if(level.getGameTime()%20==0){b.advance();if(level.hasNeighborSignal(pos)){if(b.batch==null)b.startBarrel();else if(b.batch.stage==WineBatch.Stage.READY_TO_RACK)b.rack();else if(b.batch.stage==WineBatch.Stage.BOTTLED || b.batch.stage==WineBatch.Stage.AGING && b.batch.aged>=b.batch.idealAge())b.bottle();}if(b.batch!=null)b.changed();}
    }
    public ContainerData progress(){return new ContainerData(){public int getCount(){return 10;}public void set(int i,int value){}public int get(int i){return switch(i){case 0->batch==null?0:batch.stage.ordinal()+1;case 1->machine()!=WineryBlock.Machine.BARREL?Math.min(100,pressing*100/(machine()==WineryBlock.Machine.TUB?120:60)):batch==null?0:batch.fermentation();case 2->batch==null?0:batch.quality();case 3->batch==null?0:(int)Math.min(999,batch.aged/20);case 4->batch==null?0:batch.remaining;case 5->clean?1:0;case 6->batch==null?0:batch.kind().ordinal();case 7->batch==null?0:(int)(batch.idealAge()/20);case 8->hot?2:dark?0:1;case 9->batch==null?0:batch.sugar;default->0;};}};}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public int getContainerSize(){return 4;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int amount){var s=ContainerHelper.removeItem(items,i,amount);if(!s.isEmpty())changed();return s;}
    @Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    @Override public void setItem(int i,ItemStack s){items.set(i,s);s.setCount(Math.min(s.getCount(),s.getMaxStackSize()));changed();}
    @Override public void clearContent(){items.clear();changed();}
    @Override public boolean stillValid(Player player){return level!=null && level.getBlockEntity(worldPosition)==this && GrapeTrellisBlock.permitted(player,worldPosition);}
    @Override public boolean canPlaceItem(int i,ItemStack s){if(machine()!=WineryBlock.Machine.BARREL)return i<2?fruit(s):i==2 && s.is(Items.BUCKET);return switch(i){case 0->batch==null && (MustBucketItem.batch(s)!=null || s.is(Items.WATER_BUCKET));case 1->s.is(WineryContent.YEAST.get());case 2->s.is(Items.GLASS_BOTTLE);default->false;};}
    @Override public int[] getSlotsForFace(Direction d){return d==Direction.DOWN?machine()==WineryBlock.Machine.BARREL?new int[]{3,0}:new int[]{3}:new int[]{0,1,2};}
    @Override public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction d){return canPlaceItem(i,s);}
    @Override public boolean canTakeItemThroughFace(int i,ItemStack s,Direction d){return i==3 || machine()==WineryBlock.Machine.BARREL && i==0 && s.is(Items.BUCKET);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);ContainerHelper.saveAllItems(t,items,r);var held=new CompoundTag();ContainerHelper.saveAllItems(held,reserved,r);t.put("Reserved",held);if(batch!=null)t.putByteArray("Batch",batch.encode());t.putInt("Pressing",pressing);t.putBoolean("Clean",clean);t.putString("Label",WineBatch.clean(bottleLabel));t.putBoolean("Hot",hot);t.putBoolean("Dark",dark);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);items.clear();reserved.clear();ContainerHelper.loadAllItems(t,items,r);ContainerHelper.loadAllItems(t.getCompound("Reserved"),reserved,r);batch=null;if(t.contains("Batch"))try{batch=WineBatch.decode(t.getByteArray("Batch"));}catch(java.io.IOException ignored){}pressing=Math.clamp(t.getInt("Pressing"),0,machine()==WineryBlock.Machine.TUB?120:60);pressSyncTime=level==null?0:level.getGameTime();clean=!t.contains("Clean") || t.getBoolean("Clean");bottleLabel=WineBatch.clean(t.getString("Label"));hot=t.getBoolean("Hot");dark=t.getBoolean("Dark");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
