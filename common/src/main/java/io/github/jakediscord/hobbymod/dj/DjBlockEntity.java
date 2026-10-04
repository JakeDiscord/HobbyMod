package io.github.jakediscord.hobbymod.dj;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class DjBlockEntity extends BlockEntity {
    public final MusicProject[] projects={new MusicProject(),new MusicProject()};
    public final boolean[] playing=new boolean[2],loop={true,true};
    public final int[] pattern={-1,-1},cue=new int[2],generation=new int[2];
    public final long[] started=new long[2];
    public int revision,crossfade=50;
    private UUID editor;
    private long lease;
    public DjBlockEntity(BlockPos pos,BlockState state){super(DjContent.ENTITY.get(),pos,state);}
    public boolean claim(net.minecraft.world.entity.player.Player player){
        if(editor!=null && !editor.equals(player.getUUID()) && level!=null && level.getGameTime()-lease<100){var other=level.getPlayerByUUID(editor);if(other==null || io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(other,worldPosition))return false;}
        editor=player.getUUID();lease=level==null?0:level.getGameTime();return true;
    }
    public boolean owned(net.minecraft.world.entity.player.Player p){return editor!=null && editor.equals(p.getUUID()) && claim(p);}
    public void release(net.minecraft.world.entity.player.Player p){if(Objects.equals(editor,p.getUUID()))editor=null;}
    public void replace(int deck,MusicProject project){long now=level==null?0:level.getGameTime();double barPosition=positionFrames(deck,now)/(double)projects[deck].barFrames();projects[deck]=project;cue[deck]=Math.min(cue[deck],project.bars-1);generation[deck]++;revision++;if(playing[deck]){cue[deck]=0;started[deck]=now-Math.round(barPosition*project.barFrames()*20/MusicSynth.SAMPLE_RATE);}changed();}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public long positionFrames(int deck,long tick){if(!playing[deck])return (long)cue[deck]*projects[deck].barFrames();long frame=Math.max(0,tick-started[deck])*MusicSynth.SAMPLE_RATE/20+(long)cue[deck]*projects[deck].barFrames();long length=pattern[deck]<0?projects[deck].durationFrames():projects[deck].barFrames();return loop[deck]?frame%length:Math.min(frame,length);}
    public static void tickServer(net.minecraft.world.level.Level l,BlockPos p,BlockState s,DjBlockEntity b){for(int d=0;d<2;d++)if(b.playing[d] && !b.loop[d]){long end=b.pattern[d]<0?b.projects[d].durationFrames():b.projects[d].barFrames();if(Math.max(0,l.getGameTime()-b.started[d])*MusicSynth.SAMPLE_RATE/20+(long)b.cue[d]*b.projects[d].barFrames()>=end+MusicSynth.SAMPLE_RATE){b.playing[d]=false;b.generation[d]++;b.changed();}}}
    public double gain(int deck){double angle=crossfade/100.0*Math.PI/2;return deck==0?Math.cos(angle):Math.sin(angle);}
    public int range(){if(level==null)return 24;for(var p:BlockPos.betweenClosed(worldPosition.offset(-2,-1,-2),worldPosition.offset(2,1,2)))if(level.getBlockState(p).is(DjContent.SPEAKER.get()))return 48;return 24;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);for(int d=0;d<2;d++){t.putByteArray("Deck"+d,projects[d].encode());t.putBoolean("Loop"+d,loop[d]);t.putInt("Pattern"+d,pattern[d]);t.putInt("Cue"+d,cue[d]);}t.putInt("Crossfade",crossfade);t.putInt("Revision",revision);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);for(int d=0;d<2;d++){try{projects[d]=MusicProject.decode(t.getByteArray("Deck"+d));}catch(java.io.IOException e){projects[d]=new MusicProject();}loop[d]=!t.contains("Loop"+d) || t.getBoolean("Loop"+d);pattern[d]=Math.clamp(t.getInt("Pattern"+d),-1,7);cue[d]=Math.clamp(t.getInt("Cue"+d),0,projects[d].bars-1);playing[d]=t.getBoolean("Playing"+d);started[d]=t.getLong("Started"+d);generation[d]=t.getInt("Generation"+d);}crossfade=t.contains("Crossfade")?Math.clamp(t.getInt("Crossfade"),0,100):50;revision=t.getInt("Revision");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=saveWithoutMetadata(r);for(int d=0;d<2;d++){t.putBoolean("Playing"+d,playing[d]);t.putLong("Started"+d,started[d]);t.putInt("Generation"+d,generation[d]);}return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
