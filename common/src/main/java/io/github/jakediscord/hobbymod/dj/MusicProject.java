package io.github.jakediscord.hobbymod.dj;

import java.io.*;
import java.util.*;

/** Loader-independent, bounded song format: eight channels, eight one-bar patterns and sixty-four arrangement bars. */
public final class MusicProject {
    public static final int TRACKS=8,PATTERNS=8,STEPS=16,BARS=64,MAX_NOTES=48,MAX_BYTES=20_000;
    public enum Instrument { KICK, SNARE, HAT, CLAP, BASS, KEYS, PLUCK, PAD, BELL;
        public boolean drum(){return ordinal()<4;}
        public String label(){return name().charAt(0)+name().substring(1).toLowerCase(Locale.ROOT);}
    }
    public record Note(int step,int pitch,int length,int velocity){
        public Note {if(step<0 || step>=STEPS || pitch<36 || pitch>84 || length<1 || length>STEPS-step || velocity<1 || velocity>127)throw new IllegalArgumentException("Invalid note");}
    }
    public static final class Track {
        public String name;
        public Instrument instrument;
        public int volume=80,pan,cutoff=100,delay,reverb;
        public boolean mute,solo;
        private final List<List<Note>> patterns=new ArrayList<>();
        public final int[] arrangement=new int[BARS];
        private Track(int index){instrument=Instrument.values()[index];name=instrument.label();for(int i=0;i<PATTERNS;i++)patterns.add(new ArrayList<>());Arrays.fill(arrangement,-1);}
        public List<Note> notes(int pattern){return Collections.unmodifiableList(patterns.get(pattern));}
        public boolean put(int pattern,Note n){var list=patterns.get(pattern);list.removeIf(old->old.step==n.step && old.pitch==n.pitch);if(list.size()>=MAX_NOTES)return false;list.add(n);list.sort(Comparator.comparingInt(Note::step).thenComparingInt(Note::pitch));return true;}
        public void remove(int pattern,int step,int pitch){patterns.get(pattern).removeIf(n->n.step==step && n.pitch==pitch);}
        public void clear(int pattern){patterns.get(pattern).clear();}
        public void copyPattern(int from,int to){if(from!=to){patterns.get(to).clear();patterns.get(to).addAll(patterns.get(from));}}
        public void transpose(int pattern,int amount){var old=new ArrayList<>(patterns.get(pattern));patterns.get(pattern).clear();for(var n:old)put(pattern,new Note(n.step,Math.clamp(n.pitch+amount,36,84),n.length,n.velocity));}
    }
    public UUID id=UUID.randomUUID();
    public String title="Untitled set",author="";
    public int bpm=120,swing,master=85,bars=8;
    public final Track[] tracks=new Track[TRACKS];
    public MusicProject(){for(int i=0;i<TRACKS;i++)tracks[i]=new Track(i);}
    public long barFrames(){return Math.round(MusicSynth.SAMPLE_RATE*240.0/bpm);}
    public long durationFrames(){return barFrames()*bars;}
    public double stepSeconds(int step){return (step+((step&1)==1?swing/100.0*.5:0))*15.0/bpm;}
    public boolean audible(int track){boolean anySolo=Arrays.stream(tracks).anyMatch(t->t.solo);return !tracks[track].mute && (!anySolo || tracks[track].solo);}
    public int noteCount(){int n=0;for(var t:tracks)for(var p:t.patterns)n+=p.size();return n;}
    public MusicProject copy(){try{return decode(encode());}catch(IOException e){throw new IllegalStateException(e);}}
    public byte[] encode(){try{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(0x48444a31);out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());out.writeUTF(clean(title));out.writeUTF(clean(author));out.writeShort(bpm);out.writeByte(swing);out.writeByte(master);out.writeByte(bars);
        for(var t:tracks){out.writeUTF(clean(t.name));out.writeByte(t.instrument.ordinal());for(int v:new int[]{t.volume,t.pan+100,t.cutoff,t.delay,t.reverb})out.writeByte(v);out.writeBoolean(t.mute);out.writeBoolean(t.solo);for(int n:t.arrangement)out.writeByte(n+1);for(var pattern:t.patterns){out.writeByte(pattern.size());for(var n:pattern){out.writeByte(n.step);out.writeByte(n.pitch);out.writeByte(n.length);out.writeByte(n.velocity);}}}out.flush();var data=bytes.toByteArray();if(data.length>MAX_BYTES)throw new IllegalArgumentException("Project too large");return data;
    }catch(IOException e){throw new IllegalStateException(e);}}
    public static MusicProject decode(byte[] data)throws IOException{
        if(data.length>MAX_BYTES || data.length<32)throw new IOException("Invalid project size");
        try{var in=new DataInputStream(new ByteArrayInputStream(data));if(in.readInt()!=0x48444a31)throw new IOException("Unsupported project format");var p=new MusicProject();p.id=new UUID(in.readLong(),in.readLong());p.title=text(in.readUTF());p.author=text(in.readUTF());p.bpm=range(in.readUnsignedShort(),60,200);p.swing=range(in.readUnsignedByte(),0,75);p.master=range(in.readUnsignedByte(),0,100);p.bars=range(in.readUnsignedByte(),1,BARS);
            for(var t:p.tracks){t.name=text(in.readUTF());t.instrument=Instrument.values()[range(in.readUnsignedByte(),0,Instrument.values().length-1)];t.volume=range(in.readUnsignedByte(),0,100);t.pan=range(in.readUnsignedByte(),0,200)-100;t.cutoff=range(in.readUnsignedByte(),0,100);t.delay=range(in.readUnsignedByte(),0,100);t.reverb=range(in.readUnsignedByte(),0,100);t.mute=in.readBoolean();t.solo=in.readBoolean();for(int i=0;i<BARS;i++)t.arrangement[i]=range(in.readUnsignedByte(),0,PATTERNS)-1;for(int i=0;i<PATTERNS;i++){int count=range(in.readUnsignedByte(),0,MAX_NOTES);for(int n=0;n<count;n++){var note=new Note(in.readUnsignedByte(),in.readUnsignedByte(),in.readUnsignedByte(),in.readUnsignedByte());if(t.notes(i).stream().anyMatch(old->old.step==note.step && old.pitch==note.pitch))throw new IOException("Duplicate note");t.put(i,note);}}}
            if(in.available()!=0)throw new IOException("Trailing project data");return p;
        }catch(IllegalArgumentException e){throw new IOException("Invalid project values",e);}
    }
    private static int range(int n,int min,int max)throws IOException{if(n<min || n>max)throw new IOException("Value outside project limits");return n;}
    private static String text(String s)throws IOException{if(!s.equals(clean(s)))throw new IOException("Invalid project text");return s;}
    public static String clean(String s){if(s==null)return "";String text=s.replaceAll("[\\p{Cntrl}§]","");return text.substring(0,Math.min(32,text.length()));}
    public static MusicProject demo(){var p=new MusicProject();p.title="Redstone groove";for(int bar=0;bar<p.bars;bar++)for(var t:p.tracks)t.arrangement[bar]=0;
        for(int step=0;step<16;step++){if(step%4==0)p.tracks[0].put(0,new Note(step,48,1,112));if(step==4 || step==12)p.tracks[1].put(0,new Note(step,60,1,108));if(step%2==0)p.tracks[2].put(0,new Note(step,72,1,step%4==2?90:58));}
        int[] bass={36,36,43,39};for(int i=0;i<4;i++)p.tracks[4].put(0,new Note(i*4,bass[i],3,100));for(int i=0;i<4;i++)p.tracks[6].put(0,new Note(i*4+2,60+new int[]{0,7,3,10}[i],2,88));for(int pitch:new int[]{48,51,55})p.tracks[7].put(0,new Note(0,pitch,16,60));return p;
    }
}
