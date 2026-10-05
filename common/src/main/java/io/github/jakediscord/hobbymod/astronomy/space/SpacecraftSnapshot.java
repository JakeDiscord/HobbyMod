package io.github.jakediscord.hobbymod.astronomy.space;

import java.util.UUID;

/** Immutable server snapshot, never accepted from client packets. Positions are dimension-local blocks. */
public record SpacecraftSnapshot(UUID ship,String dimension,double x,double y,double z,double qx,double qy,double qz,double qw,double vx,double vy,double vz){
    public SpacecraftSnapshot {
        if(ship==null || dimension==null || dimension.isBlank())throw new IllegalArgumentException("Ship identity required");
        for(double value:new double[]{x,y,z,qx,qy,qz,qw,vx,vy,vz})if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid ship pose");
        double length=qx*qx+qy*qy+qz*qz+qw*qw;if(Math.abs(length-1)>.001)throw new IllegalArgumentException("Ship orientation must be normalized");
    }
}
