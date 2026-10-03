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

/** Live tank dashboard. Buttons use real inventory supplies, validated on the server. */
public final class AquariumScreen extends Screen {
    private final BlockPos pos;
    private int page,residentPage;
    private int panel(){return Math.min(380,width-16);}
    private boolean remove;
    private String notice="";
    private long noticeUntil;
    private List<String> signature=List.of();
    public AquariumScreen(BlockPos pos){super(Component.literal("Aquarium"));this.pos=pos;}
    public static void register(){
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AquariumNetworking.Notice.TYPE,AquariumNetworking.Notice.CODEC,(p,c)->c.queue(()->{
            if(Minecraft.getInstance().screen instanceof AquariumScreen screen){screen.notice=p.text();screen.noticeUntil=net.minecraft.Util.getMillis()+4000;}
        }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AquariumNetworking.Open.TYPE,AquariumNetworking.Open.CODEC,(p,c)->c.queue(()->Minecraft.getInstance().setScreen(new AquariumScreen(p.pos()))));
    }
    private AquariumBlockEntity tank(){return minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof AquariumBlockEntity t?t:null;}
    @Override public boolean isPauseScreen(){return false;}
    private void send(int slot,int resident){
        var t=tank();if(t==null)return;
        NetworkManager.sendToServer(new AquariumNetworking.Action(pos,slot,resident<0?t.data.selected:resident,remove));
    }
    private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    private List<Integer> supplies(){
        List<Integer> result=new ArrayList<>();if(minecraft.player==null)return result;
        for(int i=0;i<36;i++){
            var s=minecraft.player.getInventory().getItem(i);var item=s.getItem();
            if(item instanceof AquariumFishItem || s.is(AquariumContent.FOOD.get()) || s.is(AquariumContent.FILTER.get())
                    || s.is(Items.WATER_BUCKET)||s.is(Items.BUCKET)||s.is(Items.SHEARS)||s.is(Items.SAND)||s.is(Items.GRAVEL)
                    ||s.is(Items.SEAGRASS)||s.is(Items.KELP)||s.is(Items.COBBLESTONE)||s.is(Items.STICK)||s.is(Items.MAGMA_CREAM)||s.is(Items.SNOWBALL)||s.is(Items.NAME_TAG))result.add(i);
        }
        return result;
    }
    private String supplyLabel(ItemStack stack){
        if(stack.is(Items.WATER_BUCKET))return "Fill / change water";
        if(stack.is(Items.BUCKET))return remove || tank()!=null && tank().data.fish().isEmpty()?"Drain water":"Take selected fish";
        if(stack.is(Items.SHEARS))return "Clean glass";
        if(stack.is(AquariumContent.FOOD.get()))return "Feed fish";
        if(stack.is(Items.MAGMA_CREAM))return "Warm water";
        if(stack.is(Items.SNOWBALL))return "Cool water";
        if(stack.is(Items.NAME_TAG))return "Name selected fish";
        if(stack.getItem() instanceof AquariumFishItem)return "Add "+stack.getHoverName().getString();
        return (remove?"Remove ":"Add ")+stack.getHoverName().getString();
    }
    @Override protected void init(){rebuild();}
    private void rebuild(){
        clearWidgets();var t=tank();if(t==null)return;
        int x=(width-panel())/2,y=Math.max(4,(height-228)/2);var slots=supplies();
        page=Math.max(0,Math.min(page,Math.max(0,(slots.size()-1)/4)));
        residentPage=Math.max(0,Math.min(residentPage,Math.max(0,(t.data.fish().size()-1)/4)));
        for(int j=0;j<4;j++){
            int n=residentPage*4+j;if(n<t.data.fish().size()){
                var f=t.data.fish().get(n);int index=n;
                button((n==t.data.selected?"> ":"")+f.label()+" · "+f.health+"%",x+10,y+78+j*22,panel()/2-20,()->send(-1,index));
            }
            int q=page*4+j;if(q<slots.size()){
                int slot=slots.get(q);var s=minecraft.player.getInventory().getItem(slot);
                button(supplyLabel(s)+" ×"+s.getCount(),x+panel()/2+5,y+78+j*22,panel()/2-15,()->send(slot,-1));
            }
        }
        button("<",x+10,y+170,30,()->{residentPage--;rebuild();});
        button(">",x+panel()/2-40,y+170,30,()->{residentPage++;rebuild();});
        button("<",x+panel()/2+5,y+170,30,()->{page--;rebuild();});
        button(">",x+panel()-40,y+170,30,()->{page++;rebuild();});
        button(remove?"Remove decor":"Add / use",x+panel()/2+40,y+170,panel()/2-85,()->{remove=!remove;rebuild();});
        signature=stateSignature();
    }
    private List<String> stateSignature(){
        var t=tank();if(t==null)return List.of();List<String> result=new ArrayList<>();result.add(""+t.data.selected);
        for(var f:t.data.fish())result.add(f.id+":"+f.label()+":"+f.health);
        for(int i:supplies())result.add(i+":"+minecraft.player.getInventory().getItem(i).toString());return result;
    }
    @Override public void tick(){
        var t=tank();if(t==null || minecraft.player==null || minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64){onClose();return;}
        if(!signature.equals(stateSignature()))rebuild();
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xB0202833);int x=(width-panel())/2,y=Math.max(4,(height-228)/2);
        g.fill(x,y,x+panel(),y+228,0xFF182D3A);var t=tank();if(t==null)return;var d=t.data;
        g.drawString(font,"Aquarium  ·  "+d.size.blocksWide()+" × "+d.size.blocksDeep(),x+10,y+9,0xFFFFFF);
        g.drawString(font,(d.filled?(d.warm?"Warm":"Cool")+" water":"Empty")+"  ·  Quality "+d.quality+"%  ·  Food "+d.food+"%",x+10,y+25,0xBDDDEA);
        String warning=!notice.isEmpty() && net.minecraft.Util.getMillis()<noticeUntil?notice:t.warning();g.drawString(font,warning.isEmpty()?"Healthy tank":warning,x+10,y+40,warning.isEmpty()?0x9CE5AA:0xFFD18A);
        g.drawString(font,"Filter "+(d.filter?"on":"none")+" · Plants "+d.plants+" · Rocks "+d.rocks+" · Wood "+d.wood,x+10,y+55,0xBDDDEA);
        g.drawString(font,"Fish ("+d.load()+"/"+d.size.volume()+" load)",x+10,y+65,0xFFFFFF);
        g.drawString(font,"Supplies from your inventory",x+panel()/2+5,y+65,0xFFFFFF);
        if(supplies().isEmpty()){
            g.drawString(font,"Bring a water bucket,",x+panel()/2+5,y+85,0xBDDDEA);
            g.drawString(font,"fish and decorations.",x+panel()/2+5,y+97,0xBDDDEA);
        }
        super.render(g,mx,my,partial);
        if(mx>=x+panel()/2+5 && mx<x+panel()-10 && my>=y+78 && my<y+166){
            int j=(my-y-78)/22,q=page*4+j;var slots=supplies();if(q<slots.size())g.renderTooltip(font,minecraft.player.getInventory().getItem(slots.get(q)),mx,my);
        }
        if(mx>=x+10 && mx<x+panel()/2-10 && my>=y+78 && my<y+166){
            int index=residentPage*4+(my-y-78)/22;
            if(index<d.fish().size()){
                var f=d.fish().get(index);
                g.renderTooltip(font,Component.literal(f.species.label+" · "+(f.female?"Female":"Male")+" · "+(f.acclimation>0?"Acclimating "+f.acclimation+" min":"Settled")),mx,my);
            }
        }
        g.drawString(font,"Water → cycle → plants/filter → compatible fish",x+10,y+200,0xFFFFFF);
        g.drawString(font,"Select fish; bucket takes it out. Shears clean glass.",x+10,y+214,0xBDDDEA);
    }
}
