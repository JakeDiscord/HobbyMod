package io.github.jakediscord.hobbymod.aquarium;

/** Deterministic swimming inside the fixed enclosure; no world pathfinding or entity ticks. */
public final class AquariumMotion {
    public record Pose(double x,double y,double z,double yaw,double tail){}
    public static Pose pose(AquariumData.Fish fish,AquariumData.Size size,double ticks,boolean filled){
        double phase=Math.floorMod(fish.id.getLeastSignificantBits(),10000)*.37+fish.species.ordinal()*.8;
        double t=(ticks%1_000_000)*.012*(fish.health<35?.35:1)+phase;
        double rx=(size.width-2)/2.0-.45,rz=(size.depth-2)/2.0-.45;
        double x=size.width/2.0+Math.sin(t)*rx,z=size.depth/2.0+Math.cos(t*.8)*rz;
        double y=fish.species==AquariumData.Species.CORYDORAS?1.3+Math.sin(t*.5)*.06:
                size.height/2.0+Math.sin(t*.6)*Math.max(0,(size.height-2)/2.0-.5);
        if(!filled)y=1.2;
        double dx=Math.cos(t)*rx,dz=-.8*Math.sin(t*.8)*rz;
        return new Pose(x,y,z,-Math.atan2(dz,dx),filled?Math.sin(t*8)*.15:0);
    }
    private AquariumMotion(){}
}
