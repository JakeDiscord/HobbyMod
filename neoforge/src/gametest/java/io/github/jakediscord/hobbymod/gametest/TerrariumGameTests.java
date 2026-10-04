package io.github.jakediscord.hobbymod.gametest;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.aquarium.*;
import io.github.jakediscord.hobbymod.terrarium.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("hobbymod") @PrefixGameTestTemplate(false)
public final class TerrariumGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static Player player(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().mayBuild=true;p.moveTo(Vec3.atCenterOf(h.absolutePos(POS.offset(-2,0,-2))));return p;}
    private static InteractionResult place(GameTestHelper h,Player p){var floor=h.absolutePos(POS.below());return ((AquariumKitItem)p.getMainHandItem().getItem()).place(new BlockPlaceContext(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false))));}
    private static AquariumBlockEntity tank(GameTestHelper h,Player p){h.setBlock(POS.below(),Blocks.STONE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TerrariumContent.KIT.get()));h.assertTrue(place(h,p).consumesAction(),"Terrarium should place");return (AquariumBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static void use(Player p,AquariumBlockEntity tank,Item item,int count){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,count));TerrariumActions.apply(p.getMainHandItem(),tank,p,InteractionHand.MAIN_HAND);}
    private static void pass(GameTestHelper h,String name){h.succeed();HobbyMod.LOGGER.info("TERRARIUM_TEST_PASS {}",name);}
    @GameTest(template="aquarium_empty") public static void landPlacementAndRotatedParts(GameTestHelper h){
        var p=player(h);p.setYRot(-90);var tank=tank(h,p);h.assertTrue(tank.data.terrarium!=null && tank.data.size==AquariumData.Size.MEDIUM,"Kit must enter land mode with a 2x1 model");
        h.assertTrue(tank.blocksWide()==1 && tank.blocksDeep()==2,"Rotation must swap the footprint");h.assertTrue(tank.inspect()==AquariumBlockEntity.Condition.READY,"Dry land enclosure should be ready");
        h.assertTrue(!tank.water(true) && h.getLevel().getFluidState(tank.getBlockPos()).isEmpty(),"Terrariums must not accept aquarium water");h.assertTrue(AquariumPartBlock.find(h.getLevel(),h.absolutePos(POS.south()))==tank,"Every part should open the land controller");pass(h,"landPlacementAndRotatedParts");
    }
    @GameTest(template="aquarium_empty") public static void suppliesConsumeAndLayersReturnExactly(GameTestHelper h){
        var p=player(h);var tank=tank(h,p);use(p,tank,Items.GRAVEL,2);h.assertTrue(tank.data.terrarium.drainage && p.getMainHandItem().getCount()==1,"Drainage must consume actual gravel");
        use(p,tank,Items.DIRT,2);h.assertTrue(tank.data.terrarium.substrate==TerrariumData.Substrate.SOIL && p.getMainHandItem().getCount()==1,"Soil must consume actual dirt");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);int dirtBefore=p.getInventory().countItem(Items.DIRT);use(p,tank,Items.SAND,2);h.assertTrue(tank.data.terrarium.substrate==TerrariumData.Substrate.SAND && p.getMainHandItem().getCount()==1 && p.getInventory().countItem(Items.DIRT)==dirtBefore+1,"Changing substrate returns its exact old material");
        use(p,tank,TerrariumContent.MISTER.get(),1);h.assertTrue(p.getMainHandItem().getDamageValue()==1 && tank.data.terrarium.moisture==73 && tank.data.terrarium.humidity==80,"Misting must change live state and use durability");
        use(p,tank,Items.GLOWSTONE_DUST,2);h.assertTrue(tank.data.terrarium.lamp && p.getMainHandItem().getCount()==1,"Lamp needs real glowstone");pass(h,"suppliesConsumeAndLayersReturnExactly");
    }
    @GameTest(template="aquarium_empty") public static void coloniesCollectVariantsWithoutEntities(GameTestHelper h){
        var p=player(h);var tank=tank(h,p);use(p,tank,Items.DIRT,1);use(p,tank,TerrariumContent.ISOPODS.get(),1);h.assertTrue(tank.data.terrarium.residents().size()==3 && p.getMainHandItem().isEmpty(),"A real colony introduces three residents");var first=tank.data.terrarium.residents().getFirst();
        p.setShiftKeyDown(true);use(p,tank,TerrariumContent.ISOPODS.get(),1);p.setShiftKeyDown(false);h.assertTrue(tank.data.terrarium.residents().isEmpty(),"Collect mode must remove saved residents");
        var carried=p.getInventory().items.stream().filter(s->s.is(TerrariumContent.ISOPODS.get()) && s.has(DataComponents.CUSTOM_DATA)).findFirst().orElseThrow();p.setItemInHand(InteractionHand.MAIN_HAND,carried.copy());TerrariumActions.apply(p.getMainHandItem(),tank,p,InteractionHand.MAIN_HAND);
        h.assertTrue(tank.data.terrarium.residents().getFirst().id.equals(first.id) && tank.data.terrarium.residents().getFirst().variant==first.variant,"Individual identity and color must survive collection");
        h.assertTrue(h.getLevel().getEntities(null,tank.getRenderBoundingBox()).isEmpty(),"Closed residents must not spawn entities or escape");pass(h,"coloniesCollectVariantsWithoutEntities");
    }
    @GameTest(template="aquarium_empty") public static void heatLampEmitsLightAndReturnsSupply(GameTestHelper h){
        var p=player(h);var tank=tank(h,p);use(p,tank,TerrariumContent.HEAT_LAMP.get(),2);
        h.assertTrue(tank.data.terrarium.heatLamp && p.getMainHandItem().getCount()==1,"Heat lamp consumes an actual supply");
        h.assertTrue(tank.getBlockState().getValue(AquariumControllerBlock.LIT) && tank.getBlockState().getLightEmission()==14,"Lamp must emit actual Minecraft block light");
        tank.data.terrarium.advance(true,1,0,42);h.assertTrue(tank.data.terrarium.light==14,"Lamp provides indoor ecosystem light");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);int before=p.getInventory().countItem(TerrariumContent.HEAT_LAMP.get());p.setShiftKeyDown(true);use(p,tank,TerrariumContent.HEAT_LAMP.get(),1);p.setShiftKeyDown(false);
        h.assertTrue(!tank.data.terrarium.heatLamp && !tank.getBlockState().getValue(AquariumControllerBlock.LIT) && p.getInventory().countItem(TerrariumContent.HEAT_LAMP.get())==before+2,"Removing lamp returns exactly one installed item and clears emitted light");pass(h,"heatLampEmitsLightAndReturnsSupply");
    }
    @GameTest(template="aquarium_empty") public static void plantedRotationAndCarriedEnclosurePersist(GameTestHelper h){
        var p=player(h);var tank=tank(h,p);use(p,tank,Items.MOSS_BLOCK,1);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FERN,2));h.assertTrue(TerrariumActions.add(p,tank,p.getMainHandItem(),.5,.5,0),"Native tagged fern should plant");h.assertTrue(p.getMainHandItem().getCount()==1 && TerrariumActions.plants(tank.data)==1,"Plant consumes one fern and participates in ecosystem");
        var piece=tank.data.scape.pieces().getFirst();h.assertTrue(tank.data.scape.angles(piece.id(),tank.data.size,37.5,12.25,-8.5),"Precise three-axis rotation must apply");tank.data.terrarium.open=true;tank.data.terrarium.mist();tank.data.terrarium.terrain.brush(tank.data.terrarium,.5,.5,.3,0);tank.data.terrarium.heatLamp=true;tank.changed();
        var drops=Block.getDrops(tank.getBlockState(),h.getLevel(),tank.getBlockPos(),tank);h.assertTrue(drops.getFirst().is(TerrariumContent.KIT.get()),"Mining land mode must return a terrarium kit");
        var saved=drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA);var restored=new AquariumData();AquariumNbt.load(restored,saved.copyTag().getCompound("Aquarium"));h.assertTrue(restored.terrarium!=null && restored.terrarium.open && restored.terrarium.moisture==73 && restored.scape.pieces().getFirst().yaw()==37.5,"Carried tank must retain land care and fine angles");
        h.getLevel().destroyBlock(tank.getBlockPos(),true);h.assertBlockPresent(Blocks.AIR,POS.east());p.setYRot(-90);p.setItemInHand(InteractionHand.MAIN_HAND,drops.getFirst());h.assertTrue(place(h,p).consumesAction(),"Carried enclosure must place again");var next=(AquariumBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));h.assertTrue(next.data.terrarium.moisture==73 && next.data.scape.pieces().getFirst().yaw()==37.5,"Actual replacement must restore all saved state");h.assertTrue(next.data.terrarium.heatLamp && next.getBlockState().getValue(AquariumControllerBlock.LIT) && java.util.Arrays.equals(next.data.terrarium.terrain.saved(),tank.data.terrarium.terrain.saved()),"Terrain relief and emitted heat-lamp light must survive carrying");pass(h,"plantedRotationAndCarriedEnclosurePersist");
    }
    @GameTest(template="aquarium_empty") public static void wildCritterCollectionKeepsIdentityAndCannotDuplicate(GameTestHelper h){
        var p=player(h);var wild=io.github.jakediscord.hobbymod.habitats.HabitatWildlife.CRITTER.get().create(h.getLevel());wild.setSpecies(TerrariumData.Species.GECKO,2);wild.moveTo(p.position());h.getLevel().addFreshEntity(wild);
        var id=wild.getUUID();var tool=new ItemStack(io.github.jakediscord.hobbymod.habitats.HabitatWildlife.NET.get());p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var net=(io.github.jakediscord.hobbymod.habitats.HabitatNet)tool.getItem();h.assertTrue(net.interactLivingEntity(tool,p,wild,InteractionHand.MAIN_HAND).consumesAction(),"Net must capture a real critter");
        h.assertTrue(wild.isRemoved() && tool.getDamageValue()==1,"Capture removes entity and wears net once");h.assertTrue(net.interactLivingEntity(tool,p,wild,InteractionHand.MAIN_HAND)==InteractionResult.FAIL,"Removed entity cannot duplicate a carrier");
        var carrier=p.getInventory().items.stream().filter(a->a.is(TerrariumContent.GECKOS.get())).findFirst().orElseThrow();var residents=carrier.get(DataComponents.CUSTOM_DATA).copyTag().getList("Colony",10);var resident=TerrariumNbt.resident(residents.getCompound(0));h.assertTrue(resident.id.equals(id) && resident.variant==2,"Wild individual identity and color persist in carrier");
        var tank=tank(h,p);use(p,tank,Items.DIRT,1);p.setItemInHand(InteractionHand.MAIN_HAND,carrier.copy());TerrariumActions.apply(p.getMainHandItem(),tank,p,InteractionHand.MAIN_HAND);h.assertTrue(tank.data.terrarium.residents().getFirst().id.equals(id),"Captured gecko enters the actual enclosure");pass(h,"wildCritterCollectionKeepsIdentityAndCannotDuplicate");
    }
    @GameTest(template="aquarium_empty") public static void wildFishNeedsWaterAndKeepsGenetics(GameTestHelper h){
        var p=player(h);var wild=io.github.jakediscord.hobbymod.habitats.HabitatWildlife.FISH.get().create(h.getLevel());var d=new AquariumData();var fish=d.newFish(AquariumData.Species.BETTA);wild.setNoAi(true);wild.setPersistenceRequired();wild.setResident(fish);h.assertTrue(wild.isNoAi() && wild.isPersistenceRequired(),"Assigning phenotype must preserve native entity settings");wild.moveTo(p.position());h.getLevel().addFreshEntity(wild);
        var tool=new ItemStack(io.github.jakediscord.hobbymod.habitats.HabitatWildlife.NET.get());p.setItemInHand(InteractionHand.MAIN_HAND,tool);var net=(io.github.jakediscord.hobbymod.habitats.HabitatNet)tool.getItem();
        h.assertTrue(net.interactLivingEntity(tool,p,wild,InteractionHand.MAIN_HAND)==InteractionResult.FAIL && !wild.isRemoved() && tool.getDamageValue()==0,"No water means no capture or durability loss");p.getInventory().setItem(9,new ItemStack(Items.WATER_BUCKET));
        h.assertTrue(net.interactLivingEntity(tool,p,wild,InteractionHand.MAIN_HAND).consumesAction(),"Water bucket enables capture");h.assertTrue(p.getInventory().countItem(Items.WATER_BUCKET)==0,"Captured fish carries the consumed water vessel");
        var carrier=p.getInventory().items.stream().filter(a->a.getItem() instanceof AquariumFishItem).findFirst().orElseThrow();var caught=AquariumNbt.fish(carrier.get(DataComponents.CUSTOM_DATA).copyTag());h.assertTrue(caught.id.equals(wild.getUUID()) && caught.species==fish.species && caught.colorA==fish.colorA && caught.formB==fish.formB,"Capture preserves wild identity, species and genes");pass(h,"wildFishNeedsWaterAndKeepsGenetics");
    }

    @GameTest(template="aquarium_empty") public static void naturalSpawnBiomesOfferEveryResidentSpecies(GameTestHelper h){
        var biomes=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var plains=biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);var river=biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.RIVER);var ocean=biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.WARM_OCEAN);
        h.assertTrue(plains.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap().stream().anyMatch(s->s.type==io.github.jakediscord.hobbymod.habitats.HabitatWildlife.CRITTER.get()),"Biome modifier must install actual wild land spawns");
        for(var biome:java.util.List.of(river,ocean))h.assertTrue(biome.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.WATER_AMBIENT).unwrap().stream().anyMatch(s->s.type==io.github.jakediscord.hobbymod.habitats.HabitatWildlife.FISH.get()),"Biome modifier must install actual wild fish spawns");
        var species=new java.util.HashSet<>(io.github.jakediscord.hobbymod.habitats.WildAquariumFish.speciesFor(river));species.addAll(io.github.jakediscord.hobbymod.habitats.WildAquariumFish.speciesFor(ocean));h.assertTrue(species.size()==AquariumData.Species.values().length,"Warm oceans and cool rivers together must offer all eight fish");pass(h,"naturalSpawnBiomesOfferEveryResidentSpecies");
    }

    @GameTest(template="aquarium_empty") public static void unhealthyResidentsDieAndCleanupDoesNotCreateLiveCarriers(GameTestHelper h){
        var p=player(h);var tank=tank(h,p);use(p,tank,Items.DIRT,1);use(p,tank,TerrariumContent.FROGS.get(),1);var land=tank.data.terrarium;var frog=land.residents().getFirst();frog.vigor=1;land.food=0;land.advance(true,0,8,42);h.assertTrue(!frog.alive(),"Unhealthy animals must die");
        p.setShiftKeyDown(true);use(p,tank,TerrariumContent.FROGS.get(),1);p.setShiftKeyDown(false);h.assertTrue(land.bodies()==1,"Collection must not resurrect a body into a carrier");
        var saved=new AquariumData();AquariumNbt.load(saved,AquariumNbt.save(tank.data));h.assertTrue(saved.terrarium.bodies()==1,"Saved body remains dead");int carriers=p.getInventory().countItem(TerrariumContent.FROGS.get());h.assertTrue(tank.removeBodies()==1 && tank.removeBodies()==0,"Cleanup removes a corpse exactly once");h.assertTrue(p.getInventory().countItem(TerrariumContent.FROGS.get())==carriers,"Cleanup creates no living animals");
        tank.data.terrarium=null;tank.data.filled=true;tank.data.cycle=5;var fish=tank.data.newFish(AquariumData.Species.GUPPY);tank.data.add(fish);fish.health=1;tank.data.food=0;tank.data.advance(true,false);h.assertTrue(!fish.alive(),"Unhealthy fish must die");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));AquariumContent.CONTROLLER.get().applyItem(p.getMainHandItem(),tank.getBlockState(),p.level(),tank.getBlockPos(),p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(tank.getBlockPos()),Direction.UP,tank.getBlockPos(),false));h.assertTrue(tank.data.bodies()==1 && p.getMainHandItem().is(Items.BUCKET),"Dead fish cannot be captured as living fish or consume a bucket");h.assertTrue(tank.removeBodies()==1 && tank.data.fish().isEmpty(),"Aquarium body cleanup must work on the actual tank");pass(h,"unhealthyResidentsDieAndCleanupDoesNotCreateLiveCarriers");
    }

}
