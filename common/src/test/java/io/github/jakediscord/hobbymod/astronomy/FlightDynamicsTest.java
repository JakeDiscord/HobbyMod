package io.github.jakediscord.hobbymod.astronomy;

import io.github.jakediscord.hobbymod.astronomy.space.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightDynamicsTest {
    @Test void cruiseIsBoundedAndBrakingStopsInertia(){
        var v=new SkyCatalog.Vector(0,0,0);for(int i=0;i<1000;i++)v=FlightDynamics.advance(v,35,-30,1,1,1,true);
        assertEquals(5.5,Math.sqrt(v.dot(v)),1e-8);
        for(int i=0;i<30;i++)v=FlightDynamics.advance(v,35,-30,-1,0,0,true);
        assertTrue(Math.sqrt(v.dot(v))<.001);
        v=new SkyCatalog.Vector(0,0,0);for(int i=0;i<200;i++)v=FlightDynamics.advance(v,0,0,1,0,1,false);
        assertTrue(Math.sqrt(v.dot(v))<=1.100001);
        assertEquals(new SkyCatalog.Vector(0,0,0),FlightDynamics.advance(v,Float.NaN,0,1,0,0,true));
        var right=FlightDynamics.advance(new SkyCatalog.Vector(0,0,0),0,0,0,1,0,true);assertTrue(right.x()<0 && right.z()==0,"D must move to the camera right when facing south");
    }
    @Test void arrivalRequiresCloseApproachBrakingAndPilotConsent(){
        assertFalse(FlightDynamics.canArrive(400,0,1));assertFalse(FlightDynamics.canArrive(80,2,1));assertFalse(FlightDynamics.canArrive(80,.2,0));
        assertFalse(FlightDynamics.canArrive(-1,.2,1));assertFalse(FlightDynamics.canArrive(Double.NaN,.2,1));assertTrue(FlightDynamics.canArrive(80,.2,1));
    }
    @Test void allBodiesHaveSeparatedReachableGatesAndCorrectArrivals(){
        for(var p:CelestialBodies.PLANETS){var a=SolarMap.position(p.target());assertTrue(Double.isFinite(a.dot(a)));
            for(var q:CelestialBodies.PLANETS)if(p!=q){var b=SolarMap.position(q.target());var delta=new SkyCatalog.Vector(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());assertTrue(delta.dot(delta)>200*200);}
            assertTrue(Math.sqrt(a.dot(a))<3500);assertEquals(p.solid()?"hobbymod:"+p.key():p.orbitDimension(),SolarMap.arrival(p.target()));
            assertTrue(SolarMap.habitat(SolarMap.arrival(p.target())));assertEquals(p.target(),SolarMap.body(SolarMap.arrival(p.target())));
        }
        assertEquals("minecraft:overworld",SolarMap.arrival(-1));assertFalse(SolarMap.habitat("minecraft:the_nether"));assertThrows(IllegalArgumentException.class,()->SolarMap.position(500));
    }
}
