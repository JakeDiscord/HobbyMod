package io.github.jakediscord.hobbymod.astronomy;

/** Continuous server-tick exposure. Time jumps, missed heartbeats and target changes reset it. */
public final class ObservationExposure {
    public int target=-1,ticks;
    private long lastGame=Long.MIN_VALUE,lastSky=Long.MIN_VALUE;
    public boolean advance(int id,double quality,long game,long sky){
        boolean contiguous=lastGame!=Long.MIN_VALUE && game>lastGame && game-lastGame<=10 && (sky==lastSky || Math.abs((sky-lastSky)-(game-lastGame))<=1);
        int delta=contiguous?(int)(game-lastGame):0;
        if(!contiguous || id!=target || quality<.2){ticks=0;target=id;}
        else ticks+=delta;
        lastGame=game;lastSky=sky;
        if(ticks>=240){ticks=0;return true;}return false;
    }
}
