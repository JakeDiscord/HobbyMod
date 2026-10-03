package io.github.jakediscord.hobbymod.gametest;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.registry.HobbyContent;
import io.github.jakediscord.hobbymod.sculpting.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("hobbymod") @PrefixGameTestTemplate(false)
public final class BlueprintGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    @GameTest(template="empty") public static void captureFileRoundTripAndApplyMarbleStructure(GameTestHelper h){
        h.setBlock(POS,HobbyContent.SCULPTURE.get());h.setBlock(POS.above(),HobbyContent.SCULPTURE.get());
        var bottom=(SculptureBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));bottom.volume().stroke(.5,.5,1,CarvingTool.DETAIL);
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().mayBuild=true;p.moveTo(Vec3.atCenterOf(h.absolutePos(POS)));var stack=new ItemStack(HobbyContent.BLUEPRINT.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(bottom.getBlockPos()),Direction.UP,bottom.getBlockPos(),false)));
        var captured=MarbleBlueprint.decode(MarbleBlueprintItem.bytes(stack));h.assertTrue(captured.sections().size()==2,"Blueprint captures both connected marble sections");
        var imported=MarbleBlueprint.fromJson(captured.json());MarbleBlueprintItem.store(stack,imported);
        h.setBlock(POS,HobbyContent.MARBLE.get());h.setBlock(POS.above(),HobbyContent.MARBLE.get());MarbleBlueprintItem.apply(p,h.absolutePos(POS),stack);
        var restored=(SculptureBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));h.assertTrue(restored!=null && !Arrays.equals(restored.volume().densityBytes(),new MarbleVolume().densityBytes()),"Imported blueprint applies carved geometry to fresh marble");
        h.assertTrue(Arrays.equals(restored.volume().densityBytes(),captured.sections().stream().filter(s->s.offset().equals(BlockPos.ZERO)).findFirst().orElseThrow().volume().densityBytes()),"Smooth cuts retain exact density after file round trip");
        h.succeed();HobbyMod.LOGGER.info("BLUEPRINT_TEST_PASS captureFileRoundTripAndApplyMarbleStructure");
    }
    @GameTest(template="empty") public static void missingMarbleRejectsWholeBlueprintAndSnapshotsAcknowledgeNoOps(GameTestHelper h){
        h.setBlock(POS,HobbyContent.SCULPTURE.get());var s=(SculptureBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().mayBuild=true;p.moveTo(Vec3.atCenterOf(s.getBlockPos()));var stack=new ItemStack(HobbyContent.BLUEPRINT.get());
        var volume=new MarbleVolume();volume.stroke(.5,.5,1,CarvingTool.DETAIL);MarbleBlueprintItem.store(stack,new MarbleBlueprint(List.of(new MarbleBlueprint.Section(BlockPos.ZERO,volume),new MarbleBlueprint.Section(new BlockPos(0,1,0),new MarbleVolume()))));
        boolean rejected=false;try{MarbleBlueprintItem.apply(p,s.getBlockPos(),stack);}catch(IllegalArgumentException e){rejected=true;}
        h.assertTrue(rejected && s.volume().count()==MarbleVolume.CELLS,"Missing target marble rejects transaction before changing its anchor");
        int rev=s.revision(),snapshots=s.snapshots();s.loadWithComponents(s.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(s.revision()==rev && s.snapshots()!=snapshots,"No-op update is acknowledged even without a changed geometry revision");
        h.succeed();HobbyMod.LOGGER.info("BLUEPRINT_TEST_PASS missingMarbleRejectsWholeBlueprintAndSnapshotsAcknowledgeNoOps");
    }
}
