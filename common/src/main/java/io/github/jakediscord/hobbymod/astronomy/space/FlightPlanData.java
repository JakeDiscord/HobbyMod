package io.github.jakediscord.hobbymod.astronomy.space;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Routes persist by stable ship UUID, including the captain who charted them. */
public final class FlightPlanData extends SavedData {
    private final Map<UUID,FlightRoute> routes=new HashMap<>();
    public static FlightPlanData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(FlightPlanData::new,FlightPlanData::load,null),"hobbymod_flight_plans");}
    public FlightRoute route(UUID ship){return routes.get(ship);}
    public void chart(FlightRoute route){
        var old=routes.get(route.departure().ship());if(old!=null && !old.owner().equals(route.owner()))throw new IllegalArgumentException("Another captain owns this route");
        if(old==null && routes.size()>=1024)throw new IllegalArgumentException("Flight plan limit reached");routes.put(route.departure().ship(),route);setDirty();
    }
    public boolean cancel(UUID ship,UUID owner){var old=routes.get(ship);if(old==null || !old.owner().equals(owner))return false;routes.remove(ship);setDirty();return true;}
    public static FlightPlanData load(CompoundTag tag,HolderLookup.Provider registry){
        var data=new FlightPlanData();var list=tag.getList("Routes",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(1024,list.size());i++)try{
            var t=list.getCompound(i);var p=new SpacecraftSnapshot(t.getUUID("Ship"),t.getString("Dimension"),t.getDouble("X"),t.getDouble("Y"),t.getDouble("Z"),t.getDouble("QX"),t.getDouble("QY"),t.getDouble("QZ"),t.getDouble("QW"),t.getDouble("VX"),t.getDouble("VY"),t.getDouble("VZ"));
            var route=new FlightRoute(t.getUUID("Owner"),p,t.getString("Planet"),t.getLong("Time"));data.routes.putIfAbsent(p.ship(),route);
        }catch(IllegalArgumentException ignored){}return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registry){
        var list=new ListTag();for(var route:routes.values()){var t=new CompoundTag();var p=route.departure();t.putUUID("Ship",p.ship());t.putUUID("Owner",route.owner());t.putString("Dimension",p.dimension());t.putString("Planet",route.planet());t.putLong("Time",route.chartedTime());
            t.putDouble("X",p.x());t.putDouble("Y",p.y());t.putDouble("Z",p.z());t.putDouble("QX",p.qx());t.putDouble("QY",p.qy());t.putDouble("QZ",p.qz());t.putDouble("QW",p.qw());t.putDouble("VX",p.vx());t.putDouble("VY",p.vy());t.putDouble("VZ",p.vz());list.add(t);
        }tag.put("Routes",list);return tag;
    }
}
