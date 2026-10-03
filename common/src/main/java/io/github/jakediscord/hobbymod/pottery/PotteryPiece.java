package io.github.jakediscord.hobbymod.pottery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;

/** Loaded-tick drying and a two-fire process; neither firing replaces the artist's shape. */
public final class PotteryPiece {
    public enum Stage { WET, LEATHER_HARD, DRY, BISQUE, GLAZED, FINISHED }
    public static final int DRY_INTERVAL=1200;
    public PotteryShape shape=new PotteryShape();
    public Stage stage=Stage.WET;
    public int drying,moisture=100;
    public DyeColor glaze=DyeColor.WHITE;
    public boolean airDry() {
        if(stage!=Stage.WET && stage!=Stage.LEATHER_HARD)return false;
        drying++;
        if(stage==Stage.WET && drying%20==0)moisture=Math.max(0,moisture-1);
        if(drying<DRY_INTERVAL)return false;
        drying=0;
        if(stage==Stage.WET)stage=Stage.LEATHER_HARD;
        else {stage=Stage.DRY;shape.shrink(.98);}
        return true;
    }
    public boolean rewet(){if(stage!=Stage.WET && stage!=Stage.LEATHER_HARD)return false;stage=Stage.WET;moisture=100;drying=0;return true;}
    public boolean canFire(){return shape.open() && (stage==Stage.DRY || stage==Stage.GLAZED);}
    public boolean fire(){if(!canFire())return false;shape.shrink(stage==Stage.DRY?.94:.97);stage=stage==Stage.DRY?Stage.BISQUE:Stage.FINISHED;return true;}
    public boolean glaze(DyeColor color){if(stage!=Stage.BISQUE)return false;glaze=color;stage=Stage.GLAZED;return true;}
    public CompoundTag save() {
        var tag=new CompoundTag();double[] data=shape.data();int[] encoded=new int[data.length];
        for(int i=0;i<data.length;i++)encoded[i]=(int)Math.round(data[i]*1_000_000);
        tag.putIntArray("profile",encoded);tag.putString("stage",stage.name());tag.putInt("drying",drying);
        tag.putInt("moisture",moisture);tag.putInt("glaze",glaze.getId());return tag;
    }
    public static PotteryPiece read(CompoundTag tag) {
        var p=new PotteryPiece();int[] encoded=tag.getIntArray("profile");if(encoded.length!=PotteryShape.SAMPLES+4)return p;double[] data=new double[encoded.length];
        for(int i=0;i<data.length;i++)data[i]=encoded[i]/1_000_000.0;
        p.shape=PotteryShape.read(data);
        if(encoded.length!=PotteryShape.SAMPLES+4)return p;
        try{p.stage=Stage.valueOf(tag.getString("stage"));}catch(IllegalArgumentException ignored){}
        p.drying=Math.clamp(tag.getInt("drying"),0,DRY_INTERVAL-1);p.moisture=Math.clamp(tag.getInt("moisture"),0,100);
        p.glaze=DyeColor.byId(Math.clamp(tag.getInt("glaze"),0,15));return p;
    }
    public String description(){return switch(stage){case WET->"Wet clay";case LEATHER_HARD->"Leather-hard";case DRY->"Bone dry";case BISQUE->"Bisque fired";case GLAZED->"Glazed · ready to fire";case FINISHED->"Finished pottery";};}
}
