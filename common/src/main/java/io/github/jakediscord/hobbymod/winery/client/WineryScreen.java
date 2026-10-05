package io.github.jakediscord.hobbymod.winery.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.winery.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla-sized inventory controls, readable progress and a short next-step hint. */
public final class WineryScreen extends AbstractContainerScreen<WineryMenu> {
    private Button start,rack,bottle;private EditBox label;
    public WineryScreen(WineryMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=menu.machine==WineryBlock.Machine.PRESS?192:222;inventoryLabelY=menu.machine==WineryBlock.Machine.PRESS?99:129;}
    @Override protected void init(){super.init();if(menu.machine==WineryBlock.Machine.PRESS)start=button("Press",58,76,60,0);else{start=button("Start",8,76,50,0);rack=button("Rack",62,76,50,1);bottle=button("Bottle",116,76,50,2);label=new EditBox(font,leftPos+8,topPos+110,160,14,Component.literal("Bottle label"));label.setMaxLength(32);label.setHint(Component.literal("Label (optional)"));addRenderableWidget(label);}}
    private Button button(String text,int x,int y,int w,int action){return addRenderableWidget(Button.builder(Component.literal(text),b->{if(action==2 && label!=null)NetworkManager.sendToServer(new WineryNetworking.Label(menu.containerId,label.getValue()));minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}).bounds(leftPos+x,topPos+y,w,18).build());}
    @Override protected void containerTick(){super.containerTick();int stage=menu.data.get(0);start.active=stage==0;if(menu.machine==WineryBlock.Machine.PRESS){int grapes=0;for(int i=0;i<2;i++){var stack=menu.slots.get(i).getItem();if(stack.is(WineryContent.RED.get()) || stack.is(WineryContent.WHITE.get()) || stack.is(WineryContent.GRAPES))grapes+=stack.getCount();}start.active=stage==0 && grapes>=8 && menu.slots.get(2).getItem().is(net.minecraft.world.item.Items.BUCKET) && !menu.slots.get(3).hasItem();}if(menu.machine==WineryBlock.Machine.BARREL){start.setMessage(Component.literal(menu.slots.get(0).getItem().is(net.minecraft.world.item.Items.WATER_BUCKET)?"Rinse":menu.slots.get(0).getItem().is(WineryContent.CASK.get())?"Resume":"Start"));rack.active=stage==3;bottle.active=stage==4 || stage==5;label.active=stage!=5;bottle.active=bottle.active && menu.slots.get(2).getItem().is(net.minecraft.world.item.Items.GLASS_BOTTLE);}}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){g.fill(0,0,width,height,0x55000000);renderBg(g,d,x,y);}
    private void slot(GuiGraphics g,int x,int y){g.fill(leftPos+x-1,topPos+y-1,leftPos+x+17,topPos+y+17,0xff373737);g.fill(leftPos+x,topPos+y,leftPos+x+17,topPos+y+17,0xffeeeeee);g.fill(leftPos+x,topPos+y,leftPos+x+16,topPos+y+16,0xff8b8b8b);}
    private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,font.plainSubstrByWidth(s,160),leftPos+x,topPos+y,color,false);}
    @Override protected void renderBg(GuiGraphics g,float d,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff373737);g.fill(leftPos+1,topPos+1,leftPos+imageWidth-1,topPos+imageHeight-1,0xffc6c6c6);g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+3,0xffeeeeee);g.fill(leftPos+2,topPos+imageHeight-3,leftPos+imageWidth-2,topPos+imageHeight-2,0xff686868);
        for(var s:menu.slots)slot(g,s.x,s.y);
        int stage=menu.data.get(0),progress=menu.data.get(1);boolean press=menu.machine==WineryBlock.Machine.PRESS;
        String state=press?(stage==0?"":"Pressing: "+progress+"%"):switch(stage){case 0->"";case 2->"Fermenting: "+progress+"%";case 3->"Fermented";case 4->"Age "+menu.data.get(3)+"/"+menu.data.get(7)+"s · "+(menu.data.get(8)==2?"Hot":menu.data.get(8)==0?"Dark":"Bright");case 5->"Bottled";default->"Ready";};text(g,state,8,23,0xff404040);
        g.fill(leftPos+8,topPos+34,leftPos+168,topPos+37,0xff888888);if(stage>0)g.fill(leftPos+8,topPos+34,leftPos+8+160*(stage==5?100:stage==4?Math.min(100,menu.data.get(3)*100/Math.max(1,menu.data.get(7))):progress)/100,topPos+37,0xff809650);
        if(press){text(g,"Grapes",26,61,0xff404040);text(g,"Bucket",82,61,0xff404040);text(g,"Must",137,61,0xff404040);}
        else{String[] labels={"Must","Yeast","Glass","Wine"};int[] xs={16,53,93,139};for(int i=0;i<4;i++)text(g,labels[i],xs[i],61,0xff404040);
            String quality=stage==0?"":(menu.data.get(3)<menu.data.get(7)?"Young":menu.data.get(3)<=menu.data.get(7)*3?"Balanced":"Past peak")+" · Q"+menu.data.get(2)+" · "+menu.data.get(4)+" left";text(g,quality,8,98,0xff404040);

        }
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(label!=null && label.isFocused() && key!=256)return label.keyPressed(key,scan,modifiers);return super.keyPressed(key,scan,modifiers);}
    @Override public void render(GuiGraphics g,int mx,int my,float d){super.render(g,mx,my,d);renderTooltip(g,mx,my);}
}
