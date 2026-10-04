package io.github.jakediscord.hobbymod.terrarium;

import io.github.jakediscord.hobbymod.aquarium.*;
import java.util.*;

/** Bounded, tick-driven visual agents. Routes are checked before turning or moving. */
public final class TerrariumMotion {
    public enum Activity { REST, EXPLORE, FORAGE, GROOM }
    public static final int HOP_TICKS=10; // Vanilla frog jump animation lasts half a second.
    public record Pose(double x,double z,double yaw,double bob,double gait,double head,Activity activity,double jump){
        public Pose(double x,double z,double yaw,double bob,double gait,double head,Activity activity){this(x,z,yaw,bob,gait,head,activity,-1);}
    }
    public static final class Agent {
        private final Random random;
        private double x,z,yaw,targetX,targetZ,speed,gait,clock=Double.NaN,hopStart=-1,nextHop;
        private int remaining;
        private Activity activity=Activity.REST;
        private AquariumData environment;
        private long terrainRevision;
        private List<AquariumScape.Piece> pieces;
        private List<AquariumScape.Bounds> obstacles=List.of();
        public Agent(TerrariumData.Resident r){
            random=new Random(r.id.getMostSignificantBits()^r.id.getLeastSignificantBits()*7919);
            x=.12+random.nextDouble()*.76;z=.12+random.nextDouble()*.76;yaw=random.nextDouble()*Math.PI*2;
            remaining=40+random.nextInt(100);targetX=x;targetZ=z;
        }
        public Pose advance(TerrariumData.Resident r,double ticks,AquariumData tank){
            updateObstacles(tank);
            if(!Double.isFinite(clock) && tank!=null && r.alive()){
                for(int tries=0;tries<48 && blocked(x,z,tank,r);tries++){x=.12+random.nextDouble()*.76;z=.12+random.nextDouble()*.76;}
                targetX=x;targetZ=z;
            }
            if(!Double.isFinite(clock) || ticks<clock || ticks-clock>80){clock=ticks;hopStart=-1;nextHop=ticks+40;}
            if(!r.alive()){speed=0;hopStart=-1;return new Pose(x,z,yaw,0,gait,0,Activity.REST);}
            int steps=(int)Math.min(80,Math.floor(ticks-clock));
            for(int i=0;i<steps;i++){clock++;step(r,tank);}
            double jump=hopStart>=0 && ticks-hopStart<HOP_TICKS?Math.clamp((ticks-hopStart)/HOP_TICKS,0,1):-1;
            double bob=jump>=0?Math.sin(jump*Math.PI)*.025:r.species==TerrariumData.Species.SPRINGTAIL?Math.max(0,Math.sin(gait*1.5))*Math.min(1,speed/.001)*.003:0;
            double breath=(1+Math.sin(ticks*.07+randomSeed(r)))*.0005;
            return new Pose(x,z,yaw,bob+breath,gait,Math.sin(ticks*.035+randomSeed(r))*(activity==Activity.REST?.24:.08),activity,jump);
        }
        private void rest(){speed=0;hopStart=-1;activity=random.nextInt(5)==0?Activity.GROOM:Activity.REST;remaining=60+random.nextInt(120);targetX=x;targetZ=z;}
        private void step(TerrariumData.Resident r,AquariumData tank){
            if(--remaining<=0){
                if(activity==Activity.EXPLORE || activity==Activity.FORAGE)rest();
                else if(!chooseRoute(r,tank))rest();
            }
            boolean walk=activity==Activity.EXPLORE || activity==Activity.FORAGE;
            if(!walk){speed=0;return;} // Resting agents never turn, collide or repeatedly replan.
            double angle=Math.atan2(targetZ-z,targetX-x),delta=Math.atan2(Math.sin(angle-yaw),Math.cos(angle-yaw));
            yaw+=Math.clamp(delta,-.06,.06);
            if(Math.abs(delta)>.15){speed=0;return;} // Finish one deliberate turn before travel.
            if(Math.hypot(targetX-x,targetZ-z)<.012){if(r.species==TerrariumData.Species.TREE_FROG && hopStart>=0 && clock-hopStart<HOP_TICKS){speed=0;return;}rest();return;}
            boolean frog=r.species==TerrariumData.Species.TREE_FROG;
            if(frog){
                if(hopStart>=0 && clock-hopStart>=HOP_TICKS){hopStart=-1;nextHop=clock+50+random.nextInt(50);}
                if(hopStart<0 && clock>=nextHop)hopStart=clock;
                if(hopStart<0){speed=0;return;}
            }
            double desired=switch(r.species){case SNAIL->.0005;case TREE_FROG->.010;case GECKO->.003;case SPRINGTAIL->.0018;case ISOPOD->.0012;};
            speed=frog?desired:speed+(desired-speed)*.16;
            double step=Math.min(speed,Math.hypot(targetX-x,targetZ-z)),nx=x+Math.cos(yaw)*step,nz=z+Math.sin(yaw)*step;
            if(nx<.1 || nx>.9 || nz<.1 || nz>.9 || blocked(nx,nz,tank,r)){rest();return;}
            x=nx;z=nz;gait+=step*(r.species==TerrariumData.Species.SNAIL?70:180);
        }
        private boolean chooseRoute(TerrariumData.Resident r,AquariumData tank){
            boolean forage=tank!=null && tank.terrarium.food>0 && random.nextInt(3)==0;
            for(int attempt=0;attempt<32;attempt++){
                double angle=random.nextDouble()*Math.PI*2,distance=.05+random.nextDouble()*.30;
                double tx=forage && attempt<4?.4+random.nextDouble()*.2:Math.clamp(x+Math.cos(angle)*distance,.1,.9);
                double tz=forage && attempt<4?.4+random.nextDouble()*.2:Math.clamp(z+Math.sin(angle)*distance,.1,.9);
                if(Math.hypot(tx-x,tz-z)<.025 || !clearRoute(tx,tz,tank,r))continue;
                targetX=tx;targetZ=tz;activity=forage?Activity.FORAGE:Activity.EXPLORE;remaining=160+random.nextInt(200);return true;
            }return false;
        }
        private boolean clearRoute(double tx,double tz,AquariumData tank,TerrariumData.Resident r){
            double before=tank==null?0:tank.terrarium.terrain.sample(tank.terrarium,x,z);
            int samples=Math.max(2,(int)Math.ceil(Math.hypot(tx-x,tz-z)/.01));
            for(int i=1;i<=samples;i++){
                double nx=x+(tx-x)*i/samples,nz=z+(tz-z)*i/samples;
                if(blocked(nx,nz,tank,r))return false;
                if(tank!=null){double next=tank.terrarium.terrain.sample(tank.terrarium,nx,nz);if(Math.abs(next-before)>.06)return false;before=next;}
            }return true;
        }
        private void updateObstacles(AquariumData tank){
            if(tank==null || tank.terrarium==null){obstacles=List.of();environment=null;pieces=null;return;}
            var current=tank.scape.pieces();
            if(environment==tank && pieces==current && terrainRevision==tank.terrarium.terrain.revision())return;environment=tank;pieces=current;terrainRevision=tank.terrarium.terrain.revision();
            var list=new ArrayList<AquariumScape.Bounds>();
            for(var p:current){
                if(p.material()!=AquariumScape.Material.ROCK && p.material()!=AquariumScape.Material.WOOD && p.material()!=AquariumScape.Material.BLOCK)continue;
                if(p.material()==AquariumScape.Material.BLOCK){var id=net.minecraft.resources.ResourceLocation.tryParse(p.block());if(id!=null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState().is(TerrariumActions.PLANTS))continue;}
                list.add(TerrariumTerrain.bounds(p,tank));
            }obstacles=List.copyOf(list);
        }
        private boolean blocked(double nx,double nz,AquariumData tank,TerrariumData.Resident r){
            if(tank==null || tank.terrarium==null)return false;
            var land=tank.terrarium;
            double wx=.08+nx*(tank.size.blocksWide()-.16),wz=.08+nz*(tank.size.blocksDeep()-.16);
            double floor=.10+land.terrain.sample(land,nx,nz)*(tank.size.blocksHigh()*.88-.24)/(tank.size.height-2),pad=r.species.body*.2;
            for(var b:obstacles)if(b.Y()>floor+.01 && b.y()<floor+r.species.body && wx>b.x()-pad && wx<b.X()+pad && wz>b.z()-pad && wz<b.Z()+pad)return true;
            return false;
        }
    }
    private static double randomSeed(TerrariumData.Resident r){return Math.floorMod(r.id.getLeastSignificantBits(),71);}
    public static Pose pose(TerrariumData.Resident r,double ticks){var agent=new Agent(r);agent.clock=ticks-60;return agent.advance(r,ticks,null);}
    public static double modelYaw(double heading){return -90-Math.toDegrees(heading);}
    private TerrariumMotion(){}
}
