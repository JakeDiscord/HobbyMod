package io.github.jakediscord.hobbymod.terrarium.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.aquarium.*;
import io.github.jakediscord.hobbymod.aquarium.client.AquariumOrbit;
import io.github.jakediscord.hobbymod.terrarium.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Land-care supplies and layout share the aquarium's familiar real-inventory interaction. */
public final class TerrariumScreen extends Screen {
    private static final int W=320,H=224;
    private final BlockPos pos;
    private int tab,placementSlot=-1,rotation;
    private boolean remove;
    private String notice="";
    private long until;
    private Button editor,lid;
    public TerrariumScreen(BlockPos pos){super(Component.literal("Terrarium"));this.pos=pos;}
    private int left(){return (width-W)/2;}private int top(){return Math.max(0,(height-H)/2);}
    private AquariumBlockEntity tank(){return minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof AquariumBlockEntity t && t.data.terrarium!=null?t:null;}
    public void feedback(String text){notice=text;until=net.minecraft.Util.getMillis()+4500;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float p){}
    private void send(int slot){NetworkManager.sendToServer(new AquariumNetworking.Action(pos,slot,-1,remove,null));}
    @Override protected void init(){
        int x=left(),y=top();String[] names={"Ecosystem","Layout","Guide"};
        for(int i=0;i<3;i++){int n=i;addRenderableWidget(Button.builder(Component.literal(names[i]),b->{tab=n;placementSlot=-1;}).bounds(x+8+i*101,y+18,98,20).build());}
        addRenderableWidget(Button.builder(Component.literal(remove?"Remove / collect":"Add mode"),b->{remove=!remove;b.setMessage(Component.literal(remove?"Remove / collect":"Add mode"));}).bounds(x+184,y+102,126,18).build());
        editor=addRenderableWidget(Button.builder(Component.literal("Edit in 3D"),b->AquariumOrbit.begin(pos)).bounds(x+184,y+192,126,18).build());
        lid=addRenderableWidget(Button.builder(Component.literal("Open lid"),b->send(-2)).bounds(x+184,y+174,126,18).build());
    }
    @Override public void tick(){if(tank()==null || minecraft.player==null || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(minecraft.player,pos))onClose();}
    private void text(GuiGraphics g,String s,int x,int y,int max){g.drawString(font,font.plainSubstrByWidth(s,max),x,y,0x404040,false);}
    private void bar(GuiGraphics g,String label,int value,int x,int y,int color){text(g,label,x,y,140);g.fill(x,y+10,x+140,y+16,0xff555555);g.fill(x+1,y+11,x+1+value*138/100,y+15,0xff000000|color);}
    private static void frame(GuiGraphics g,int x,int y){g.fill(x,y,x+W,y+H,0xff373737);g.fill(x+1,y+1,x+W-1,y+H-1,0xfff7f7f7);g.fill(x+3,y+3,x+W-3,y+H-3,0xffc6c6c6);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        var tank=tank();if(tank==null)return;var habitat=tank.data;var d=habitat.terrarium;int x=left(),y=top();
        editor.visible=tab==1;lid.visible=tab==0;lid.setMessage(Component.literal(d.open?"Close lid":"Open lid"));
        g.fill(0,0,width,height,0x99000000);frame(g,x,y);text(g,"Terrarium",x+9,y+7,180);text(g,d.open?"Ventilated":"Closed",x+246,y+7,66);
        if(tab==0){
            bar(g,"Moisture "+d.moisture+"%",d.moisture,x+9,y+44,0x579b83);bar(g,"Humidity "+d.humidity+"%",d.humidity,x+164,y+44,0x5b94bb);
            bar(g,"Light "+d.light+"/15",d.light*100/15,x+9,y+65,0xc0ad66);bar(g,"Leaf litter "+d.food+"%",d.food,x+164,y+65,0x9b865d);
            text(g,"Substrate: "+d.substrate.name().toLowerCase(java.util.Locale.ROOT),x+9,y+86,145);text(g,"Drainage: "+(d.drainage?"gravel":"none"),x+164,y+86,145);
            int spring=0,isopods=0;for(var r:d.residents())if(r.species==TerrariumData.Species.SPRINGTAIL)spring++;else isopods++;
            text(g,"Springtails "+spring+" · Isopods "+isopods,x+9,y+106,168);
        }else if(tab==1){
            text(g,remove?"Click a piece to collect it":"Choose a block, then click the top view",x+9,y+44,300);
            g.fill(x+9,y+56,x+151,y+99,0xff46674e);g.fill(x+11,y+58,x+149,y+97,d.substrate==TerrariumData.Substrate.SAND?0xffb9ad86:d.substrate==TerrariumData.Substrate.MOSS?0xff648c47:0xff866d53);
            g.enableScissor(x+11,y+58,x+149,y+97);for(var p:habitat.scape.pieces())g.renderItem(new ItemStack(AquariumControllerBlock.decorItem(p)),x+3+(int)(p.x()*138),y+50+(int)(p.z()*39));g.disableScissor();
            text(g,"Plants "+TerrariumActions.plants(habitat),x+164,y+57,144);text(g,"Pieces "+habitat.scape.pieces().size()+"/28",x+164,y+72,144);text(g,"Rotate / sculpt in 3D",x+164,y+88,144);
        }else{
            text(g,"1. Add gravel drainage and soil, sand or moss.",x+9,y+44,300);
            text(g,"2. Arrange moss, fern, azalea, poppy or oak.",x+9,y+58,300);
            text(g,"3. Mist; add a lamp if the room is dark.",x+9,y+72,300);
            text(g,"4. Optional colonies enjoy moist plants + litter.",x+9,y+86,300);
            text(g,"Closed, comfortable colonies slowly breed.",x+9,y+100,170);
        }
        text(g,remove?"Inventory · click to remove / collect":"Inventory · click a supply to use it",x+9,y+124,300);
        for(int i=0;i<36;i++){int slot=i<27?i+9:i-27,px=x+8+(i%9)*18,py=y+137+(i/9)*18;g.fill(px,py,px+18,py+18,0xff373737);g.fill(px+1,py+1,px+18,py+18,0xffffffff);g.fill(px+1,py+1,px+17,py+17,0xff8b8b8b);var stack=minecraft.player.getInventory().getItem(slot);g.renderItem(stack,px+1,py+1);g.renderItemDecorations(font,stack,px+1,py+1);if(slot==placementSlot)g.fill(px,py,px+18,py+18,0x5544bb44);}
        text(g,d.heatLamp?"Heat lamp · 24–28°C":d.warm?"Warm 24–28°C":"Mild 18–24°C",x+184,y+140,126);text(g,d.residents().size()+"/48 residents",x+184,y+153,126);
        if(tab==0 && !d.residents().isEmpty())text(g,"Comfort "+(int)d.residents().stream().mapToInt(r->r.vigor).average().orElse(100)+"%",x+184,y+165,126);
        if(tab==1){text(g,"Drag the 3D handles",x+184,y+169,126);text(g,"Rings rotate",x+184,y+181,126);}else if(tab==2){text(g,"No random deaths",x+184,y+168,126);text(g,"Collect colonies in",x+184,y+181,126);text(g,"Remove / collect mode.",x+184,y+194,126);}
        text(g,!notice.isEmpty() && net.minecraft.Util.getMillis()<until?notice:tank.warning(),x+9,y+214,300);super.render(g,mx,my,partial);
        int slot=inventoryAt(mx,my);if(slot>=0){var stack=minecraft.player.getInventory().getItem(slot);if(!stack.isEmpty()){
            var lines=new java.util.ArrayList<Component>();lines.add(stack.getHoverName());
            stack.getItem().appendHoverText(stack,Item.TooltipContext.of(minecraft.level),lines,TooltipFlag.NORMAL);
            lines.add(Component.literal(tab==1 && AquariumControllerBlock.material(stack)!=null?"Choose, place, then edit in 3D.":remove?"Click to remove or collect.":"Click to use this supply."));
            g.renderComponentTooltip(font,lines,mx,my);
        }}
    }
    private int inventoryAt(double mx,double my){int x=(int)mx-left()-8,y=(int)my-top()-137;if(x<0 || x>=162 || y<0 || y>=72)return -1;int i=x/18+y/18*9;return i<27?i+9:i-27;}
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(super.mouseClicked(mx,my,button))return true;var t=tank();if(t==null)return false;int slot=inventoryAt(mx,my),x=left(),y=top();
        if(slot>=0){var stack=minecraft.player.getInventory().getItem(slot);if(tab==1 && !remove && AquariumControllerBlock.material(stack)!=null)placementSlot=slot;else send(slot);return true;}
        if(tab==1 && mx>=x+11 && mx<x+149 && my>=y+58 && my<y+97){double px=Math.clamp((mx-x-11)/138,.08,.92),pz=Math.clamp((my-y-58)/39,.08,.92);
            if(remove){int nearest=-1;double best=.06;for(int i=0;i<t.data.scape.pieces().size();i++){var p=t.data.scape.pieces().get(i);double distance=Math.pow(p.x()-px,2)+Math.pow(p.z()-pz,2);if(distance<best){best=distance;nearest=i;}}if(nearest>=0){var p=t.data.scape.pieces().get(nearest);NetworkManager.sendToServer(new AquariumNetworking.Decor(pos,-1,nearest,p.x(),p.z(),0));}}
            else if(placementSlot>=0)NetworkManager.sendToServer(new AquariumNetworking.Decor(pos,placementSlot,-1,px,pz,rotation));return true;
        }return false;
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(tab==1){rotation=Math.floorMod(rotation+(int)Math.signum(v),4);return true;}return super.mouseScrolled(x,y,h,v);}
}
