package io.github.jakediscord.hobbymod.gametest;

import io.github.jakediscord.hobbymod.registry.HobbyContent;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.sculpting.MarbleStatueBlock;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hobbymod")
@PrefixGameTestTemplate(false)
public final class SculptingGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static Player player(GameTestHelper helper, boolean creative) {
        Player player = helper.makeMockPlayer(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        player.getAbilities().instabuild = creative;
        player.getAbilities().mayBuild = true;
        player.setYRot(90); // Looking west: a carved statue should face east.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HobbyContent.CHISEL.get()));
        return player;
    }

    private static InteractionResult carve(GameTestHelper helper, Player player) {
        BlockPos pos = helper.absolutePos(POS);
        return HobbyContent.CHISEL.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    @GameTest(template = "empty")
    public static void survivalCarving(GameTestHelper helper) {
        helper.setBlock(POS, HobbyContent.MARBLE.get());
        Player player = player(helper, false);
        helper.assertTrue(carve(helper, player).consumesAction(), "Carving should succeed");
        helper.assertBlockPresent(HobbyContent.MARBLE_STATUE.get(), POS);
        helper.assertBlockProperty(POS, MarbleStatueBlock.FACING, Direction.EAST);
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Survival carving must use one durability");
        helper.succeed();
        HobbyMod.LOGGER.info("SCULPTING_TEST_PASS survivalCarving");
    }

    @GameTest(template = "empty")
    public static void creativeCarving(GameTestHelper helper) {
        helper.setBlock(POS, HobbyContent.MARBLE.get());
        Player player = player(helper, true);
        carve(helper, player);
        helper.assertBlockPresent(HobbyContent.MARBLE_STATUE.get(), POS);
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "Creative carving must preserve durability");
        helper.succeed();
        HobbyMod.LOGGER.info("SCULPTING_TEST_PASS creativeCarving");
    }

    @GameTest(template = "empty")
    public static void unrelatedStone(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        Player player = player(helper, false);
        helper.assertTrue(carve(helper, player) == InteractionResult.PASS, "Other blocks should pass through");
        helper.assertBlockPresent(Blocks.STONE, POS);
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "Other stone must not damage the chisel");
        helper.succeed();
        HobbyMod.LOGGER.info("SCULPTING_TEST_PASS unrelatedStone");
    }

    @GameTest(template = "empty")
    public static void lastUseBreaksChisel(GameTestHelper helper) {
        helper.setBlock(POS, HobbyContent.MARBLE.get());
        Player player = player(helper, false);
        player.getMainHandItem().setDamageValue(127);
        carve(helper, player);
        helper.assertBlockPresent(HobbyContent.MARBLE_STATUE.get(), POS);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Chisel should break after its 128th use");
        helper.succeed();
        HobbyMod.LOGGER.info("SCULPTING_TEST_PASS lastUseBreaksChisel");
    }

    @GameTest(template = "empty")
    public static void alreadyCarved(GameTestHelper helper) {
        helper.setBlock(POS, HobbyContent.MARBLE_STATUE.get());
        Player player = player(helper, false);
        helper.assertTrue(carve(helper, player) == InteractionResult.PASS, "Statues cannot be carved again");
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "Repeated use must not waste durability");
        helper.succeed();
        HobbyMod.LOGGER.info("SCULPTING_TEST_PASS alreadyCarved");
    }
}
