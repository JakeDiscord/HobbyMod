package io.github.jakediscord.hobbymod.terrarium;

import io.github.jakediscord.hobbymod.aquarium.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrariumHobbyTest {
    @Test void decliningHealthRecoversUntilDeathAndBodiesRequireRemoval(){
        var d=new TerrariumData();d.substrate=TerrariumData.Substrate.SOIL;d.introduce(TerrariumData.Species.ISOPOD,17);
        for(int i=0;i<20;i++)d.advance(true,0,0,17);assertTrue(d.residents().stream().allMatch(r->r.vigor<100 && r.alive()));
        d.humidity=65;d.food=80;d.lamp=true;for(int i=0;i<30;i++){d.moisture=60;d.advance(true,3,0,17);}assertTrue(d.residents().stream().allMatch(r->r.vigor==100));assertEquals(12,d.light);
        int count=d.residents().size();d.food=0;for(int i=0;i<100;i++)d.advance(true,0,0,17);assertEquals(count,d.bodies());assertTrue(d.take(TerrariumData.Species.ISOPOD).isEmpty());
        var loaded=TerrariumNbt.load(TerrariumNbt.save(d));loaded.residents().getFirst().age=2;loaded.food=100;loaded.humidity=65;for(int i=0;i<20;i++){loaded.moisture=60;loaded.advance(true,3,12,17);}assertEquals(count,loaded.bodies());assertEquals(2,loaded.residents().getFirst().age,"Dead juveniles must not continue growing");
        assertEquals(count,loaded.removeBodies());assertEquals(0,loaded.removeBodies());assertTrue(loaded.residents().isEmpty());
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
    @Test void buriedDecorStaysAboveFloorAndResetPreservesPositionAndScale(){
        var d=new AquariumData();d.size=AquariumData.Size.MEDIUM;d.terrarium=new TerrariumData();d.terrarium.substrate=TerrariumData.Substrate.SAND;d.terrarium.drainage=true;
        var id=UUID.randomUUID();d.scape.add(new AquariumScape.Piece(AquariumScape.Material.BLOCK,.5,.5,0,0,1,1,1,id,"minecraft:stone"));
        assertTrue(d.scape.transform(id,d.size,.5,-.1,.5,0,1,1,1));TerrariumTerrain.fitDecor(d);
        var p=d.scape.pieces().getFirst();assertEquals(-.1,p.y(),1e-9);assertTrue(TerrariumTerrain.bounds(p,d).y()<.1+.42*.32);
        assertTrue(d.scape.angles(id,d.size,34,20,-9));var tilted=d.scape.pieces().getFirst();assertTrue(d.scape.angles(id,d.size,0,0,0));p=d.scape.pieces().getFirst();assertEquals(tilted.x(),p.x());assertEquals(tilted.y(),p.y());assertEquals(tilted.scaleX(),p.scaleX());assertEquals(0,p.pitch());
        var restored=new AquariumData();AquariumNbt.load(restored,AquariumNbt.save(d));assertEquals(p.y(),restored.scape.pieces().getFirst().y(),1e-9);
        assertTrue(d.scape.transform(id,d.size,.5,-.45,.5,0,1,1,1));TerrariumTerrain.fitDecor(d);assertTrue(TerrariumTerrain.bounds(d.scape.pieces().getFirst(),d).y()>=.105-1e-9);
        var aquatic=AquariumNbt.save(d);aquatic.remove("Terrarium");AquariumNbt.load(restored,aquatic);assertTrue(restored.scape.pieces().isEmpty());
    }
    @Test void allSpeciesExploreRestTurnGraduallyAndHandleClockJumps(){
        for(var species:TerrariumData.Species.values()){
            var r=new TerrariumData.Resident(new UUID(91,species.ordinal()+4),species,0);var agent=new TerrariumMotion.Agent(r);
            var first=agent.advance(r,0,null);var prev=first;boolean moved=false,rested=false;
            for(int t=1;t<4000;t++){
                var p=agent.advance(r,t,null);assertTrue(p.x()>=.07 && p.x()<=.93 && p.z()>=.07 && p.z()<=.93);assertTrue(Math.hypot(p.x()-prev.x(),p.z()-prev.z())<(species==TerrariumData.Species.TREE_FROG?.0101:.0041));
                assertTrue(Math.abs(Math.atan2(Math.sin(p.yaw()-prev.yaw()),Math.cos(p.yaw()-prev.yaw())))<=.151);
                moved|=Math.hypot(p.x()-first.x(),p.z()-first.z())>.025;rested|=p.activity()==TerrariumMotion.Activity.REST;prev=p;
            }
            assertTrue(moved,species.toString());assertTrue(rested);var jump=agent.advance(r,999999,null);assertEquals(prev.x(),jump.x());var rewind=agent.advance(r,-1,null);assertEquals(jump.z(),rewind.z());
        }
    }
    @Test void newAnimalsKeepIdentityVariantsAndStarterCounts(){
        var d=new TerrariumData();d.substrate=TerrariumData.Substrate.SOIL;
        for(var species:TerrariumData.Species.values())assertTrue(d.introduce(species,51));assertEquals(9,d.residents().size());
        var restored=TerrariumNbt.load(TerrariumNbt.save(d));assertEquals(9,restored.residents().size());
        for(int i=0;i<9;i++){assertEquals(d.residents().get(i).id,restored.residents().get(i).id);assertEquals(d.residents().get(i).species,restored.residents().get(i).species);}
    }

    @Test void trappedResidentsRestWithoutSpinningAndResumeAfterObstacleRemoval(){
        for(var species:TerrariumData.Species.values()){
            var d=new AquariumData();d.size=AquariumData.Size.MEDIUM;d.terrarium=new TerrariumData();d.terrarium.substrate=TerrariumData.Substrate.SAND;
            var r=new TerrariumData.Resident(new UUID(66,species.ordinal()+11),species,0);var agent=new TerrariumMotion.Agent(r);var initial=agent.advance(r,0,d);
            d.scape.add(new AquariumScape.Piece(AquariumScape.Material.ROCK,initial.x(),initial.z(),0,0,2,2,2,UUID.randomUUID(),""));
            var prev=initial;for(int t=1;t<=1000;t++){var next=agent.advance(r,t,d);assertEquals(initial.x(),next.x(),1e-9);assertEquals(initial.z(),next.z(),1e-9);assertEquals(initial.yaw(),next.yaw(),1e-9);prev=next;}
            d.scape.remove(0);boolean moved=false;for(int t=1001;t<=2500;t++){var next=agent.advance(r,t,d);moved|=Math.hypot(next.x()-initial.x(),next.z()-initial.z())>.02;}assertTrue(moved,species.toString());
            r.vigor=0;var body=agent.advance(r,2501,d);for(int t=2502;t<2700;t++){var next=agent.advance(r,t,d);assertEquals(body,next);}
        }
    }
    @Test void motionAndHopTimingDoNotDependOnRenderFrameRate(){
        var r=new TerrariumData.Resident(new UUID(72,19),TerrariumData.Species.TREE_FROG,0);var slow=new TerrariumMotion.Agent(r);var fast=new TerrariumMotion.Agent(r);slow.advance(r,0,null);fast.advance(r,0,null);
        int hopping=0;for(int tick=1;tick<=2000;tick++){
            for(int frame=1;frame<=6;frame++)fast.advance(r,tick-1+frame/6.0,null);
            var a=slow.advance(r,tick,null);var b=fast.advance(r,tick,null);assertEquals(a,b);if(a.jump()>=0)hopping++;
        }assertTrue(hopping>0 && hopping<500,"Hops are brief, separated by idle intervals");
    }

}
