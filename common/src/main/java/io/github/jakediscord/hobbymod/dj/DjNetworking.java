package io.github.jakediscord.hobbymod.dj;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class DjNetworking {
    public static final int SAVE=0,PLAY=1,STOP=2,LOOP=3,MODE=4,CUE=5,SYNC=6,CROSS=7,LOAD=8,BURN=9,DEMO=10,NEW=11,HEARTBEAT=12,RELEASE=13;
    public record Action(BlockPos pos,int revision,int kind,int deck,int value,byte[] data) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","dj_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.revision);b.writeVarInt(p.kind);b.writeVarInt(p.deck);b.writeVarInt(p.value);b.writeByteArray(p.data);},b->new Action(b.readBlockPos(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readByteArray(MusicProject.MAX_BYTES)));public Type<Action> type(){return TYPE;}
    }
    public record State(BlockPos pos,CompoundTag data,String message,boolean open) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","dj_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeNbt(p.data);b.writeUtf(p.message,160);b.writeBoolean(p.open);},b->new State(b.readBlockPos(),b.readNbt(),b.readUtf(160),b.readBoolean()));public Type<State> type(){return TYPE;}
    }
    /** Commands are authorized at the workstation, bounded and revision checked before any inventory changes. */
    public static String work(Player p,DjBlockEntity b,Action a){
        if(a.kind<0 || a.kind>RELEASE || a.deck<0 || a.deck>1)return "Invalid DJ command.";
        if(!b.owned(p))return "Your editing session expired. Reopen the workstation.";
        if(a.kind==HEARTBEAT)return "";
        if(a.kind==RELEASE){b.release(p);return "";}
        if(a.revision!=b.revision)return "The set changed. Your studio has been refreshed.";
        int d=a.deck;long now=p.level().getGameTime();
        switch(a.kind){
            case SAVE->{try{var project=MusicProject.decode(a.data);project.author=MusicProject.clean(p.getName().getString());b.replace(d,project);return "Saved";}catch(java.io.IOException e){return "Invalid project: "+e.getMessage();}}
            case PLAY->{b.playing[d]=true;b.started[d]=now;b.generation[d]++;}
            case STOP->{b.playing[d]=false;b.generation[d]++;}
            case LOOP->{b.loop[d]=!b.loop[d];b.generation[d]++;}
            case MODE->{if(a.value< -1 || a.value>7)return "Choose a song or pattern A–H.";b.pattern[d]=a.value;b.cue[d]=0;b.started[d]=now;b.generation[d]++;}
            case CUE->{if(a.value<0 || a.value>=b.projects[d].bars)return "Cue outside this song.";b.cue[d]=a.value;b.started[d]=now;b.generation[d]++;}
            case SYNC->{b.projects[d].bpm=b.projects[1-d].bpm;b.playing[d]=b.playing[1-d];b.started[d]=b.playing[d]?b.started[1-d]:now;b.cue[d]=Math.min(b.cue[1-d],b.projects[d].bars-1);b.generation[d]++;}
            case CROSS->{if(a.value<0 || a.value>100)return "Invalid crossfader.";b.crossfade=a.value;}
            case LOAD->{ItemStack disc=heldDisc(p);var project=DjDiscItem.read(disc);if(project==null)return "Hold a recorded music disc to load this deck.";b.playing[d]=false;b.cue[d]=0;b.replace(d,project);return "Loaded "+project.title;}
            case BURN->{int slot=-1;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(DjContent.BLANK.get())){slot=i;break;}if(slot<0 && !p.getAbilities().instabuild)return "Carry a blank music disc to save your song.";
                var project=b.projects[d].copy();project.author=MusicProject.clean(p.getName().getString());var disc=DjDiscItem.create(project);if(!p.getAbilities().instabuild)p.getInventory().getItem(slot).shrink(1);if(!p.addItem(disc))p.drop(disc,false);return "Music disc saved: "+project.title;}
            case DEMO->{b.playing[d]=false;b.cue[d]=0;b.replace(d,MusicProject.demo());return "Demo loaded. Edit any note to make it yours.";}
            case NEW->{b.playing[d]=false;b.cue[d]=0;b.replace(d,new MusicProject());return "New empty project.";}
            default->{return "Invalid DJ command.";}
        }
        b.revision++;b.changed();return "";
    }
    private static ItemStack heldDisc(Player p){return p.getMainHandItem().is(DjContent.DISC.get())?p.getMainHandItem():p.getOffhandItem();}
    private record Rate(long tick,int count){}
    private static final Map<ServerPlayer,Rate> RATE=new WeakHashMap<>();
    public static void register(){if(Platform.getEnvironment()==Env.SERVER)NetworkManager.registerS2CPayloadType(State.TYPE,State.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Action.TYPE,Action.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer p) || !p.level().hasChunkAt(a.pos) || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,a.pos) || !(p.level().getBlockEntity(a.pos) instanceof DjBlockEntity b))return;
            long now=p.level().getGameTime();var old=RATE.get(p);var rate=old==null || now-old.tick>=20?new Rate(now,1):new Rate(old.tick,old.count+1);RATE.put(p,rate);String message=rate.count>20?"Too many commands. Try again.":work(p,b,a);
            if(a.kind==HEARTBEAT || a.kind==RELEASE)return;p.inventoryMenu.broadcastChanges();NetworkManager.sendToPlayer(p,new State(a.pos,b.getUpdateTag(p.registryAccess()),message,false));
        }));
    }
    private DjNetworking(){}
}
