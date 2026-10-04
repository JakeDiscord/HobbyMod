package io.github.jakediscord.hobbymod.aquarium.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.aquarium.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

/** Crisp inventory-style care, resident journal and item-backed aquascaping. */
public final class AquariumScreen extends Screen {
    private final BlockPos pos;
    private Button editor,bodies,previousPage,nextPage;
    private int tab,page,selected,placementSlot=-1,rotation;
    private boolean remove;
    private String notice="";
    private long noticeUntil;
    private static final int W=320,H=224;
    private int left(){return (width-W)/2;} private int top(){return Math.max(0,(height-H)/2);}
    public AquariumScreen(BlockPos pos){super(Component.literal("Aquarium"));this.pos=pos;}
    public static void register(){
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AquariumNetworking.Notice.TYPE,AquariumNetworking.Notice.CODEC,(p,c)->c.queue(()->{if(Minecraft.getInstance().screen instanceof AquariumScreen s){s.notice=p.text();s.noticeUntil=net.minecraft.Util.getMillis()+4500;}else if(Minecraft.getInstance().screen instanceof io.github.jakediscord.hobbymod.terrarium.client.TerrariumScreen s)s.feedback(p.text());else if(Minecraft.getInstance().screen instanceof AquariumEditScreen s)s.feedback(p.text());}));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AquariumNetworking.Open.TYPE,AquariumNetworking.Open.CODEC,(p,c)->c.queue(()->Minecraft.getInstance().setScreen(care(p.pos()))));
    }
    public static Screen care(BlockPos pos){var mc=Minecraft.getInstance();return mc.level!=null && mc.level.getBlockEntity(pos) instanceof AquariumBlockEntity t && t.data.terrarium!=null?new io.github.jakediscord.hobbymod.terrarium.client.TerrariumScreen(pos):new AquariumScreen(pos);}
    private AquariumBlockEntity tank(){return minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof AquariumBlockEntity t?t:null;}
    @Override public boolean isPauseScreen(){return false;}
    // Screen.render normally calls Minecraft's blur. Never blur our already-drawn inventory panel.
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    private void send(int slot){var t=tank();var id=t==null || t.data.fish().isEmpty()?null:t.data.fish().get(Math.clamp(selected,0,t.data.fish().size()-1)).id;NetworkManager.sendToServer(new AquariumNetworking.Action(pos,slot,selected,remove,id));}
    @Override protected void init(){
        clearWidgets();int x=left(),y=top();String[] labels={"Fish","Layout"};
        for(int i=0;i<2;i++){final int n=i;addRenderableWidget(Button.builder(Component.literal(labels[i]),b->{tab=n;page=0;placementSlot=-1;}).bounds(x+8+i*153,y+18,151,20).build());}
        bodies=addRenderableWidget(Button.builder(Component.literal("Remove bodies"),b->NetworkManager.sendToServer(new AquariumNetworking.Action(pos,-3,-1,false,null))).bounds(x+184,y+192,126,18).build());
        editor=addRenderableWidget(Button.builder(Component.literal("Edit in 3D"),b->AquariumOrbit.begin(pos)).bounds(x+184,y+192,126,18).build());
        previousPage=addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);}).bounds(x+9,y+102,20,18).build());
        nextPage=addRenderableWidget(Button.builder(Component.literal(">"),b->{var t=tank();page=Math.min(t==null?0:Math.max(0,(t.data.fish().size()-1)/3),page+1);}).bounds(x+130,y+102,20,18).build());
        addRenderableWidget(Button.builder(Component.literal(remove?"Remove mode":"Add mode"),b->{remove=!remove;b.setMessage(Component.literal(remove?"Remove mode":"Add mode"));}).bounds(x+184,y+102,126,18).build());
    }
    @Override public void tick(){var t=tank();if(t==null || minecraft.player==null || minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64){onClose();return;}selected=Math.clamp(selected,0,Math.max(0,t.data.fish().size()-1));page=Math.clamp(page,0,Math.max(0,(t.data.fish().size()-1)/3));}
    private static void frame(GuiGraphics g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,0xff373737);g.fill(x+1,y+1,x+w-1,y+h-1,0xfff7f7f7);g.fill(x+2,y+2,x+w-2,y+h-2,0xff8b8b8b);g.fill(x+3,y+3,x+w-3,y+h-3,0xffc6c6c6);}
    private static void slot(GuiGraphics g,int x,int y){g.fill(x,y,x+18,y+18,0xff373737);g.fill(x+1,y+1,x+18,y+18,0xffffffff);g.fill(x+1,y+1,x+17,y+17,0xff8b8b8b);}
    private void text(GuiGraphics g,String s,int x,int y,int color,int max){g.drawString(font,font.plainSubstrByWidth(s,max),x,y,color,false);}
    private void bar(GuiGraphics g,String label,int value,int x,int y,int color){text(g,label,x,y,0x404040,145);g.fill(x,y+10,x+140,y+16,0xff555555);g.fill(x+1,y+11,x+1+value*138/100,y+15,0xff000000|color);}
    private static ItemStack fishIcon(AquariumData.Species s){return new ItemStack(AquariumContent.FISH.get(s).get());}
    private void fishPanel(GuiGraphics g,AquariumData d,int x,int y){
        text(g,"Residents "+d.fish().size()+"  ·  Load "+d.load()+"/"+d.size.volume(),x+9,y+42,0x404040,300);
        for(int i=0;i<3;i++){int n=page*3+i;if(n>=d.fish().size())break;var f=d.fish().get(n);int row=y+55+i*15;
            if(n==selected)g.fill(x+8,row-1,x+151,row+14,0xffa3b8a1);g.renderItem(fishIcon(f.species),x+9,row-1);
            text(g,f.label(),x+28,row,0x303030,82);text(g,f.alive()?f.health+"%":"Dead",x+115,row,0x303030,32);
        }
        bar(g,d.filled?"Water: "+d.quality+"%":"Water: empty",d.filled?d.quality:0,x+164,y+43,d.quality>=65?0x68a34d:0xcc6a44);
        bar(g,"Food: "+d.food+"% (~"+d.foodMinutes()+" min)",d.food,x+164,y+63,0xb3914a);
        bar(g,d.cycle>=5?"Filter cycled":"Cycling: "+d.cycle+"/5",d.cycle*20,x+164,y+83,0x5b94bb);

    }
    private void scapePanel(GuiGraphics g,AquariumData d,int x,int y){
        text(g,"Layout",x+9,y+43,0x404040,300);
        g.fill(x+9,y+56,x+151,y+99,0xff466f80);g.fill(x+11,y+58,x+149,y+97,d.substrate?(d.gravel?0xff8c8d8b:0xffb9ad86):0xff7199a2);
        g.enableScissor(x+11,y+58,x+149,y+97);
        for(int i=0;i<d.scape.pieces().size();i++){var p=d.scape.pieces().get(i);g.renderItem(new ItemStack(AquariumControllerBlock.decorItem(p)),x+3+(int)(p.x()*138),y+50+(int)(p.z()*39));}
        g.disableScissor();
        text(g,d.substrate?(d.gravel?"Gravel bed":"Sand bed"):"No substrate",x+164,y+56,0x404040,144);
        text(g,"Plants "+d.plants+" · Rocks "+d.rocks,x+164,y+69,0x404040,144);
        text(g,"Wood "+d.wood+" · Filter "+(d.filter?"yes":"no"),x+164,y+82,0x404040,144);

    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        if(editor!=null)editor.visible=tab==1;
        g.fill(0,0,width,height,0x99000000);int x=left(),y=top();frame(g,x,y,W,H);var t=tank();if(t==null)return;var d=t.data;
        bodies.visible=tab==0;bodies.active=d.bodies()>0;bodies.setMessage(Component.literal("Remove bodies ("+d.bodies()+")"));previousPage.visible=nextPage.visible=tab==0;
        text(g,d.size.name().substring(0,1)+d.size.name().substring(1).toLowerCase()+" Aquarium",x+9,y+7,0x404040,180);
        text(g,d.filled?(d.warm?"Warm water":"Cool water"):"Empty",x+228,y+7,0x404040,85);
        if(tab==0)fishPanel(g,d,x,y);else scapePanel(g,d,x,y);
        text(g,"Inventory",x+9,y+124,0x404040,302);
        for(int i=0;i<36;i++){int col=i%9,row=i/9;int px=x+8+col*18,py=y+137+row*18;slot(g,px,py);var stack=minecraft.player.getInventory().getItem(i<27?i+9:i-27);g.renderItem(stack,px+1,py+1);g.renderItemDecorations(font,stack,px+1,py+1);if((i<27?i+9:i-27)==placementSlot)g.fill(px,py,px+18,py+18,0x5544bb44);}
        if(tab==0 && !d.fish().isEmpty()){
            var f=d.fish().get(selected);text(g,f.label(),x+184,y+140,0x303030,124);
            text(g,(f.female?"Female":"Male")+" · "+(f.age<8?"Fry":"Adult"),x+184,y+154,0x555555,126);
            text(g,f.alive()?"Health "+f.health+"%":"Dead",x+184,y+168,0x555555,126);
            if(f.alive() && f.acclimation>0)text(g,"Acclimating "+f.acclimation+" min",x+184,y+182,0x555555,126);
        }
        String status=net.minecraft.Util.getMillis()<noticeUntil?notice:t.warning();
        text(g,status,x+9,y+214,status.equals(t.warning()) && !status.isEmpty()?0x8b4b27:0x404040,300);
        super.render(g,mx,my,partial);
        int index=inventoryAt(mx,my);if(index>=0){var stack=minecraft.player.getInventory().getItem(index);if(!stack.isEmpty()){
            var lines=new ArrayList<Component>();lines.add(stack.getHoverName());stack.getItem().appendHoverText(stack,Item.TooltipContext.of(minecraft.level),lines,TooltipFlag.NORMAL);
            if(stack.getItem() instanceof AquariumFishItem item){var saved=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);var f=saved==null?new AquariumData.Fish(new UUID(0,-1),item.species):AquariumNbt.fish(saved.copyTag());if(f!=null){String why=d.canAdd(f);if(!why.isEmpty())lines.add(Component.literal(why));}}
            g.renderComponentTooltip(font,lines,mx,my);
        }}
    }

    private int inventoryAt(double mx,double my){int x=(int)mx-left()-8,y=(int)my-top()-137;if(x<0 || x>=162 || y<0 || y>=72)return -1;int i=x/18+(y/18)*9;return i<27?i+9:i-27;}
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(super.mouseClicked(mx,my,button))return true;var t=tank();if(t==null)return false;int x=left(),y=top(),slot=inventoryAt(mx,my);
        if(slot>=0){var stack=minecraft.player.getInventory().getItem(slot);if(tab==1 && !remove && AquariumControllerBlock.material(stack)!=null && !( !t.data.substrate && (stack.is(Items.SAND) || stack.is(Items.GRAVEL))))placementSlot=slot;else send(slot);return true;}
        if(tab==0 && mx>=x+8 && mx<x+151 && my>=y+54 && my<y+100){selected=Math.min(t.data.fish().size()-1,page*3+(int)(my-y-54)/15);return true;}
        if(tab==1 && mx>=x+11 && mx<x+149 && my>=y+58 && my<y+97){double px=Math.clamp((mx-x-11)/138,.08,.92),pz=Math.clamp((my-y-58)/39,.08,.92);
            if(remove){int nearest=-1;double best=.06;var list=t.data.scape.pieces();for(int i=0;i<list.size();i++){var p=list.get(i);double dist=Math.pow(p.x()-px,2)+Math.pow(p.z()-pz,2);if(dist<best){best=dist;nearest=i;}}if(nearest>=0){var p=list.get(nearest);NetworkManager.sendToServer(new AquariumNetworking.Decor(pos,-1,nearest,p.x(),p.z(),rotation));}}
            else if(placementSlot>=0)NetworkManager.sendToServer(new AquariumNetworking.Decor(pos,placementSlot,-1,px,pz,rotation));return true;
        }
        return false;
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(tab==1){rotation=Math.floorMod(rotation+(int)Math.signum(v),4);return true;}return super.mouseScrolled(x,y,h,v);}
}
