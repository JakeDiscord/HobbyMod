package io.github.jakediscord.hobbymod.astronomy.space.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.astronomy.*;
import io.github.jakediscord.hobbymod.astronomy.client.AstronomyClient;
import io.github.jakediscord.hobbymod.astronomy.space.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class NavigationScreen extends Screen {
    private int left,top,widthPanel,panelHeight,rowHeight;
    public NavigationScreen(){super(Component.literal("Flight navigation"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    private void destination(String label,int id,int row){addRenderableWidget(Button.builder(Component.literal(label),b->{if(minecraft.player.getVehicle() instanceof PrototypeShip ship)NetworkManager.sendToServer(new FlightNetworking.Destination(ship.getId(),id));onClose();}).bounds(left+14,top+43+row*rowHeight,widthPanel-28,rowHeight-2).build());}
    @Override protected void init(){
        widthPanel=Math.min(310,width-24);left=(width-widthPanel)/2;panelHeight=Math.min(250,height-16);rowHeight=height<255?17:20;top=(height-panelHeight)/2;int row=0;destination("Earth / return to launch site",-1,row++);
        for(var p:CelestialBodies.PLANETS)if(ObservationAccess.discovered(AstronomyClient.journal,p.target()))destination(p.name()+" / "+(p.solid()?"surface":"orbital dock"),p.target(),row++);
        addRenderableWidget(Button.builder(Component.literal("Resume flight"),b->onClose()).bounds(left+14,top+panelHeight-27,widthPanel-28,18).build());
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        g.fill(0,0,width,height,0xA008101A);g.fill(left,top,left+widthPanel,top+panelHeight,0xF0132533);g.fill(left,top,left+widthPanel,top+2,0xFF83DFE4);
        g.drawString(font,"PROTOTYPE / NAVIGATION",left+14,top+12,0xD2EFF2,false);g.drawString(font,"Set a course, then fly it",left+14,top+27,0x8CBAC7,false);
        g.drawString(font,"Only recorded planets are listed",left+14,top+panelHeight-44,0x8CBAC7,false);super.render(g,x,y,partial);
    }
}
