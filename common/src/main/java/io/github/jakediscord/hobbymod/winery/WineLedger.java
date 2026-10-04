package io.github.jakediscord.hobbymod.winery;

import java.util.*;

/** An immutable bottling record and a shared issue count prevent duplicate containers minting extra bottles. */
public final class WineLedger {
    public static final class Entry {
        private final WineBatch wine;private int issued;
        public Entry(WineBatch wine,int issued){if(wine.stage!=WineBatch.Stage.BOTTLED || issued<0 || issued>WineBatch.BOTTLES)throw new IllegalArgumentException("Invalid bottled record");this.wine=wine.copy();this.issued=issued;}
        public WineBatch wine(){return wine.copy();}public int issued(){return issued;}
    }
    private final Map<UUID,Entry> entries=new LinkedHashMap<>();
    public boolean finish(WineBatch wine){if(wine.stage!=WineBatch.Stage.BOTTLED || entries.containsKey(wine.id))return false;entries.put(wine.id,new Entry(wine,WineBatch.BOTTLES-wine.remaining));return true;}
    public int issue(UUID id,int requested){var entry=entries.get(id);if(entry==null || requested<1)return 0;int n=Math.min(requested,WineBatch.BOTTLES-entry.issued);entry.issued+=n;return n;}
    public Entry get(UUID id){return entries.get(id);}
    public int remaining(UUID id){var e=entries.get(id);return e==null?WineBatch.BOTTLES:WineBatch.BOTTLES-e.issued;}
    public Collection<Entry> entries(){return Collections.unmodifiableCollection(entries.values());}
    public void restore(WineBatch wine,int issued){entries.putIfAbsent(wine.id,new Entry(wine,issued));}
}
