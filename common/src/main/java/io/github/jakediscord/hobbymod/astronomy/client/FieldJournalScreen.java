package io.github.jakediscord.hobbymod.astronomy.client;

import io.github.jakediscord.hobbymod.astronomy.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class FieldJournalScreen extends Screen {
    private int constellationIndex,tab,page,selected=-1,left,top,w,h;
    private Button previous,next;
    private final List<Integer> visible=new ArrayList<>();
    public FieldJournalScreen(){super(Component.literal("Astronomy field journal"));}
    private Button button(String s,int x,int y,int width,Runnable r){return addRenderableWidget(Button.builder(Component.literal(s),b->r.run()).bounds(x,y,width,20).build());}
    @Override protected void init(){w=Math.min(420,width-24);h=Math.min(270,height-24);left=(width-w)/2;top=(height-h)/2;button("Catalog",left+10,top+32,82,()->{tab=0;page=0;});button("Constellations",left+96,top+32,100,()->{tab=1;page=0;});button("Collections",left+200,top+32,86,()->tab=2);previous=button("<",left+10,top+h-28,24,()->page=Math.max(0,page-1));next=button(">",left+38,top+h-28,24,()->page=Math.min(48/Math.max(1,(h-108)/18),page+1));button("Close",left+w-70,top+h-28,60,this::onClose);}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        previous.visible=next.visible=tab==0;g.fill(0,0,width,height,0xB0070B12);g.fill(left,top,left+w,top+h,0xFF101C29);g.fill(left,top,left+w,top+3,0xFF65B9D1);g.drawString(font,"FIELD JOURNAL / ASTRONOMY",left+10,top+13,0xD5E9F2,false);super.render(g,mx,my,partial);
        if(AstronomyClient.catalog==null)return;visible.clear();
        if(tab==2){collections(g);return;}
        if(tab==1){int y=top+66;for(var c:AstronomyClient.catalog.constellations){long found=Arrays.stream(c.stars()).filter(id->AstronomyClient.journal.get(id)!=null).count();if(c==AstronomyClient.catalog.constellations.get(constellationIndex))g.fill(left+8,y-3,left+w/2,y+27,0xFF244150);g.drawString(font,c.name(),left+14,y,0xE2EDF2,false);g.drawString(font,found+" / "+c.stars().length+" reference stars",left+14,y+13,0x8EA7B8,false);y+=34;}constellation(g,left+w-136,top+72,120,112);return;}
        int rows=Math.max(1,(h-108)/18),start=page*rows;var targets=AstronomyClient.catalog.targets;
        for(int i=start;i<Math.min(start+rows,targets.size());i++){var t=targets.get(i);var e=AstronomyClient.journal.get(i);int y=top+64+(i-start)*18;visible.add(i);if(i==selected)g.fill(left+8,y-2,left+w/2-5,y+14,0xFF244150);String name=e==null?(t.deep()?"Uncharted "+t.kind().name().toLowerCase(Locale.ROOT):t.name()):t.name();g.drawString(font,font.plainSubstrByWidth(name,w/2-68),left+12,y,e==null?0x869CAC:0xD5E9F2,false);g.drawString(font,e==null?"—":e.completeness+"%",left+w/2-49,y,0x77C7C7,false);}
        var target=AstronomyClient.catalog.target(selected);if(target!=null){int x=left+w/2+9,y=top+66;var e=AstronomyClient.journal.get(selected);g.drawString(font,font.plainSubstrByWidth(e==null && target.deep()?"Uncharted target":target.name(),w/2-75),x,y,0xDAEAF2,false);g.drawString(font,target.kind().name()+String.format(Locale.ROOT," / MAG %.1f",target.magnitude()),x,y+17,0x77C7C7,false);int yy=y+37;for(var line:font.split(Component.literal(target.description()),w/2-24)){g.drawString(font,line,x,yy,0x9EB5C4,false);yy+=11;}if(e!=null){g.drawString(font,e.observations+" observations",x,yy+10,0xDAEAF2,false);g.drawString(font,String.format(Locale.ROOT,"Best quality %.0f%%",e.best*100),x,yy+23,0xDAEAF2,false);}sketch(g,left+w-35,top+77,target,e!=null);var direction=target.direction(AstronomyClient.time(partial));g.drawString(font,String.format(Locale.ROOT,"AZ %.1f°  ALT %+.1f°",(Math.toDegrees(Math.atan2(-direction.x(),direction.z()))+540)%360,Math.toDegrees(Math.asin(direction.y()))),x,top+h-43,0x7EACBD,false);}
        g.drawString(font,(page+1)+" / "+((targets.size()+rows-1)/rows),left+72,top+h-22,0x8EA7B8,false);
    }
    private void sketch(GuiGraphics g,int x,int y,SkyCatalog.Object t,boolean found){if(!found)return;int c=0xFF000000|t.color();if(t.kind()==SkyCatalog.Kind.PLANET){g.fill(x+12,y-9,x+17,y+12,0xFF000000|((t.color()>>1)&0x7F7F7F));g.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hobbymod","textures/environment/planets.png"),x-12,y-12,24,24,(t.id()-22)*16,0,16,16,128,16);if(t.name().equals("Saturn")){g.fill(x-24,y-2,x-12,y+1,0xFFCDBD88);g.fill(x+12,y-2,x+25,y+1,0xFFCDBD88);}}else{var r=new Random(AstronomyClient.seed+t.id());for(int i=0;i<26;i++){int xx=x+r.nextInt(62)-31,yy=y+r.nextInt(36)-18;g.fill(xx,yy,xx+1,yy+1,c);}}}
    private void constellation(GuiGraphics g,int x,int y,int w,int h){var c=AstronomyClient.catalog.constellations.get(constellationIndex);var points=new java.util.HashMap<Integer,int[]>();double lo=Double.POSITIVE_INFINITY,hi=-lo,down=lo,up=-lo;for(int id:c.stars()){var a=AstronomyClient.catalog.target(id);lo=Math.min(lo,a.ra());hi=Math.max(hi,a.ra());down=Math.min(down,a.dec());up=Math.max(up,a.dec());}for(int id:c.stars()){var a=AstronomyClient.catalog.target(id);points.put(id,new int[]{x+10+(int)((a.ra()-lo)/(hi-lo)*(w-20)),y+10+(int)((up-a.dec())/(up-down)*(h-20))});}for(var edge:c.edges()){var a=points.get(edge[0]);var b=points.get(edge[1]);line(g,a[0],a[1],b[0],b[1],0xFF446C84);}for(int id:c.stars()){var p=points.get(id);g.fill(p[0]-1,p[1]-1,p[0]+2,p[1]+2,0xFFD6E7FF);}}
    private void collections(GuiGraphics g){
        int x=left+16,y=top+64;
        g.drawString(font,"OBSERVING COLLECTIONS",x,y,0xD5E9F2,false);
        long planets=AstronomyClient.catalog.targets.stream().filter(t->t.kind()==SkyCatalog.Kind.PLANET && AstronomyClient.journal.get(t.id())!=null).count();
        long deep=AstronomyClient.catalog.targets.stream().filter(t->t.deep() && AstronomyClient.journal.get(t.id())!=null).count();
        long charts=AstronomyClient.catalog.constellations.stream().filter(c->Arrays.stream(c.stars()).allMatch(id->AstronomyClient.journal.get(id)!=null)).count();
        long complete=AstronomyClient.journal.entries().values().stream().filter(e->e.completeness>=100).count();
        String[] goals={"Solar system   "+planets+" / 7 planets recorded","Constellation atlas   "+charts+" / 4 charts recorded","Deep-sky survey   "+deep+" / 20 discoveries","Detailed records   "+complete+" / 49 completed"};
        for(int i=0;i<goals.length;i++){g.fill(x-4,y+17+i*22,left+w-12,y+37+i*22,0xFF1D3444);g.drawString(font,goals[i],x,y+23+i*22,0x98D4D2,false);}
        g.drawString(font,"Record with a telescope; return on later nights.",x,top+h-43,0x9EB5C4,false);
    }
    public static void line(GuiGraphics g,int x,int y,int X,int Y,int color){int steps=Math.max(Math.abs(X-x),Math.abs(Y-y));for(int i=0;i<=steps;i++){int xx=x+(X-x)*i/Math.max(1,steps),yy=y+(Y-y)*i/Math.max(1,steps);g.fill(xx,yy,xx+1,yy+1,color);}}
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;if(tab==1 && x>=left+8 && x<left+w/2 && y>=top+66 && y<top+202){constellationIndex=Math.clamp((int)(y-top-66)/34,0,3);return true;}if(tab==0 && y>=top+64 && x>=left+8 && x<left+w/2){int index=(int)(y-top-64)/18;if(index>=0 && index<visible.size()){selected=visible.get(index);return true;}}return false;}
}
