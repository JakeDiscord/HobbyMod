package io.github.jakediscord.hobbymod.gametest;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.winery.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("hobbymod") @PrefixGameTestTemplate(false)
public final class WineryGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static WineryBlockEntity machine(GameTestHelper h,boolean barrel){h.setBlock(POS.below(),Blocks.STONE);h.setBlock(POS,barrel?WineryContent.BARREL.get():WineryContent.PRESS.get());return (WineryBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static WineBatch must(){return WineBatch.press(Collections.nCopies(8,new WineBatch.Fruit(true,4,true)),82);}
    private static Player player(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().mayBuild=true;p.moveTo(Vec3.atCenterOf(h.absolutePos(POS)));return p;}
    private static void age(WineryBlockEntity b){b.batch.advance(b.getLevel().getGameTime()+1200,false,true);b.batch.rack(b.getLevel().getGameTime());b.batch.advance(b.getLevel().getGameTime()+2400,false,true);}
    private static void pass(GameTestHelper h,String name){h.succeed();HobbyMod.LOGGER.info("WINERY_TEST_PASS {}",name);}

    @GameTest(template="empty") public static void cuttingsBonemealAndHarvestRegrowth(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.DIRT);h.setBlock(POS,WineryContent.TRELLIS.get());var p=player(h);var pos=h.absolutePos(POS);var l=h.getLevel();var cutting=new ItemStack(WineryContent.WHITE_CUTTING.get(),2);var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
        l.getBlockState(pos).useItemOn(cutting,l,p,net.minecraft.world.InteractionHand.MAIN_HAND,hit);h.assertTrue(cutting.getCount()==1 && l.getBlockState(pos).getValue(GrapeTrellisBlock.VINE)==GrapeTrellisBlock.Vine.WHITE,"Planting uses one cutting and preserves cultivar");
        for(int i=0;i<4;i++)WineryContent.TRELLIS.get().performBonemeal(l,l.random,pos,l.getBlockState(pos));h.assertTrue(l.getBlockState(pos).getValue(GrapeTrellisBlock.AGE)==4 && !WineryContent.TRELLIS.get().isValidBonemealTarget(l,pos,l.getBlockState(pos)),"Bonemeal reaches ripe fruit, not late harvest");
        l.getBlockState(pos).useWithoutItem(l,p,hit);var fruit=p.getInventory().getItem(0);h.assertTrue(fruit.is(WineryContent.WHITE.get()) && fruit.getCount()==4 && GrapeItem.fruit(fruit).ripeness()==4 && l.getBlockState(pos).getValue(GrapeTrellisBlock.AGE)==1,"Harvest gives ripe white fruit and regrows the vine");
        p.getAbilities().mayBuild=false;l.setBlock(pos,l.getBlockState(pos).setValue(GrapeTrellisBlock.AGE,5),3);l.getBlockState(pos).useWithoutItem(l,p,hit);h.assertTrue(l.getBlockState(pos).getValue(GrapeTrellisBlock.AGE)==5,"Protected harvest leaves fruit intact");pass(h,"cuttingsBonemealAndHarvestRegrowth");
    }
    @GameTest(template="empty",timeoutTicks=100) public static void poweredBarrelRacksAndBottlesAtMaturity(GameTestHelper h){
        var b=machine(h,true);b.setItem(0,MustBucketItem.create(must()));b.setItem(1,new ItemStack(WineryContent.YEAST.get()));b.startBarrel();b.batch.lastClock=h.getLevel().getGameTime()-1200;b.setItem(2,new ItemStack(Items.GLASS_BOTTLE,4));h.setBlock(POS.offset(1,0,0),Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(25,()->{h.assertTrue(b.batch.stage==WineBatch.Stage.AGING,"Powered barrel racks completed fermentation");b.batch.lastClock=h.getLevel().getGameTime()-2400;});
        h.runAfterDelay(50,()->{h.assertTrue(b.batch==null && b.getItem(3).getCount()==4 && b.getItem(2).isEmpty(),"Powered barrel bottles at maturity");pass(h,"poweredBarrelRacksAndBottlesAtMaturity");});
    }
    @GameTest(template="empty",timeoutTicks=100) public static void pressingConsumesEightAndRefundsInterruptedWork(GameTestHelper h){
        var b=machine(h,false);b.setItem(0,GrapeItem.harvest(true,4,true,5));b.setItem(1,GrapeItem.harvest(false,5,true,5));b.setItem(2,new ItemStack(Items.BUCKET,2));
        h.assertTrue(b.startPress(),"Mixed press starts");h.assertTrue(b.getItem(0).isEmpty() && b.getItem(1).getCount()==2 && b.getItem(2).getCount()==1,"Exactly eight fruit and one bucket reserved");
        var drops=b.contentsForDrop();h.assertTrue(drops.stream().filter(s->b.fruit(s)).mapToInt(ItemStack::getCount).sum()==10,"Breaking an unfinished press refunds fruit once");
        var copy=new WineryBlockEntity(b.getBlockPos(),b.getBlockState());copy.loadWithComponents(b.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.batch.id.equals(b.batch.id) && copy.contentsForDrop().stream().mapToInt(ItemStack::getCount).sum()==12,"Pending reservations survive saving");
        h.runAfterDelay(65,()->{var batch=MustBucketItem.batch(b.getItem(3));h.assertTrue(batch!=null && batch.kind()==WineBatch.Kind.ROSE && b.batch==null,"Animated pressing creates one blended must");pass(h,"pressingConsumesEightAndRefundsInterruptedWork");});
    }
    @GameTest(template="empty") public static void completeProcessConsumesYeastAndFourGlassBottles(GameTestHelper h){
        var b=machine(h,true);b.setItem(0,MustBucketItem.create(must()));h.assertTrue(!b.startBarrel(),"Yeast is required");b.setItem(1,new ItemStack(WineryContent.YEAST.get(),2));h.assertTrue(b.startBarrel(),"Must ferments");h.assertTrue(b.getItem(0).is(Items.BUCKET) && b.getItem(1).getCount()==1 && !b.clean,"Must returns its bucket and uses one yeast");
        h.assertTrue(!b.rack() && !b.bottle(),"Cannot rack or bottle raw must");age(b);b.bottleLabel="Village Reserve";var id=b.batch.id;b.setItem(2,new ItemStack(Items.GLASS_BOTTLE,6));h.assertTrue(b.bottle(),"Aged wine bottles");var result=b.getItem(3);
        h.assertTrue(result.getCount()==4 && b.getItem(2).getCount()==2 && b.batch==null,"Four bottles from one batch");h.assertTrue(WineBottleItem.batchId(result).equals(id) && WineBottleItem.descriptor(result).getString("Label").equals("Village Reserve"),"Bottle label and UUID retained");h.assertTrue(!WineBottleItem.descriptor(result).contains("WineBatch") && WineCellarData.get(h.getLevel()).ledger.get(id)!=null,"Bottles reference the central record");pass(h,"completeProcessConsumesYeastAndFourGlassBottles");
    }
    @GameTest(template="empty") public static void resumedCaskDoesNotMintEmptyBuckets(GameTestHelper h){
        var b=machine(h,true);var batch=must();batch.start(100,true);b.setItem(0,MustBucketItem.portable(batch));h.assertTrue(b.startBarrel() && b.getItem(0).isEmpty(),"Resuming a cask creates no bucket");var snapshot=b.saveWithoutMetadata(h.getLevel().registryAccess());var resumed=new WineryBlockEntity(b.getBlockPos(),b.getBlockState());resumed.setLevel(h.getLevel());resumed.loadWithComponents(snapshot,h.getLevel().registryAccess());resumed.batch.lastClock=h.getLevel().getGameTime()-1400;resumed.advance();h.assertTrue(resumed.batch.stage==WineBatch.Stage.READY_TO_RACK && resumed.batch.rested==200,"Unloaded elapsed time advances fermentation and resting");h.assertTrue(resumed.contentsForDrop().stream().filter(s->s.is(WineryContent.CASK.get())).count()==1,"Active batch travels in exactly one cask");pass(h,"resumedCaskDoesNotMintEmptyBuckets");
    }
    @GameTest(template="empty") public static void duplicateCasksCannotProduceExtraBottles(GameTestHelper h){
        var b=machine(h,true);b.setItem(0,MustBucketItem.create(must()));b.setItem(1,new ItemStack(WineryContent.YEAST.get()));b.startBarrel();age(b);var duplicate=b.batch.copy();b.setItem(2,new ItemStack(Items.GLASS_BOTTLE,2));h.assertTrue(b.bottle(),"First half bottles");var sealed=b.batch.copy();b.removeItem(3,16);b.setItem(2,new ItemStack(Items.GLASS_BOTTLE,8));h.assertTrue(b.bottle() && b.getItem(3).getCount()==2,"Only remaining half bottles");b.removeItem(3,16);b.setItem(0,MustBucketItem.portable(duplicate));h.assertTrue(!b.startBarrel(),"An old unsealed copy cannot replace a finalized vintage");b.setItem(0,MustBucketItem.portable(sealed));h.assertTrue(b.startBarrel() && !b.bottle() && b.getItem(2).getCount()==6,"Sealed duplicate cannot issue any more bottles or consume glass");pass(h,"duplicateCasksCannotProduceExtraBottles");
    }
    @GameTest(template="empty") public static void cellarRinsingAndInventoryPermissions(GameTestHelper h){
        var b=machine(h,true);b.clean=false;b.setItem(0,new ItemStack(Items.WATER_BUCKET));h.assertTrue(b.rinse() && b.clean && b.getItem(0).is(Items.BUCKET),"Rinsing consumes water and preserves bucket");h.assertTrue(!b.canPlaceItem(3,new ItemStack(WineryContent.BOTTLE.get())) && !b.canPlaceItem(1,new ItemStack(Items.DIRT)),"Wrong inputs and output insertion rejected");h.assertTrue(b.canTakeItemThroughFace(0,b.getItem(0),Direction.DOWN) && !b.canTakeItemThroughFace(1,new ItemStack(WineryContent.YEAST.get()),Direction.DOWN),"Hoppers take output and returned buckets, not ingredients");var p=player(h);var menu=new WineryMenu(7,p.getInventory(),b);p.moveTo(Vec3.atCenterOf(h.absolutePos(POS.offset(40,0,0))));h.assertTrue(!menu.clickMenuButton(p,0),"Distant player cannot operate machine");pass(h,"cellarRinsingAndInventoryPermissions");
    }
    @GameTest(template="empty",timeoutTicks=100) public static void poweredPressAndHopperOutput(GameTestHelper h){
        var b=machine(h,false);h.setBlock(POS.offset(1,0,0),Blocks.REDSTONE_BLOCK);h.setBlock(POS.below(),Blocks.HOPPER);var hopper=(HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.below()));b.setItem(0,GrapeItem.harvest(true,4,true,8));b.setItem(2,new ItemStack(Items.BUCKET));h.runAfterDelay(90,()->{h.assertTrue(MustBucketItem.batch(hopper.getItem(0))!=null && b.getItem(3).isEmpty(),"Redstone starts the press and a real hopper extracts its batch");pass(h,"poweredPressAndHopperOutput");});
    }
    @GameTest(template="empty") public static void racksAndBottleStackIdentitySurviveSaving(GameTestHelper h){
        h.setBlock(POS,WineryContent.RACK.get());var rack=(WineRackBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));var a=must().bottled("Reserve");var b=must().bottled("Reserve");var bottle=WineBottleItem.create(a,4);h.assertTrue(!ItemStack.isSameItemSameComponents(bottle,WineBottleItem.create(b,4)),"Separate vintages never merge even with matching names");for(int i=0;i<4;i++)h.assertTrue(rack.insert(bottle),"Rack holds bottle");h.assertTrue(!rack.insert(bottle),"Rack stops at four");var restored=new WineRackBlockEntity(rack.getBlockPos(),rack.getBlockState());restored.loadWithComponents(rack.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(restored.take().getCount()==1 && WineBottleItem.batchId(restored.take()).equals(a.id),"Rack retains single bottles with UUID");pass(h,"racksAndBottleStackIdentitySurviveSaving");
    }
    @GameTest(template="empty") public static void tastingReturnsGlassAndUsesCentralVintage(GameTestHelper h){
        var b=machine(h,true);b.setItem(0,MustBucketItem.create(must()));b.setItem(1,new ItemStack(WineryContent.YEAST.get()));b.startBarrel();age(b);b.setItem(2,new ItemStack(Items.GLASS_BOTTLE,4));b.bottle();var p=player(h);var bottle=b.getItem(3).copyWithCount(1);var result=bottle.getItem().finishUsingItem(bottle,h.getLevel(),p);h.assertTrue(result.is(Items.GLASS_BOTTLE) && p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED),"Tasting quality wine grants a short buff and returns glass");p.getAbilities().instabuild=true;bottle=b.getItem(3).copyWithCount(1);h.assertTrue(bottle.getItem().finishUsingItem(bottle,h.getLevel(),p).getCount()==1,"Creative tasting does not consume a bottle");pass(h,"tastingReturnsGlassAndUsesCentralVintage");
    }
}
