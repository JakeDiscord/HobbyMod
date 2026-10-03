package io.github.jakediscord.hobbymod.terrarium;

import java.util.*;

/** A gentle, bounded land ecosystem; residents are saved records, never world entities. */
public final class TerrariumData {
    public static final int MAX_RESIDENTS=48;
    public enum Substrate { NONE,SOIL,SAND,MOSS }
    public enum Species { SPRINGTAIL,ISOPOD }
    public static final class Resident {
        public final UUID id;public final Species species;
        public UUID parent;public int variant,age=8,vigor=100;
        public Resident(UUID id,Species species,int variant){this.id=id;this.species=species;this.variant=variant;}
    }
    public Substrate substrate=Substrate.NONE;
    public boolean drainage,open,lamp,warm=true;
    public int humidity=65,moisture=55,light=8,food,minutes,births;
    public long nextId=1;
    private final List<Resident> residents=new ArrayList<>();
    private List<Resident> snapshot;
    public List<Resident> residents(){if(snapshot==null)snapshot=List.copyOf(residents);return snapshot;}
    public boolean accept(Resident r){
        if(r==null || r.id==null || r.species==null || r.variant<0 || r.variant>3 || residents.size()>=MAX_RESIDENTS || residents.stream().anyMatch(a->a.id.equals(r.id)))return false;
        r.age=Math.clamp(r.age,0,1_000_000);r.vigor=Math.clamp(r.vigor,0,100);residents.add(r);snapshot=null;return true;
    }
    public Resident newResident(Species species,long seed){var id=new UUID(seed,nextId++);return new Resident(id,species,new Random(id.getLeastSignificantBits()^seed).nextInt(4));}
    public boolean introduce(Species species,long seed){
        if(substrate==Substrate.NONE || residents.size()+3>MAX_RESIDENTS)return false;
        for(int i=0;i<3;i++)accept(newResident(species,seed));return true;
    }
    public List<Resident> take(Species species){
        var result=new ArrayList<Resident>();for(var r:residents)if(r.species==species && result.size()<3)result.add(r);
        residents.removeAll(result);snapshot=null;return List.copyOf(result);
    }
    public void mist(){moisture=Math.min(100,moisture+18);humidity=Math.min(100,humidity+15);}
    public String advice(int plants){
        if(substrate==Substrate.NONE)return "Choose a substrate, then arrange your plants.";
        if(moisture<25)return "A light mist would help.";
        if(moisture>85 && !drainage)return "Add gravel drainage or open the lid.";
        if(humidity<40 && plants>0)return "Close the lid or mist to raise humidity.";
        if(light<5 && plants>0)return "Move into light or add a lamp.";
        if(!residents.isEmpty() && food==0)return "Offer a little leaf litter.";
        return "Comfortable ecosystem · "+births+" young born";
    }
    public void advance(boolean intact,int plants,int daylight,long seed){
        minutes=Math.min(1_000_000,minutes+1);light=lamp?12:Math.clamp(daylight,0,15);
        if(!intact)return;
        moisture=Math.clamp(moisture-(open?2:minutes%(warm?4:6)==0?1:0),0,100);
        int target=Math.clamp(30+moisture/2+Math.min(8,plants*2)-(open?15:0),0,100);
        humidity+=Integer.compare(target,humidity)*Math.min(3,Math.abs(target-humidity));
        if(!residents.isEmpty() && minutes%5==0)food=Math.max(0,food-1);
        boolean comfortable=moisture>=30 && moisture<=85 && humidity>=45 && food>0;
        for(var r:residents){r.age=Math.min(1_000_000,r.age+1);r.vigor=Math.clamp(r.vigor+(comfortable?2:-1),20,100);}
        if(comfortable && plants>0 && !open && minutes%8==0){
            for(var species:Species.values()){
                var adults=residents.stream().filter(r->r.species==species && r.age>=8 && r.vigor>=70).toList();
                if(adults.size()<2 || residents.size()>=MAX_RESIDENTS)continue;
                var child=newResident(species,seed);child.variant=adults.get((int)Math.floorMod(nextId,adults.size())).variant;child.age=0;child.parent=adults.getFirst().id;
                if(accept(child))births=Math.min(1_000_000,births+1);
            }
        }
    }
}
