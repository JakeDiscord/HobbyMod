package io.github.jakediscord.hobbymod.aquarium;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Bounded resident simulation. No fish entities, recursive lineage or offline catch-up. */
public final class AquariumData {
    public static final int MAX_FISH=32,MAX_PLANTS=16,MAX_ROCKS=8;
    public enum Size {
        SMALL(5,4,4),MEDIUM(7,4,5),LARGE(9,5,6);
        public final int width,height,depth;
        Size(int w,int h,int d){width=w;height=h;depth=d;}
        public int volume(){return (width-2)*(height-2)*(depth-2);}
        public boolean shell(int x,int y,int z){return y==0 || x==0 || z==0 || x==width-1 || z==depth-1;}
    }
    public enum Species {
        GUPPY("Guppy",true,1,0xF7A14D),NEON_TETRA("Neon Tetra",true,1,0x36BCE8),
        ZEBRA_DANIO("Zebra Danio",false,1,0xC4CFEB),GOLDFISH("Goldfish",false,3,0xF0A132),
        CORYDORAS("Corydoras",true,2,0xB9A183),BETTA("Betta",true,2,0xD457B2),
        ANGELFISH("Angelfish",true,3,0xC7D4E0),CHERRY_BARB("Cherry Barb",true,1,0xC95042);
        public final String label;
        public final boolean warm;
        public final int load,color;
        Species(String label,boolean warm,int load,int color){this.label=label;this.warm=warm;this.load=load;this.color=color;}
    }
    public static final class Fish {
        public final UUID id;
        public final Species species;
        public UUID mother,father;
        public int colorA,colorB,formA,formB,age=8,health=100,acclimation=2,cooldown;
        public boolean female;
        public String name="";
        public Fish(UUID id,Species species){this.id=id;this.species=species;}
        public double size(){return .35+.65*Math.min(1,Math.max(0,age)/8.0);}
        public int color(){
            int shift=(colorA+colorB)%16;
            int base=species.color;
            int r=Math.min(255,Math.max(20,((base>>16)&255)+(shift-8)*8));
            int g=Math.min(255,Math.max(20,((base>>8)&255)+(8-shift)*5));
            int b=Math.min(255,Math.max(20,(base&255)+(shift-8)*6));
            return (r<<16)|(g<<8)|b;
        }
        public String label(){return name.isEmpty()?species.label:name;}
    }
    private final List<Fish> fish=new ArrayList<>();
    public Size size=Size.SMALL;
    public long seed,nextId=1;
    public int quality=100,cycle,food,algae,plants,rocks,wood,steps,selected;
    public boolean filled,warm=true,substrate,filter;
    public List<Fish> fish(){return List.copyOf(fish);}
    public int load(){return fish.stream().mapToInt(f->f.species.load).sum();}
    public int capacity(){return Math.min(MAX_FISH,size.volume());}
    public Fish selected(){return fish.isEmpty()?null:fish.get(Math.floorMod(selected,fish.size()));}
    public void selectNext(){if(!fish.isEmpty())selected=Math.floorMod(selected+1,fish.size());}
    public static boolean compatible(Fish a,Fish b){
        if(a.species==Species.BETTA && b.species==Species.BETTA && !a.female && !b.female)return false;
        if((a.species==Species.BETTA && b.species==Species.GUPPY)||(b.species==Species.BETTA && a.species==Species.GUPPY))return false;
        return !((a.species==Species.ANGELFISH && b.species==Species.NEON_TETRA)||(b.species==Species.ANGELFISH && a.species==Species.NEON_TETRA));
    }
    public String canAdd(Fish f){
        if(!filled)return "Fill the tank.";
        if(cycle<5)return "Water is cycling: "+cycle+"/5 min.";
        if(quality<65)return "Change the water first.";
        if(f.species.warm!=warm)return f.species.label+" needs "+(f.species.warm?"warm":"cool")+" water.";
        if(fish.size()>=MAX_FISH)return "Resident limit reached.";
        if(load()+f.species.load>size.volume())return "Stocking load full ("+load()+"/"+size.volume()+").";
        if(fish.stream().anyMatch(a->a.id.equals(f.id)))return "That fish is already here.";
        for(Fish a:fish)if(!compatible(a,f))return "Incompatible with "+a.species.label+".";
        return "";
    }
    public boolean add(Fish f){if(!canAdd(f).isEmpty())return false;f.acclimation=2;fish.add(f);selected=fish.size()-1;return true;}
    public Fish capture(){Fish f=selected();if(f!=null){fish.remove(f);selected=Math.max(0,Math.min(selected,fish.size()-1));}return f;}
    public Fish newFish(Species species){
        Random random=new Random(seed+nextId*7919);
        Fish f=new Fish(new UUID(seed,nextId++),species);
        f.colorA=random.nextInt(16);f.colorB=random.nextInt(16);f.formA=random.nextInt(4);f.formB=random.nextInt(4);f.female=random.nextBoolean();return f;
    }
    public void waterChange(){if(!filled){filled=true;cycle=0;}quality=Math.min(100,quality+25);algae=Math.max(0,algae-15);}
    public void drain(){filled=false;cycle=0;}
    public void feed(){if(food>=60)quality=Math.max(0,quality-8);food=Math.min(100,food+20);}
    public void clean(){algae=Math.max(0,algae-30);quality=Math.min(100,quality+10);}
    public String issue(boolean intact){
        if(!intact)return "Repair glass / refill water.";
        if(!filled)return "Fill the tank.";
        if(cycle<5)return "Cycling "+cycle+"/5 min";
        if(load()>size.volume())return "Overstocked: move fish.";
        if(quality<50)return "Poor water: change / clean.";
        if(algae>70)return "Algae: clean glass.";
        if(!fish.isEmpty() && food==0)return "Feed fish.";
        for(Fish f:fish){
            if(f.species.warm!=warm)return "Wrong temperature.";
            for(Fish other:fish)if(other!=f && !compatible(f,other))return "Incompatible fish.";
        }
        return "";
    }
    public void advance(boolean intact,boolean bright){
        steps=Math.min(1_000_000,steps+1);
        if(filled && intact){
            if(quality>=60)cycle=Math.min(5,cycle+1);
            algae=clamp(algae+(bright?2:0)+(food>50?3:0)-plants/4,100);
            quality=clamp(quality-Math.max(fish.isEmpty()?0:1,load()/4)-(food>60?3:0)-(algae>70?2:0)+plants/3+(filter?3:0),100);
        }
        boolean fed=food>0;
        for(Fish f:fish){
            f.age=clamp(f.age+1,1_000_000);f.cooldown=Math.max(0,f.cooldown-1);
            if(intact && filled)f.acclimation=Math.max(0,f.acclimation-1);
            boolean stress=!intact || !filled || quality<50 || !fed || f.species.warm!=warm || load()>size.volume();
            for(Fish other:fish)if(other!=f && !compatible(f,other))stress=true;
            f.health=clamp(f.health+(stress?-8:4),100);
        }
        food=Math.max(0,food-Math.max(1,load()/2));
        if(intact && filled && cycle==5 && quality>=75 && fed)breed();
    }
    private void breed(){
        Random random=new Random(seed+steps*104729L);
        List<Fish> parents=List.copyOf(fish);int births=0;
        for(Fish mother:parents){
            if(!mother.female || !ready(mother) || births>=4)continue;
            for(Fish father:parents){
                if(father.female || father.species!=mother.species || !ready(father))continue;
                Fish child=new Fish(new UUID(seed,nextId++),mother.species);
                child.mother=mother.id;child.father=father.id;child.age=0;child.acclimation=0;
                child.colorA=random.nextBoolean()?mother.colorA:mother.colorB;
                child.colorB=random.nextBoolean()?father.colorA:father.colorB;
                child.formA=random.nextBoolean()?mother.formA:mother.formB;
                child.formB=random.nextBoolean()?father.formA:father.formB;child.female=random.nextBoolean();
                if(canAdd(child).isEmpty()){
                    fish.add(child);mother.cooldown=6;father.cooldown=6;births++;
                }
                break;
            }
        }
    }
    private static boolean ready(Fish f){return f.age>=6 && f.health>=80 && f.acclimation==0 && f.cooldown==0;}
    public static int clamp(int n,int max){return Math.max(0,Math.min(max,n));}
    public void clearForLoad(){fish.clear();}
    public boolean acceptLoaded(Fish f){
        if(f==null || f.id==null || f.species==null || fish.size()>=MAX_FISH || fish.stream().anyMatch(a->a.id.equals(f.id)))return false;
        f.colorA=clamp(f.colorA,15);f.colorB=clamp(f.colorB,15);f.formA=clamp(f.formA,3);f.formB=clamp(f.formB,3);
        f.age=clamp(f.age,1_000_000);f.health=clamp(f.health,100);f.acclimation=clamp(f.acclimation,2);f.cooldown=clamp(f.cooldown,6);
        f.name=f.name==null?"":f.name.substring(0,Math.min(32,f.name.length()));fish.add(f);return true;
    }
}
