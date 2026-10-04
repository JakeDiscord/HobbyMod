package io.github.jakediscord.hobbymod.aquarium;

import net.minecraft.nbt.*;

/** Shared encoding for carried fish and tank residents. Records and counts are bounded on load. */
public final class AquariumNbt {
    public static CompoundTag fish(AquariumData.Fish f){
        CompoundTag t=new CompoundTag();t.putUUID("Id",f.id);t.putString("Species",f.species.name());
        if(f.mother!=null)t.putUUID("Mother",f.mother);if(f.father!=null)t.putUUID("Father",f.father);
        t.putInt("ColorA",f.colorA);t.putInt("ColorB",f.colorB);t.putInt("FormA",f.formA);t.putInt("FormB",f.formB);
        t.putInt("Age",f.age);t.putInt("Health",f.health);t.putInt("Acclimation",f.acclimation);t.putInt("Cooldown",f.cooldown);
        t.putBoolean("Female",f.female);t.putString("Name",f.name);return t;
    }
    public static AquariumData.Fish fish(CompoundTag t){
        if(!t.hasUUID("Id"))return null;
        AquariumData.Species species;
        try{species=AquariumData.Species.valueOf(t.getString("Species"));}catch(IllegalArgumentException e){return null;}
        AquariumData.Fish f=new AquariumData.Fish(t.getUUID("Id"),species);
        if(t.hasUUID("Mother"))f.mother=t.getUUID("Mother");if(t.hasUUID("Father"))f.father=t.getUUID("Father");
        f.colorA=AquariumData.clamp(t.getInt("ColorA"),15);f.colorB=AquariumData.clamp(t.getInt("ColorB"),15);
        f.formA=AquariumData.clamp(t.getInt("FormA"),3);f.formB=AquariumData.clamp(t.getInt("FormB"),3);
        f.age=AquariumData.clamp(t.getInt("Age"),1_000_000);f.health=AquariumData.clamp(t.getInt("Health"),100);
        f.acclimation=AquariumData.clamp(t.getInt("Acclimation"),2);f.cooldown=AquariumData.clamp(t.getInt("Cooldown"),6);
        f.female=t.getBoolean("Female");String name=t.getString("Name");f.name=name.substring(0,Math.min(32,name.length()));return f;
    }
    public static CompoundTag save(AquariumData d){
        CompoundTag t=new CompoundTag();if(d.terrarium!=null)t.put("Terrarium",io.github.jakediscord.hobbymod.terrarium.TerrariumNbt.save(d.terrarium));t.putString("Size",d.size.name());t.putLong("Seed",d.seed);t.putLong("NextId",d.nextId);
        t.putInt("Quality",d.quality);t.putInt("Cycle",d.cycle);t.putInt("Food",d.food);t.putInt("Algae",d.algae);
        t.putInt("Plants",d.plants);t.putInt("Rocks",d.rocks);t.putInt("Wood",d.wood);t.putInt("Steps",d.steps);t.putInt("Selected",d.selected);
        t.putBoolean("Filled",d.filled);t.putBoolean("Warm",d.warm);t.putBoolean("Substrate",d.substrate);t.putBoolean("Filter",d.filter);
        t.putBoolean("Gravel",d.gravel);t.putInt("Births",d.births);t.putInt("Discovered",d.discovered);d.ensureScape();ListTag decor=new ListTag();
        for(var p:d.scape.pieces()){var o=new CompoundTag();o.putString("Material",p.material().name());o.putDouble("X",p.x());o.putDouble("Z",p.z());o.putInt("Rotation",p.rotation());o.putDouble("Y",p.y());o.putDouble("ScaleX",p.scaleX());o.putDouble("ScaleY",p.scaleY());o.putDouble("ScaleZ",p.scaleZ());o.putUUID("Id",p.id());o.putString("Block",p.block());o.putDouble("Yaw",p.yaw());o.putDouble("Pitch",p.pitch());o.putDouble("Roll",p.roll());decor.add(o);}t.put("Scape",decor);
        ListTag residents=new ListTag();for(var f:d.fish())residents.add(fish(f));t.put("Residents",residents);return t;
    }
    public static void load(AquariumData d,CompoundTag t){
        d.clearForLoad();d.terrarium=t.contains("Terrarium")?io.github.jakediscord.hobbymod.terrarium.TerrariumNbt.load(t.getCompound("Terrarium")):null;try{d.size=AquariumData.Size.valueOf(t.getString("Size"));}catch(IllegalArgumentException e){d.size=AquariumData.Size.SMALL;}
        d.seed=t.getLong("Seed");d.nextId=Math.max(1,Math.min(1_000_000_000L,t.getLong("NextId")));
        d.quality=AquariumData.clamp(t.getInt("Quality"),100);d.cycle=AquariumData.clamp(t.getInt("Cycle"),5);
        d.food=AquariumData.clamp(t.getInt("Food"),100);d.algae=AquariumData.clamp(t.getInt("Algae"),100);
        d.plants=AquariumData.clamp(t.getInt("Plants"),AquariumData.MAX_PLANTS);d.rocks=AquariumData.clamp(t.getInt("Rocks"),AquariumData.MAX_ROCKS);d.wood=AquariumData.clamp(t.getInt("Wood"),4);
        d.steps=AquariumData.clamp(t.getInt("Steps"),1_000_000);d.selected=AquariumData.clamp(t.getInt("Selected"),31);
        d.filled=t.getBoolean("Filled");d.warm=t.getBoolean("Warm");d.substrate=t.getBoolean("Substrate");d.filter=t.getBoolean("Filter");
        d.gravel=t.getBoolean("Gravel");d.births=AquariumData.clamp(t.getInt("Births"),1_000_000);d.discovered=AquariumData.clamp(t.getInt("Discovered"),255);d.scape.clear();
        if(t.contains("Scape")){var decor=t.getList("Scape",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(28,decor.size());i++){var o=decor.getCompound(i);if(d.terrarium==null && o.getDouble("Y")<0)continue;try{d.scape.add(new AquariumScape.Piece(AquariumScape.Material.valueOf(o.getString("Material")),o.getDouble("X"),o.getDouble("Z"),Math.floorMod(o.getInt("Rotation"),4),o.getDouble("Y"),o.contains("ScaleX")?o.getDouble("ScaleX"):1,o.contains("ScaleY")?o.getDouble("ScaleY"):1,o.contains("ScaleZ")?o.getDouble("ScaleZ"):1,o.hasUUID("Id")?o.getUUID("Id"):java.util.UUID.randomUUID(),o.getString("Block"),o.getDouble("Yaw"),o.getDouble("Pitch"),o.getDouble("Roll")));}catch(IllegalArgumentException ignored){}}d.syncScape();}else d.ensureScape();
        io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(d);
        ListTag residents=t.getList("Residents",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(AquariumData.MAX_FISH,residents.size());i++)d.acceptLoaded(fish(residents.getCompound(i)));
        for(var f:d.fish())if(f.id.getMostSignificantBits()==d.seed && f.id.getLeastSignificantBits()>0 && f.id.getLeastSignificantBits()<1_000_000_000L)d.nextId=Math.max(d.nextId,f.id.getLeastSignificantBits()+1);
    }
    private AquariumNbt(){}
}
