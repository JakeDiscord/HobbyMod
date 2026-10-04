package io.github.jakediscord.hobbymod.dj;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlaylistTest {
    private static MusicProject twoCompletePatterns(){
        var p=new MusicProject();p.bars=1;
        for(int channel=0;channel<8;channel++){
            p.tracks[channel].put(0,new MusicProject.Note(0,48+channel,2,100));
            p.tracks[channel].put(1,new MusicProject.Note(4,60+channel,2,90));
        }
        return p;
    }

    @Test void copyingAndClearingWholePatternsKeepsEveryChannel(){
        var p=twoCompletePatterns();p.copyPattern(1,3);
        for(int channel=0;channel<8;channel++)assertEquals(p.tracks[channel].notes(1),p.tracks[channel].notes(3));
        p.clearPattern(1);assertTrue(MusicSynth.schedule(p,1).isEmpty());assertEquals(8,MusicSynth.schedule(p,3).size());
    }

    @Test void allNewInstrumentIdsSurviveProjectRoundTrips()throws Exception{
        for(var instrument:MusicProject.Instrument.values()){
            var p=twoCompletePatterns();p.tracks[0].instrument=instrument;p.placeClip(7,0,1);
            var q=MusicProject.decode(p.encode());assertEquals(instrument,q.tracks[0].instrument);
            assertEquals(MusicSynth.schedule(p,-1),MusicSynth.schedule(q,-1));
        }
    }

    @Test void overlappingClipsPlayEveryInstrumentAndFollowMixer(){
        var p=twoCompletePatterns();p.placeClip(2,0,0);p.placeClip(7,0,1);
        var events=MusicSynth.schedule(p,-1);assertEquals(16,events.size());
        for(int channel=0;channel<8;channel++){
            final int c=channel;
            assertEquals(2,events.stream().filter(e->e.track()==c).count());
        }
        p.tracks[4].solo=true;assertEquals(2,MusicSynth.schedule(p,-1).size());
        p.tracks[4].mute=true;assertTrue(MusicSynth.schedule(p,-1).isEmpty());
    }

    @Test void lanesAreInterchangeableAndErasingOneKeepsTheOther(){
        var p=twoCompletePatterns();p.placeClip(0,0,0);p.placeClip(1,0,1);
        var expected=MusicSynth.schedule(p,-1);
        p.placeClip(0,0,-1);p.placeClip(6,0,0);
        assertEquals(expected,MusicSynth.schedule(p.copy(),-1));
        p.placeClip(1,0,-1);assertEquals(MusicSynth.schedule(p,0),MusicSynth.schedule(p,-1));
        p.clearSong();assertTrue(p.songEmpty());assertTrue(MusicSynth.schedule(p,-1).isEmpty());
        assertEquals(8,MusicSynth.schedule(p,1).size(),"Pattern preview ignores the playlist");
    }

    @Test void repeatingOnlyFillsChosenLaneAndDemoDoesNotMultiplyVoices(){
        var p=twoCompletePatterns();p.bars=3;p.placeClip(7,1,1);p.repeatPattern(2,0);
        assertEquals(1,p.playlist[7][1]);assertEquals(32,MusicSynth.schedule(p,-1).size());
        for(int bar=0;bar<3;bar++)assertEquals(0,p.playlist[2][bar]);
        var demo=MusicProject.demo();
        assertEquals(MusicSynth.schedule(demo,0).size()*demo.bars,MusicSynth.schedule(demo,-1).size());
    }

    @Test void oldChannelSongsMigrateWithoutAddingOrDroppingInstruments()throws Exception{
        // This fixture was written by the original HDJ1 encoder, before playlist changes.
        byte[] bytes;try(var in=getClass().getResourceAsStream("/dj/legacy-channel-song.hobbytrack")){assertNotNull(in);bytes=in.readAllBytes();}
        var p=MusicProject.decode(bytes);var events=MusicSynth.schedule(p,-1);
        assertEquals(17,events.size());assertEquals(events,MusicSynth.schedule(p.copy(),-1));
        for(int c=0;c<8;c++){
            final int channel=c;
            var notes=events.stream().filter(e->e.track()==channel).toList();
            assertEquals(c==7?3:2,notes.size());assertEquals(c<4?48+c:60+c,notes.getFirst().note().pitch());
            assertEquals(48+c,notes.get(1).note().pitch());
        }
        assertFalse(p.fullPattern(0,0));assertTrue(p.fullPattern(0,1));
        p.placeClip(0,0,0);assertTrue(p.fullPattern(0,0));
        assertEquals(21,MusicSynth.schedule(p,-1).size(),"Replacing a migrated clip includes the entire pattern");
        assertArrayEquals(p.encode(),MusicProject.decode(p.encode()).encode());
    }

    @Test void layeredSongAudioAndWavIncludeBothPatterns()throws Exception{
        var p=twoCompletePatterns();p.placeClip(0,0,0);p.placeClip(1,0,1);
        byte[] mixed=new byte[(int)(p.durationFrames()+MusicSynth.SAMPLE_RATE)*4];
        new MusicSynth(p,-1,false,0).read(mixed,mixed.length/4);
        var single=p.copy();single.placeClip(1,0,-1);byte[] dry=new byte[mixed.length];
        new MusicSynth(single,-1,false,0).read(dry,dry.length/4);assertFalse(Arrays.equals(dry,mixed));
        var wav=new ByteArrayOutputStream();MusicSynth.wav(p,wav);
        assertArrayEquals(mixed,Arrays.copyOfRange(wav.toByteArray(),44,wav.size()));
    }
}
