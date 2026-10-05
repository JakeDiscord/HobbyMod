package io.github.jakediscord.hobbymod.gametest;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.astronomy.*;
import io.github.jakediscord.hobbymod.astronomy.space.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("hobbymod") @PrefixGameTestTemplate(false)
public final class FlightGameTests {
    private static final BlockPos POS=new BlockPos(4,2,4);
    // A detached server player exercises riding and server authorization without joining
    // the player list or claiming a negotiated network connection in a headless test.
    private static net.minecraft.server.level.ServerPlayer pilot(GameTestHelper h){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"flight-test");
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
        };
        return p;
    }
    private static void pass(GameTestHelper h,String name){h.succeed();HobbyMod.LOGGER.info("FLIGHT_TEST_PASS {}",name);}
    @GameTest(template="aquarium_empty") public static void shipPlacementCostsOneAndBlockedPlacementCostsNothing(GameTestHelper h){
        for(int x=1;x<=7;x++)for(int z=1;z<=7;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().mayBuild=true;p.moveTo(Vec3.atCenterOf(h.absolutePos(POS.north(3))));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FlightContent.ITEM.get(),2));var floor=h.absolutePos(POS.below());
        var c=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false));h.setBlock(POS,Blocks.STONE);
        h.assertTrue(!FlightContent.ITEM.get().useOn(c).consumesAction() && p.getMainHandItem().getCount()==2,"Obstructed launch must not consume the craft");h.setBlock(POS,Blocks.AIR);
        h.assertTrue(FlightContent.ITEM.get().useOn(c).consumesAction() && p.getMainHandItem().getCount()==1,"Deployment consumes exactly one ship");h.assertEntitiesPresent(FlightContent.SHIP.get(),1);pass(h,"shipPlacementCostsOneAndBlockedPlacementCostsNothing");
    }
    @GameTest(template="aquarium_empty",timeoutTicks=80) public static void destinationsAreDiscoveredAndControlsExpire(GameTestHelper h){
        var p=pilot(h);var ship=new PrototypeShip(FlightContent.SHIP.get(),h.getLevel());ship.moveTo(Vec3.atCenterOf(h.absolutePos(POS)));ship.owner=p.getUUID();ship.setHome(h.absolutePos(POS));h.getLevel().addFreshEntity(ship);p.startRiding(ship,true);
        var j=AstronomyData.get(p.server).journal(p.getUUID());j.load(24,0,0,0,0,0,Long.MIN_VALUE);
        h.assertTrue(!ship.select(p,24),"Unknown destinations rejected by server");j.record(24,1,18000,1000);h.assertTrue(ship.select(p,24),"Recorded Mars can be charted");
        h.assertTrue(FlightPlanData.get(p.server).route(ship.getUUID())!=null,"Selection creates a real saved flight plan");h.assertTrue(!ship.select(p,19),"Stars are not landing destinations");
        ship.controls(p,Float.NaN,0,1,0,0);ship.tick();h.assertTrue(ship.getDeltaMovement().horizontalDistance()<.001,"Nonfinite inputs cannot move the craft");ship.controls(p,0,0,1,0,0);
        h.runAfterDelay(35,()->{h.assertTrue(ship.getDeltaMovement().length()<.08,"Missing input must stop the craft instead of continuing thrust");ship.discard();p.stopRiding();pass(h,"destinationsAreDiscoveredAndControlsExpire");});
    }
    @GameTest(template="aquarium_empty") public static void carryingShipPreservesHomeAndPacksOnce(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var ship=new PrototypeShip(FlightContent.SHIP.get(),h.getLevel());ship.owner=p.getUUID();ship.setHome(new BlockPos(25,80,40));ship.setOnGround(true);h.getLevel().addFreshEntity(ship);p.setShiftKeyDown(true);ship.interact(p,InteractionHand.MAIN_HAND);ship.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(ship.isRemoved(),"Packing removes the deployed ship");var item=p.getInventory().items.stream().filter(s->s.is(FlightContent.ITEM.get())).findFirst().orElseThrow();h.assertTrue(item.getCount()==1,"Exactly one craft is returned");var copy=new PrototypeShip(FlightContent.SHIP.get(),h.getLevel());copy.readPortable(item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag());h.assertTrue(copy.getUUID().equals(ship.getUUID()) && copy.home().equals(ship.home()) && copy.owner.equals(p.getUUID()),"Launch site and ownership survive packing");
        var bad=new CompoundTag();bad.putInt("Destination",500);copy.readPortable(bad);h.assertTrue(copy.destination()==-1,"Malformed destination falls back to Earth");pass(h,"carryingShipPreservesHomeAndPacksOnce");
    }
    @GameTest(template="aquarium_empty",timeoutTicks=400) public static void planetaryDimensionsAndShipIdentitySurviveTransfer(GameTestHelper h){
        var server=h.getLevel().getServer();var mars=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse("hobbymod:mars")));h.assertTrue(mars!=null,"Mars is an actual registered world");
        var ship=new PrototypeShip(FlightContent.SHIP.get(),h.getLevel());ship.owner=java.util.UUID.randomUUID();ship.setHome(h.absolutePos(POS));ship.moveTo(Vec3.atCenterOf(h.absolutePos(POS)));h.getLevel().addFreshEntity(ship);var id=ship.getUUID();mars.getChunk(0,0);
        var moved=(PrototypeShip)ship.changeDimension(new DimensionTransition(mars,new Vec3(.5,120,.5),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING));h.assertTrue(moved!=null && moved.getUUID().equals(id) && moved.home().equals(ship.home()) && moved.owner.equals(ship.owner),"Transfer preserves stable ship identity and launch site");
        var returned=(PrototypeShip)moved.changeDimension(new DimensionTransition(h.getLevel(),Vec3.atCenterOf(h.absolutePos(POS)),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING));h.assertTrue(returned!=null && returned.getUUID().equals(id),"A ship can return without duplicating identity");returned.discard();
        for(var planet:CelestialBodies.PLANETS)h.assertTrue(server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(SolarMap.arrival(planet.target()))))!=null,"Every arrival world exists");pass(h,"planetaryDimensionsAndShipIdentitySurviveTransfer");
    }
    @GameTest(template="aquarium_empty",timeoutTicks=400) public static void arrivalsLoadTerrainAndClearTheWholeShip(GameTestHelper h){
        var p=pilot(h);var mars=FlightTravel.level(p,"hobbymod:mars");var landing=FlightTravel.arrivalPosition(mars,java.util.UUID.randomUUID());var ship=new PrototypeShip(FlightContent.SHIP.get(),mars);ship.setPos(landing);
        h.assertTrue(mars.noCollision(ship,ship.getBoundingBox().inflate(.1)),"Unloaded terrain must be generated before choosing a clear landing height");
        var orbit=FlightTravel.level(p,"hobbymod:orbit/jupiter");var dock=FlightTravel.arrivalPosition(orbit,java.util.UUID.randomUUID());ship=new PrototypeShip(FlightContent.SHIP.get(),orbit);ship.setPos(dock);
        h.assertTrue(orbit.noCollision(ship,ship.getBoundingBox().inflate(.1)),"Orbital docking must leave room for the entire hull");h.assertTrue(orbit.getBlockState(BlockPos.containing(dock).atY(96)).is(Blocks.WHITE_CONCRETE),"Gas giant arrivals have a real platform");pass(h,"arrivalsLoadTerrainAndClearTheWholeShip");
    }

}
