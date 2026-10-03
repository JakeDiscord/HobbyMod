import io.github.jakediscord.hobbymod.aquarium.*;
import java.util.*;

/** Java 21 checks kept out of release jars. World, NBT and interaction tests live in GameTests. */
public final class AquariumChecks {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static AquariumData ready(int seed){
        var tank=new AquariumData();tank.seed=seed;tank.size=AquariumData.Size.LARGE;tank.waterChange();
        for(int i=0;i<5;i++)tank.advance(true,true);
        tank.filter=true;tank.plants=12;return tank;
    }
    public static void main(String[] args){
        check(AquariumData.Species.values().length==8,"eight species required");
        for(var s:AquariumData.Size.values()){
            int volume=0;
            for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)for(int z=0;z<s.depth;z++)if(!s.shell(x,y,z) && y<s.height-1)volume++;
            check(volume==s.volume(),"enclosure volume mismatch");
        }
        check(AquariumData.Size.SMALL.blocksWide()==1 && AquariumData.Size.SMALL.blocksDeep()==1,"small store tank must be 1x1");
        check(AquariumData.Size.MEDIUM.blocksWide()==2 && AquariumData.Size.MEDIUM.blocksDeep()==1,"medium store tank must be 2x1");
        check(AquariumData.Size.LARGE.blocksWide()==4 && AquariumData.Size.LARGE.blocksDeep()==2,"large store tank must be 4x2");
        var cycling=new AquariumData();cycling.seed=7;
        var starter=cycling.newFish(AquariumData.Species.GUPPY);
        check(!cycling.add(starter),"dry tank accepted fish");cycling.waterChange();
        check(!cycling.add(starter),"uncycled tank accepted fish");
        for(int i=0;i<5;i++)cycling.advance(true,false);
        check(cycling.add(starter),"cycled tank rejected compatible fish");check(starter.acclimation==2,"missing acclimation");
        check(!cycling.add(starter),"duplicate identity accepted");
        cycling.feed();cycling.advance(true,false);check(starter.acclimation==1,"acclimation not gradual");
        cycling.advance(true,false);check(starter.acclimation==0,"acclimation failed to finish");
        check(!cycling.canAdd(cycling.newFish(AquariumData.Species.GOLDFISH)).isEmpty(),"wrong temperature accepted");
        var betta=cycling.newFish(AquariumData.Species.BETTA);check(!cycling.canAdd(betta).isEmpty(),"betta/guppy conflict accepted");
        var community=ready(4);var angel=community.newFish(AquariumData.Species.ANGELFISH);check(community.add(angel),"angel refused");
        check(!community.add(community.newFish(AquariumData.Species.NEON_TETRA)),"predation incompatibility accepted");
        var boys=ready(5);var a=boys.newFish(AquariumData.Species.BETTA);var b=boys.newFish(AquariumData.Species.BETTA);a.female=false;b.female=false;
        check(boys.add(a) && !boys.add(b),"male betta territoriality failed");
        var moved=cycling.capture();check(moved.id.equals(starter.id) && cycling.fish().isEmpty(),"capture lost identity or duplicated resident");
        check(community.add(moved) && community.fish().stream().anyMatch(f->f.id.equals(starter.id)),"transfer lost identity");
        community.drain();int count=community.fish().size();community.advance(false,false);
        check(community.fish().size()==count && moved.health<100,"drain removed fish or failed to show stress");
        community.waterChange();community.quality=100;community.food=50;community.advance(true,false);
        check(moved.health>92,"fish did not recover after care");
        int fedQuality=community.quality;community.food=80;community.feed();check(community.quality<fedQuality,"overfeeding not penalized");
        community.algae=90;int dirty=community.quality;community.clean();check(community.algae==60 && community.quality>=dirty,"cleaning failed");
        var clock=new AquariumClock();check(!clock.poll(0),"clock grew on load");
        for(int i=1;i<1200;i++)check(!clock.poll(i),"chemistry update early");check(clock.poll(1200),"chemistry update missing");
        check(!clock.poll(99999) && !clock.poll(-100),"time jump caused catch-up");clock.reset();check(!clock.poll(99999),"reload caused catch-up");
        int total=0;
        for(int seed=1;seed<=1000;seed++){
            var tank=ready(seed);
            var mom=tank.newFish(AquariumData.Species.GUPPY);var dad=tank.newFish(AquariumData.Species.GUPPY);mom.female=true;dad.female=false;
            mom.colorA=1;mom.colorB=3;dad.colorA=8;dad.colorB=12;mom.formA=0;mom.formB=1;dad.formA=2;dad.formB=3;
            check(tank.add(mom) && tank.add(dad),"pair could not be added");
            for(int minute=0;minute<150;minute++){
                tank.food=50;tank.advance(true,true);
                check(tank.fish().size()<=32 && tank.load()<=tank.size.volume(),"mass breeding exceeded cap");
                var ids=new HashSet<UUID>();
                for(var fish:tank.fish()){
                    check(ids.add(fish.id),"duplicate resident ID");
                    check(fish.health>=0 && fish.health<=100 && fish.colorA>=0 && fish.colorA<16,"invalid resident state");
                    if(fish.mother!=null){
                        var mother=tank.fish().stream().filter(f->f.id.equals(fish.mother)).findFirst().orElseThrow();
                        var father=tank.fish().stream().filter(f->f.id.equals(fish.father)).findFirst().orElseThrow();
                        check(fish.colorA==mother.colorA || fish.colorA==mother.colorB,"mother allele not inherited");
                        check(fish.colorB==father.colorA || fish.colorB==father.colorB,"father allele not inherited");
                        check(fish.formA==mother.formA || fish.formA==mother.formB,"mother form not inherited");
                        check(fish.formB==father.formA || fish.formB==father.formB,"father form not inherited");
                    }
                }
            }
            check(tank.fish().size()>2,"healthy pair never bred");total+=tank.fish().size();
            tank.size=AquariumData.Size.SMALL;check(tank.issue(true).contains("Overstocked"),"resizing did not report overstocking");
            int before=tank.fish().size();for(int minute=0;minute<40;minute++){tank.food=0;tank.advance(false,false);}
            check(tank.fish().size()==before,"stress caused invisible random deaths");
        }
        var invalid=new AquariumData();
        for(int i=0;i<10000;i++){
            var fish=new AquariumData.Fish(new UUID(0,i),AquariumData.Species.GUPPY);fish.colorA=-1;fish.formA=999;fish.age=Integer.MAX_VALUE;fish.name="x".repeat(1000);
            invalid.acceptLoaded(fish);
        }
        check(invalid.fish().size()==32,"corrupt oversized resident list not bounded");
        check(!invalid.acceptLoaded(invalid.fish().get(0)),"duplicate loaded identity accepted");
        check(invalid.fish().get(0).name.length()==32 && invalid.fish().get(0).formA==3,"invalid fish fields not sanitized");
        long poses=0;
        for(var size:AquariumData.Size.values())for(var species:AquariumData.Species.values())for(int id=1;id<=32;id++){
            var fish=new AquariumData.Fish(new UUID(7,id),species);
            for(int tick=0;tick<4000;tick+=10){
                var p=AquariumMotion.pose(fish,size,tick+.5,true);
                check(p.x()>1.3 && p.x()<size.width-1.3 && p.z()>1.3 && p.z()<size.depth-1.3,"swim path crossed glass");
                check(p.y()>1.1 && p.y()<size.height-1.1 && Double.isFinite(p.yaw()),"swim path left water or became invalid");poses++;
            }
        }
        System.out.println("AQUARIUM_PASS: 1000 breeding tanks, "+total+" residents, "+poses+" bounded swim poses; care, cycling, genetics, caps, stress recovery, resize and unload clock");
    }
}
