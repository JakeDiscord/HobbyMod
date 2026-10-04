package io.github.jakediscord.hobbymod.astronomy;

import java.util.*;
/** IDs and observation records only; celestial data stays in the catalog. */
public final class ObservationJournal {
    public static final class Entry {public int completeness,observations,nightPoints;public double best;public long night=Long.MIN_VALUE,lastTick=Long.MIN_VALUE;}
    private final Map<Integer,Entry> entries=new TreeMap<>();
    public Map<Integer,Entry> entries(){return Collections.unmodifiableMap(entries);}
    public Entry get(int id){return entries.get(id);}
    public boolean record(int id,double quality,long celestialTime,long gameTick){
        if(id<0 || id>=49 || !Double.isFinite(quality) || quality<.2 || quality>1)return false;
        var e=entries.computeIfAbsent(id,k->new Entry());if(e.lastTick!=Long.MIN_VALUE && gameTick>=e.lastTick && gameTick-e.lastTick<600)return false;
        long night=Math.floorDiv(celestialTime,24000);if(e.night!=night){e.night=night;e.nightPoints=0;}
        int amount=Math.min(50-e.nightPoints,20+(int)Math.round(quality*15));if(amount<=0 || e.completeness>=100)return false;
        e.completeness=Math.min(100,e.completeness+amount);e.nightPoints+=amount;e.observations++;e.best=Math.max(e.best,quality);e.lastTick=gameTick;return true;
    }
    public void load(int id,int completeness,int observations,int nightPoints,double best,long night,long last){if(id<0 || id>=49 || !Double.isFinite(best))return;var e=new Entry();e.completeness=Math.clamp(completeness,0,100);e.observations=Math.clamp(observations,0,100000);e.nightPoints=Math.clamp(nightPoints,0,50);e.best=Math.clamp(best,0,1);e.night=night;e.lastTick=last;entries.put(id,e);}
}
