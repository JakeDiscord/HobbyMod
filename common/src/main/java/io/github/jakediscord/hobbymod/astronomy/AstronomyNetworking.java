package io.github.jakediscord.hobbymod.astronomy;

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
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.*;
public final class AstronomyNetworking {
    public static final int SYNC=0,JOURNAL=1,SCOPE=2,NAKED=3,CLOSE=4,AIM=5;
    public record Action(int kind,BlockPos pos,float yaw,float pitch,float focus,float magnification) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","astronomy_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.kind);b.writeBlockPos(p.pos);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeFloat(p.focus);b.writeFloat(p.magnification);},b->new Action(b.readVarInt(),b.readBlockPos(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat()));public Type<Action> type(){return TYPE;}
    }
    public record MountTarget(BlockPos pos,int id,boolean tracking) implements CustomPacketPayload {
        public static final Type<MountTarget> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","astronomy_mount_target"));
        public static final StreamCodec<RegistryFriendlyByteBuf,MountTarget> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeVarInt(p.id);b.writeBoolean(p.tracking);},b->new MountTarget(b.readBlockPos(),b.readVarInt(),b.readBoolean()));
        public Type<MountTarget> type(){return TYPE;}
    }
    public record State(long seed,long sky,long tick,int kind,BlockPos pos,CompoundTag journal,int target,int exposure,double quality,String message,int tracking) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","astronomy_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->{b.writeLong(p.seed);b.writeLong(p.sky);b.writeLong(p.tick);b.writeVarInt(p.kind);b.writeBlockPos(p.pos);b.writeNbt(p.journal);b.writeVarInt(p.target);b.writeVarInt(p.exposure);b.writeDouble(p.quality);b.writeUtf(p.message,160);b.writeVarInt(p.tracking);},b->new State(b.readLong(),b.readLong(),b.readLong(),b.readVarInt(),b.readBlockPos(),b.readNbt(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readUtf(160),b.readVarInt()));public Type<State> type(){return TYPE;}
    }
    private static final class Session {BlockPos pos;float yaw,pitch,focus,mag;long heartbeat,lastAction,lastTarget=Long.MIN_VALUE;boolean spyglass;int tracking=-1;final ObservationExposure exposure=new ObservationExposure();double quality;String message="";Session(BlockPos p){pos=p;}}
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> REQUESTS=new WeakHashMap<>();
    public static boolean hasJournal(net.minecraft.world.entity.player.Player p){for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(p.getInventory().getItem(slot).is(AstronomyContent.JOURNAL.get()))return true;return false;}
    public static void send(ServerPlayer p,int kind,Session session){var data=AstronomyData.get(p.server);NetworkManager.sendToPlayer(p,new State(data.seed,p.server.overworld().getDayTime(),p.server.overworld().getGameTime(),kind,session==null || session.pos==null?BlockPos.ZERO:session.pos,AstronomyData.encode(data.journal(p.getUUID())),session==null?-1:session.exposure.target,session==null?0:session.exposure.ticks,session==null?0:session.quality,session==null?"":session.message,session==null?-1:session.tracking));}
    public static void openJournal(ServerPlayer p){send(p,JOURNAL,null);}
    public static boolean allowed(net.minecraft.world.entity.player.Player p,BlockPos pos){return p.isAlive() && p.level().dimension()==Level.OVERWORLD && p.distanceToSqr(Vec3.atCenterOf(pos))<36 && p.level().hasChunkAt(pos) && io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,pos) && p.level().getBlockEntity(pos) instanceof TelescopeBlockEntity;}
    public static void openTelescope(ServerPlayer p,BlockPos pos){
        if(!allowed(p,pos))return;long now=p.level().getGameTime();
        for(var entry:SESSIONS.entrySet())if(entry.getKey()!=p && Objects.equals(entry.getValue().pos,pos) && now-entry.getValue().heartbeat<80){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Telescope in use"),true);return;}
        var b=(TelescopeBlockEntity)p.level().getBlockEntity(pos);var s=new Session(pos.immutable());s.yaw=b.yaw;s.pitch=b.pitch;s.focus=.72f;s.mag=b.magnification;s.heartbeat=now;SESSIONS.put(p,s);p.connection.send(b.getUpdatePacket());send(p,SCOPE,s);
        p.level().playSound(null,pos,net.minecraft.sounds.SoundEvents.SPYGLASS_USE,net.minecraft.sounds.SoundSource.BLOCKS,.35f,.8f);
    }
    private static boolean clear(ServerPlayer p,Vec3 eye,SkyCatalog.Vector direction){Vec3 end=eye.add(direction.x()*96,direction.y()*96,direction.z()*96);BlockGetter loaded=new BlockGetter(){
            public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos){return p.level().hasChunkAt(pos)?p.level().getBlockEntity(pos):null;}
            public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos pos){return p.level().hasChunkAt(pos)?p.level().getBlockState(pos):net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();}
            public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos){return p.level().hasChunkAt(pos)?p.level().getFluidState(pos):net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();}
            public int getHeight(){return p.level().getHeight();}public int getMinBuildHeight(){return p.level().getMinBuildHeight();}
        };return loaded.clip(new ClipContext(eye,end,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    private static void tick(net.minecraft.server.MinecraftServer server){
        long game=server.overworld().getGameTime();if(game%5!=0)return;var data=AstronomyData.get(server);long sky=server.overworld().getDayTime();
        for(var p:server.getPlayerList().getPlayers()){
            Session s=SESSIONS.get(p);boolean spy=p.isUsingItem() && p.getUseItem().is(net.minecraft.world.item.Items.SPYGLASS);
            if(s==null && !spy){if(game%200==0)send(p,SYNC,null);continue;}
            if(s==null){s=new Session(null);s.mag=5;s.focus=.72f;s.spyglass=true;}
            if(s.spyglass && !spy){SESSIONS.remove(p);continue;}
            if(p.level().dimension()!=Level.OVERWORLD || !p.isAlive() || (s.pos!=null && !allowed(p,s.pos)) || (!spy && game-s.heartbeat>60)){if(SESSIONS.remove(p)!=null)send(p,CLOSE,null);continue;}
            var b=s.pos==null?null:(TelescopeBlockEntity)p.level().getBlockEntity(s.pos);var eye=b==null?p.getEyePosition():b.lens(s.yaw,s.pitch);
            if(s.tracking>=0){var tracked=data.catalog.target(s.tracking);
                if(b==null || !ObservationAccess.canTrack(data.journal(p.getUUID()),tracked,b.large()))s.tracking=-1;
                else{var direction=tracked.direction(sky);s.yaw=(float)Math.toDegrees(Math.atan2(-direction.x(),direction.z()));s.pitch=(float)-Math.toDegrees(Math.asin(direction.y()));b.yaw=s.yaw;b.pitch=s.pitch;if(game%20==0)b.changed();}
            }
            if(spy){s.yaw=p.getYRot();s.pitch=p.getXRot();}
            var aim=SkyCatalog.aim(s.yaw,s.pitch);var target=data.catalog.nearest(aim,sky,Math.max(.18,1.5/s.mag));
            double aperture=b==null?(spy?35:0):b.aperture();double quality=SkyCatalog.quality(target,sky,.72,s.mag,aperture,p.level().getRainLevel(1),p.level().getBrightness(LightLayer.BLOCK,BlockPos.containing(eye)),target!=null && clear(p,eye,target.direction(sky)));
            if(b==null && target!=null && target.magnitude()>(spy?4:2.5))quality=0;
            s.quality=quality;
            if(!hasJournal(p))s.message="Carry a field journal to record";else if(target==null)s.message="";else if(quality==0)s.message=SkyCatalog.night(sky)<.4?"Daylight":p.level().isRaining()?"Cloud cover":target.direction(sky).y()<.08?"Below horizon":target.deep() && aperture<60?"Larger aperture needed":"Obstructed view";else if(quality<.2)s.message="Low observation quality";else s.message="";
            var entry=target==null?null:data.journal(p.getUUID()).get(target.id());boolean eligible=true;
            if(entry!=null && quality>=.2 && hasJournal(p)){
                if(entry.completeness>=100){s.message="Catalog entry complete";eligible=false;}
                else if(entry.night==Math.floorDiv(sky,24000) && entry.nightPoints>=50){s.message="Night record complete";eligible=false;}
                else if(game>=entry.lastTick && game-entry.lastTick<600){s.message="Next observation in "+((600-(game-entry.lastTick)+19)/20)+"s";eligible=false;}
            }
            if(s.exposure.advance(target==null?-1:target.id(),hasJournal(p) && eligible?quality:0,game,sky) && target!=null){if(data.journal(p.getUUID()).record(target.id(),quality,sky,game)){data.setDirty();s.message="Observation saved";p.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.35f,1.35f);p.displayClientMessage(net.minecraft.network.chat.Component.literal("Recorded "+target.name()+" · "+data.journal(p.getUUID()).get(target.id()).completeness+"%"),false);}else s.message="Return for another observation";send(p,SYNC,s);}
            if(spy && !SESSIONS.containsKey(p)){SESSIONS.put(p,s);s.heartbeat=game;}if(spy)s.heartbeat=game;
            if(game%20==0)send(p,SYNC,s);
        }
    }
    public static boolean targetAllowed(net.minecraft.world.entity.player.Player p,BlockPos pos,int id,boolean tracking){
        if(!allowed(p,pos) || p.level().getServer()==null)return false;
        var data=AstronomyData.get(p.level().getServer());var target=data.catalog.target(id);var mount=(TelescopeBlockEntity)p.level().getBlockEntity(pos);
        return ObservationAccess.canPoint(data.journal(p.getUUID()),target) && (!tracking || ObservationAccess.canTrack(data.journal(p.getUUID()),target,mount.large()));
    }
    /** Returns false for forged, unknown, distant, busy or unsupported target requests. */
    public static boolean requestTarget(ServerPlayer p,BlockPos pos,int id,boolean tracking){
        var session=SESSIONS.get(p);if(session==null || !Objects.equals(session.pos,pos) || !allowed(p,pos))return false;
        if(id==-1){session.tracking=-1;send(p,SYNC,session);return true;}
        long game=p.level().getGameTime();if(session.lastTarget!=Long.MIN_VALUE && game>=session.lastTarget && game-session.lastTarget<3)return false;
        session.lastTarget=game;var data=AstronomyData.get(p.server);var target=data.catalog.target(id);
        var mount=(TelescopeBlockEntity)p.level().getBlockEntity(pos);
        if(!targetAllowed(p,pos,id,tracking)){
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Record a planet before tracking it"),true);send(p,SYNC,session);return false;
        }
        if(target.direction(p.level().getDayTime()).y()<.08)return false;
        var d=target.direction(p.level().getDayTime());session.yaw=(float)Math.toDegrees(Math.atan2(-d.x(),d.z()));session.pitch=(float)-Math.toDegrees(Math.asin(d.y()));session.tracking=tracking?id:-1;
        session.heartbeat=game;mount.yaw=session.yaw;mount.pitch=session.pitch;mount.changed();send(p,SYNC,session);return true;
    }
    public static void register(){
        if(Platform.getEnvironment()==Env.SERVER)NetworkManager.registerS2CPayloadType(State.TYPE,State.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,MountTarget.TYPE,MountTarget.CODEC,(request,context)->context.queue(()->{
            if(context.getPlayer() instanceof ServerPlayer p)requestTarget(p,request.pos(),request.id(),request.tracking());
        }));
        dev.architectury.event.events.common.PlayerEvent.PLAYER_JOIN.register(p->send(p,SYNC,null));
        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(SESSIONS::remove);
        dev.architectury.event.events.common.TickEvent.SERVER_POST.register(AstronomyNetworking::tick);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Action.TYPE,Action.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer p))return;var s=SESSIONS.get(p);long game=p.server.overworld().getGameTime();
            if(a.kind==CLOSE){SESSIONS.remove(p);return;}
            if(a.kind!=AIM){var previous=REQUESTS.get(p);if(previous!=null && game>=previous && game-previous<5)return;REQUESTS.put(p,game);}
            if(a.kind==JOURNAL){if(hasJournal(p))openJournal(p);return;}
            if(a.kind==NAKED)return; // The field journal never opens an optical view.
            if(a.kind!=AIM || s==null || (s.pos!=null && !s.pos.equals(a.pos)) || game-s.lastAction<3)return;
            if(!Float.isFinite(a.yaw) || !Float.isFinite(a.pitch) || !Float.isFinite(a.magnification))return;
            s.lastAction=game;s.heartbeat=game;if(s.tracking<0){s.yaw=net.minecraft.util.Mth.wrapDegrees(a.yaw);s.pitch=Math.clamp(a.pitch,-89,15);}s.focus=.72f;
            var b=s.pos!=null && allowed(p,s.pos)?(TelescopeBlockEntity)p.level().getBlockEntity(s.pos):null;s.mag=b==null?1:Math.clamp(a.magnification,b.large()?12:6,b.large()?100:40);
            if(b!=null){b.yaw=s.yaw;b.pitch=s.pitch;b.focus=s.focus;b.magnification=s.mag;if(game%20<5)b.changed();}
        }));
    }
    private AstronomyNetworking(){}
}
