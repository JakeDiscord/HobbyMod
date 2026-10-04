package io.github.jakediscord.hobbymod.terrarium;

import java.util.Random;

/** UUID-seeded cosmetic crawlers: smooth, bounded movement with no entity/pathfinding cost. */
public final class TerrariumMotion {
    public record Pose(double x,double z,double yaw,double bob){}
    public static Pose pose(TerrariumData.Resident r,double ticks){
        long seed=r.id.getMostSignificantBits()^r.id.getLeastSignificantBits()*7919;double period=300+Math.floorMod(seed,300),time=ticks/period;
        long step=(long)Math.floor(time);double t=time-step,u=t*t*(3-2*t);var a=point(seed,step);var b=point(seed,step+1);
        return new Pose(.1+.8*(a[0]*(1-u)+b[0]*u),.1+.8*(a[1]*(1-u)+b[1]*u),Math.atan2(b[1]-a[1],b[0]-a[0]),Math.sin(ticks*.12+seed%31)*.002);
    }
    /** Models face -Z; mathematical path headings are measured from +X. */
    public static double modelYaw(double heading){return -90-Math.toDegrees(heading);}
    private static double[] point(long seed,long step){var r=new Random(seed+step*104729);return new double[]{r.nextDouble(),r.nextDouble()};}
    private TerrariumMotion(){}
}
