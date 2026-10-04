package io.github.jakediscord.hobbymod.dj;

import java.io.*;
import java.util.*;

/** Deterministic stereo synthesizer. No game classes, assets, audio device or network needed to render a song. */
public final class MusicSynth {
    public static final int SAMPLE_RATE=22050;
    public record Event(long frame,int track,MusicProject.Note note){}
    private static final double TAU=Math.PI*2;
    private final MusicProject project;
    private final List<Event> events;
    private final ArrayList<Voice> voices=new ArrayList<>();
    private final float[][] echoes=new float[8][SAMPLE_RATE*2];
    private final double[] filter=new double[8],alpha=new double[8],left=new double[8],right=new double[8];
    private final float[] roomL=new float[1579],roomR=new float[1993];
    private long frame;
    private int next,echoIndex,roomIndex;
    private boolean loop;
    public MusicSynth(MusicProject p,int pattern,boolean loop,long offset){project=p.copy();this.loop=loop;events=schedule(project,pattern);frame=loop?Math.floorMod(offset,length(pattern)):Math.clamp(offset,0,length(pattern));while(next<events.size() && events.get(next).frame<frame){var event=events.get(next++);long elapsed=frame-event.frame;if(elapsed<SAMPLE_RATE*(event.note.length()*15.0/project.bpm+.8)){var voice=new Voice(project.tracks[event.track].instrument,event.track,event.note,project.bpm);voice.seek((int)elapsed);if(!voice.finished()){if(voices.size()>=64)voices.removeFirst();voices.add(voice);}}}
        for(int i=0;i<8;i++){var t=project.tracks[i];alpha[i]=1-Math.exp(-TAU*(100+Math.pow(t.cutoff/100.0,2)*9000)/SAMPLE_RATE);double pan=(t.pan+100)/200.0*Math.PI/2;left[i]=Math.cos(pan)*t.volume/100.0;right[i]=Math.sin(pan)*t.volume/100.0;}
        length=length(pattern);
    }
    private final long length;
    private long length(int pattern){return pattern<0?project.durationFrames():project.barFrames();}
    public static List<Event> schedule(MusicProject p,int pattern){var list=new ArrayList<Event>();int bars=pattern<0?p.bars:1;boolean[] audible=new boolean[MusicProject.TRACKS];for(int t=0;t<audible.length;t++)audible[t]=p.audible(t);
        for(int bar=0;bar<bars;bar++)for(int lane=0;lane<(pattern<0?MusicProject.LANES:1);lane++){
            int selected=pattern<0?p.playlist[lane][bar]:pattern;if(selected<0)continue;
            for(int track=0;track<MusicProject.TRACKS;track++){
                if(!audible[track] || pattern<0 && !p.clipIncludes(lane,bar,track))continue;
                for(var n:p.tracks[track].notes(selected))list.add(new Event(Math.round((bar*240.0/p.bpm+p.stepSeconds(n.step()))*SAMPLE_RATE),track,n));
            }
        }
        list.sort(Comparator.comparingLong(Event::frame).thenComparingInt(Event::track).thenComparingInt(e->e.note.pitch()));return List.copyOf(list);
    }
    public long frame(){return frame;}
    public void read(byte[] pcm,int frames){if(frames*4>pcm.length)throw new IllegalArgumentException("PCM buffer too short");int delayFrames=(int)Math.round(SAMPLE_RATE*30.0/project.bpm);
        for(int out=0;out<frames;out++){
            if(loop && frame>=length){frame=0;next=0;}
            while(next<events.size() && events.get(next).frame<=frame){var e=events.get(next++);if(voices.size()>=64)voices.removeFirst();voices.add(new Voice(project.tracks[e.track].instrument,e.track,e.note,project.bpm));}
            double l=0,r=0,sendL=0,sendR=0;double[] sums=trackSamples;
            Arrays.fill(sums,0);for(int i=voices.size()-1;i>=0;i--){var v=voices.get(i);sums[v.track]+=v.sample();if(v.finished())voices.remove(i);}
            int read=Math.floorMod(echoIndex-delayFrames,echoes[0].length);
            for(int t=0;t<8;t++){var track=project.tracks[t];filter[t]+=alpha[t]*(sums[t]-filter[t]);double delayed=echoes[t][read],signal=filter[t]+delayed*track.delay/140.0;echoes[t][echoIndex]=(float)(filter[t]+delayed*.32);double a=signal*left[t],b=signal*right[t];l+=a;r+=b;sendL+=a*track.reverb/100.0;sendR+=b*track.reverb/100.0;}
            int ri=roomIndex%roomL.length,rj=roomIndex%roomR.length;double roomA=roomL[ri],roomB=roomR[rj];roomL[ri]=(float)(sendL*.4+roomB*.62);roomR[rj]=(float)(sendR*.4+roomA*.62);l=(l+roomA)*project.master/100.0;r=(r+roomB)*project.master/100.0;
            write(pcm,out*4,l);write(pcm,out*4+2,r);frame++;echoIndex=(echoIndex+1)%echoes[0].length;roomIndex++;}
    }
    private final double[] trackSamples=new double[8];
    private static void write(byte[] b,int at,double v){short n=(short)Math.round(Math.clamp(v/(1+Math.abs(v)),-1,1)*32767);b[at]=(byte)n;b[at+1]=(byte)(n>>8);}
    private static final class Voice {
        final MusicProject.Instrument instrument;final int track;final double gain,hold,release,attack,increment;int age,noise=0x591bc;double phase,kickPhase,kickFrequency=160;
        Voice(MusicProject.Instrument i,int track,MusicProject.Note n,int bpm){instrument=i;this.track=track;gain=n.velocity()/127.0*.34;double duration=n.length()*15.0/bpm*SAMPLE_RATE;
            hold=i.drum()?SAMPLE_RATE*.025:duration;
            attack=SAMPLE_RATE*switch(i){case PAD->.075;case STRINGS->.11;case CHOIR->.09;case FLUTE->.018;case BRASS->.022;default->60.0/SAMPLE_RATE;};
            release=SAMPLE_RATE*switch(i){case KICK->.27;case SNARE,CLAP->.16;case HAT->.06;case PLUCK->.18;case BELL->.65;case PAD->.22;case TOM->.35;case RIM->.07;case SHAKER->.11;case COWBELL->.24;case CRASH->.72;case RIDE->.48;case PIANO->.25;case ELECTRIC_PIANO->.3;case GUITAR,MARIMBA->.2;case STRINGS,CHOIR->.4;case VIBRAPHONE->.55;case GLOCKENSPIEL->.7;default->.08;};
            increment=440*Math.pow(2,(n.pitch()-69)/12.0)/SAMPLE_RATE;
        }
        void seek(int samples){age=samples;phase=increment*samples%1;double decay=Math.pow(.9988,samples);kickFrequency=42+118*decay;kickPhase=(42.0*samples+118*.9988*(1-decay)/(1-.9988))/SAMPLE_RATE;}
        boolean finished(){return age>hold+release;}
        double sample(){double env=Math.min(1,age/attack)*Math.max(0,age<hold?1:1-(age-hold)/release),wave;noise^=noise<<13;noise^=noise>>>17;noise^=noise<<5;double white=noise/(double)Integer.MAX_VALUE;phase+=increment;phase-=Math.floor(phase);double s=Math.sin(TAU*phase);
            wave=switch(instrument){case KICK->{kickFrequency=42+(kickFrequency-42)*.9988;kickPhase+=kickFrequency/SAMPLE_RATE;yield Math.sin(TAU*kickPhase)*1.8;}case SNARE->white*.85+s*.35;case HAT->white*(age%2==0?1:-.55)*.5;case CLAP->white*(age<240 || age>380 && age<600 || age>750?.85:.1);case BASS->s+.35*Math.sin(TAU*phase*2);case KEYS->s+.28*Math.sin(TAU*phase*3)+.12*Math.sin(TAU*phase*5);case PLUCK->(s+.2*Math.sin(TAU*phase*2))*Math.max(.1,1-age/(hold+release));case PAD->s*.6+Math.sin(TAU*phase*2)*.18+Math.sin(TAU*phase*3)*.12;case BELL->s*.7+Math.sin(TAU*phase*2.76)*.3;
                case PIANO->(s+.32*Math.sin(TAU*phase*2)+.18*Math.sin(TAU*phase*3)+.08*Math.sin(TAU*phase*7))*Math.exp(-age/(double)SAMPLE_RATE*1.6);
                case ELECTRIC_PIANO->Math.sin(TAU*phase+1.8*Math.exp(-age/(double)SAMPLE_RATE*3)*Math.sin(TAU*phase*2))*.85;
                case ORGAN->s*.65+Math.sin(TAU*phase*2)*.3+Math.sin(TAU*phase*4)*.16+Math.sin(TAU*phase*8)*.08;
                case GUITAR->(s+.38*Math.sin(TAU*phase*2)+.2*Math.sin(TAU*phase*3)+white*Math.exp(-age/180.0)*.3)*Math.exp(-age/(double)SAMPLE_RATE*2.5);
                case STRINGS->s*.65+Math.sin(TAU*phase*2)*.28+Math.sin(TAU*phase*3)*.19+Math.sin(TAU*phase*4)*.12+Math.sin(TAU*phase*5)*.08;
                case CHOIR->s*.55+Math.sin(TAU*phase*3)*.3+Math.sin(TAU*phase*5)*.16+Math.sin(TAU*phase*7)*.08;
                case FLUTE->s*.9+Math.sin(TAU*phase*2)*.08+white*.018;
                case BRASS->s*.7+Math.sin(TAU*phase*2)*.4+Math.sin(TAU*phase*3)*.24+Math.sin(TAU*phase*4)*.13;
                case LEAD->(s+Math.sin(TAU*phase*3)/3+Math.sin(TAU*phase*5)/5+Math.sin(TAU*phase*7)/7)*.85;
                case SUB_BASS->s*.95;
                case ACID_BASS->s*.8+Math.sin(TAU*phase*2)*.48+Math.sin(TAU*phase*3)*.32+Math.sin(TAU*phase*4)*.22+Math.sin(TAU*phase*5)*.14;
                case TOM->Math.sin(TAU*(increment*SAMPLE_RATE*.42*age/SAMPLE_RATE+.018*(1-Math.exp(-age/900.0))))*1.3;
                case RIM->(Math.sin(TAU*phase*3.8)*.6+Math.sin(TAU*phase*7.3)*.35+white*.15)*Math.exp(-age/700.0);
                case SHAKER->white*.6*Math.abs(Math.sin(age*.043));
                case COWBELL->Math.sin(TAU*phase*1.8)*.65+Math.sin(TAU*phase*2.67)*.45;
                case CRASH->(white*.65+Math.sin(TAU*phase*7.31)*.18+Math.sin(TAU*phase*11.17)*.12)*Math.exp(-age/(double)SAMPLE_RATE*2);
                case RIDE->Math.sin(TAU*phase*5.17)*.38+Math.sin(TAU*phase*8.63)*.25+white*.22;
                case VIBRAPHONE->(s+.22*Math.sin(TAU*phase*4))*(.85+.15*Math.sin(TAU*5*age/SAMPLE_RATE))*Math.exp(-age/(double)SAMPLE_RATE*1.1);
                case MARIMBA->(s+.35*Math.sin(TAU*phase*4)+.1*Math.sin(TAU*phase*9))*Math.exp(-age/(double)SAMPLE_RATE*4);
                case GLOCKENSPIEL->(s*.6+Math.sin(TAU*phase*2.82)*.35+Math.sin(TAU*phase*5.4)*.18)*Math.exp(-age/(double)SAMPLE_RATE*1.8);
            };age++;return wave*gain*env;}
    }
    public static void wav(MusicProject project,OutputStream stream)throws IOException{
        long frames=project.durationFrames()+SAMPLE_RATE;int bytes=Math.toIntExact(frames*4);var out=new DataOutputStream(stream);out.writeBytes("RIFF");le(out,36+bytes,4);out.writeBytes("WAVEfmt ");le(out,16,4);le(out,1,2);le(out,2,2);le(out,SAMPLE_RATE,4);le(out,SAMPLE_RATE*4,4);le(out,4,2);le(out,16,2);out.writeBytes("data");le(out,bytes,4);var synth=new MusicSynth(project,-1,false,0);byte[] block=new byte[4096*4];while(frames>0){int n=(int)Math.min(4096,frames);synth.read(block,n);out.write(block,0,n*4);frames-=n;}out.flush();
    }
    private static void le(DataOutputStream out,int n,int bytes)throws IOException{for(int i=0;i<bytes;i++)out.writeByte(n>>i*8);}
}
