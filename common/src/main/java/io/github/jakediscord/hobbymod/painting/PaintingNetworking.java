package io.github.jakediscord.hobbymod.painting;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.*;
public final class PaintingNetworking {
    public record Open(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","painting_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->b.writeBlockPos(p.pos),b->new Open(b.readBlockPos()));public Type<Open> type(){return TYPE;}
    }
    public record Ack(BlockPos pos,String message) implements CustomPacketPayload {
        public static final Type<Ack> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","painting_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Ack> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeUtf(p.message,256);},b->new Ack(b.readBlockPos(),b.readUtf(256)));public Type<Ack> type(){return TYPE;}
    }
    /** All raster and inventory changes are computed by the server, never uploaded as an image. */
    public record Action(BlockPos pos,UUID canvas,int revision,int kind,int first,int second,int mix,int size,int opacity,int tool,int sample,int resolution,String title,float[] points) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hobbymod","painting_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeUUID(p.canvas);for(int n:new int[]{p.revision,p.kind,p.first,p.second,p.mix,p.size,p.opacity,p.tool,p.sample,p.resolution})b.writeVarInt(n);b.writeUtf(p.title,48);b.writeVarInt(p.points.length);for(float v:p.points)b.writeFloat(v);},b->{var pos=b.readBlockPos();var id=b.readUUID();int[] n=new int[10];for(int i=0;i<10;i++)n[i]=b.readVarInt();String title=b.readUtf(48);int length=b.readVarInt();if(length<0 || length>64)throw new IllegalArgumentException("Painting stroke too large");float[] points=new float[length];for(int i=0;i<length;i++)points[i]=b.readFloat();return new Action(pos,id,n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],title,points);});public Type<Action> type(){return TYPE;}
    }
    public static String work(Player p,PaintingBlockEntity b,Action a){
        var d=b.painting;if(d==null || !d.id.equals(a.canvas) || d.revision!=a.revision)return "Canvas changed. Try again.";
        if(a.kind==3){PaintingBlock.give(p,CanvasItem.create(d));b.painting=null;if(!b.easel())p.level().removeBlock(b.getBlockPos(),false);else b.changed();return "Canvas returned to your inventory.";}
        if(!b.easel())return "Mount this canvas on an easel to paint.";
        if(a.kind==1)return PaintPalette.load(p,a.first)?"Dye added to your palette.":"Carry a palette and that dye; each dye adds 512 paint.";
        if(a.kind==4){if(d.signed)return "Choose Edit before signing this painting again.";d.title=PaintingNbt.clean(a.title,48);d.author=p.getName().getString();d.signed=true;d.revision++;b.changed();return "Painting signed. Take it to hang on a wall.";}
        if(a.kind==5){d.signed=false;d.revision++;b.changed();return "Canvas ready to edit.";}
        if(d.signed)return "Choose Edit before adding paint.";
        if(a.kind==2){if(!PaintingData.validResolution(a.resolution) || d.signed)return "Choose 16, 32, 64 or 128 pixels on the short edge.";if(!d.resize(a.resolution))d.revision++;d.configured=true;b.changed();return "Resolution: "+d.width()+" × "+d.height()+". Artwork preserved.";}
        if(a.kind!=0 || a.first<0 || a.first>15 || a.second<0 || a.second>15 || a.mix<0 || a.mix>100 || a.size<1 || a.size>16 || a.opacity<1 || a.opacity>100 || a.tool<0 || a.tool>4 || !d.validPoints(a.points))return "Invalid brush stroke.";
        if(!p.getInventory().contains(new net.minecraft.world.item.ItemStack(PaintingContent.BRUSH.get())) && !p.getOffhandItem().is(PaintingContent.BRUSH.get()))return "Carry a paintbrush to paint.";
        int color=PaintPalette.color(a.first,a.second,a.mix);if(a.sample>=0){if(a.sample>=d.width()*d.height() || !d.inside(a.sample%d.width(),a.sample/d.width()))return "Sample a point on the canvas.";color=d.pixel(a.sample%d.width(),a.sample/d.width());}
        var painted=d.copy();int changed=painted.paint(a.points,color,a.size,a.opacity,a.tool);if(changed==0)return "";
        if(!PaintPalette.pay(p,a.first,a.second,a.mix,Math.max(1,(changed+63)/64)))return "Load more dye into your palette.";
        b.painting=painted;b.changed();return "";
    }
    private static final Map<ServerPlayer,Long> LAST=new WeakHashMap<>();
    public static void register(){
        if(Platform.getEnvironment()==Env.SERVER){NetworkManager.registerS2CPayloadType(Open.TYPE,Open.CODEC);NetworkManager.registerS2CPayloadType(Ack.TYPE,Ack.CODEC);}
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,Action.TYPE,Action.CODEC,(a,c)->c.queue(()->{
            if(!(c.getPlayer() instanceof ServerPlayer p) || !p.level().hasChunkAt(a.pos) || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,a.pos) || !(p.level().getBlockEntity(a.pos) instanceof PaintingBlockEntity b))return;
            long now=p.level().getGameTime();String message;Long last=LAST.get(p);
            if(a.kind==0 && last!=null && now-last<2)message="";else{LAST.put(p,now);message=work(p,b,a);}
            p.inventoryMenu.broadcastChanges();if(!b.isRemoved())p.connection.send(b.getUpdatePacket());NetworkManager.sendToPlayer(p,new Ack(a.pos,message));
        }));
    }
    private PaintingNetworking(){}
}
