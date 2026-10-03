package io.github.jakediscord.hobbymod.aquarium;

/** Seeded wandering: individual waypoints, gliding starts/stops and gradual turns, without world AI. */
public final class AquariumMotion {
    public record Pose(double x,double y,double z,double yaw,double tail){}
    private static double random(long seed,long segment,int axis){
        long h=seed+segment*0x9E3779B97F4A7C15L+axis*0x632BE59BD9B4E019L;
        h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;h^=h>>>31;
        return (h>>>11)*0x1.0p-53;
    }
    private static double target(long seed,long segment,int axis){return .12+random(seed,segment,axis)*.76;}
    private static double coordinate(long seed,long segment,int axis,double t){
        double a=target(seed,segment,axis),b=target(seed,segment+1,axis),c=target(seed,segment+2,axis),d=target(seed,segment+3,axis);
        // Uniform cubic B-spline stays inside its waypoint hull and has continuous velocity.
        return (a*Math.pow(1-t,3)+b*(3*t*t*t-6*t*t+4)+c*(-3*t*t*t+3*t*t+3*t+1)+d*t*t*t)/6;
    }
    public static Pose pose(AquariumData.Fish fish,AquariumData.Size size,double ticks,boolean filled){
        long seed=fish.id.getMostSignificantBits()^Long.rotateLeft(fish.id.getLeastSignificantBits(),23);
        double duration=85+random(seed,0,7)*85;
        double elapsed=ticks*(fish.health<35?.35:1)/duration+random(seed,0,8)*20;
        long segment=(long)Math.floor(elapsed);double t=elapsed-segment;
        double w=size.width-2-.9,d=size.depth-2-.9;
        double x=1.45+coordinate(seed,segment,0,t)*w,z=1.45+coordinate(seed,segment,2,t)*d;
        double y=fish.species==AquariumData.Species.CORYDORAS?1.28+coordinate(seed,segment,1,t)*.12:
                1.3+coordinate(seed,segment,1,t)*Math.max(0,size.height-2.6);
        double nextX=coordinate(seed,segment,0,t+.001)-coordinate(seed,segment,0,t-.001);
        double nextZ=coordinate(seed,segment,2,t+.001)-coordinate(seed,segment,2,t-.001);
        return new Pose(x,filled?y:1.2,z,-Math.atan2(nextZ*d,nextX*w),filled?Math.sin(ticks*.28+seed%100)*.035:0);
    }
    private AquariumMotion(){}
}
