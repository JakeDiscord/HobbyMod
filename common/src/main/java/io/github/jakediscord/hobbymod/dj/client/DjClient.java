package io.github.jakediscord.hobbymod.dj.client;
import dev.architectury.networking.NetworkManager;
import dev.architectury.event.events.client.ClientTickEvent;
import io.github.jakediscord.hobbymod.dj.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Shared playback coordinator. Each loader supplies the sound-engine stream adapter. */
public final class DjClient {
    public interface Playback {void close();void gain(float value);}
    public interface Backend {Playback play(MusicProject project,int pattern,boolean loop,long offset);}
    public static Backend backend;
    private record Key(BlockPos pos,int deck){}
    private static final class Active {Playback sound;DjBlockEntity owner;int generation;long seen;}
    private static final Map<Key,Active> ACTIVE=new HashMap<>();
    private static Object world;
    public static void init(){
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register(c->{dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(DjContent.ENTITY.get(),DjRenderer::new);dev.architectury.registry.client.rendering.RenderTypeRegistry.register(net.minecraft.client.renderer.RenderType.cutout(),DjContent.WORKSTATION.get());});
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,DjNetworking.State.TYPE,DjNetworking.State.CODEC,(p,c)->c.queue(()->{
            var mc=Minecraft.getInstance();if(mc.level==null || !(mc.level.getBlockEntity(p.pos()) instanceof DjBlockEntity b))return;b.loadWithComponents(p.data(),mc.level.registryAccess());
            if(p.open())mc.setScreen(new DjScreen(p.pos()));else{DjScreen editor=mc.screen instanceof DjScreen s?s:mc.screen instanceof DjEditorOverlay overlay?overlay.editor():null;if(editor!=null && editor.pos.equals(p.pos()))editor.ack(p.message());}
        }));
        ClientTickEvent.CLIENT_POST.register(DjClient::tick);
    }
    public static void tickBlock(Level level,BlockPos pos,BlockState state,DjBlockEntity b){var mc=Minecraft.getInstance();if(mc.player==null || backend==null)return;
        if(world!=level){stopAll();world=level;}long now=level.getGameTime();double distance=mc.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);int range=b.range();
        for(int d=0;d<2;d++){var key=new Key(pos.immutable(),d);var a=ACTIVE.get(key);
            if(!b.playing[d] || distance>range*range){if(a!=null){a.sound.close();ACTIVE.remove(key);}continue;}
            if(a==null || a.generation!=b.generation[d] || a.owner!=b){if(a!=null)a.sound.close();if(a==null && ACTIVE.size()>=4)continue;a=new Active();a.owner=b;a.generation=b.generation[d];a.sound=backend.play(b.projects[d],b.pattern[d],b.loop[d],b.positionFrames(d,now));ACTIVE.put(key,a);}
            a.seen=now;a.sound.gain((float)(b.gain(d)*Math.max(0,1-Math.sqrt(distance)/range)));
        }
    }
    private static void tick(Minecraft mc){if(mc.level==null){stopAll();world=null;return;}long now=mc.level.getGameTime();var it=ACTIVE.values().iterator();while(it.hasNext()){var a=it.next();if(a.owner.isRemoved() || now-a.seen>5){a.sound.close();it.remove();}}}
    private static void stopAll(){ACTIVE.values().forEach(a->a.sound.close());ACTIVE.clear();}
    private DjClient(){}
}
