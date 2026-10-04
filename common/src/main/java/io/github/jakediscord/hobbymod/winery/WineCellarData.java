package io.github.jakediscord.hobbymod.winery;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Bottles store references; complete immutable vintages are saved once per batch in the overworld. */
public final class WineCellarData extends SavedData {
    public final WineLedger ledger=new WineLedger();
    public static WineCellarData get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(WineCellarData::new,WineCellarData::load,null),"hobbymod_wine_cellar");}
    private static WineCellarData load(CompoundTag tag,HolderLookup.Provider registries){var data=new WineCellarData();for(var t:tag.getList("Vintages",Tag.TAG_COMPOUND)){var row=(CompoundTag)t;try{data.ledger.restore(WineBatch.decode(row.getByteArray("Batch")),Math.clamp(row.getInt("Issued"),0,4));}catch(java.io.IOException | IllegalArgumentException ignored){}}return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){var list=new ListTag();for(var entry:ledger.entries()){var row=new CompoundTag();row.putByteArray("Batch",entry.wine().encode());row.putInt("Issued",entry.issued());list.add(row);}tag.put("Vintages",list);return tag;}
}
