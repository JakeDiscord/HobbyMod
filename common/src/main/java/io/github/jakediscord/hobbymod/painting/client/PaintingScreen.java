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

/** Two movable work panels; paint is previewed immediately and committed by the server. */
public final class PaintingScreen extends Screen {
    private final BlockPos pos;
    private int first=DyeColor.BLACK.getId(),second=DyeColor.WHITE.getId(),mix,size=2,opacity=100,tool,sample=-1;
    private boolean waiting,picking,drawing,panning,closing,paletteVisible=true;
    private double zoom=1,panX,panY,dragOffsetX,dragOffsetY;
    private long sentAt,lastSend;
    private String notice="";
    private final ArrayList<Float> queued=new ArrayList<>();
    private float[] inFlight=new float[0];
    private PaintingData preview,hover;
    private final UUID hoverId=UUID.randomUUID();
    private String hoverKey="";
    private EditBox title;
    private Button resolutionButton,sizeButton,opacityButton,mixButton,pickButton,signButton;
    private final List<Button> toolButtons=new ArrayList<>();
    private final Map<AbstractWidget,int[]> positions=new LinkedHashMap<>();
    private static final int CW=196,CH=158,PW=146,PH=218;
    private int canvasX=-1,canvasY,paletteX,paletteY,dragPanel;
    private boolean choosing;

    public PaintingScreen(BlockPos pos){super(Component.literal("Painting studio"));this.pos=pos;}
    public static void register(){
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,PaintingNetworking.Open.TYPE,PaintingNetworking.Open.CODEC,(p,c)->c.queue(()->Minecraft.getInstance().setScreen(new PaintingScreen(p.pos()))));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,PaintingNetworking.Ack.TYPE,PaintingNetworking.Ack.CODEC,(p,c)->c.queue(()->{
            if(Minecraft.getInstance().screen instanceof PaintingScreen s && s.pos.equals(p.pos())){
                s.waiting=false;s.inFlight=new float[0];s.notice=p.message();s.rebuildPreview();
                var d=s.data();if(s.choosing && d!=null && d.configured){s.choosing=false;s.init();}
            }
        }));
    }
    private PaintingBlockEntity block(){return minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof PaintingBlockEntity b?b:null;}
    private PaintingData base(){var b=block();return b==null?null:b.painting;}
    private PaintingData data(){return preview!=null?preview:base();}
    private boolean editable(){var b=block();return b!=null && b.easel() && b.painting!=null && !b.painting.signed;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float delta){}
    private Button button(String label,int panel,int x,int y,int w,Runnable action){
        var b=Button.builder(Component.literal(label),v->action.run()).bounds(0,0,w,18).build();
        positions.put(b,new int[]{panel,x,y});addRenderableWidget(b);return b;
    }
    private void positionWidgets(){positions.forEach((w,p)->{w.setX((p[0]==1?canvasX:paletteX)+p[1]);w.setY((p[0]==1?canvasY:paletteY)+p[2]);w.visible=p[0]==1 || paletteVisible;});}
    @Override protected void init(){
        String oldTitle=title==null?null:title.getValue();clearWidgets();positions.clear();toolButtons.clear();
        if(canvasX<0){canvasX=12;canvasY=Math.max(6,(height-CH)/2);paletteX=width-PW-10;paletteY=Math.max(4,(height-PH)/2);}
        canvasX=Math.clamp(canvasX,0,Math.max(0,width-CW));canvasY=Math.clamp(canvasY,0,Math.max(0,height-CH));
        paletteX=Math.clamp(paletteX,0,Math.max(0,width-PW));paletteY=Math.clamp(paletteY,0,Math.max(0,height-PH));
        var d=base();choosing=d!=null && !d.configured && editable();
        if(choosing){
            for(int i=0;i<4;i++){int r=PaintingData.RESOLUTIONS[i];button(Integer.toString(r),1,8+i*46,48,42,()->control(2,first,r));}
            positionWidgets();return;
        }
        resolutionButton=button("Pixels",1,8,CH-22,70,this::changeResolution);
        button("Palette",1,82,CH-22,62,()->{paletteVisible=!paletteVisible;positionWidgets();});
        button("PNG",1,148,CH-22,40,this::export);
        String[] tools={"Brush","Box","Pen","Fill","Soft","Pick"};
        for(int i=0;i<6;i++){final int n=i;var b=button(tools[i],2,8+(i%3)*44,19+(i/3)*20,42,()->{if(n==5)picking=!picking;else{tool=n;picking=false;}hoverKey="";});if(i==5)pickButton=b;else toolButtons.add(b);}
        sizeButton=button("Size",2,8,62,62,()->{size=size==16?1:size*2;hoverKey="";});
        opacityButton=button("Opacity",2,74,62,64,()->{opacity=opacity==100?25:opacity+25;hoverKey="";});
        mixButton=button("Mix",2,8,140,80,()->{mix=mix==100?0:mix+25;sample=-1;hoverKey="";});
        title=new EditBox(font,0,0,130,16,Component.literal("Painting title"));title.setMaxLength(48);title.setHint(Component.literal("Title"));title.setValue(oldTitle!=null?oldTitle:d==null?"":d.title);positions.put(title,new int[]{2,8,164});addRenderableWidget(title);
        signButton=button("Sign",2,8,186,62,()->{var canvas=data();if(canvas!=null)control(canvas.signed?5:4,first,0);});
        button("Take",2,74,186,64,()->control(3,first,0));positionWidgets();
    }
    private void changeResolution(){
        var d=data();if(d==null)return;int next=d.resolution==128?16:d.resolution*2;
        if(next<d.resolution && Arrays.stream(d.pixels()).anyMatch(c->c!=PaintingData.LINEN)){
            minecraft.setScreen(new Screen(Component.literal("Lower resolution?")){
                @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Keep detail"),b->minecraft.setScreen(PaintingScreen.this)).bounds(width/2-98,height/2+12,96,20).build());addRenderableWidget(Button.builder(Component.literal("Use "+next),b->{minecraft.setScreen(PaintingScreen.this);control(2,first,next);resetView();}).bounds(width/2+2,height/2+12,96,20).build());}
                @Override public void renderBackground(GuiGraphics g,int x,int y,float delta){}
                @Override public boolean isPauseScreen(){return false;}
                @Override public void render(GuiGraphics g,int x,int y,float delta){panel(g,width/2-108,height/2-38,216,80);g.drawCenteredString(font,"Lower painting resolution?",width/2,height/2-27,0xffeeeeee);g.drawCenteredString(font,"Small details will be lost.",width/2,height/2-10,0xffb8b8b8);super.render(g,x,y,delta);}
                @Override public void onClose(){minecraft.setScreen(PaintingScreen.this);}
            });
        }else{control(2,first,next);resetView();}
    }
    private void resetView(){zoom=1;panX=panY=0;sample=-1;hoverKey="";}
    private void control(int kind,int color,int r){if(waiting)return;if(!queued.isEmpty()){flush();return;}send(kind,color,r,new float[0]);}
    private void send(int kind,int color,int r,float[] xy){
        var d=base();if(d==null)return;waiting=true;sentAt=net.minecraft.Util.getMillis();lastSend=sentAt;
        NetworkManager.sendToServer(new PaintingNetworking.Action(pos,d.id,d.revision,kind,color,second,mix,size,opacity,tool,sample,r,title==null?d.title:title.getValue(),xy));
        inFlight=kind==0?xy:new float[0];rebuildPreview();
    }
    private int currentColor(PaintingData d){return sample>=0 && sample<d.width()*d.height()?d.pixel(sample%d.width(),sample/d.width()):PaintPalette.color(first,second,mix);}
    private float[] queuedPoints(){float[] p=new float[queued.size()];for(int i=0;i<p.length;i++)p[i]=queued.get(i);return p;}
    private void rebuildPreview(){
        var d=base();preview=null;hoverKey="";if(d==null)return;
        if(inFlight.length>0 || !queued.isEmpty()){
            preview=d.copy();int color=currentColor(d);
            if(inFlight.length>0)preview.paint(inFlight,color,size,opacity,tool);
            if(!queued.isEmpty() && !(drawing && queued.size()==2))preview.paint(queuedPoints(),color,size,opacity,tool);
        }
        PaintingTextures.invalidate(d.id);
    }
    private void flush(){
        if(waiting || queued.isEmpty() || net.minecraft.Util.getMillis()-lastSend<110)return;
        float[] xy=queuedPoints();queued.clear();
        if(drawing){queued.add(xy[xy.length-2]);queued.add(xy[xy.length-1]);}send(0,first,0,xy);
    }
    @Override public void tick(){
        var b=block();if(b==null || b.painting==null || minecraft.player==null || minecraft.player.distanceToSqr(pos.getCenter())>64){super.onClose();return;}
        if(waiting && net.minecraft.Util.getMillis()-sentAt>3000){waiting=false;inFlight=new float[0];queued.clear();rebuildPreview();notice="No server response. Reopen the easel.";}
        if(!waiting && (!drawing || queued.size()>2))flush();if(closing && !waiting && queued.isEmpty())super.onClose();
    }
    private double fit(PaintingData d){return Math.min(180.0/d.width(),108.0/d.height());}
    private int drawW(PaintingData d){return (int)Math.round(d.width()*fit(d)*zoom);}
    private int drawH(PaintingData d){return (int)Math.round(d.height()*fit(d)*zoom);}
    private int drawX(PaintingData d){return canvasX+8+(180-drawW(d))/2+(int)Math.round(panX);}
    private int drawY(PaintingData d){return canvasY+22+(108-drawH(d))/2+(int)Math.round(panY);}
    private boolean over(double x,double y){return x>=canvasX+8 && x<canvasX+188 && y>=canvasY+22 && y<canvasY+130;}
    private boolean overPalette(double x,double y){return paletteVisible && x>=paletteX && x<paletteX+PW && y>=paletteY && y<paletteY+PH;}
    private float[] point(double x,double y){
        var d=data();if(d==null)return null;
        double px=(x-drawX(d))*d.width()/drawW(d)-.5,py=(y-drawY(d))*d.height()/drawH(d)-.5;
        if(px<-.5 || py<-.5 || px>=d.width()-.5 || py>=d.height()-.5)return null;
        return new float[]{(float)Math.clamp(px,0,d.width()-1),(float)Math.clamp(py,0,d.height()-1)};
    }
    private void queue(float[] p){if(queued.size()>=64)queued.subList(2,4).clear();queued.add(p[0]);queued.add(p[1]);rebuildPreview();}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0){
            if(overPalette(x,y) && y<paletteY+16){dragPanel=2;dragOffsetX=x-paletteX;dragOffsetY=y-paletteY;return true;}
            if(x>=canvasX && x<canvasX+CW && y>=canvasY && y<canvasY+16){dragPanel=1;dragOffsetX=x-canvasX;dragOffsetY=y-canvasY;return true;}
        }
        if(super.mouseClicked(x,y,button))return true;if(choosing)return false;var d=data();if(d==null)return false;
        if(overPalette(x,y)){
            if(x>=paletteX+8 && x<paletteX+136 && y>=paletteY+86 && y<paletteY+122){
                if(waiting || !queued.isEmpty() || !editable())return true;
                int color=((int)x-paletteX-8)/16+((int)y-paletteY-86)/18*8;
                sample=-1;if(hasShiftDown())second=color;else first=color;
                var a=PaintPalette.amounts(PaintPalette.find(minecraft.player));hoverKey="";
                if(button==1 || a[color]==0)control(1,color,0);return true;
            }
            return true;
        }
        if(over(x,y)){
            if(button==2){panning=true;return true;}var p=point(x,y);if(p==null)return true;
            if(picking){sample=Math.round(p[1])*d.width()+Math.round(p[0]);picking=false;hoverKey="";return true;}
            if(button==0 && editable()){drawing=tool!=3;queue(p);flush();return true;}
        }return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(dragPanel!=0 && button==0){
            if(dragPanel==1){canvasX=Math.clamp((int)Math.round(x-dragOffsetX),0,Math.max(0,width-CW));canvasY=Math.clamp((int)Math.round(y-dragOffsetY),0,Math.max(0,height-CH));}
            else{paletteX=Math.clamp((int)Math.round(x-dragOffsetX),0,Math.max(0,width-PW));paletteY=Math.clamp((int)Math.round(y-dragOffsetY),0,Math.max(0,height-PH));}
            positionWidgets();return true;
        }
        if(panning && button==2){panX+=dx;panY+=dy;return true;}
        if(drawing && button==0){var p=point(x,y);if(p!=null && over(x,y))queue(p);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0){dragPanel=0;if(drawing){if(queued.size()==2)queued.clear();drawing=false;rebuildPreview();flush();return true;}}if(button==2)panning=false;return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){
        if(!choosing && over(x,y) && !overPalette(x,y)){double old=zoom;zoom=Math.clamp(zoom*(v>0?1.25:1/1.25),1,8);if(zoom==1)panX=panY=0;else{panX=(panX-(x-canvasX-98))*(zoom/old)+(x-canvasX-98);panY=(panY-(y-canvasY-76))*(zoom/old)+(y-canvasY-76);}return true;}
        return super.mouseScrolled(x,y,h,v);
    }
    private void export(){var d=data();if(d==null || waiting || !queued.isEmpty()){notice="Finish your stroke before exporting.";return;}try{Path dir=minecraft.gameDirectory.toPath().resolve("hobbymod-paintings");Files.createDirectories(dir);try(var image=PaintingTextures.image(d)){image.writeToFile(dir.resolve(d.id+".png"));}notice="PNG saved.";}catch(Exception e){notice="Could not save PNG.";}}
    private static void panel(GuiGraphics g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,0xff171a1c);g.fill(x+1,y+1,x+w-1,y+h-1,0xff383c3e);g.fill(x+1,y+1,x+w-1,y+16,0xff24282a);}
    private void text(GuiGraphics g,String s,int x,int y,int max,int color){g.drawString(font,font.plainSubstrByWidth(s,max),x,y,color,false);}
    private PaintingData hoverPreview(PaintingData d,float[] p){
        // Same continuous coordinates, mask, size, opacity and algorithm as the server.
        String key=d.revision+":"+Arrays.toString(p)+":"+size+":"+opacity+":"+tool+":"+currentColor(d);
        if(!key.equals(hoverKey)){hover=d.copy();hover.id=hoverId;hover.paint(p,currentColor(d),size,opacity,tool);PaintingTextures.invalidate(hoverId);hoverKey=key;}
        return hover;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        if(!waiting && (!drawing || queued.size()>2))flush();var d=data();if(d==null)return;
        if(choosing){panel(g,canvasX,canvasY,CW,84);text(g,"Canvas resolution",canvasX+8,canvasY+5,180,0xffeeeeee);text(g,d.shape.label+" · pixels on the short edge",canvasX+8,canvasY+23,180,0xffbbbbbb);text(g,"Change it later in Pixels.",canvasX+8,canvasY+70,180,0xffbbbbbb);super.render(g,mx,my,delta);return;}
        panel(g,canvasX,canvasY,CW,CH);
        text(g,d.title.isBlank()?d.shape.label+" Canvas":d.title,canvasX+8,canvasY+5,130,0xffeeeeee);
        text(g,d.width()+"×"+d.height(),canvasX+CW-52,canvasY+5,48,0xffbbbbbb);
        var p=over(mx,my) && !overPalette(mx,my)?point(mx,my):null;
        boolean brushHover=p!=null && editable() && !picking && !drawing && !waiting && queued.isEmpty();
        PaintingData shown=brushHover?hoverPreview(d,p):d;
        g.enableScissor(canvasX+8,canvasY+22,canvasX+188,canvasY+130);
        g.blit(PaintingTextures.texture(shown),drawX(d),drawY(d),drawW(d),drawH(d),0,0,d.width(),d.height(),d.width(),d.height());
        if(p!=null){
            // A crosshair stays exactly under the pointer, while the artwork previews the actual stamp.
            g.fill(mx-3,my,mx+4,my+1,0xff111111);g.fill(mx,my-3,mx+1,my+4,0xff111111);g.fill(mx,my,mx+1,my+1,0xffffffff);
        }g.disableScissor();
        for(var e:positions.entrySet())if(e.getKey() instanceof Button b)b.active=!waiting && queued.isEmpty();
        resolutionButton.setMessage(Component.literal("Pixels: "+d.resolution));resolutionButton.active=editable() && !waiting && queued.isEmpty();
        // Panel toggling and exports remain available on a signed or displayed canvas.
        for(var e:positions.entrySet())if(e.getValue()[0]==1 && e.getValue()[1]==82)e.getKey().active=true;
        if(paletteVisible){
            panel(g,paletteX,paletteY,PW,PH);text(g,"Palette & tools",paletteX+8,paletteY+5,130,0xffeeeeee);
            var a=PaintPalette.amounts(PaintPalette.find(minecraft.player));
            for(int i=0;i<16;i++){
                int x=paletteX+8+(i%8)*16,y=paletteY+86+(i/8)*18;
                g.fill(x,y,x+16,y+17,i==first?0xffeeeeee:i==second?0xffe9ba69:0xff151719);
                g.fill(x+1,y+1,x+15,y+15,0xff000000|DyeColor.byId(i).getTextureDiffuseColor());
                if(a[i]==0)g.fill(x+1,y+1,x+15,y+15,0x99505050);else g.fill(x+1,y+15,x+1+Math.max(1,a[i]*14/PaintPalette.MAX),y+16,0xffdddddd);
            }
            text(g,"Shift: mix · Right: add dye",paletteX+8,paletteY+125,130,0xffbbbbbb);
            g.fill(paletteX+94,paletteY+140,paletteX+138,paletteY+158,0xff000000|currentColor(d));
            sizeButton.setMessage(Component.literal("Size: "+size));opacityButton.setMessage(Component.literal(opacity+"%"));mixButton.setMessage(Component.literal("Mix: "+mix+"%"));pickButton.setMessage(Component.literal(picking?"Pick…":"Pick"));
            for(int i=0;i<toolButtons.size();i++)toolButtons.get(i).setMessage(Component.literal(new String[]{"Brush","Box","Pen","Fill","Soft"}[i]).withStyle(!picking && tool==i?net.minecraft.ChatFormatting.YELLOW:net.minecraft.ChatFormatting.WHITE));
            signButton.setMessage(Component.literal(d.signed?"Edit":"Sign"));signButton.active=block()!=null && block().easel() && !waiting && queued.isEmpty();title.setEditable(editable());
            text(g,notice.isBlank()?(d.signed?"By "+d.author:"Wheel: zoom · Middle: pan"):notice,paletteX+8,paletteY+207,130,0xffbbbbbb);
        }
        if(!editable())for(var e:positions.entrySet())if(e.getKey() instanceof Button b && e.getValue()[0]==2 && b!=signButton && !b.getMessage().getString().equals("Take"))b.active=false;
        super.render(g,mx,my,delta);
        if(paletteVisible && mx>=paletteX+8 && mx<paletteX+136 && my>=paletteY+86 && my<paletteY+122){int i=(mx-paletteX-8)/16+(my-paletteY-86)/18*8;var a=PaintPalette.amounts(PaintPalette.find(minecraft.player));g.renderComponentTooltip(font,List.of(Component.translatable("color.minecraft."+DyeColor.byId(i).getName()),Component.literal(a[i]+" paint · dye adds 512")),mx,my);}
    }
    @Override public void onClose(){if(waiting || !queued.isEmpty()){if(drawing && queued.size()==2)queued.clear();drawing=false;closing=true;notice="Finishing stroke…";flush();return;}super.onClose();}
}
