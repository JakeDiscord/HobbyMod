package io.github.jakediscord.hobbymod.sculpting.client;

import io.github.jakediscord.hobbymod.sculpting.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.lwjgl.glfw.GLFW;

/** Transparent input capture over the real world; no preview model, background, or tool buttons. */
@Environment(EnvType.CLIENT)
public final class OrbitControls extends Screen {
    private boolean carving,orbiting;
    private double mouseX,mouseY;
    private float partialTick;
    public OrbitControls() { super(Component.literal("Marble carving")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partialTick) {}
    @Override public void render(GuiGraphics g,int x,int y,float partialTick) {
        this.partialTick=partialTick;mouseX=x;mouseY=y;
        SculptureOrbit.pick(x,y,partialTick);
        var held=SculptureNetworking.heldTool(minecraft.player);
        String tool=held.getItem() instanceof ChiselItem item?item.tool().title:"Hold a carving tool";
        String remaining=String.format(java.util.Locale.ROOT," · %.1f%% marble",SculptureOrbit.remainingPercent());
        g.drawString(font,tool+remaining,8,8,0xffeedcc0);
    }
    @Override public void tick() { if(carving && !orbiting){SculptureOrbit.pick(mouseX,mouseY,partialTick);SculptureOrbit.stroke();} }
    @Override public boolean mouseClicked(double x,double y,int button) {
        mouseX=x;mouseY=y;SculptureOrbit.breakStroke();
        SculptureOrbit.pick(x,y,partialTick);
        orbiting=button==1 || button==2 || (button==0 && hasAltDown());
        carving=button==0 && !orbiting;
        if(carving)SculptureOrbit.stroke();return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        mouseX=x;mouseY=y;
        if(orbiting) { SculptureOrbit.yaw+=(float)(dx*0.4);SculptureOrbit.pitch=Math.clamp(SculptureOrbit.pitch+(float)(dy*0.4),-85,85); }
        else if(carving) { SculptureOrbit.pick(x,y,partialTick);SculptureOrbit.stroke(); }
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button) { carving=false;orbiting=false;SculptureOrbit.breakStroke();return true; }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        carving=false;SculptureOrbit.breakStroke();
        if(hasShiftDown())SculptureOrbit.pan(vertical*0.25);
        else if(hasControlDown())SculptureOrbit.distance=Math.clamp(SculptureOrbit.distance*Math.pow(0.9,vertical),1.1,5);
        else if(vertical!=0)select(Math.floorMod(minecraft.player.getInventory().selected-(int)Math.signum(vertical),9));
        return true;
    }
    private void select(int slot) {
        carving=false;SculptureOrbit.breakStroke(); minecraft.player.getInventory().selected=slot;
        minecraft.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(key==GLFW.GLFW_KEY_ESCAPE) { onClose();return true; }
        if(key==GLFW.GLFW_KEY_M) { SculptureOrbit.mirror=!SculptureOrbit.mirror;SculptureOrbit.breakStroke();return true; }
        if(key==GLFW.GLFW_KEY_F) { SculptureOrbit.frame();return true; }
        if(minecraft.options.keyInventory.matches(key,scanCode)) { carving=false;orbiting=false; minecraft.setScreen(new InventoryScreen(minecraft.player));return true; }
        for(int i=0;i<9;i++) if(minecraft.options.keyHotbarSlots[i].matches(key,scanCode)) { select(i);return true; }
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public void removed() { carving=false;orbiting=false; }
    @Override public void onClose() { SculptureOrbit.close(); }
}
