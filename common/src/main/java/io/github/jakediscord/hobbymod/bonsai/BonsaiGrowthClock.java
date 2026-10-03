package io.github.jakediscord.hobbymod.bonsai;

/** Loaded-tick clock: missing ticks and reversed time reset the interval without catch-up. */
public final class BonsaiGrowthClock {
    public static final int INTERVAL=1200;
    private long previous=Long.MIN_VALUE;
    private int loadedTicks;
    public boolean poll(long gameTime){
        if(previous==Long.MIN_VALUE || gameTime-previous!=1){previous=gameTime;loadedTicks=0;return false;}
        previous=gameTime;
        if(++loadedTicks<INTERVAL)return false;
        loadedTicks=0;return true;
    }
    public void reset(){previous=Long.MIN_VALUE;loadedTicks=0;}
}
