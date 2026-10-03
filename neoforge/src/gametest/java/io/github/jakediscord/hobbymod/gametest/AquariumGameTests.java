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
    @GameTest(template="aquarium_empty") public static void brokenGlassAndMissingWater(GameTestHelper h){
        var tank=tank(h);tank.water(true);tank.data.cycle=5;tank.data.add(tank.data.newFish(AquariumData.Species.GUPPY));
        h.setBlock(POS.offset(2,1,1),Blocks.AIR);
        h.assertTrue(tank.inspect()==AquariumBlockEntity.Condition.MISSING_WATER,"Missing water must be visible");
        tank.water(true);h.setBlock(POS.offset(2,1,0),Blocks.AIR);
        h.assertTrue(tank.inspect()==AquariumBlockEntity.Condition.BROKEN_GLASS,"Broken wall not detected");
        h.assertTrue(tank.data.fish().size()==1,"Wall damage lost resident data");
        h.setBlock(POS.offset(2,1,0),Blocks.GLASS);tank.water(true);
        h.assertTrue(tank.inspect()==AquariumBlockEntity.Condition.READY,"Repair/refill failed");pass(h,"brokenGlassAndMissingWater");
    }
    @GameTest(template="aquarium_empty") public static void occupiedFootprintDoesNotOverwrite(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.STONE);h.setBlock(POS.offset(4,1,1),Blocks.DIAMOND_BLOCK);
        Player p=player(h);h.assertTrue(!place(h,p).consumesAction(),"Obstructed footprint must fail before mutation");
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK,POS.offset(4,1,1));h.assertBlockPresent(Blocks.AIR,POS);
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
}
