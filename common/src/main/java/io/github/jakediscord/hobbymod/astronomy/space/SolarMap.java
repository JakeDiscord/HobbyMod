package io.github.jakediscord.hobbymod.astronomy.space;

import io.github.jakediscord.hobbymod.astronomy.*;

/** A fixed, compressed transfer map. Time commands cannot move arrival gates through pilots. */
public final class SolarMap {
    public static final String TRANSFER="hobbymod:solar_system";
    public static final SkyCatalog.Vector EARTH=new SkyCatalog.Vector(850,128,0);
    public static SkyCatalog.Vector position(int target){
        if(target==-1)return EARTH;
        var planet=CelestialBodies.target(target);if(planet==null)throw new IllegalArgumentException("Unknown destination");
        var p=planet.position(0).unit().scale(400+450*Math.sqrt(planet.orbitAU()));
        return new SkyCatalog.Vector(p.x(),128+p.y()*.15,p.z());
    }
    public static String name(int target){var p=CelestialBodies.target(target);return p==null?"Earth":p.name();}
    public static String arrival(int target){var p=CelestialBodies.target(target);return p==null?"minecraft:overworld":p.surfaceDimension().orElse(p.orbitDimension());}
    public static boolean habitat(String dimension){return dimension.equals(TRANSFER) || CelestialBodies.PLANETS.stream().anyMatch(p->p.orbitDimension().equals(dimension) || p.surfaceDimension().orElse("").equals(dimension));}
    public static boolean voidSpace(String dimension){return dimension.equals(TRANSFER) || dimension.startsWith("hobbymod:orbit/");}
    public static int body(String dimension){return CelestialBodies.PLANETS.stream().filter(p->p.orbitDimension().equals(dimension) || p.surfaceDimension().orElse("").equals(dimension)).mapToInt(CelestialBodies.Planet::target).findFirst().orElse(-1);}
    private SolarMap(){}
}
