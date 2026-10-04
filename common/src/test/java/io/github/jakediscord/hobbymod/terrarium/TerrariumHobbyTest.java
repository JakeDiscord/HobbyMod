package io.github.jakediscord.hobbymod.terrarium;

import io.github.jakediscord.hobbymod.aquarium.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrariumHobbyTest {
    @Test void gentleConditionsNeverDeleteResidentsAndRecover(){
        var d=new TerrariumData();assertFalse(d.introduce(TerrariumData.Species.ISOPOD,17));d.substrate=TerrariumData.Substrate.SOIL;assertTrue(d.introduce(TerrariumData.Species.ISOPOD,17));
        for(int i=0;i<500;i++)d.advance(true,0,0,17);assertEquals(3,d.residents().size());assertTrue(d.residents().stream().allMatch(r->r.vigor>=20));
        d.moisture=50;d.humidity=65;d.food=80;d.lamp=true;for(int i=0;i<20;i++){d.mist();d.moisture=60;d.advance(true,3,0,17);}assertTrue(d.residents().stream().allMatch(r->r.vigor>=60));assertEquals(12,d.light);
    }
    @Test void breedingIsBoundedAndCollectingKeepsIndividualVariants(){
        var d=new TerrariumData();d.substrate=TerrariumData.Substrate.MOSS;d.food=100;d.introduce(TerrariumData.Species.SPRINGTAIL,44);d.introduce(TerrariumData.Species.ISOPOD,44);
        for(int i=0;i<3000;i++){d.moisture=55;d.food=100;d.advance(true,5,12,44);}
        assertEquals(TerrariumData.MAX_RESIDENTS,d.residents().size());assertTrue(d.births>0);assertTrue(d.residents().stream().anyMatch(r->r.parent!=null));
        var taken=d.take(TerrariumData.Species.ISOPOD);assertEquals(3,taken.size());for(var r:taken){var copy=TerrariumNbt.resident(TerrariumNbt.resident(r));assertEquals(r.id,copy.id);assertEquals(r.variant,copy.variant);assertTrue(d.accept(copy));assertFalse(d.accept(copy));}
    }
    @Test void landStateAndArbitraryRotationSurviveNbtAndLegacyTanksStayAquatic(){
        var d=new AquariumData();d.size=AquariumData.Size.MEDIUM;d.terrarium=new TerrariumData();d.terrarium.substrate=TerrariumData.Substrate.SAND;d.terrarium.drainage=true;d.terrarium.open=true;d.terrarium.lamp=true;d.terrarium.introduce(TerrariumData.Species.ISOPOD,918);d.substrate=true;
        var id=UUID.randomUUID();d.scape.add(new AquariumScape.Piece(AquariumScape.Material.BLOCK,.5,.5,0,.1,2,1.5,2,id,"minecraft:oak_planks"));assertTrue(d.scape.angles(id,d.size,37.5,12.25,-8.5));
        var restored=new AquariumData();AquariumNbt.load(restored,AquariumNbt.save(d));assertNotNull(restored.terrarium);assertTrue(restored.terrarium.open && restored.terrarium.drainage && restored.terrarium.lamp);assertEquals(TerrariumData.Substrate.SAND,restored.terrarium.substrate);assertEquals(3,restored.terrarium.residents().size());assertEquals(d.scape.pieces(),restored.scape.pieces());
        AquariumNbt.load(restored,AquariumNbt.save(new AquariumData()));assertNull(restored.terrarium);
    }
    @Test void rotatedBoundingBoxesRemainInsideAndInvalidAnglesAreRejected(){
        var r=new Random(42);for(var size:AquariumData.Size.values())for(int i=0;i<2000;i++){
            var scape=new AquariumScape();var id=UUID.randomUUID();scape.add(new AquariumScape.Piece(AquariumScape.Material.BLOCK,.08+r.nextDouble()*.84,.08+r.nextDouble()*.84,0,r.nextDouble()*.92,.15+r.nextDouble()*3.85,.15+r.nextDouble()*3.85,.15+r.nextDouble()*3.85,id,"minecraft:stone"));
            assertTrue(scape.angles(id,size,r.nextDouble()*720-360,r.nextDouble()*720-360,r.nextDouble()*720-360));var p=scape.pieces().getFirst();var b=AquariumScape.bounds(p,size);
            assertTrue(b.x()>=.079 && b.z()>=.079 && b.y()>=.1);assertTrue(b.X()<=size.blocksWide()-.079 && b.Z()<=size.blocksDeep()-.079 && b.Y()<=size.blocksHigh()*.88-.13,b.toString());
            assertFalse(scape.angles(id,size,Double.NaN,0,0));assertFalse(scape.angles(id,size,0,361,0));
        }
    }
    @Test void modelForwardMatchesCrawlerTravelHeading(){
        for(int i=-360;i<=360;i++){
            double heading=Math.toRadians(i),yaw=Math.toRadians(TerrariumMotion.modelYaw(heading));
            assertEquals(Math.cos(heading),-Math.sin(yaw),1e-9);assertEquals(Math.sin(heading),-Math.cos(yaw),1e-9);
        }
    }
    @Test void sculptedTerrainIsBoundedSmoothAndSurvivesCarry(){
        var d=new AquariumData();d.terrarium=new TerrariumData();d.terrarium.substrate=TerrariumData.Substrate.SAND;d.terrarium.drainage=true;d.terrarium.heatLamp=true;
        var t=d.terrarium;assertEquals(.42,t.terrain.sample(t,.5,.5),1e-9);
        for(int i=0;i<25;i++)t.terrain.brush(t,.25,.5,.3,0);
        assertTrue(t.terrain.sample(t,.25,.5)>.60);for(int i=0;i<30;i++)t.terrain.brush(t,.75,.5,.3,1);
        assertTrue(t.terrain.sample(t,.75,.5)<.30);var before=t.terrain.saved();assertTrue(t.terrain.brush(t,.25,.5,.3,2));assertFalse(java.util.Arrays.equals(before,t.terrain.saved()));
        for(double x=0;x<=1;x+=.02)for(double z=0;z<=1;z+=.02){double y=t.terrain.sample(t,x,z);assertTrue(y>=.20-1e-9 && y<=1.2);}
        assertFalse(t.terrain.brush(t,Double.NaN,0,.2,0));assertFalse(t.terrain.brush(t,.5,.5,99,0));assertFalse(t.terrain.brush(t,.5,.5,.2,4));
        var restored=new AquariumData();AquariumNbt.load(restored,AquariumNbt.save(d));assertArrayEquals(t.terrain.saved(),restored.terrarium.terrain.saved());assertTrue(restored.terrarium.heatLamp);
        restored.terrarium.advance(true,3,0,42);assertEquals(14,restored.terrarium.light);
    }
    @Test void raisedTerrainKeepsDecorationsWithinGlass(){
        var d=new AquariumData();d.size=AquariumData.Size.MEDIUM;d.terrarium=new TerrariumData();d.terrarium.substrate=TerrariumData.Substrate.SAND;d.terrarium.drainage=true;
        var id=UUID.randomUUID();d.scape.add(new AquariumScape.Piece(AquariumScape.Material.BLOCK,.5,.5,0,0,2,3,2,id,"minecraft:fern"));
        for(int i=0;i<30;i++)d.terrarium.terrain.brush(d.terrarium,.5,.5,.5,0);
        var loaded=new AquariumData();AquariumNbt.load(loaded,AquariumNbt.save(d));assertTrue(TerrariumTerrain.bounds(loaded.scape.pieces().getFirst(),loaded).Y()<=loaded.size.blocksHigh()*.88-.14+1e-8);
        TerrariumTerrain.fitDecor(d);
        var p=d.scape.pieces().getFirst();var b=TerrariumTerrain.bounds(p,d);assertTrue(b.Y()<=d.size.blocksHigh()*.88-.14+1e-8);
        assertEquals(.1+d.terrarium.terrain.sample(d.terrarium,.5,.5)*(.88-.24)/(d.size.height-2),b.y(),1e-9);
    }
    @Test void manyCrawlersStayInsideAndClockPausesOnUnloadOrDiscontinuousTime(){
        for(int i=0;i<1000;i++){var r=new TerrariumData.Resident(new UUID(918,i),TerrariumData.Species.ISOPOD,i%4);for(int t=-1000;t<=10000;t+=50){var p=TerrariumMotion.pose(r,t);assertTrue(p.x()>=.1 && p.x()<=.9 && p.z()>=.1 && p.z()<=.9);assertTrue(Double.isFinite(p.yaw()));}}
        var clock=new AquariumClock();for(int t=0;t<1190;t++)assertFalse(clock.poll(t));assertFalse(clock.poll(10000));for(int t=10001;t<11200;t++)assertFalse(clock.poll(t));assertTrue(clock.poll(11200));
        var d=new TerrariumData();d.moisture=60;d.humidity=70;d.advance(false,3,8,1);assertEquals(60,d.moisture);assertEquals(70,d.humidity);
    }
}
