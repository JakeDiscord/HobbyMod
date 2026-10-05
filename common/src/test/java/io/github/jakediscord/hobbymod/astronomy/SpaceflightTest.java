package io.github.jakediscord.hobbymod.astronomy;

import io.github.jakediscord.hobbymod.astronomy.space.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class SpaceflightTest {
    @Test void unknownObjectsCannotBeAutomaticallyPointedAtOrTracked(){
        var journal=new ObservationJournal();var sky=new SkyCatalog(1);var planet=sky.target(25);
        assertFalse(ObservationAccess.canPoint(journal,planet));assertFalse(ObservationAccess.canTrack(journal,planet,true));
        journal.load(25,100,0,0,1,0,0);assertFalse(ObservationAccess.canPoint(journal,planet));
        assertFalse(journal.record(25,1,18000,1000)); // Completed-but-unobserved imported rows still do not unlock motors.
        journal.load(25,0,0,0,0,0,Long.MIN_VALUE);assertTrue(journal.record(25,1,18000,1000));
        assertTrue(ObservationAccess.canPoint(journal,planet));assertTrue(ObservationAccess.canTrack(journal,planet,true));assertFalse(ObservationAccess.canTrack(journal,planet,false));
        journal.record(19,1,18000,1000);assertTrue(ObservationAccess.canPoint(journal,sky.target(19)));assertFalse(ObservationAccess.canTrack(journal,sky.target(19),true));assertFalse(ObservationAccess.canPoint(journal,sky.target(999)));
    }
    @Test void cubeFramesAreSpatialAndChangeWithCelestialRotation(){
        for(int id:new int[]{0,19,22,25,26,28})for(double time:new double[]{0,18000,42000,999999}){
            var f=CelestialGeometry.frame(id,time);assertEquals(1,f.x().dot(f.x()),1e-10);assertEquals(1,f.y().dot(f.y()),1e-10);assertEquals(1,f.z().dot(f.z()),1e-10);assertEquals(0,f.x().dot(f.y()),1e-10);assertEquals(0,f.y().dot(f.z()),1e-10);
            var c=CelestialGeometry.cube(new SkyCatalog.Vector(1,2,3),.006,f);assertEquals(8,c.length);assertEquals(8,Arrays.stream(c).distinct().count());
            var edges=new HashMap<String,Integer>();for(var face:CelestialGeometry.FACES){assertEquals(4,Arrays.stream(face).distinct().count());for(int i=0;i<4;i++){int a=face[i],b=face[(i+1)%4];edges.merge(Math.min(a,b)+":"+Math.max(a,b),1,Integer::sum);}}
            assertEquals(12,edges.size());assertTrue(edges.values().stream().allMatch(n->n==2));
            for(var v:c){var d=new SkyCatalog.Vector(v.x()-1,v.y()-2,v.z()-3);assertEquals(Math.sqrt(3)*.006,Math.sqrt(d.dot(d)),1e-10);}
        }
        assertNotEquals(CelestialGeometry.frame(25,18000),CelestialGeometry.frame(25,18000+600));
    }
    @Test void skyAndFutureNavigationUseTheSamePlanetEphemeris(){
        var c=new SkyCatalog(19);var keys=new HashSet<String>();for(var body:CelestialBodies.PLANETS){assertTrue(keys.add(body.key()));assertEquals(body.name(),c.target(body.target()).name());for(long time:new long[]{0,18000,42000,999999}){
            var direction=CelestialGeometry.observer(body.position(time).unit(),time);var sky=c.target(body.target()).direction(time);assertEquals(sky.x(),direction.x(),1e-9);assertEquals(sky.y(),direction.y(),1e-9);assertEquals(sky.z(),direction.z(),1e-9);
        }}
        assertEquals(7,keys.size());assertTrue(CelestialBodies.named("mars").surfaceDimension().isPresent());assertTrue(CelestialBodies.named("jupiter").surfaceDimension().isEmpty());
    }
    @Test void routesRequireRealDiscoveryAndFiniteShipPoses(){
        var owner=UUID.randomUUID();var ship=new SpacecraftSnapshot(UUID.randomUUID(),"minecraft:overworld",1,2,3,0,0,0,1,0,0,0);var journal=new ObservationJournal();
        assertThrows(IllegalArgumentException.class,()->FlightRoute.chart(owner,ship,"mars",18000,journal));
        journal.record(24,.8,18000,100);var route=FlightRoute.chart(owner,ship,"mars",18000,journal);assertEquals("hobbymod:orbit/mars",route.arrivalDimension());assertEquals(CelestialBodies.named("mars").position(42000),route.orbitalPosition(42000));
        assertThrows(IllegalArgumentException.class,()->FlightRoute.chart(owner,ship,"made_up",18000,journal));
        assertThrows(IllegalArgumentException.class,()->new SpacecraftSnapshot(ship.ship(),"minecraft:overworld",Double.NaN,2,3,0,0,0,1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new SpacecraftSnapshot(ship.ship(),"minecraft:overworld",1,2,3,0,0,0,4,0,0,0));
    }
}
