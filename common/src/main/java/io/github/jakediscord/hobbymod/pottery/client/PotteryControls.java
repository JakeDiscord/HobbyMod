package io.github.jakediscord.hobbymod.pottery.client;

import io.github.jakediscord.hobbymod.pottery.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.lwjgl.glfw.GLFW;

public final class PotteryControls extends Screen {
    private boolean shaping,orbiting,clickPending;
    private double selected,push,lift;
    private float partial;
    public PotteryControls(){super(Component.literal("Pottery wheel"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        this.partial=partial;PotteryOrbit.pick(x,y,partial);var w=PotteryOrbit.wheel();if(w==null || w.piece==null)return;
        var held=minecraft.player.getMainHandItem();String tool=held.isEmpty()?"Hands":held.getHoverName().getString();
        g.drawString(font,tool+" · "+w.piece.description()+(w.piece.stage==PotteryPiece.Stage.WET?" · "+w.piece.moisture+"% moisture":""),8,8,0xffeedcc0);
        if(PotteryOrbit.hit!=null){int r=5;g.hLine(x-r,x+r,y,0xffffcf73);g.vLine(x,y-r,y+r,0xffffcf73);}
    }
    @Override public void tick(){
        if(orbiting)return;
        if(clickPending){if(PotteryOrbit.send(selected,0,0,false))clickPending=false;else return;}
        var held=minecraft.player.getMainHandItem();boolean continuous=shaping && held.getItem() instanceof PotteryToolItem tool && tool.tool!=PotteryToolItem.Tool.WIRE;
        if(!continuous && Math.abs(push)+Math.abs(lift)<.0001)return;
        double p=Math.clamp(push,-.024,.024),l=Math.clamp(lift,-.024,.024);
        if(PotteryOrbit.send(selected,p,l,false)){push-=p;lift-=l;}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        PotteryOrbit.pick(x,y,partial);orbiting=button==1 || button==2 || (button==0 && hasAltDown());shaping=button==0 && !orbiting && PotteryOrbit.hit!=null;
        push=0;lift=0;
        if(shaping){var w=PotteryOrbit.wheel();selected=Math.clamp(PotteryOrbit.hit.y()/w.piece.shape.height(),0,1);clickPending=!PotteryOrbit.send(selected,0,0,false);}return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(orbiting){PotteryOrbit.yaw+=(float)(dx*.4);PotteryOrbit.pitch=Math.clamp(PotteryOrbit.pitch+(float)(dy*.4),-80,85);}
        else if(shaping){push=Math.clamp(push+dx*.002,-.15,.15);lift=Math.clamp(lift-dy*.002,-.15,.15);}
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){shaping=false;orbiting=false;return true;}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){
        shaping=false;clickPending=false;push=0;lift=0;
        if(hasControlDown())PotteryOrbit.distance=Math.clamp(PotteryOrbit.distance*Math.pow(.9,v),1,4);
        else if(v!=0)select(Math.floorMod(minecraft.player.getInventory().selected-(int)Math.signum(v),9));return true;
    }
    private void select(int slot){shaping=false;clickPending=false;push=0;lift=0;minecraft.player.getInventory().selected=slot;minecraft.getConnection().send(new ServerboundSetCarriedItemPacket(slot));}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        if(minecraft.options.keyInventory.matches(key,scan)){shaping=false;orbiting=false;minecraft.setScreen(new InventoryScreen(minecraft.player));return true;}
        if(key==GLFW.GLFW_KEY_F){PotteryOrbit.pitch=30;PotteryOrbit.distance=1.8;return true;}
        for(int i=0;i<9;i++)if(minecraft.options.keyHotbarSlots[i].matches(key,scan)){select(i);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void removed(){shaping=false;orbiting=false;clickPending=false;push=0;lift=0;}
    @Override public void onClose(){PotteryOrbit.close();}
}
