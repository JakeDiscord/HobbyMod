package io.github.jakediscord.hobbymod.painting.client;
import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.painting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;
import java.nio.file.*;

/** Inventory-style painting studio with bounded queued strokes and server acknowledgements. */
public final class PaintingScreen extends Screen {
    private final BlockPos pos;private int first=DyeColor.BLACK.getId(),second=DyeColor.WHITE.getId(),mix,size=2,opacity=100,tool,sample=-1;
    private boolean waiting,picking,drawing,panning,closing;private double zoom=1,panX,panY;private long sentAt,lastSend;private String notice="";
    private final ArrayList<Float> queued=new ArrayList<>();private PaintingData preview;private EditBox title;
    private Button resolutionButton,sizeButton,opacityButton,mixButton,pickButton,signButton;
    private static final int W=320,H=232;
    private int left(){return (width-W)/2;}private int top(){return Math.max(0,(height-H)/2);}
    public PaintingScreen(BlockPos pos){super(Component.literal("Painting studio"));this.pos=pos;}
    public static void register(){NetworkManager.registerReceiver(NetworkManager.Side.S2C,PaintingNetworking.Open.TYPE,PaintingNetworking.Open.CODEC,(p,c)->c.queue(()->Minecraft.getInstance().setScreen(new PaintingScreen(p.pos()))));NetworkManager.registerReceiver(NetworkManager.Side.S2C,PaintingNetworking.Ack.TYPE,PaintingNetworking.Ack.CODEC,(p,c)->c.queue(()->{if(Minecraft.getInstance().screen instanceof PaintingScreen s && s.pos.equals(p.pos())){s.waiting=false;s.preview=null;var b=s.block();if(b!=null && b.painting!=null)PaintingTextures.invalidate(b.painting.id);s.notice=p.message();}}));}
    private PaintingBlockEntity block(){return minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof PaintingBlockEntity b?b:null;}
    private PaintingData data(){var b=block();return preview!=null?preview:b==null?null:b.painting;}
    private boolean editable(){var b=block();return b!=null && b.easel() && b.painting!=null && !b.painting.signed;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float delta){}
    private Button button(String label,int x,int y,int w,Runnable action){var b=Button.builder(Component.literal(label),v->action.run()).bounds(left()+x,top()+y,w,18).build();addRenderableWidget(b);return b;}
    @Override protected void init(){clearWidgets();int x=left(),y=top();
        resolutionButton=button("Resolution",124,20,93,()->{var d=data();if(d!=null){int next=d.resolution==128?16:d.resolution*2;if(next<d.resolution && java.util.Arrays.stream(d.pixels()).anyMatch(c->c!=PaintingData.LINEN)){minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(ok->{minecraft.setScreen(this);if(ok){control(2,first,next);zoom=1;panX=panY=0;sample=-1;}},Component.literal("Lower painting resolution?"),Component.literal("Small details will be lost. Your canvas shape stays the same.")));}else{control(2,first,next);zoom=1;panX=panY=0;sample=-1;}}});
        button("PNG",240,20,70,this::export);
        String[] tools={"Brush","Box","Pen","Fill","Soft"};for(int i=0;i<5;i++){final int n=i;button(tools[i],240+(i%2)*36,43+(i/2)*20,34,()->{tool=n;picking=false;});}
        pickButton=button("Pick",276,83,34,()->picking=!picking);
        sizeButton=button("Size",240,110,70,()->size=size==16?1:size*2);
        opacityButton=button("Opacity",240,130,70,()->opacity=opacity==100?25:opacity+25);
        mixButton=button("Mix",240,150,70,()->{mix=mix==100?0:mix+25;sample=-1;});
        button("Take",240,209,70,()->control(3,first,0));
        title=new EditBox(font,x+165,y+184,66,16,Component.literal("Painting title"));title.setMaxLength(48);title.setHint(Component.literal("Title"));var d=data();title.setValue(d==null?"":d.title);addRenderableWidget(title);
        signButton=button("Sign",165,205,66,()->{var canvas=data();if(canvas!=null)control(canvas.signed?5:4,first,0);});
    }
    private void control(int kind,int color,int r){if(waiting)return;if(!queued.isEmpty()){flush();return;}send(kind,color,r,new float[0]);}
    private void send(int kind,int color,int r,float[] xy){var b=block();if(b==null || b.painting==null)return;var d=b.painting;waiting=true;sentAt=net.minecraft.Util.getMillis();lastSend=sentAt;
        NetworkManager.sendToServer(new PaintingNetworking.Action(pos,d.id,d.revision,kind,color,second,mix,size,opacity,tool,sample,r,title==null?"":title.getValue(),xy));
        if(kind==0){preview=d.copy();preview.paint(xy,currentColor(d),size,opacity,tool);PaintingTextures.invalidate(d.id);}
    }
    private int currentColor(PaintingData d){return sample>=0 && sample<d.width()*d.height()?d.pixel(sample%d.width(),sample/d.width()):PaintPalette.color(first,second,mix);}
    private void flush(){if(waiting || queued.isEmpty() || net.minecraft.Util.getMillis()-lastSend<110)return;float[] xy=new float[Math.min(64,queued.size())];for(int i=0;i<xy.length;i++)xy[i]=queued.get(i);queued.subList(0,xy.length).clear();if(drawing){queued.add(xy[xy.length-2]);queued.add(xy[xy.length-1]);}send(0,first,0,xy);}
    @Override public void tick(){var b=block();if(b==null || b.painting==null || minecraft.player==null || minecraft.player.distanceToSqr(pos.getCenter())>64){super.onClose();return;}if(waiting && net.minecraft.Util.getMillis()-sentAt>3000){waiting=false;preview=null;queued.clear();notice="No server response. Reopen the easel.";}if(!waiting && (!drawing || queued.size()>2))flush();if(closing && !waiting && queued.isEmpty())super.onClose();}
    private double fit(PaintingData d){return Math.min(214.0/d.width(),132.0/d.height());}
    private int drawW(PaintingData d){return (int)Math.round(d.width()*fit(d)*zoom);}private int drawH(PaintingData d){return (int)Math.round(d.height()*fit(d)*zoom);}
    private int drawX(PaintingData d){return left()+8+(216-drawW(d))/2+(int)panX;}private int drawY(PaintingData d){return top()+42+(134-drawH(d))/2+(int)panY;}
    private boolean over(double x,double y){return x>=left()+8 && x<left()+224 && y>=top()+42 && y<top()+176;}
    private float[] point(double x,double y){var d=data();if(d==null)return null;double px=(x-drawX(d))*d.width()/drawW(d),py=(y-drawY(d))*d.height()/drawH(d);if(px<0 || py<0 || px>=d.width() || py>=d.height())return null;return new float[]{(float)Math.clamp(Math.floor(px),0,d.width()-1),(float)Math.clamp(Math.floor(py),0,d.height()-1)};}
    private void queue(float[] point){if(queued.size()>=64)queued.subList(2,4).clear();queued.add(point[0]);queued.add(point[1]);}
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;var d=data();if(d==null)return false;
        int col=((int)x-left()-8)/18,row=((int)y-top()-184)/18;
        if(x>=left()+8 && x<left()+152 && y>=top()+184 && y<top()+220 && col>=0 && col<8 && row>=0 && row<2){if(waiting || !queued.isEmpty())return true;int color=col+row*8;sample=-1;if(hasShiftDown())second=color;else first=color;var a=PaintPalette.amounts(PaintPalette.find(minecraft.player));if(button==1 || a[color]==0)control(1,color,0);return true;}
        if(over(x,y)){if(button==2){panning=true;return true;}var p=point(x,y);if(p==null)return true;if(picking){sample=(int)p[1]*d.width()+(int)p[0];picking=false;return true;}if(button==0 && editable()){drawing=tool!=3;queue(p);if(tool==3)flush();return true;}}
        return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(panning && button==2){panX+=dx;panY+=dy;return true;}if(drawing && button==0 && over(x,y)){var p=point(x,y);if(p!=null)queue(p);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0 && drawing){drawing=false;flush();return true;}if(button==2)panning=false;return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(over(x,y)){var d=data();double old=zoom;zoom=Math.clamp(zoom*(v>0?1.5:1/1.5),1,8);if(zoom==1){panX=panY=0;}else if(d!=null){panX=(panX-(x-left()-116))*(zoom/old)+(x-left()-116);panY=(panY-(y-top()-109))*(zoom/old)+(y-top()-109);}return true;}return super.mouseScrolled(x,y,h,v);}
    private void export(){var d=data();if(d==null || waiting || !queued.isEmpty()){notice="Finish your stroke before exporting.";return;}try{Path dir=minecraft.gameDirectory.toPath().resolve("hobbymod-paintings");Files.createDirectories(dir);try(var image=PaintingTextures.image(d)){image.writeToFile(dir.resolve(d.id+".png"));}notice="PNG saved in hobbymod-paintings.";}catch(Exception e){notice="Could not save PNG: "+e.getClass().getSimpleName();}}
    private void text(GuiGraphics g,String s,int x,int y,int max){g.drawString(font,font.plainSubstrByWidth(s,max),x,y,0x404040,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){int x=left(),y=top();g.fill(0,0,width,height,0x99000000);g.fill(x,y,x+W,y+H,0xff373737);g.fill(x+1,y+1,x+W-1,y+H-1,0xfff7f7f7);g.fill(x+3,y+3,x+W-3,y+H-3,0xffc6c6c6);var d=data();if(d==null)return;
        text(g,d.title.isBlank()?d.shape.label+" Canvas":d.title,x+8,y+7,220);text(g,d.width()+" × "+d.height()+" · "+String.format(Locale.ROOT,"%.1f×",zoom),x+8,y+25,114);
        g.fill(x+7,y+41,x+225,y+177,0xff373737);g.fill(x+8,y+42,x+224,y+176,0xff858585);g.enableScissor(x+8,y+42,x+224,y+176);g.blit(PaintingTextures.texture(d),drawX(d),drawY(d),drawW(d),drawH(d),0,0,d.width(),d.height(),d.width(),d.height());g.disableScissor();
        if(over(mx,my) && editable() && !picking){var p=point(mx,my);if(p!=null){int diameter=tool==2?1:size;int bw=Math.max(1,(int)(diameter*fit(d)*zoom));int cx=drawX(d)+(int)(p[0]*fit(d)*zoom),cy=drawY(d)+(int)(p[1]*fit(d)*zoom);g.enableScissor(x+8,y+42,x+224,y+176);g.renderOutline(cx-bw/2,cy-bw/2,bw+1,bw+1,0xff000000|currentColor(d));g.disableScissor();}}
        var a=PaintPalette.amounts(PaintPalette.find(minecraft.player));for(int i=0;i<16;i++){int px=x+8+(i%8)*18,py=y+184+(i/8)*18;g.fill(px,py,px+17,py+17,i==first?0xffffffff:i==second?0xffffd477:0xff373737);g.fill(px+1,py+1,px+16,py+16,0xff8b8b8b);g.renderItem(new ItemStack(DyeItem.byColor(DyeColor.byId(i))),px+1,py);if(a[i]==0)g.fill(px+1,py+1,px+16,py+16,0x77505050);else g.fill(px+1,py+15,px+1+Math.max(1,a[i]*15/PaintPalette.MAX),py+16,0xff000000|DyeColor.byId(i).getTextureDiffuseColor());}
        for(var child:children())if(child instanceof Button button)button.active=!waiting && queued.isEmpty();
        String[] toolNames={"Brush","Box","Pen","Fill","Soft"};for(var child:children())if(child instanceof Button button)for(int i=0;i<toolNames.length;i++)if(button.getMessage().getString().equals(toolNames[i]))button.setMessage(Component.literal(toolNames[i]).withStyle(!picking && i==tool?net.minecraft.ChatFormatting.YELLOW:net.minecraft.ChatFormatting.WHITE));
        resolutionButton.setMessage(Component.literal("Pixels: "+d.resolution));sizeButton.setMessage(Component.literal("Size: "+size));opacityButton.setMessage(Component.literal(opacity+"% paint"));mixButton.setMessage(Component.literal("Mix: "+mix+"%"));pickButton.setMessage(Component.literal(picking?"Pick...":"Pick"));signButton.setMessage(Component.literal(d.signed?"Edit":"Sign"));resolutionButton.active=editable() && !waiting;signButton.active=block()!=null && block().easel() && !waiting;
        g.fill(x+240,y+174,x+310,y+202,0xff373737);g.fill(x+241,y+175,x+309,y+201,0xff000000|currentColor(d));
        if(!editable())for(var child:children())if(child instanceof Button button && !button.getMessage().getString().equals("Take") && !button.getMessage().getString().equals("PNG") && !button.getMessage().getString().equals("Edit"))button.active=false;title.setEditable(editable());
        String status=!notice.isBlank()?notice:d.signed?"By "+d.author+" · Signed":PaintPalette.find(minecraft.player).isEmpty()?"Carry a palette and paintbrush; click a dye to load.":"Shift: mix color · Wheel: zoom · Middle drag: pan";text(g,status,x+8,y+222,302);
        super.render(g,mx,my,delta);
        if(mx>=x+8 && mx<x+152 && my>=y+184 && my<y+220){int i=(mx-x-8)/18+((my-y-184)/18)*8;g.renderComponentTooltip(font,List.of(Component.translatable("color.minecraft."+DyeColor.byId(i).getName()),Component.literal(a[i]+" paint · one dye adds 512"),Component.literal("Left: choose · Shift: mix color"),Component.literal("Right: load one dye from inventory")),mx,my);}
    }
    @Override public void onClose(){if(waiting || !queued.isEmpty()){drawing=false;closing=true;notice="Finishing your stroke...";flush();return;}super.onClose();}
}
