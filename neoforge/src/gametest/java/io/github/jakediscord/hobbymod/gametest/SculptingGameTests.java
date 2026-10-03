package io.github.jakediscord.hobbymod.gametest;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.registry.HobbyContent;
import io.github.jakediscord.hobbymod.sculpting.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hobbymod")
@PrefixGameTestTemplate(false)
public final class SculptingGameTests {
    private static final BlockPos POS = new BlockPos(1,1,1);
    private static final int CELL = MarbleVolume.index(16,16,31);
    private static Player player(GameTestHelper h, boolean creative) {
        Player p=h.makeMockPlayer(creative?GameType.CREATIVE:GameType.SURVIVAL);
        p.setUUID(UUID.randomUUID());
        p.getAbilities().instabuild=creative; p.getAbilities().mayBuild=true;
        p.moveTo(Vec3.atCenterOf(h.absolutePos(POS)));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HobbyContent.CHISEL.get()));
        return p;
    }
    private static SculptureBlockEntity blank(GameTestHelper h) {
        h.setBlock(POS,HobbyContent.SCULPTURE.get());
        return (SculptureBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static InteractionResult open(GameTestHelper h, Player p) {
        BlockPos pos=h.absolutePos(POS);
        return HobbyContent.CHISEL.get().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
    }
    private static void pass(GameTestHelper h,String name) { h.succeed(); HobbyMod.LOGGER.info("SCULPTING_TEST_PASS {}",name); }

    @GameTest(template="empty") public static void openingPreservesSolidMarble(GameTestHelper h) {
        h.setBlock(POS,HobbyContent.MARBLE.get()); Player p=player(h,false);
        h.assertTrue(open(h,p).consumesAction(),"Workspace should open");
        h.assertBlockPresent(HobbyContent.SCULPTURE.get(),POS);
        var s=(SculptureBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
        h.assertTrue(s.volume().count()==MarbleVolume.CELLS,"Opening must not create a preset or remove stone");
        h.assertTrue(p.getMainHandItem().getDamageValue()==0,"Opening must not wear the chisel");
        pass(h,"openingPreservesSolidMarble");
    }
    @GameTest(template="empty") public static void survivalStrokeAndSymmetry(GameTestHelper h) {
        var s=blank(h); Player p=player(h,false);
        h.assertTrue(s.carve(p,p.getMainHandItem(),CELL,true,0)==2,"Mirrored detail stroke removes two cells");
        h.assertTrue(s.volume().count()==MarbleVolume.CELLS-2,"Remaining marble must match actual chips");
        h.assertTrue(p.getMainHandItem().getDamageValue()==1,"One stroke wears the tool once");
        h.assertTrue(s.revision()==1,"Stroke changes the synchronized revision");
        pass(h,"survivalStrokeAndSymmetry");
    }
    @GameTest(template="empty") public static void creativeAndPolishing(GameTestHelper h) {
        var s=blank(h); Player p=player(h,true);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HobbyContent.RASP.get()));
        h.assertTrue(s.carve(p,p.getMainHandItem(),CELL,false,0)>0,"Rasp should finish exposed marble");
        h.assertTrue(s.volume().count()==MarbleVolume.CELLS,"Polishing cannot remove material");
        h.assertTrue(s.volume().polishedCount()>0,"Finished surfaces are recorded");
        h.assertTrue(p.getMainHandItem().getDamageValue()==0,"Creative preserves tools");
        pass(h,"creativeAndPolishing");
    }
    @GameTest(template="empty") public static void stalePacketsAndUndoOwnership(GameTestHelper h) {
        var s=blank(h); Player artist=player(h,false), guest=player(h,false);
        s.carve(artist,artist.getMainHandItem(),CELL,false,0);
        h.assertTrue(s.carve(guest,guest.getMainHandItem(),MarbleVolume.index(0,16,16),false,0)==0,"Stale revisions must not carve");
        h.assertTrue(!s.undo(guest,1),"Another player cannot undo the artist's work");
        h.assertTrue(s.undo(artist,1),"Artist can undo the most recent stroke");
        h.assertTrue(s.volume().count()==MarbleVolume.CELLS,"Undo restores the stone");
        h.assertTrue(artist.getMainHandItem().getDamageValue()==1,"Undo must not repair tools");
        pass(h,"stalePacketsAndUndoOwnership");
    }
    @GameTest(template="empty") public static void persistenceAndUpdateTag(GameTestHelper h) {
        var s=blank(h); Player p=player(h,true);
        s.carve(p,p.getMainHandItem(),CELL,false,0);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(HobbyContent.RASP.get()));
        s.carve(p,p.getMainHandItem(),MarbleVolume.index(0,16,16),false,1);
        var registries=h.getLevel().registryAccess();
        var restored=(SculptureBlockEntity)BlockEntity.loadStatic(h.absolutePos(POS),s.getBlockState(),s.saveWithFullMetadata(registries),registries);
        h.assertTrue(restored!=null && restored.volume().count()==s.volume().count(),"Saved shapes survive chunk reload");
        h.assertTrue(restored.volume().polishedCount()==s.volume().polishedCount(),"Finish survives chunk reload");
        var clientCopy=new SculptureBlockEntity(h.absolutePos(POS),s.getBlockState());
        clientCopy.loadWithComponents(s.getUpdateTag(registries),registries);
        h.assertTrue(clientCopy.revision()==s.revision() && clientCopy.volume().count()==s.volume().count(),"Client update carries shape and revision");
        pass(h,"persistenceAndUpdateTag");
    }
    @GameTest(template="empty") public static void miningAndReplacementPreserveCarving(GameTestHelper h) {
        var s=blank(h); Player p=player(h,true);
        s.carve(p,p.getMainHandItem(),CELL,false,0);
        BlockPos absolute=h.absolutePos(POS);
        var drops=Block.getDrops(s.getBlockState(),h.getLevel(),absolute,s);
        h.assertTrue(drops.size()==1 && drops.getFirst().is(HobbyContent.SCULPTURE_ITEM.get()),"Mining must drop a sculpture");
        ItemStack drop=drops.getFirst();
        h.setBlock(POS,Blocks.AIR); h.setBlock(POS.below(),Blocks.STONE);
        p.setItemInHand(InteractionHand.MAIN_HAND,drop);
        var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute.below()),Direction.UP,absolute.below(),false));
        h.assertTrue(drop.useOn(context).consumesAction(),"Carved sculpture should place again");
        var placed=h.getLevel().getBlockEntity(absolute);
        h.assertTrue(placed instanceof SculptureBlockEntity && ((SculptureBlockEntity)placed).volume().count()==MarbleVolume.CELLS-1,"Pickup and placement must preserve the carving");
        pass(h,"miningAndReplacementPreserveCarving");
    }
    @GameTest(template="empty") public static void invalidTargetsAndToolBreakage(GameTestHelper h) {
        var s=blank(h); Player p=player(h,false);
        h.assertTrue(s.carve(p,p.getMainHandItem(),-1,true,0)==0,"Invalid cell cannot carve");
        h.assertTrue(s.carve(p,p.getMainHandItem(),MarbleVolume.index(16,16,16),false,0)==0,"Buried cells cannot be targeted");
        p.getMainHandItem().setDamageValue(127);
        h.assertTrue(s.carve(p,p.getMainHandItem(),CELL,false,0)==1,"Final use still carves");
        h.assertTrue(p.getMainHandItem().isEmpty(),"Tool breaks on last durability");
        h.assertTrue(!SculptureNetworking.permitted(p,h.absolutePos(POS),ItemStack.EMPTY),"Missing tool cannot edit");
        p.moveTo(10000,100,10000);
        h.assertTrue(!SculptureNetworking.permitted(p,h.absolutePos(POS),new ItemStack(HobbyContent.CHISEL.get())),"Remote edits are rejected");
        pass(h,"invalidTargetsAndToolBreakage");
    }
    @GameTest(template="empty") public static void unrelatedStoneStaysUntouched(GameTestHelper h) {
        h.setBlock(POS,Blocks.STONE); Player p=player(h,false);
        h.assertTrue(open(h,p)==InteractionResult.PASS,"Other stone cannot open the editor");
        h.assertBlockPresent(Blocks.STONE,POS);
        h.assertTrue(p.getMainHandItem().getDamageValue()==0,"Wrong targets must not wear tools");
        pass(h,"unrelatedStoneStaysUntouched");
    }
}
