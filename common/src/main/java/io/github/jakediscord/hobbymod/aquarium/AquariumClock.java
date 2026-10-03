package io.github.jakediscord.hobbymod.aquarium;

/** One-minute chemistry clock. Unloaded chunks and discontinuous time never catch up. */
public final class AquariumClock {
    private long previous=Long.MIN_VALUE;
    private int ticks;
    public boolean poll(long time){
        if(previous==Long.MIN_VALUE || time-previous!=1){previous=time;ticks=0;return false;}
        previous=time;if(++ticks<1200)return false;ticks=0;return true;
    }
    public void reset(){previous=Long.MIN_VALUE;ticks=0;}
}
