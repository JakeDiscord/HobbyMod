package io.github.jakediscord.hobbymod.neoforge.dj;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.dj.*;
import io.github.jakediscord.hobbymod.dj.client.DjClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;

/** NeoForge's streaming extension keeps synthesis in Minecraft's audio mixer and Records volume category. */
@EventBusSubscriber(modid=HobbyMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class DjAudio {
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->DjClient.backend=(p,pattern,loop,offset)->{var sound=new StreamSound(p,pattern,loop,offset);Minecraft.getInstance().getSoundManager().play(sound);return sound;});}
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("hobbymod","dj_stream");
    private static final class StreamSound extends AbstractTickableSoundInstance implements DjClient.Playback {
        private final MusicProject project;private final int pattern;private final boolean repeat;private final long offset;
        StreamSound(MusicProject p,int pattern,boolean repeat,long offset){super(SoundEvent.createVariableRangeEvent(ID),SoundSource.RECORDS,SoundInstance.createUnseededRandom());project=p.copy();this.pattern=pattern;this.repeat=repeat;this.offset=offset;relative=true;attenuation=SoundInstance.Attenuation.NONE;volume=1;pitch=1;looping=false;}
        @Override public WeighedSoundEvents resolve(SoundManager manager){sound=new Sound(ID,ConstantFloat.of(1),ConstantFloat.of(1),1,Sound.Type.FILE,true,false,48);var event=new WeighedSoundEvents(ID,null);event.addSound(sound);return event;}
        @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library,Sound sound,boolean loop){return CompletableFuture.completedFuture(new SynthStream(project,pattern,repeat,offset));}
        @Override public void tick(){}
        @Override public void gain(float value){volume=value;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void close(){stop();Minecraft.getInstance().getSoundManager().stop(this);}
    }
    private static final class SynthStream implements AudioStream {
        private final MusicSynth synth;private final long end;private final boolean loop;private long read;private boolean closed;
        SynthStream(MusicProject p,int pattern,boolean loop,long offset){synth=new MusicSynth(p,pattern,loop,offset);this.loop=loop;end=(pattern<0?p.durationFrames():p.barFrames())+MusicSynth.SAMPLE_RATE;read=offset;}
        public AudioFormat getFormat(){return new AudioFormat(MusicSynth.SAMPLE_RATE,16,2,true,false);}
        public ByteBuffer read(int requested){if(closed || !loop && read>=end)return BufferUtils.createByteBuffer(0);int frames=Math.max(1,requested/4);if(!loop)frames=(int)Math.min(frames,end-read);byte[] bytes=new byte[frames*4];synth.read(bytes,frames);read+=frames;var buffer=BufferUtils.createByteBuffer(bytes.length);buffer.put(bytes).flip();return buffer;}
        public void close(){closed=true;}
    }
    private DjAudio(){}
}
