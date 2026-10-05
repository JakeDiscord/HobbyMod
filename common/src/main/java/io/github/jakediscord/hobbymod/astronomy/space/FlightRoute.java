package io.github.jakediscord.hobbymod.astronomy.space;

import io.github.jakediscord.hobbymod.astronomy.*;
import java.util.UUID;

/** A charted route is not a launch authorization: terrain, propulsion and dimension transfer come later. */
public record FlightRoute(UUID owner,SpacecraftSnapshot departure,String planet,long chartedTime){
    public FlightRoute {
        if(owner==null || departure==null || CelestialBodies.named(planet)==null)throw new IllegalArgumentException("Invalid flight route");
    }
    public static FlightRoute chart(UUID owner,SpacecraftSnapshot ship,String key,long time,ObservationJournal journal){
        var body=CelestialBodies.named(key);if(body==null || !ObservationAccess.discovered(journal,body.target()))throw new IllegalArgumentException("Discover the destination first");
        return new FlightRoute(owner,ship,key,time);
    }
    public String arrivalDimension(){return CelestialBodies.named(planet).orbitDimension();}
    public SkyCatalog.Vector orbitalPosition(double time){return CelestialBodies.named(planet).position(time);}
}
