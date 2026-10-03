package io.github.jakediscord.hobbymod.gametest;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.pottery.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("hobbymod")
@PrefixGameTestTemplate(false)
public final class PotteryGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static Player player(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().mayBuild=true;p.moveTo(Vec3.atCenterOf(h.absolutePos(POS)));return p;}
    private static PotteryBlockEntity wheel(GameTestHelper h){h.setBlock(POS,PotteryContent.WHEEL.get());return (PotteryBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static void use(GameTestHelper h,Player p,BlockPos pos,ItemStack stack){
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);var absolute=h.absolutePos(pos);
        h.getBlockState(pos).useItemOn(stack,h.getLevel(),p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false));
    }
    private static void pass(GameTestHelper h,String name){h.succeed();HobbyMod.LOGGER.info("POTTERY_TEST_PASS {}",name);}
    @GameTest(template="empty") public static void clayLoadingAndRealThrowing(GameTestHelper h){
        var w=wheel(h);var p=player(h);var clay=new ItemStack(PotteryContent.CLAY.get(),2);use(h,p,POS,clay);
        h.assertTrue(w.piece!=null && !w.piece.shape.open() && clay.getCount()==1,"Prepared clay loads a solid lump and is consumed once");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);double mass=w.piece.shape.mass();
        h.assertTrue(w.work(p,1,0,0,w.revision),"Hands open the center");int revision=w.revision;
        double radius=w.piece.shape.radius(.5);
        h.assertTrue(w.work(p,.5,.02,0,revision) && w.piece.shape.radius(.5)>radius,"Hands reshape the actual profile");
        h.assertTrue(Math.abs(w.piece.shape.volume(w.piece.shape.wall())-mass)<1e-8,"Throwing conserves the original clay");
        h.assertTrue(!w.work(p,.5,.01,0,revision),"Stale packets cannot change the pot");
        h.assertTrue(!w.work(p,Double.NaN,0,0,w.revision),"Nonfinite coordinates are rejected");
        pass(h,"clayLoadingAndRealThrowing");
    }
    @GameTest(template="empty") public static void wirePickupAndPlacementPreserveTheShape(GameTestHelper h){
        var w=wheel(h);var p=player(h);use(h,p,POS,new ItemStack(PotteryContent.CLAY.get()));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        w.work(p,1,0,0,w.revision);w.work(p,.6,.02,.015,w.revision);double[] expected=w.piece.shape.data();
        var wire=new ItemStack(PotteryContent.WIRE.get());use(h,p,POS,wire);
        h.assertTrue(w.piece==null && wire.getDamageValue()==1,"Wire releases the pot and wears once");
        ItemStack pot=ItemStack.EMPTY;for(var stack:p.getInventory().items)if(stack.is(PotteryContent.POT_ITEM.get())){pot=stack;break;}
        h.assertTrue(!pot.isEmpty(),"Cut pot enters the inventory");
        var stored=PotteryPotItem.piece(pot);
        for(int i=0;i<expected.length;i++)h.assertTrue(Math.abs(stored.shape.data()[i]-expected[i])<.000002,"Stored pot retains its individual profile");
        double expectedRadius=stored.shape.radius(.6);
        var target=POS.above();h.setBlock(POS,net.minecraft.world.level.block.Blocks.STONE);
        p.setItemInHand(InteractionHand.MAIN_HAND,pot);
        var absolute=h.absolutePos(POS);pot.useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false)));
        var placed=(PotteryBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(target));
        h.assertTrue(placed!=null && Math.abs(placed.piece.shape.radius(.6)-expectedRadius)<.000002,"Picked pot can be placed with the same design");
        pass(h,"wirePickupAndPlacementPreserveTheShape");
    }
    @GameTest(template="empty") public static void dryingTrimmingAndRewetting(GameTestHelper h){
        var w=wheel(h);var p=player(h);use(h,p,POS,new ItemStack(PotteryContent.CLAY.get()));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);w.work(p,1,0,0,w.revision);
        w.spinUntil=0;for(int i=0;i<PotteryPiece.DRY_INTERVAL;i++)PotteryBlockEntity.tick(h.getLevel(),w.getBlockPos(),w.getBlockState(),w);
        h.assertTrue(w.piece.stage==PotteryPiece.Stage.LEATHER_HARD,"Idle clay becomes leather-hard");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PotteryContent.LOOP.get()));double before=w.piece.shape.mass();
        h.assertTrue(w.work(p,.1,0,0,w.revision) && w.piece.shape.mass()<before,"Loop trims actual clay from the foot");
        var bucket=new ItemStack(Items.WATER_BUCKET);use(h,p,POS,bucket);
        h.assertTrue(w.piece.stage==PotteryPiece.Stage.WET && w.piece.moisture==100 && p.getMainHandItem().is(Items.BUCKET),"Water rehydrates leather-hard clay and returns the bucket");
        pass(h,"dryingTrimmingAndRewetting");
    }
    @GameTest(template="empty") public static void kilnEnforcesFuelDryClayAndCooling(GameTestHelper h){
        h.setBlock(POS,PotteryContent.KILN.get());var k=(KilnBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
        var piece=new PotteryPiece();piece.shape.openCenter();
        h.assertTrue(!k.insert(PotteryPotItem.create(piece)),"Wet clay cannot enter the firing process");
        piece.stage=PotteryPiece.Stage.DRY;h.assertTrue(k.insert(PotteryPotItem.create(piece)),"Dry pot enters kiln");
        for(int i=0;i<30;i++)KilnBlockEntity.tick(h.getLevel(),k.getBlockPos(),k.getBlockState(),k);
        h.assertTrue(k.firing==0,"No fuel means no firing");k.addFuel();
        for(int i=0;i<KilnBlockEntity.FIRING_TICKS;i++)KilnBlockEntity.tick(h.getLevel(),k.getBlockPos(),k.getBlockState(),k);
        h.assertTrue(PotteryPotItem.piece(k.vessel).stage==PotteryPiece.Stage.BISQUE && k.cooling==KilnBlockEntity.COOLING_TICKS,"First firing produces bisque, then cooling");
        h.assertTrue(k.extract().isEmpty(),"Hot pottery cannot be collected");
        for(int i=0;i<KilnBlockEntity.COOLING_TICKS;i++)KilnBlockEntity.tick(h.getLevel(),k.getBlockPos(),k.getBlockState(),k);
        h.assertTrue(!k.extract().isEmpty() && k.vessel.isEmpty(),"Cooled pot can be collected exactly once");
        pass(h,"kilnEnforcesFuelDryClayAndCooling");
    }
    @GameTest(template="empty") public static void glazeSecondFireAndDisplayFlower(GameTestHelper h){
        h.setBlock(POS,PotteryContent.POT.get());var pot=(PotteryBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));var p=player(h);
        pot.piece.shape.openCenter();pot.piece.stage=PotteryPiece.Stage.BISQUE;
        var glaze=new ItemStack(PotteryContent.GLAZES.get(DyeColor.CYAN).get(),2);use(h,p,POS,glaze);
        h.assertTrue(pot.piece.stage==PotteryPiece.Stage.GLAZED && glaze.getCount()==1,"Bisque receives one cyan glaze");
        var original=pot.piece.shape.radius(.5);var kilnPos=POS.above();h.setBlock(kilnPos,PotteryContent.KILN.get());
        var k=(KilnBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(kilnPos));k.insert(PotteryPotItem.create(pot.piece));k.addFuel();
        for(int i=0;i<KilnBlockEntity.FIRING_TICKS+KilnBlockEntity.COOLING_TICKS;i++)KilnBlockEntity.tick(h.getLevel(),k.getBlockPos(),k.getBlockState(),k);
        var finished=PotteryPotItem.piece(k.extract());h.assertTrue(finished.stage==PotteryPiece.Stage.FINISHED && finished.glaze==DyeColor.CYAN,"Second firing preserves the glaze color");
        h.assertTrue(Math.abs(finished.shape.radius(.5)-original*.97)<.000002,"The original profile shrinks instead of being replaced");
        pot.piece=finished;pot.changed();var flower=new ItemStack(Items.POPPY);use(h,p,POS,flower);
        h.assertTrue(pot.flower.is(Items.POPPY) && flower.isEmpty(),"Finished pottery holds a real consumed flower");
        var saved=PotteryBlock.preserved(pot);var tag=saved.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();
        var restored=new PotteryBlockEntity(pot.getBlockPos(),pot.getBlockState());restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.flower.is(Items.POPPY) && restored.piece.glaze==DyeColor.CYAN,"Decoration and glaze survive pickup");
        pass(h,"glazeSecondFireAndDisplayFlower");
    }
    @GameTest(template="empty") public static void kilnAndWheelStateSurviveReload(GameTestHelper h){
        var w=wheel(h);w.piece=new PotteryPiece();w.piece.shape.openCenter();w.piece.drying=500;w.piece.moisture=42;w.changed();
        var restored=(PotteryBlockEntity)BlockEntity.loadStatic(w.getBlockPos(),w.getBlockState(),w.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored!=null && restored.piece.moisture==42 && restored.piece.drying==500,"Wheel work survives chunk reload");
        var pos=POS.above();h.setBlock(pos,PotteryContent.KILN.get());var k=(KilnBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var piece=new PotteryPiece();piece.shape.openCenter();piece.stage=PotteryPiece.Stage.DRY;k.insert(PotteryPotItem.create(piece));k.addFuel();
        for(int i=0;i<80;i++)KilnBlockEntity.tick(h.getLevel(),k.getBlockPos(),k.getBlockState(),k);
        var copy=(KilnBlockEntity)BlockEntity.loadStatic(k.getBlockPos(),k.getBlockState(),k.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy!=null && copy.firing==80 && copy.burn==k.burn && !copy.vessel.isEmpty(),"Kiln retains firing progress, remaining fuel and its unique pot");
        pass(h,"kilnAndWheelStateSurviveReload");
    }
    @GameTest(template="empty") public static void emptyHandCollectsCooledPotteryAndPlacedPots(GameTestHelper h){
        h.setBlock(POS,PotteryContent.KILN.get());var k=(KilnBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));var p=player(h);
        var piece=new PotteryPiece();piece.shape.openCenter();piece.stage=PotteryPiece.Stage.BISQUE;
        k.vessel=PotteryPotItem.create(piece);k.completed=true;k.cooling=0;
        var hit=new BlockHitResult(Vec3.atCenterOf(k.getBlockPos()),Direction.UP,k.getBlockPos(),false);
        var result=h.getBlockState(POS).useItemOn(ItemStack.EMPTY,h.getLevel(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(result==ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"An empty hand reaches the kiln collection interaction");
        h.getBlockState(POS).useWithoutItem(h.getLevel(),p,hit);
        h.assertTrue(k.vessel.isEmpty() && p.getInventory().items.stream().anyMatch(s->s.is(PotteryContent.POT_ITEM.get())),"Empty-hand interaction collects the cooled pot");
        var placed=POS.above();h.setBlock(placed,PotteryContent.POT.get());p.setShiftKeyDown(true);
        var absolute=h.absolutePos(placed);var potHit=new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false);
        h.assertTrue(h.getBlockState(placed).useItemOn(ItemStack.EMPTY,h.getLevel(),p,InteractionHand.MAIN_HAND,potHit)==ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"Empty hand reaches pottery pickup too");
        h.getBlockState(placed).useWithoutItem(h.getLevel(),p,potHit);
        h.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR,placed);
        h.assertTrue(p.getInventory().items.stream().filter(s->s.is(PotteryContent.POT_ITEM.get())).mapToInt(ItemStack::getCount).sum()==2,"Picked pot enters inventory exactly once");
        pass(h,"emptyHandCollectsCooledPotteryAndPlacedPots");
    }
}
