package io.github.jakediscord.hobbymod.astronomy.client;

import io.github.jakediscord.hobbymod.astronomy.*;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;
/** Mouse drag fine slews the real world camera; wheel focuses, Shift-wheel changes magnification. */
public final class EyepieceScreen extends Screen {
    public final BlockPos pos;public final boolean naked;
    private int tracking=-1;
    public float yaw,pitch,focus=.5f,mag=8;
    private long sent=Long.MIN_VALUE;
    private boolean oldHud;
    public EyepieceScreen(BlockPos pos,boolean naked){super(Component.literal("Telescope eyepiece"));this.pos=pos;this.naked=naked;var mc=net.minecraft.client.Minecraft.getInstance();oldHud=mc.options.hideGui;if(mc.player!=null){yaw=mc.player.getYRot();pitch=mc.player.getXRot();}if(!naked && mc.level!=null && mc.level.getBlockEntity(pos) instanceof TelescopeBlockEntity b){yaw=b.yaw;pitch=b.pitch;focus=b.focus;mag=b.magnification;}if(naked){mag=1;focus=.72f;}}
    @Override protected void init(){minecraft.options.hideGui=true;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    public void updateTracking(){if(tracking<0 || AstronomyClient.catalog==null)return;var d=AstronomyClient.catalog.target(tracking).direction(AstronomyClient.time(net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));yaw=(float)Math.toDegrees(Math.atan2(-d.x(),d.z()));pitch=(float)-Math.toDegrees(Math.asin(d.y()));}
    private void gamepad(){if(!minecraft.isWindowActive())return;try(var stack=org.lwjgl.system.MemoryStack.stackPush()){var pad=org.lwjgl.glfw.GLFWGamepadState.malloc(stack);for(int joystick=GLFW.GLFW_JOYSTICK_1;joystick<=GLFW.GLFW_JOYSTICK_LAST;joystick++){if(!GLFW.glfwJoystickIsGamepad(joystick) || !GLFW.glfwGetGamepadState(joystick,pad))continue;float x=deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X))*.9f+deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X))*.12f,y=deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y))*.9f+deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y))*.12f;if(x!=0 || y!=0){tracking=-1;yaw=net.minecraft.util.Mth.wrapDegrees(yaw+x*8/mag);pitch=Math.clamp(pitch+y*8/mag,-89,15);}if(!naked)focus=Math.clamp(focus+(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)-pad.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER))*.004f,0,1);break;}}}
    private static float deadzone(float value){return Math.abs(value)<.15f?0:value;}
    public void heartbeat(){gamepad();updateTracking();if(minecraft.level==null || minecraft.player==null)return;if(!naked && (!(minecraft.level.getBlockEntity(pos) instanceof TelescopeBlockEntity) || minecraft.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>36)){onClose();return;}long now=minecraft.level.getGameTime();if(sent==Long.MIN_VALUE || now-sent>=5 || now<sent){sent=now;NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.AIM,pos,yaw,pitch,focus,mag));}}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){tracking=-1;yaw=net.minecraft.util.Mth.wrapDegrees(yaw+(float)(dx*(hasShiftDown()?.008:.07)*8/mag));pitch=Math.clamp(pitch+(float)(dy*(hasShiftDown()?.008:.07)*8/mag),-89,15);return true;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(naked)return true;if(hasShiftDown()){boolean large=minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof TelescopeBlockEntity b && b.large();mag=Math.clamp((float)(mag*Math.pow(1.12,vertical)),large?12:6,large?100:40);}else focus=Math.clamp(focus+(float)vertical*.025f,0,1);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==GLFW.GLFW_KEY_T){if(!naked && minecraft.level.getBlockEntity(pos) instanceof TelescopeBlockEntity b && b.large()){var near=AstronomyClient.catalog==null?null:AstronomyClient.catalog.nearest(SkyCatalog.aim(yaw,pitch),AstronomyClient.time(0),Math.max(.3,35/mag));tracking=tracking>=0?-1:near==null?-1:near.id();}return true;}if(key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_RIGHT || key==GLFW.GLFW_KEY_UP || key==GLFW.GLFW_KEY_DOWN){tracking=-1;float step=(hasShiftDown()?.02f:.15f)*8/mag;if(key==GLFW.GLFW_KEY_LEFT)yaw-=step;if(key==GLFW.GLFW_KEY_RIGHT)yaw+=step;if(key==GLFW.GLFW_KEY_UP)pitch-=step;if(key==GLFW.GLFW_KEY_DOWN)pitch+=step;pitch=Math.clamp(pitch,-89,15);return true;}if(key==GLFW.GLFW_KEY_J){NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.JOURNAL,pos,0,0,0,0));return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        updateTracking();int cx=width/2,cy=height/2,r=(int)(Math.min(width*.40,height*.39));
        for(int yy=0;yy<height;yy++){int dy=yy-cy;if(Math.abs(dy)>=r)g.fill(0,yy,width,yy+1,0xFF04080E);else{int span=(int)Math.sqrt(r*r-dy*dy);g.fill(0,yy,cx-span,yy+1,0xFF04080E);g.fill(cx+span,yy,width,yy+1,0xFF04080E);g.fill(cx-span-1,yy,cx-span+1,yy+1,0xFF4F6673);g.fill(cx+span-1,yy,cx+span+1,yy+1,0xFF4F6673);}}
        int color=0xFF709FAA;g.fill(cx-14,cy,cx-5,cy+1,color);g.fill(cx+5,cy,cx+14,cy+1,color);g.fill(cx,cy-14,cx+1,cy-5,color);g.fill(cx,cy+5,cx+1,cy+14,color);
        g.drawString(font,naked?"SKY OBSERVATION":"OPTICAL OBSERVATION",12,12,0x9EB5C4,false);
        g.drawString(font,String.format(Locale.ROOT,"AZ %06.1f°   ALT %+05.1f°",Math.floorMod((int)(yaw*10+1800),3600)/10.0,-pitch),12,26,0xDAE7EE,false);
        if(!naked){g.drawString(font,String.format(Locale.ROOT,"%.0f×   FOCUS %.0f%%"+(tracking>=0?"   TRACKING":""),mag,focus*100),12,40,0xDAE7EE,false);g.fill(12,53,112,56,0xFF243442);g.fill(12,53,12+(int)(focus*100),56,0xFF73BED1);}
        var state=AstronomyClient.state;var target=state==null || AstronomyClient.catalog==null?null:AstronomyClient.catalog.target(state.target());
        var entry=target==null?null:AstronomyClient.journal.get(target.id());String name=target==null?"No target":entry==null?"Unidentified "+target.kind().name().toLowerCase(Locale.ROOT):target.name();
        g.drawCenteredString(font,name,cx,height-32,0xE3EFF4);
        if(state!=null){String line=state.message().isEmpty()?target==null?"":String.format(Locale.ROOT,"QUALITY %.0f%%   EXPOSURE %.1fs / 12s",state.quality()*100,state.exposure()/20.0):state.message();g.drawCenteredString(font,line,cx,height-18,0x80BBC7);}
        g.drawString(font,naked?"Drag aim · Arrow keys fine slew":"Drag aim · Wheel focus · Shift-wheel zoom"+(!naked && minecraft.level.getBlockEntity(pos) instanceof TelescopeBlockEntity b && b.large()?" · T track":""),12,height-58,0x8DA5B3,false);g.drawString(font,"J Journal   Esc Exit",width-font.width("J Journal   Esc Exit")-12,12,0x8DA5B3,false);
    }
    @Override public void removed(){minecraft.options.hideGui=oldHud;NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.CLOSE,pos,0,0,0,0));}
}
