package io.github.jakediscord.hobbymod.aquarium.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.aquarium.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Direct in-world selection, movement and independent axis stretching. */
public final class AquariumEditScreen extends Screen {
    private boolean stretch,dragging,orbiting;
    private int axis=-1;
    private float partial;
    private AquariumScape.Piece original;
    private Vec3 start;
    private String notice="";
    public AquariumEditScreen(){super(Component.literal("Aquarium 3D editor"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    public void feedback(String text){notice=text;}
    private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        button("Back to care",8,8,100,this::onClose);
        button("Move",8,34,48,()->{stretch=false;axis=-1;});button("Stretch",60,34,48,()->{stretch=true;if(axis<0)axis=0;});
        for(int i=0;i<3;i++){int n=i;button("XYZ".substring(i,i+1),8+i*35,60,30,()->axis=n);}
        button("Rotate 90°",8,86,100,()->{var p=piece();if(p!=null){change(p,p.x(),p.y(),p.z(),(p.rotation()+1)%4,p.scaleX(),p.scaleY(),p.scaleZ());send();}});
        button("Reset size",8,112,100,()->{var p=piece();if(p!=null){change(p,p.x(),p.y(),p.z(),p.rotation(),1,1,1);send();}});
    }
    private AquariumScape.Piece piece(){var t=AquariumOrbit.tank();if(t==null || AquariumOrbit.selected==null)return null;return t.data.scape.pieces().stream().filter(p->p.id().equals(AquariumOrbit.selected)).findFirst().orElse(null);}
    private void change(AquariumScape.Piece p,double x,double y,double z,int r,double sx,double sy,double sz){
        var t=AquariumOrbit.tank();if(t==null)return;
        t.data.scape.transform(p.id(),t.data.size,Math.clamp(x,.08,.92),Math.clamp(y,0,.92),Math.clamp(z,.08,.92),r,Math.clamp(sx,.15,4),Math.clamp(sy,.15,4),Math.clamp(sz,.15,4));
    }
    private void send(){var p=piece();var t=AquariumOrbit.tank();if(p==null || t==null)return;NetworkManager.sendToServer(new AquariumNetworking.Transform(t.getBlockPos(),p.id(),p.x(),p.y(),p.z(),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ()));notice="Saved with aquarium";}
    @Override public void tick(){
        var t=AquariumOrbit.tank();
        if(t==null || minecraft.player==null || !minecraft.player.isAlive() || minecraft.player.hurtTime>0 || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(minecraft.player,t.getBlockPos())){AquariumOrbit.close();minecraft.setScreen(null);}
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        this.partial=partial;g.fill(4,4,112,140,0xAA15242A);super.render(g,x,y,partial);
        g.drawString(font,(stretch?"Stretch ":"Move ")+(axis<0?"XZ":"XYZ".charAt(axis)),8,147,0xFFFFFF);
        var p=piece();if(p!=null){g.drawString(font,AquariumControllerBlock.decorItem(p).getDescription().getString(),8,162,0xFFE6A0);g.drawString(font,String.format(java.util.Locale.ROOT,"Size %.2f / %.2f / %.2f",p.scaleX(),p.scaleY(),p.scaleZ()),8,176,0xFFFFFF);}
        g.drawString(font,"Click piece; left-drag to edit",8,height-43,0xFFFFFF);
        g.drawString(font,"Right-drag orbit · Wheel zoom · Shift-drag height",8,height-29,0xFFFFFF);
        if(!notice.isEmpty())g.drawString(font,notice,8,height-15,0xFFE6A0);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;
        orbiting=button==1 || button==2;
        if(button==0){AquariumOrbit.selected=AquariumOrbit.pick(x,y,partial);original=piece();dragging=original!=null;
            if(dragging){var t=AquariumOrbit.tank();var b=AquariumScape.bounds(original,t.data.size);start=AquariumOrbit.plane(x,y,partial,t.getBlockPos().getY()+b.y());}
        }
        return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(orbiting){AquariumOrbit.yaw+=(float)(dx*.4);AquariumOrbit.pitch=Math.clamp(AquariumOrbit.pitch+(float)(dy*.4),-70,80);return true;}
        var p=piece();var t=AquariumOrbit.tank();if(!dragging || p==null || t==null)return true;
        if(stretch){double factor=Math.exp((dx-dy)*.012);change(p,p.x(),p.y(),p.z(),p.rotation(),p.scaleX()*(axis==0?factor:1),p.scaleY()*(axis==1?factor:1),p.scaleZ()*(axis==2?factor:1));}
        else if(axis==1 || hasShiftDown())change(p,p.x(),p.y()-dy/(160*t.data.size.blocksHigh()),p.z(),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ());
        else{
            var b=AquariumScape.bounds(original,t.data.size);var hit=AquariumOrbit.plane(x,y,partial,t.getBlockPos().getY()+b.y());
            if(hit!=null && start!=null){var delta=hit.subtract(start);change(p,original.x()+(axis==2?0:delta.x/(t.data.size.blocksWide()-.16)),p.y(),original.z()+(axis==0?0:delta.z/(t.data.size.blocksDeep()-.16)),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ());}
        }
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(dragging)send();dragging=false;orbiting=false;original=null;return true;}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){AquariumOrbit.distance=Math.clamp(AquariumOrbit.distance*Math.pow(.9,v),1,12);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void onClose(){
        var t=AquariumOrbit.tank();if(dragging && original!=null)change(original,original.x(),original.y(),original.z(),original.rotation(),original.scaleX(),original.scaleY(),original.scaleZ());
        AquariumOrbit.close();minecraft.setScreen(t==null?null:new AquariumScreen(t.getBlockPos()));
    }
    @Override public void removed(){AquariumOrbit.close();}
}
