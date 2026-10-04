package io.github.jakediscord.hobbymod.terrarium;

import io.github.jakediscord.hobbymod.aquarium.*;
import java.util.Random;

/** Bounded cosmetic steering, independent of world entities and server care intervals. */
public final class TerrariumMotion {
    public enum Activity { REST, EXPLORE, FORAGE, GROOM }
    public record Pose(double x,double z,double yaw,double bob,double gait,double head,Activity activity){}
    /** A client keeps one small agent per saved resident. No catch-up loops on chunk reload. */
    public static final class Agent {
        private final Random random;
        private double x,z,yaw,targetX,targetZ,speed,gait,clock=Double.NaN;
        private int remaining;
        private Activity activity=Activity.REST;
        public Agent(TerrariumData.Resident r){
            random=new Random(r.id.getMostSignificantBits()^r.id.getLeastSignificantBits()*7919);
            x=.12+random.nextDouble()*.76;z=.12+random.nextDouble()*.76;yaw=random.nextDouble()*Math.PI*2;
            remaining=20+random.nextInt(80);targetX=x;targetZ=z;
        }
        public Pose advance(TerrariumData.Resident r,double ticks,AquariumData tank){
            if(!Double.isFinite(clock) && tank!=null){for(int tries=0;tries<48 && blocked(x,z,tank,r);tries++){x=.12+random.nextDouble()*.76;z=.12+random.nextDouble()*.76;}targetX=x;targetZ=z;}
            if(!Double.isFinite(clock) || ticks<clock || ticks-clock>80)clock=ticks;
            int steps=(int)Math.min(80,Math.floor(ticks-clock));
            for(int i=0;i<steps;i++){step(r,tank);clock++;}
            double moving=Math.min(1,speed/.001),hop=r.species==TerrariumData.Species.TREE_FROG?Math.max(0,Math.sin((clock+randomSeed(r))*.22))*moving*.035:r.species==TerrariumData.Species.SPRINGTAIL?Math.max(0,Math.sin(gait*1.5))*moving*.004:0;
            double breath=(1+Math.sin(ticks*.09+randomSeed(r)))*.0007;
            return new Pose(x,z,yaw,hop+breath,gait,Math.sin(ticks*.055+randomSeed(r))*(activity==Activity.REST?.32:.10),activity);
        }
        private void step(TerrariumData.Resident r,AquariumData tank){
            if(--remaining<=0){
                if(activity==Activity.EXPLORE || activity==Activity.FORAGE){activity=random.nextInt(4)==0?Activity.GROOM:Activity.REST;remaining=35+random.nextInt(120);}
                else {
                    activity=tank!=null && tank.terrarium.food>0 && random.nextInt(3)==0?Activity.FORAGE:Activity.EXPLORE;
                    targetX=activity==Activity.FORAGE?.45+random.nextDouble()*.10:.10+random.nextDouble()*.80;
                    targetZ=activity==Activity.FORAGE?.45+random.nextDouble()*.10:.10+random.nextDouble()*.80;
                    remaining=70+random.nextInt(190);
                }
            }
            boolean walk=activity==Activity.EXPLORE || activity==Activity.FORAGE;
            double desired=walk?switch(r.species){case SNAIL->.00055;case TREE_FROG->.0032;case GECKO->.0038;case SPRINGTAIL->.002;case ISOPOD->.0013;}:0;
            if(r.species==TerrariumData.Species.TREE_FROG && Math.sin((clock+randomSeed(r))*.22)<-.2)desired=0;
            speed+=(desired-speed)*.16;
            double angle=Math.atan2(targetZ-z,targetX-x),delta=Math.atan2(Math.sin(angle-yaw),Math.cos(angle-yaw));if(walk)yaw+=Math.clamp(delta,-.07,.07);
            double nx=x+Math.cos(yaw)*speed,nz=z+Math.sin(yaw)*speed;
            if(nx<.07 || nx>.93 || nz<.07 || nz>.93 || blocked(nx,nz,tank,r)){
                speed=0;targetX=.15+random.nextDouble()*.70;targetZ=.15+random.nextDouble()*.70;yaw+=.15;
            }else{x=nx;z=nz;gait+=speed*(r.species==TerrariumData.Species.SNAIL?70:180);}
            if(Math.hypot(targetX-x,targetZ-z)<.025 && walk)remaining=0;
        }
        private boolean blocked(double nx,double nz,AquariumData tank,TerrariumData.Resident r){
            if(tank==null || tank.terrarium==null)return false;
            var land=tank.terrarium;
            if(Math.abs(land.terrain.sample(land,nx,nz)-land.terrain.sample(land,x,z))>.025)return true;
            double wx=.08+nx*(tank.size.blocksWide()-.16),wz=.08+nz*(tank.size.blocksDeep()-.16);
            double floor=.10+land.terrain.sample(land,nx,nz)*(tank.size.blocksHigh()*.88-.24)/(tank.size.height-2);
            for(var p:tank.scape.pieces()){
                if(p.material()!=AquariumScape.Material.ROCK && p.material()!=AquariumScape.Material.WOOD && p.material()!=AquariumScape.Material.BLOCK)continue;
                if(p.material()==AquariumScape.Material.BLOCK){var id=net.minecraft.resources.ResourceLocation.tryParse(p.block());if(id!=null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState().is(TerrariumActions.PLANTS))continue;}
                var b=TerrariumTerrain.bounds(p,tank);double pad=r.species.body*.2;
                if(b.Y()>floor+.01 && b.y()<floor+r.species.body && wx>b.x()-pad && wx<b.X()+pad && wz>b.z()-pad && wz<b.Z()+pad)return true;
            }return false;
        }
    }
    private static double randomSeed(TerrariumData.Resident r){return Math.floorMod(r.id.getLeastSignificantBits(),71);}
    /** Stateless seeded inspection pose; live renderers retain Agent instances. */
    public static Pose pose(TerrariumData.Resident r,double ticks){var agent=new Agent(r);agent.clock=ticks-60;return agent.advance(r,ticks,null);}
    /** Procedural models face -Z; path headings are measured from +X. */
    public static double modelYaw(double heading){return -90-Math.toDegrees(heading);}
    private TerrariumMotion(){}
}
