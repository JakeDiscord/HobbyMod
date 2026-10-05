package io.github.jakediscord.hobbymod.astronomy.space;

import io.github.jakediscord.hobbymod.astronomy.SkyCatalog;

/** Server flight model: bounded inputs, inertial cruise, braking and altitude thrusters. */
public final class FlightDynamics {
    public static SkyCatalog.Vector advance(SkyCatalog.Vector velocity,float yaw,float pitch,int forward,int strafe,int vertical,boolean space){
        if(!Float.isFinite(yaw) || !Float.isFinite(pitch))return new SkyCatalog.Vector(0,0,0);
        forward=Math.clamp(forward,-1,1);strafe=Math.clamp(strafe,-1,1);vertical=Math.clamp(vertical,-1,1);
        var aim=SkyCatalog.aim(yaw,Math.clamp(pitch,-85,85));double radians=Math.toRadians(yaw),acceleration=space?.16:.065;
        double drag=forward<0?.68:space?.997:.92;
        var v=new SkyCatalog.Vector(velocity.x()*drag+(forward>0?aim.x()*acceleration:0)-Math.cos(radians)*strafe*acceleration*.65,
            velocity.y()*drag+(forward>0?aim.y()*acceleration:0)+vertical*acceleration,
            velocity.z()*drag+(forward>0?aim.z()*acceleration:0)-Math.sin(radians)*strafe*acceleration*.65);
        double speed=Math.sqrt(v.dot(v)),limit=space?5.5:1.1;return speed>limit?v.scale(limit/speed):v;
    }
    public static boolean canArrive(double distance,double speed,int vertical){return Double.isFinite(distance) && Double.isFinite(speed) && distance<=100 && distance>=0 && speed<.75 && vertical>0;}
    private FlightDynamics(){}
}
