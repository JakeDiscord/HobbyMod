package io.github.jakediscord.hobbymod.terrarium;

import net.minecraft.nbt.*;

public final class TerrariumNbt {
    public static CompoundTag resident(TerrariumData.Resident r){var t=new CompoundTag();t.putUUID("Id",r.id);t.putString("Species",r.species.name());t.putInt("Variant",r.variant);t.putInt("Age",r.age);t.putInt("Vigor",r.vigor);if(r.parent!=null)t.putUUID("Parent",r.parent);return t;}
    public static TerrariumData.Resident resident(CompoundTag t){
        if(!t.hasUUID("Id"))return null;
        try{var r=new TerrariumData.Resident(t.getUUID("Id"),TerrariumData.Species.valueOf(t.getString("Species")),Math.clamp(t.getInt("Variant"),0,3));r.age=Math.clamp(t.getInt("Age"),0,1_000_000);r.vigor=Math.clamp(t.getInt("Vigor"),20,100);if(t.hasUUID("Parent"))r.parent=t.getUUID("Parent");return r;}catch(IllegalArgumentException e){return null;}
    }
    public static CompoundTag save(TerrariumData d){
        var t=new CompoundTag();t.putString("Substrate",d.substrate.name());t.putBoolean("Drainage",d.drainage);t.putBoolean("Open",d.open);t.putBoolean("Lamp",d.lamp);t.putBoolean("Warm",d.warm);t.putBoolean("HeatLamp",d.heatLamp);t.putIntArray("Terrain",d.terrain.saved());
        t.putInt("Humidity",d.humidity);t.putInt("Moisture",d.moisture);t.putInt("Light",d.light);t.putInt("Food",d.food);t.putInt("Minutes",d.minutes);t.putInt("Births",d.births);t.putLong("NextId",d.nextId);
        var agents=new ListTag();for(var r:d.residents())agents.add(resident(r));t.put("Residents",agents);return t;
    }
    public static TerrariumData load(CompoundTag t){
        var d=new TerrariumData();try{d.substrate=TerrariumData.Substrate.valueOf(t.getString("Substrate"));}catch(IllegalArgumentException ignored){}
        d.heatLamp=t.getBoolean("HeatLamp");d.terrain.load(t.getIntArray("Terrain"));
        d.drainage=t.getBoolean("Drainage");d.open=t.getBoolean("Open");d.lamp=t.getBoolean("Lamp");d.warm=!t.contains("Warm") || t.getBoolean("Warm");
        d.humidity=Math.clamp(t.getInt("Humidity"),0,100);d.moisture=Math.clamp(t.getInt("Moisture"),0,100);d.light=Math.clamp(t.getInt("Light"),0,15);d.food=Math.clamp(t.getInt("Food"),0,100);d.minutes=Math.clamp(t.getInt("Minutes"),0,1_000_000);d.births=Math.clamp(t.getInt("Births"),0,1_000_000);d.nextId=Math.clamp(t.getLong("NextId"),1,1_000_000_000);
        var residents=t.getList("Residents",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(residents.size(),TerrariumData.MAX_RESIDENTS);i++){var r=resident(residents.getCompound(i));if(d.accept(r) && r.id.getLeastSignificantBits()>0 && r.id.getLeastSignificantBits()<1_000_000_000)d.nextId=Math.max(d.nextId,r.id.getLeastSignificantBits()+1);}
        return d;
    }
    private TerrariumNbt(){}
}
