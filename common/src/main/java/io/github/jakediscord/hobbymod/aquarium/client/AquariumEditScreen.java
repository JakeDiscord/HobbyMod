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
    private boolean dragging,orbiting;
    private AquariumGizmo.Handle handle;
    private double startMouseX,startMouseY,screenAxisX,screenAxisY;
    private float partial;
    private AquariumScape.Piece original;
    private Vec3 start;
    private String notice="";
    private net.minecraft.client.gui.components.EditBox[] angles;
    private java.util.UUID angleSelection;
    public AquariumEditScreen(){super(Component.literal("Habitat 3D editor"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    public void feedback(String text){notice=text;}
    private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        button("Back to care",8,8,100,this::onClose);
        button("Rotate 90°",8,34,100,()->{var p=piece();if(p!=null){change(p,p.x(),p.y(),p.z(),(p.rotation()+1)%4,p.scaleX(),p.scaleY(),p.scaleZ());send();angleSelection=null;}});
        button("Reset size",8,60,100,()->{var p=piece();if(p!=null){change(p,p.x(),p.y(),p.z(),p.rotation(),1,1,1);send();}});
        var t=AquariumOrbit.tank();if(t!=null && t.data.terrarium!=null){
            angles=new net.minecraft.client.gui.components.EditBox[3];
            for(int i=0;i<3;i++){angles[i]=new net.minecraft.client.gui.components.EditBox(font,42,130+i*22,66,18,Component.literal(new String[]{"Yaw","Pitch","Roll"}[i]+" degrees"));angles[i].setMaxLength(24);addRenderableWidget(angles[i]);}
            angleSelection=null;
        }

    }
    private AquariumScape.Piece piece(){var t=AquariumOrbit.tank();if(t==null || AquariumOrbit.selected==null)return null;return t.data.scape.pieces().stream().filter(p->p.id().equals(AquariumOrbit.selected)).findFirst().orElse(null);}
    private void change(AquariumScape.Piece p,double x,double y,double z,int r,double sx,double sy,double sz){
        var t=AquariumOrbit.tank();if(t==null)return;
        t.data.scape.transform(p.id(),t.data.size,Math.clamp(x,.08,.92),Math.clamp(y,0,.92),Math.clamp(z,.08,.92),r,Math.clamp(sx,.15,4),Math.clamp(sy,.15,4),Math.clamp(sz,.15,4));
    }
    private void send(){var p=piece();var t=AquariumOrbit.tank();if(p==null || t==null)return;NetworkManager.sendToServer(new AquariumNetworking.Transform(t.getBlockPos(),p.id(),p.x(),p.y(),p.z(),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ()));notice="Layout saved";}
    @Override public void tick(){
        var t=AquariumOrbit.tank();
        if(t==null || minecraft.player==null || !minecraft.player.isAlive() || minecraft.player.hurtTime>0 || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(minecraft.player,t.getBlockPos())){AquariumOrbit.close();minecraft.setScreen(null);}
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        this.partial=partial;g.fill(4,4,112,84,0xAA15242A);super.render(g,x,y,partial);
        AquariumGizmo.draw(g,x,y,partial,handle);
        if(angles!=null){
            var selected=piece();if(selected!=null && !selected.id().equals(angleSelection)){
                angleSelection=selected.id();double[] values={selected.rotation()*90+selected.yaw(),selected.pitch(),selected.roll()};for(int i=0;i<3;i++)angles[i].setValue(Double.toString(values[i]));
            }
            g.fill(4,124,112,198,0xAA15242A); // Labels sit beside live numeric fields.
            for(int i=0;i<3;i++){g.drawString(font,new String[]{"Yaw","Pitch","Roll"}[i],8,135+i*22,0xFFFFFF);angles[i].render(g,x,y,partial);}
            g.drawString(font,"Rotation°",8,118,0xFFFFFF);
        }
        var p=piece();if(p!=null){g.drawString(font,AquariumControllerBlock.decorItem(p).getDescription().getString(),8,94,0xFFE6A0);g.drawString(font,String.format(java.util.Locale.ROOT,"Size %.2f / %.2f / %.2f",p.scaleX(),p.scaleY(),p.scaleZ()),8,108,0xFFFFFF);}
        g.drawString(font,angles==null?"Select piece · Drag arrows to move; cubes to scale":"Arrows move · Cubes scale · Enter applies rotation",8,height-43,0xFFFFFF);
        g.drawString(font,"Right-drag orbit · Wheel zoom",8,height-29,0xFFFFFF);
        if(!notice.isEmpty())g.drawString(font,notice,8,height-15,0xFFE6A0);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;
        orbiting=button==1 || button==2;
        if(button==0){
            handle=AquariumGizmo.pick(x,y,partial);
            if(handle==null)AquariumOrbit.selected=AquariumOrbit.pick(x,y,partial);
            original=piece();dragging=original!=null;startMouseX=x;startMouseY=y;
            if(dragging){
                var t=AquariumOrbit.tank();var b=AquariumScape.bounds(original,t.data.size);
                start=AquariumOrbit.plane(x,y,partial,t.getBlockPos().getY()+b.y());
                if(handle!=null){var a=AquariumGizmo.project(handle.end(),partial);var e=AquariumGizmo.project(handle.end().add(handle.direction()),partial);
                    if(a==null || e==null){dragging=false;handle=null;}
                    else{screenAxisX=e.x()-a.x();screenAxisY=e.y()-a.y();if(screenAxisX*screenAxisX+screenAxisY*screenAxisY<4){dragging=false;handle=null;}}
                }
            }
        }
        return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(orbiting){AquariumOrbit.yaw+=(float)(dx*.4);AquariumOrbit.pitch=Math.clamp(AquariumOrbit.pitch+(float)(dy*.4),-70,80);return true;}
        var p=piece();var t=AquariumOrbit.tank();if(!dragging || p==null || t==null)return true;
        if(handle!=null){
            double delta=((x-startMouseX)*screenAxisX+(y-startMouseY)*screenAxisY)/(screenAxisX*screenAxisX+screenAxisY*screenAxisY);
            if(handle.scale()){
                double factor=Math.exp(delta*2);
                change(p,original.x(),original.y(),original.z(),original.rotation(),original.scaleX()*(handle.axis()==0?factor:1),original.scaleY()*(handle.axis()==1?factor:1),original.scaleZ()*(handle.axis()==2?factor:1));
            }else{
                var v=handle.direction().scale(delta);var s=t.data.size;
                change(p,original.x()+v.x/(s.blocksWide()-.16),original.y()+v.y/(s.blocksHigh()*.88-.24),original.z()+v.z/(s.blocksDeep()-.16),original.rotation(),original.scaleX(),original.scaleY(),original.scaleZ());
            }
        }else if(hasShiftDown())change(p,p.x(),p.y()-dy/(160*t.data.size.blocksHigh()),p.z(),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ());
        else{
            var b=AquariumScape.bounds(original,t.data.size);var hit=AquariumOrbit.plane(x,y,partial,t.getBlockPos().getY()+b.y());
            if(hit!=null && start!=null){var delta=hit.subtract(start);change(p,original.x()+delta.x/(t.data.size.blocksWide()-.16),p.y(),original.z()+delta.z/(t.data.size.blocksDeep()-.16),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ());}
        }
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(dragging)send();dragging=false;orbiting=false;original=null;handle=null;return true;}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){AquariumOrbit.distance=Math.clamp(AquariumOrbit.distance*Math.pow(.9,v),1,12);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if((key==GLFW.GLFW_KEY_ENTER || key==GLFW.GLFW_KEY_KP_ENTER) && angles!=null){
            var p=piece();var t=AquariumOrbit.tank();if(p==null || t==null)return true;
            try{double yaw=Double.parseDouble(angles[0].getValue()),pitch=Double.parseDouble(angles[1].getValue()),roll=Double.parseDouble(angles[2].getValue());
                if(t.data.scape.angles(p.id(),t.data.size,yaw,pitch,roll)){NetworkManager.sendToServer(new AquariumNetworking.Angles(t.getBlockPos(),p.id(),yaw,pitch,roll));notice="Rotation saved";}else notice="Use finite degrees between -360 and 360.";
            }catch(NumberFormatException e){notice="Enter a number for each rotation axis.";}return true;
        }
        if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void onClose(){
        var t=AquariumOrbit.tank();if(dragging && original!=null)change(original,original.x(),original.y(),original.z(),original.rotation(),original.scaleX(),original.scaleY(),original.scaleZ());
        AquariumOrbit.close();minecraft.setScreen(t==null?null:AquariumScreen.care(t.getBlockPos()));
    }
    @Override public void removed(){AquariumOrbit.close();}
}
