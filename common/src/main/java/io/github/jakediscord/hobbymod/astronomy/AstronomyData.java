package io.github.jakediscord.hobbymod.astronomy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
public final class AstronomyData extends SavedData {
    public final long seed;
    public final SkyCatalog catalog;
    private final Map<UUID,ObservationJournal> players=new HashMap<>();
    public AstronomyData(long seed){this.seed=seed;catalog=new SkyCatalog(seed);}
    public ObservationJournal journal(UUID id){return players.computeIfAbsent(id,k->new ObservationJournal());}
    public static AstronomyData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(()->new AstronomyData(s.overworld().getSeed()^new Random(s.overworld().getSeed()).nextLong()),AstronomyData::load,null),"hobbymod_astronomy");}
    public static CompoundTag encode(ObservationJournal journal){var t=new CompoundTag();var list=new ListTag();journal.entries().forEach((id,e)->{var a=new CompoundTag();a.putInt("Id",id);a.putInt("Completeness",e.completeness);a.putInt("Observations",e.observations);a.putInt("NightPoints",e.nightPoints);a.putDouble("Best",e.best);a.putLong("Night",e.night);a.putLong("Last",e.lastTick);list.add(a);});t.put("Entries",list);return t;}
    public static ObservationJournal decode(CompoundTag t){var j=new ObservationJournal();var a=t.getList("Entries",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(49,a.size());i++){var e=a.getCompound(i);j.load(e.getInt("Id"),e.getInt("Completeness"),e.getInt("Observations"),e.getInt("NightPoints"),e.getDouble("Best"),e.getLong("Night"),e.getLong("Last"));}return j;}
    private static AstronomyData load(CompoundTag t,HolderLookup.Provider r){var d=new AstronomyData(t.getLong("Seed"));var list=t.getList("Players",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var p=list.getCompound(i);if(p.hasUUID("Player"))d.players.put(p.getUUID("Player"),decode(p));}return d;}
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider r){t.putLong("Seed",seed);var list=new ListTag();players.forEach((id,j)->{var p=encode(j);p.putUUID("Player",id);list.add(p);});t.put("Players",list);return t;}
}
