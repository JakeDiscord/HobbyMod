package io.github.jakediscord.hobbymod.astronomy.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.astronomy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import io.github.jakediscord.hobbymod.sculpting.client.SculptureOrbit;
public final class AstronomyClient {
    public static SkyCatalog catalog;
    public static long seed,sky,tick;
    private static long lastSky=Long.MIN_VALUE;private static boolean skyAdvancing;
    public static AstronomyNetworking.State state;
    public static ObservationJournal journal=new ObservationJournal();
    public static double time(float partial){var l=Minecraft.getInstance().level;return l==null?sky:l.getDayTime()+(skyAdvancing?partial:0);}
    public static boolean active(){return Minecraft.getInstance().screen instanceof EyepieceScreen;}
    public static EyepieceScreen scope(){return Minecraft.getInstance().screen instanceof EyepieceScreen s?s:null;}
    public static SculptureOrbit.CameraPose camera(){var s=scope();var mc=Minecraft.getInstance();if(s==null || mc.player==null)return null;s.updateTracking();var eye=mc.player.getEyePosition();if(!s.naked && mc.level!=null && mc.level.getBlockEntity(s.pos) instanceof TelescopeBlockEntity b)eye=b.lens(s.yaw,s.pitch);return new SculptureOrbit.CameraPose(eye,s.yaw,s.pitch);}
    public static void init(){
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register(c->dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(AstronomyContent.ENTITY.get(),TelescopeRenderer::new));
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(mc->{if(mc.level==null){catalog=null;state=null;lastSky=Long.MIN_VALUE;journal=new ObservationJournal();}else{long current=mc.level.getDayTime();skyAdvancing=lastSky!=Long.MIN_VALUE && current==lastSky+1;lastSky=current;if(scope()!=null)scope().heartbeat();}});
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AstronomyNetworking.State.TYPE,AstronomyNetworking.State.CODEC,(p,c)->c.queue(()->{
            var mc=Minecraft.getInstance();if(mc.level==null)return;if(catalog==null || seed!=p.seed()){seed=p.seed();catalog=new SkyCatalog(seed);}sky=p.sky();tick=mc.level.getGameTime();state=p;journal=AstronomyData.decode(p.journal());if(scope()!=null && scope().pos.equals(p.pos()))scope().receiveTracking(p.tracking());
            if(p.kind()==AstronomyNetworking.JOURNAL)mc.setScreen(new FieldJournalScreen());
            else if(p.kind()==AstronomyNetworking.SCOPE)mc.setScreen(new EyepieceScreen(p.pos(),p.kind()==AstronomyNetworking.NAKED));
            else if(p.kind()==AstronomyNetworking.CLOSE && active())mc.setScreen(null);
        }));
    }
    private AstronomyClient(){}
}
