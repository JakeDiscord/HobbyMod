package io.github.jakediscord.hobbymod.astronomy.client;

import io.github.jakediscord.hobbymod.astronomy.*;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import java.util.*;
public final class FieldJournalScreen extends Screen {
    private int hovered=-1,mouseX,mouseY;
    private int constellationIndex,tab,page,selected=-1,left,top,w,h;
    private final List<Integer> visible=new ArrayList<>();
    public FieldJournalScreen(){super(Component.literal("Astronomy field journal"));}
    private void button(String s,int x,int y,int width,Runnable r){addRenderableWidget(Button.builder(Component.literal(s),b->r.run()).bounds(x,y,width,20).build());}
    @Override protected void init(){w=Math.min(420,width-24);h=Math.min(270,height-24);left=(width-w)/2;top=(height-h)/2;button("Catalog",left+10,top+32,82,()->{tab=0;page=0;});button("Constellations",left+96,top+32,100,()->{tab=1;page=0;});button("Sky map",left+200,top+32,78,()->tab=2);button("Observe",left+w-86,top+32,76,()->NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.NAKED,BlockPos.ZERO,0,0,0,0)));button("<",left+10,top+h-28,24,()->page=Math.max(0,page-1));button(">",left+38,top+h-28,24,()->page=Math.min(48/Math.max(1,(h-108)/18),page+1));button("Close",left+w-70,top+h-28,60,this::onClose);}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        mouseX=mx;mouseY=my;g.fill(0,0,width,height,0xB0070B12);g.fill(left,top,left+w,top+h,0xFF101C29);g.fill(left,top,left+w,top+3,0xFF65B9D1);g.drawString(font,"FIELD JOURNAL / ASTRONOMY",left+10,top+13,0xD5E9F2,false);super.render(g,mx,my,partial);
        if(AstronomyClient.catalog==null)return;visible.clear();
        if(tab==2){chart(g,left+12,top+64,w-24,h-102,partial);return;}
        if(tab==1){int y=top+66;for(var c:AstronomyClient.catalog.constellations){long found=Arrays.stream(c.stars()).filter(id->AstronomyClient.journal.get(id)!=null).count();if(c==AstronomyClient.catalog.constellations.get(constellationIndex))g.fill(left+8,y-3,left+w/2,y+27,0xFF244150);g.drawString(font,c.name(),left+14,y,0xE2EDF2,false);g.drawString(font,found+" / "+c.stars().length+" reference stars",left+14,y+13,0x8EA7B8,false);y+=34;}constellation(g,left+w-136,top+72,120,112);return;}
        int rows=Math.max(1,(h-108)/18),start=page*rows;var targets=AstronomyClient.catalog.targets;
        for(int i=start;i<Math.min(start+rows,targets.size());i++){var t=targets.get(i);var e=AstronomyClient.journal.get(i);int y=top+64+(i-start)*18;visible.add(i);if(i==selected)g.fill(left+8,y-2,left+w/2-5,y+14,0xFF244150);String name=e==null?(t.deep()?"Uncharted "+t.kind().name().toLowerCase(Locale.ROOT):t.name()):t.name();g.drawString(font,font.plainSubstrByWidth(name,w/2-68),left+12,y,e==null?0x869CAC:0xD5E9F2,false);g.drawString(font,e==null?"—":e.completeness+"%",left+w/2-49,y,0x77C7C7,false);}
        var target=AstronomyClient.catalog.target(selected);if(target!=null){int x=left+w/2+9,y=top+66;var e=AstronomyClient.journal.get(selected);g.drawString(font,font.plainSubstrByWidth(e==null && target.deep()?"Uncharted target":target.name(),w/2-75),x,y,0xDAEAF2,false);g.drawString(font,target.kind().name()+String.format(Locale.ROOT," / MAG %.1f",target.magnitude()),x,y+17,0x77C7C7,false);int yy=y+37;for(var line:font.split(Component.literal(target.description()),w/2-24)){g.drawString(font,line,x,yy,0x9EB5C4,false);yy+=11;}if(e!=null){g.drawString(font,e.observations+" observations",x,yy+10,0xDAEAF2,false);g.drawString(font,String.format(Locale.ROOT,"Best quality %.0f%%",e.best*100),x,yy+23,0xDAEAF2,false);}sketch(g,left+w-35,top+77,target,e!=null);var direction=target.direction(AstronomyClient.time(partial));g.drawString(font,String.format(Locale.ROOT,"AZ %.1f°  ALT %+.1f°",(Math.toDegrees(Math.atan2(-direction.x(),direction.z()))+540)%360,Math.toDegrees(Math.asin(direction.y()))),x,top+h-43,0x7EACBD,false);}
        g.drawString(font,(page+1)+" / "+((targets.size()+rows-1)/rows),left+72,top+h-22,0x8EA7B8,false);
    }
    private void sketch(GuiGraphics g,int x,int y,SkyCatalog.Object t,boolean found){if(!found)return;int c=0xFF000000|t.color();if(t.kind()==SkyCatalog.Kind.PLANET){for(int dy=-13;dy<=13;dy++){int dx=(int)Math.sqrt(169-dy*dy);g.fill(x-dx,y+dy,x+dx+1,y+dy+1,c);}if(t.name().equals("Saturn")){g.fill(x-24,y-2,x+25,y,0xFFCDBD88);}}else{var r=new Random(AstronomyClient.seed+t.id());for(int i=0;i<26;i++){int xx=x+r.nextInt(62)-31,yy=y+r.nextInt(36)-18;g.fill(xx,yy,xx+1,yy+1,c);}}}
    private void constellation(GuiGraphics g,int x,int y,int w,int h){var c=AstronomyClient.catalog.constellations.get(constellationIndex);var points=new java.util.HashMap<Integer,int[]>();double lo=Double.POSITIVE_INFINITY,hi=-lo,down=lo,up=-lo;for(int id:c.stars()){var a=AstronomyClient.catalog.target(id);lo=Math.min(lo,a.ra());hi=Math.max(hi,a.ra());down=Math.min(down,a.dec());up=Math.max(up,a.dec());}for(int id:c.stars()){var a=AstronomyClient.catalog.target(id);points.put(id,new int[]{x+10+(int)((a.ra()-lo)/(hi-lo)*(w-20)),y+10+(int)((up-a.dec())/(up-down)*(h-20))});}for(var edge:c.edges()){var a=points.get(edge[0]);var b=points.get(edge[1]);line(g,a[0],a[1],b[0],b[1],0xFF446C84);}for(int id:c.stars()){var p=points.get(id);g.fill(p[0]-1,p[1]-1,p[0]+2,p[1]+2,0xFFD6E7FF);}}
    private void chart(GuiGraphics g,int x,int y,int w,int h,float partial){
        double time=AstronomyClient.time(partial);g.fill(x,y,x+w,y+h,0xFF08121F);hovered=-1;var points=new HashMap<Integer,int[]>();
        for(var t:AstronomyClient.catalog.targets){var d=t.direction(time);if(d.y()<=0)continue;double az=(Math.atan2(-d.x(),d.z())+Math.PI+Math.PI*2)%(Math.PI*2);int xx=x+(int)(az/(Math.PI*2)*w),yy=y+h-18-(int)(d.y()*(h-29));points.put(t.id(),new int[]{xx,yy});if(Math.hypot(mouseX-xx,mouseY-yy)<6)hovered=t.id();}
        for(var c:AstronomyClient.catalog.constellations)for(var edge:c.edges()){var a=points.get(edge[0]);var b=points.get(edge[1]);if(a!=null && b!=null && Math.abs(a[0]-b[0])<w/2)line(g,a[0],a[1],b[0],b[1],0xFF294153);}
        for(var t:AstronomyClient.catalog.targets){var p=points.get(t.id());if(p==null)continue;int size=hovered==t.id()?2:1;g.fill(p[0]-size,p[1]-size,p[0]+size+1,p[1]+size+1,0xFF000000|t.color());}
        String[] cardinal={"N","E","S","W","N"};for(int i=0;i<5;i++)g.drawString(font,cardinal[i],x+Math.min(w-7,i*w/4),y+h-11,0x65859B,false);
        if(hovered>=0){var t=AstronomyClient.catalog.target(hovered);g.renderTooltip(font,Component.literal(t.deep() && AstronomyClient.journal.get(t.id())==null?"Uncharted "+t.kind().name().toLowerCase(Locale.ROOT):t.name()),mouseX,mouseY);}
    }
    public static void line(GuiGraphics g,int x,int y,int X,int Y,int color){int steps=Math.max(Math.abs(X-x),Math.abs(Y-y));for(int i=0;i<=steps;i++){int xx=x+(X-x)*i/Math.max(1,steps),yy=y+(Y-y)*i/Math.max(1,steps);g.fill(xx,yy,xx+1,yy+1,color);}}
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;if(tab==2 && hovered>=0){selected=hovered;page=selected/Math.max(1,(h-108)/18);tab=0;return true;}if(tab==1 && x>=left+8 && x<left+w/2 && y>=top+66 && y<top+202){constellationIndex=Math.clamp((int)(y-top-66)/34,0,3);return true;}if(tab==0 && y>=top+64 && x>=left+8 && x<left+w/2){int index=(int)(y-top-64)/18;if(index>=0 && index<visible.size()){selected=visible.get(index);return true;}}return false;}
}
