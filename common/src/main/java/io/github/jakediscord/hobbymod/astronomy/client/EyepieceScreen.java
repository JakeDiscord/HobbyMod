package io.github.jakediscord.hobbymod.astronomy.client;

import io.github.jakediscord.hobbymod.astronomy.*;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import java.util.*;

/** Optical controls belong to equipment; the journal is only a record. */
public final class EyepieceScreen extends Screen {
    public final BlockPos pos;
    public final boolean naked;
    public float yaw,pitch,focus=.5f,mag=8;
    private int tracking=-1,category,page,panel;
    private long sent=Long.MIN_VALUE;
    private final boolean oldHud;
    private final List<SkyCatalog.Object> choices=new ArrayList<>();
    public EyepieceScreen(BlockPos pos,boolean naked){
        super(Component.literal("Telescope"));this.pos=pos;this.naked=naked;
        var mc=net.minecraft.client.Minecraft.getInstance();oldHud=mc.options.hideGui;
        if(mc.player!=null){yaw=mc.player.getYRot();pitch=mc.player.getXRot();}
        if(!naked && mc.level!=null && mc.level.getBlockEntity(pos) instanceof TelescopeBlockEntity b){yaw=b.yaw;pitch=b.pitch;focus=b.focus;mag=b.magnification;}
        if(naked){mag=1;focus=.72f;}
    }
    private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,18).build());}
    private boolean large(){return minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof TelescopeBlockEntity b && b.large();}
    private int rows(){return Math.max(2,(height-165)/18);}
    @Override protected void init(){
        minecraft.options.hideGui=true;panel=width-138;
        button("Planets",panel+6,30,43,()->{category=0;page=0;});
        button("Stars",panel+51,30,37,()->{category=1;page=0;});
        button("Deep",panel+90,30,42,()->{category=2;page=0;});
        button("<",panel+8,height-98,24,()->page=Math.max(0,page-1));
        button(">",panel+34,height-98,24,()->page=Math.min(Math.max(0,(choices.size()-1)/rows()),page+1));
        button("−",panel+8,height-31,22,()->zoom(-1));
        button("+",panel+32,height-31,22,()->zoom(1));
        button("Journal",panel+64,height-31,67,()->NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.JOURNAL,pos,0,0,0,0)));
    }
    private void zoom(double amount){mag=Math.clamp((float)(mag*Math.pow(1.25,amount)),large()?12:6,large()?100:40);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    public void updateTracking(){if(tracking<0 || AstronomyClient.catalog==null)return;point(AstronomyClient.catalog.target(tracking));}
    private void point(SkyCatalog.Object target){
        if(target==null)return;var d=target.direction(AstronomyClient.time(minecraft.getTimer().getGameTimeDeltaPartialTick(false)));
        yaw=(float)Math.toDegrees(Math.atan2(-d.x(),d.z()));pitch=(float)-Math.toDegrees(Math.asin(d.y()));
    }
    private void gamepad(){
        if(!minecraft.isWindowActive())return;
        try(var stack=org.lwjgl.system.MemoryStack.stackPush()){
            var pad=GLFWGamepadState.malloc(stack);
            for(int joystick=GLFW.GLFW_JOYSTICK_1;joystick<=GLFW.GLFW_JOYSTICK_LAST;joystick++){
                if(!GLFW.glfwJoystickIsGamepad(joystick) || !GLFW.glfwGetGamepadState(joystick,pad))continue;
                float x=deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X))*.9f+deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X))*.12f;
                float y=deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y))*.9f+deadzone(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y))*.12f;
                if(x!=0 || y!=0){tracking=-1;yaw=net.minecraft.util.Mth.wrapDegrees(yaw+x*8/mag);pitch=Math.clamp(pitch+y*8/mag,-89,15);}
                focus=Math.clamp(focus+(pad.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)-pad.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER))*.004f,0,1);break;
            }
        }
    }
    private static float deadzone(float value){return Math.abs(value)<.15f?0:value;}
    public void heartbeat(){
        gamepad();updateTracking();if(minecraft.level==null || minecraft.player==null)return;
        if(!(minecraft.level.getBlockEntity(pos) instanceof TelescopeBlockEntity) || minecraft.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>36){onClose();return;}
        long now=minecraft.level.getGameTime();if(sent==Long.MIN_VALUE || now-sent>=5 || now<sent){sent=now;NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.AIM,pos,yaw,pitch,focus,mag));}
    }
    private boolean focusAt(double x,double y){if(x<panel+10 || x>width-10 || y<height-65 || y>height-51)return false;focus=Math.clamp((float)(x-panel-10)/118,0,1);return true;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;
        if(button!=0)return false;if(focusAt(x,y))return true;
        if(x>=panel+6 && x<width-6 && y>=56 && y<56+rows()*18){int i=page*rows()+(int)(y-56)/18;if(i<choices.size()){tracking=-1;var target=choices.get(i);mag=target.kind()==SkyCatalog.Kind.PLANET?18:target.deep()?24:32;point(target);return true;}}
        return x<panel;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button!=0)return false;if(focusAt(x,y))return true;if(x>=panel)return false;
        tracking=-1;yaw=net.minecraft.util.Mth.wrapDegrees(yaw+(float)(dx*(hasShiftDown()?.012:.12)*8/mag));pitch=Math.clamp(pitch+(float)(dy*(hasShiftDown()?.012:.12)*8/mag),-89,15);return true;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(hasShiftDown())zoom(vertical);else focus=Math.clamp(focus+(float)vertical*.025f,0,1);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_T){if(large()){var near=AstronomyClient.catalog==null?null:AstronomyClient.catalog.nearest(SkyCatalog.aim(yaw,pitch),AstronomyClient.time(0),Math.max(.3,35/mag));tracking=tracking>=0?-1:near==null?-1:near.id();}return true;}
        if(key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_RIGHT || key==GLFW.GLFW_KEY_UP || key==GLFW.GLFW_KEY_DOWN){tracking=-1;float step=(hasShiftDown()?.02f:.15f)*8/mag;if(key==GLFW.GLFW_KEY_LEFT)yaw-=step;if(key==GLFW.GLFW_KEY_RIGHT)yaw+=step;if(key==GLFW.GLFW_KEY_UP)pitch-=step;if(key==GLFW.GLFW_KEY_DOWN)pitch+=step;pitch=Math.clamp(pitch,-89,15);return true;}
        if(key==GLFW.GLFW_KEY_J){NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.JOURNAL,pos,0,0,0,0));return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        updateTracking();int cx=panel/2,cy=height/2-5,r=Math.max(35,Math.min(panel/2-12,(height-88)/2));
        for(int yy=0;yy<height;yy++){int dy=yy-cy,span=Math.abs(dy)>=r?0:(int)Math.sqrt(r*r-dy*dy);g.fill(0,yy,cx-span,yy+1,0xFF0A111B);g.fill(cx+span,yy,width,yy+1,0xFF0A111B);if(span>0){g.fill(cx-span-2,yy,cx-span,yy+1,0xFF395B70);g.fill(cx+span,yy,cx+span+2,yy+1,0xFF395B70);}}
        g.fill(panel,0,width,height,0xFF142331);g.fill(panel,0,panel+1,height,0xFF416174);
        int color=0xFF93D5D8;g.fill(cx-12,cy,cx-5,cy+1,color);g.fill(cx+5,cy,cx+12,cy+1,color);g.fill(cx,cy-12,cx+1,cy-5,color);g.fill(cx,cy+5,cx+1,cy+12,color);
        g.drawString(font,large()?"OBSERVATORY / 180 mm":"TELESCOPE / 80 mm",10,9,0xD7E9F1,false);
        g.drawString(font,String.format(Locale.ROOT,"%.0f×   AZ %.1f°   ALT %+.1f°",mag,(yaw+540)%360,-pitch),10,23,0x86ACBD,false);
        g.drawString(font,"TARGET FINDER",panel+8,10,0xA4DCE1,false);
        choices.clear();double time=AstronomyClient.time(partial);
        if(AstronomyClient.catalog!=null)for(var t:AstronomyClient.catalog.targets)if(t.direction(time).y()>.08 && (category==0?t.kind()==SkyCatalog.Kind.PLANET:category==1?t.kind()==SkyCatalog.Kind.STAR:t.deep()))choices.add(t);
        page=Math.min(page,Math.max(0,(choices.size()-1)/rows()));
        for(int i=0;i<rows() && page*rows()+i<choices.size();i++){
            var t=choices.get(page*rows()+i);int yy=56+i*18;boolean hover=x>=panel+6 && x<width-6 && y>=yy && y<yy+18;
            g.fill(panel+6,yy,width-6,yy+17,hover?0xFF315467:0xFF1D3444);
            String name=t.deep() && AstronomyClient.journal.get(t.id())==null?"Uncharted "+t.kind().name().toLowerCase(Locale.ROOT):t.name();
            g.drawString(font,font.plainSubstrByWidth(name,111),panel+10,yy+4,0xD3E7EF,false);
            if(hover)g.renderTooltip(font,Component.literal("Point at "+name),x,y);
        }
        if(choices.isEmpty())g.drawString(font,"None above horizon",panel+8,60,0x88A3B6,false);
        g.drawString(font,(page+1)+" / "+Math.max(1,(choices.size()+rows()-1)/rows()),panel+68,height-93,0x86ACBD,false);
        g.drawString(font,"Focus "+Math.round(focus*100)+"% / "+(Math.abs(focus-.72)<.06?"Sharp":"Soft"),panel+10,height-77,0xD7E9F1,false);
        g.fill(panel+10,height-62,width-10,height-57,0xFF344C5C);int marker=panel+10+(int)(focus*118);g.fill(marker-2,height-65,marker+2,height-54,0xFF8BD8D4);
        g.drawString(font,"Scroll focus · Shift zoom",panel+8,height-46,0x86ACBD,false);
        var state=AstronomyClient.state;var target=state==null || AstronomyClient.catalog==null?null:AstronomyClient.catalog.target(state.target());var entry=target==null?null:AstronomyClient.journal.get(target.id());
        String name=target==null?"Choose a target →":entry==null?"Unidentified "+target.kind().name().toLowerCase(Locale.ROOT):target.name();
        g.drawCenteredString(font,name,cx,height-43,0xE4F1F6);
        String status=state==null?"":state.message().isEmpty()?target==null?"Drag to aim":String.format(Locale.ROOT,"Hold steady · %.1f / 12s · Quality %.0f%%",state.exposure()/20.0,state.quality()*100):state.message();
        g.drawCenteredString(font,font.plainSubstrByWidth(status,panel-16),cx,height-28,0x96D2D0);
        g.fill(12,height-15,panel-12,height-12,0xFF263C4B);if(state!=null)g.fill(12,height-15,12+(int)((panel-24)*Math.clamp(state.exposure()/240.0,0,1)),height-12,0xFF8BD8D4);
        g.drawString(font,tracking>=0?"Tracking on · T to release":large()?"Drag aim · T track · Esc exit":"Drag aim · Esc exit",10,36,0x86ACBD,false);
        super.render(g,x,y,partial);
    }
    @Override public void removed(){minecraft.options.hideGui=oldHud;NetworkManager.sendToServer(new AstronomyNetworking.Action(AstronomyNetworking.CLOSE,pos,0,0,0,0));}
}
