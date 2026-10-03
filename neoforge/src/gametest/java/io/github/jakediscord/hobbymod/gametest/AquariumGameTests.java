package io.github.jakediscord.hobbymod.gametest;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.aquarium.*;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("hobbymod")
@PrefixGameTestTemplate(false)
public final class AquariumGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static Player player(GameTestHelper h){
        Player p=h.makeMockPlayer(GameType.SURVIVAL);p.setUUID(UUID.randomUUID());p.getAbilities().mayBuild=true;
        p.moveTo(Vec3.atCenterOf(h.absolutePos(POS.offset(-2,0,-2))));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AquariumContent.KITS.get(AquariumData.Size.SMALL).get()));return p;
    }
    private static InteractionResult place(GameTestHelper h,Player p){
        BlockPos floor=h.absolutePos(POS.below());
        return ((AquariumKitItem)p.getMainHandItem().getItem()).place(new BlockPlaceContext(new UseOnContext(p,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false))));
    }
    private static AquariumBlockEntity tank(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.STONE);Player p=player(h);
        h.assertTrue(place(h,p).consumesAction(),"Kit must place a complete shell");
        var tank=(AquariumBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
        h.assertTrue(tank!=null,"Controller missing");return tank;
    }
    private static void pass(GameTestHelper h,String name){h.succeed();HobbyMod.LOGGER.info("AQUARIUM_TEST_PASS {}",name);}
    @GameTest(template="aquarium_empty") public static void tankFillAndDrain(GameTestHelper h){
        var tank=tank(h);h.assertTrue(tank.inspect()==AquariumBlockEntity.Condition.EMPTY,"New tank should be dry");
        h.assertTrue(tank.water(true) && tank.inspect()==AquariumBlockEntity.Condition.READY,"Filled tank not validated");
        tank.data.cycle=5;var fish=tank.data.newFish(AquariumData.Species.GUPPY);h.assertTrue(tank.data.add(fish),"Fish refused");
        h.assertTrue(tank.water(false) && tank.inspect()==AquariumBlockEntity.Condition.EMPTY,"Drain not reflected in enclosure");
        tank.data.advance(false,false);h.assertTrue(tank.data.fish().size()==1 && fish.health<100,"Draining must cause readable stress, not deletion");
        h.assertTrue(tank.water(true),"Tank should be refillable");pass(h,"tankFillAndDrain");
    }
    @GameTest(template="aquarium_empty") public static void compactTankDoesNotCreateWorldWater(GameTestHelper h){
        var tank=tank(h);tank.water(true);
        h.assertBlockPresent(Blocks.AIR,POS.above());h.assertBlockPresent(Blocks.AIR,POS.east());
        h.assertTrue(tank.data.size.blocksWide()==1 && tank.data.size.blocksDeep()==1,"Small tank must fit one block");
        h.assertTrue(tank.inspect()==AquariumBlockEntity.Condition.READY,"Contained water should be ready");
        pass(h,"compactTankDoesNotCreateWorldWater");
    }
    @GameTest(template="aquarium_empty") public static void compactPartsRouteAndDismantle(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.STONE);h.setBlock(POS.offset(2,0,0),Blocks.DIAMOND_BLOCK);
        Player p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AquariumContent.KITS.get(AquariumData.Size.MEDIUM).get()));
        h.assertTrue(place(h,p).consumesAction(),"Medium tank should place");
        var tank=AquariumPartBlock.find(h.getLevel(),h.absolutePos(POS.east()));
        h.assertTrue(tank!=null && tank.getBlockPos().equals(h.absolutePos(POS)),"Part must route to its own tank");
        h.getLevel().destroyBlock(h.absolutePos(POS.east()),true);
        h.assertBlockPresent(Blocks.AIR,POS);h.assertBlockPresent(Blocks.AIR,POS.east());
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK,POS.offset(2,0,0));pass(h,"compactPartsRouteAndDismantle");
    }
    @GameTest(template="aquarium_empty") public static void occupiedFootprintDoesNotOverwrite(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.STONE);h.setBlock(POS.east(),Blocks.DIAMOND_BLOCK);
        Player p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AquariumContent.KITS.get(AquariumData.Size.MEDIUM).get()));h.assertTrue(!place(h,p).consumesAction(),"Obstructed footprint must fail before mutation");
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK,POS.east());h.assertBlockPresent(Blocks.AIR,POS);
        h.assertTrue(p.getMainHandItem().getCount()==1,"Failed kit placement consumed item");pass(h,"occupiedFootprintDoesNotOverwrite");
    }
    @GameTest(template="aquarium_empty") public static void persistencePacketsAndCarriedFish(GameTestHelper h){
        var tank=tank(h);tank.water(true);tank.data.seed=42;tank.data.cycle=5;tank.data.quality=73;tank.data.plants=3;
        var fish=tank.data.newFish(AquariumData.Species.GUPPY);fish.colorA=3;fish.colorB=12;fish.mother=UUID.randomUUID();fish.name="Coral";tank.data.add(fish);
        var registries=h.getLevel().registryAccess();
        var restored=(AquariumBlockEntity)BlockEntity.loadStatic(tank.getBlockPos(),tank.getBlockState(),tank.saveWithFullMetadata(registries),registries);
        h.assertTrue(restored!=null && restored.data.fish().getFirst().id.equals(fish.id),"Chunk persistence lost fish identity");
        h.assertTrue(restored.data.fish().getFirst().mother.equals(fish.mother) && restored.data.quality==73,"Persistence lost lineage/care");
        var copy=new AquariumBlockEntity(tank.getBlockPos(),tank.getBlockState());copy.loadWithComponents(tank.getUpdateTag(registries),registries);
        h.assertTrue(copy.data.fish().getFirst().colorB==12 && copy.data.plants==3,"Update packet lost genes/aquascape");
        ItemStack captured=AquariumFishItem.capture(fish);var decoded=((AquariumFishItem)captured.getItem()).resident(captured,tank.data);
        h.assertTrue(decoded.id.equals(fish.id) && decoded.name.equals("Coral"),"Capture lost individual fish");
        var drops=Block.getDrops(tank.getBlockState(),h.getLevel(),tank.getBlockPos(),tank);
        h.assertTrue(drops.size()==1 && drops.getFirst().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),"Mining lost tank contents");pass(h,"persistencePacketsAndCarriedFish");
    }
    @GameTest(template="aquarium_empty") public static void aquascapingConsumesReturnsAndSavesExactItems(GameTestHelper h){
        var tank=tank(h);var p=player(h);tank.data.substrate=true;var kelp=new ItemStack(Items.KELP,2);
        h.assertTrue(AquariumControllerBlock.addDecor(p,tank,kelp,.2,.8,3) && kelp.getCount()==1,"Custom plant placement consumes one real kelp");
        h.assertTrue(!AquariumControllerBlock.addDecor(p,tank,kelp,Double.NaN,.5,0) && kelp.getCount()==1,"Invalid layout coordinates cannot consume supplies");
        var restored=(AquariumBlockEntity)BlockEntity.loadStatic(tank.getBlockPos(),tank.getBlockState(),tank.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.data.scape.pieces().equals(tank.data.scape.pieces()),"Carrying/loading retains exact aquascape positions and rotation");
        h.assertTrue(AquariumControllerBlock.removeDecor(p,tank,0) && tank.data.plants==0 && p.getInventory().items.stream().anyMatch(s->s.is(Items.KELP)),"Removing kelp returns kelp and updates plant count");
        pass(h,"aquascapingConsumesReturnsAndSavesExactItems");
    }
    @GameTest(template="aquarium_empty") public static void starterFeedingAndDrainProtection(GameTestHelper h){
        var tank=tank(h);var p=player(h);tank.water(true);tank.data.filter=true;var pos=tank.getBlockPos();var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AquariumContent.STARTER.get()));AquariumContent.CONTROLLER.get().applyItem(p.getMainHandItem(),tank.getBlockState(),h.getLevel(),pos,p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tank.data.cycle==5 && !p.getMainHandItem().is(AquariumContent.STARTER.get()) && p.getInventory().items.stream().filter(s->s.is(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum()==1,"Starter is consumed, cycles a filled filter and returns one bottle");
        var f=tank.data.newFish(AquariumData.Species.GUPPY);tank.data.add(f);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AquariumContent.FOOD.get()));
        AquariumContent.CONTROLLER.get().applyItem(p.getMainHandItem(),tank.getBlockState(),h.getLevel(),pos,p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tank.fedAt==h.getLevel().getGameTime() && tank.data.food==20,"Feeding triggers real reserve and timed visual reaction");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));p.setShiftKeyDown(true);AquariumContent.CONTROLLER.get().applyItem(p.getMainHandItem(),tank.getBlockState(),h.getLevel(),pos,p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tank.data.filled && p.getMainHandItem().is(Items.BUCKET),"Occupied tanks cannot accidentally be drained");
        pass(h,"starterFeedingAndDrainProtection");
    }

    @GameTest(template="aquarium_empty") public static void largeBoundsAndThreeDimensionalPersistence(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.STONE);Player p=player(h);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AquariumContent.KITS.get(AquariumData.Size.LARGE).get()));
        h.assertTrue(place(h,p).consumesAction(),"Large kit must place");
        var tank=(AquariumBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
        var bounds=tank.getRenderBoundingBox();
        h.assertTrue(bounds.getXsize()==4 && bounds.getYsize()==2 && bounds.getZsize()==2,"Render bounds must include every part, not only the origin");
        tank.data.scape.add(new AquariumScape.Piece(AquariumScape.Material.BLOCK,.5,.5,1,.2,1.5,.8,1.2,UUID.randomUUID(),"minecraft:oak_planks"));
        var original=tank.data.scape.pieces().getFirst();
        var registries=h.getLevel().registryAccess();
        var copy=new AquariumBlockEntity(tank.getBlockPos(),tank.getBlockState());copy.loadWithComponents(tank.getUpdateTag(registries),registries);
        h.assertTrue(copy.data.scape.pieces().getFirst().equals(original),"Update packet must retain 3D transform and block identity");
        var drops=Block.getDrops(tank.getBlockState(),h.getLevel(),tank.getBlockPos(),tank);
        var saved=drops.getFirst().get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        var restored=new AquariumData();AquariumNbt.load(restored,saved.copyTag().getCompound("Aquarium"));
        h.assertTrue(restored.scape.pieces().getFirst().equals(original),"Carried kit must retain 3D transforms");pass(h,"largeBoundsAndThreeDimensionalPersistence");
    }

}
