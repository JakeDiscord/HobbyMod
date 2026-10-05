package io.github.jakediscord.hobbymod.astronomy;

import java.util.*;

/** Stable destination identities and physical metadata shared by sky rendering and future navigation. */
public final class CelestialBodies {
    public record Planet(String key,String name,int target,double orbitAU,double period,double longitude,double rotationDays,double tilt,double gravity,double pressure,boolean solid){
        public SkyCatalog.Vector position(double ticks){
            double angle=longitude+ticks/24000/period*Math.PI*2,dec=-.1+Math.sin(angle)*.08;
            return new SkyCatalog.Vector(Math.cos(dec)*Math.cos(angle)*orbitAU,Math.sin(dec)*orbitAU,Math.cos(dec)*Math.sin(angle)*orbitAU);
        }
        public String orbitDimension(){return "hobbymod:orbit/"+key;}
        public Optional<String> surfaceDimension(){return solid?Optional.of("hobbymod:"+key):Optional.empty();}
    }
    public static final List<Planet> PLANETS=List.of(
        new Planet("mercury","Mercury",22,.39,8,0,58.6,.03,3.7,0,true),
        new Planet("venus","Venus",23,.72,12,1.42,-243,177.4,8.87,92,true),
        new Planet("mars","Mars",24,1.52,24,2.84,1.03,25.2,3.71,.006,true),
        new Planet("jupiter","Jupiter",25,5.2,60,4.26,.414,3.1,24.79,0,false),
        new Planet("saturn","Saturn",26,9.58,100,5.68,.444,26.7,10.44,0,false),
        new Planet("uranus","Uranus",27,19.2,160,7.10,-.718,97.8,8.69,0,false),
        new Planet("neptune","Neptune",28,30.05,220,8.52,.671,28.3,11.15,0,false)
    );
    public static Planet target(int id){return id>=22 && id<=28?PLANETS.get(id-22):null;}
    public static Planet named(String key){return PLANETS.stream().filter(p->p.key().equals(key)).findFirst().orElse(null);}
    private CelestialBodies(){}
}
