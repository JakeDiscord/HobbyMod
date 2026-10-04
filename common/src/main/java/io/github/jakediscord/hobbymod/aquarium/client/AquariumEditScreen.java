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
    private boolean terrainMode,painting;
    private Button resetRotation,terrainToggle;
    private int brushMode;
    private double brushRadius=.28,mouseX,mouseY,rotationDelta;
    private Vec3 rotationLast;
    private long lastStamp;
    private boolean strokeSet;
    private double strokeX,strokeZ,strokeMouseX,strokeMouseY;
    private final java.util.List<Button> terrainButtons=new java.util.ArrayList<>();
    public AquariumEditScreen(){super(Component.literal("Habitat 3D editor"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    public void feedback(String text){notice=text;}
    private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        button("Back to care",8,8,100,this::onClose);
        button("Reset size",8,34,100,()->{var p=piece();if(p!=null){change(p,p.x(),p.y(),p.z(),p.rotation(),1,1,1);send();}});
        terrainButtons.clear();var t=AquariumOrbit.tank();if(t==null || t.data.terrarium==null){button("Reset rotation",8,86,100,()->{var p=piece();if(p!=null){change(p,p.x(),p.y(),p.z(),0,p.scaleX(),p.scaleY(),p.scaleZ());send();notice="Rotation reset";}});button("Rotate 90°",8,60,100,()->{var p=piece();if(p!=null){change(p,p.x(),p.y(),p.z(),(p.rotation()+1)%4,p.scaleX(),p.scaleY(),p.scaleZ());send();}});}
        if(t!=null && t.data.terrarium!=null){
            resetRotation=addRenderableWidget(Button.builder(Component.literal("Reset rotation"),b->{var p=piece();var tank=AquariumOrbit.tank();if(p!=null && tank!=null){tank.data.scape.angles(p.id(),tank.data.size,0,0,0);io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(tank.data);NetworkManager.sendToServer(new AquariumNetworking.Angles(tank.getBlockPos(),p.id(),0,0,0));notice="Rotation reset";}}).bounds(8,60,100,20).build());
            terrainToggle=addRenderableWidget(Button.builder(Component.literal("Edit terrain"),b->{terrainMode=!terrainMode;AquariumOrbit.selected=null;b.setMessage(Component.literal(terrainMode?"Edit objects":"Edit terrain"));}).bounds(8,86,100,20).build());
            String[] names={"Raise","Lower","Smooth"};for(int i=0;i<3;i++){int mode=i;var b=addRenderableWidget(Button.builder(Component.literal(names[i]),v->{brushMode=mode;}).bounds(8,100+i*24,100,20).build());terrainButtons.add(b);}
        }

    }
    private AquariumScape.Piece piece(){var t=AquariumOrbit.tank();if(t==null || AquariumOrbit.selected==null)return null;return t.data.scape.pieces().stream().filter(p->p.id().equals(AquariumOrbit.selected)).findFirst().orElse(null);}
    private void change(AquariumScape.Piece p,double x,double y,double z,int r,double sx,double sy,double sz){
        var t=AquariumOrbit.tank();if(t==null)return;
        t.data.scape.transform(p.id(),t.data.size,Math.clamp(x,.08,.92),Math.clamp(y,t.data.terrarium==null?0:-.45,.92),Math.clamp(z,.08,.92),r,Math.clamp(sx,.15,4),Math.clamp(sy,.15,4),Math.clamp(sz,.15,4));
        io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(t.data);
    }
    private void send(){var p=piece();var t=AquariumOrbit.tank();if(p==null || t==null)return;NetworkManager.sendToServer(new AquariumNetworking.Transform(t.getBlockPos(),p.id(),p.x(),p.y(),p.z(),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ()));notice="Layout saved";}
    @Override public void tick(){
        if(painting)stamp(mouseX,mouseY);
        var t=AquariumOrbit.tank();
        if(t==null || minecraft.player==null || !minecraft.player.isAlive() || minecraft.player.hurtTime>0 || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(minecraft.player,t.getBlockPos())){AquariumOrbit.close();minecraft.setScreen(null);}
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        this.partial=partial;mouseX=x;mouseY=y;if(resetRotation!=null){resetRotation.visible=!terrainMode;resetRotation.active=piece()!=null;terrainToggle.setY(terrainMode?60:86);}for(var b:terrainButtons)b.visible=terrainMode;
        g.fill(4,4,112,terrainMode?176:110,0xAA15242A);super.render(g,x,y,partial);
        if(terrainMode){
            AquariumGizmo.terrainCursor(g,x,y,partial,brushRadius,painting && strokeSet?strokeX:Double.NaN,strokeZ);
            g.drawString(font,"Terrain brush",8,88,0xFFFFFF);g.drawString(font,new String[]{"Raise","Lower","Smooth"}[brushMode]+" · "+String.format(java.util.Locale.ROOT,"%.2f",brushRadius),8,176,0xFFE6A0);
        }else{
            AquariumGizmo.draw(g,x,y,partial,handle);var p=piece();if(p!=null){
                g.drawString(font,AquariumControllerBlock.decorItem(p).getDescription().getString(),8,118,0xFFE6A0);
                g.drawString(font,String.format(java.util.Locale.ROOT,"Size %.2f / %.2f / %.2f",p.scaleX(),p.scaleY(),p.scaleZ()),8,132,0xFFFFFF);
                if(AquariumOrbit.tank().data.terrarium!=null)g.drawString(font,String.format(java.util.Locale.ROOT,"Rotation %.1f / %.1f / %.1f",p.rotation()*90+p.yaw(),p.pitch(),p.roll()),8,146,0xFFFFFF);
            }
        }
        g.drawString(font,terrainMode?"Hold to sculpt · Shift lowers · Shift-wheel brush size":AquariumOrbit.tank().data.terrarium!=null?"Arrows move · Cubes scale · Curved arrows rotate":"Arrows move · Cubes scale",8,height-43,0xFFFFFF);
        g.drawString(font,"Right-drag orbit · Wheel zoom",8,height-29,0xFFFFFF);
        if(!notice.isEmpty())g.drawString(font,notice,8,height-15,0xFFE6A0);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;
        orbiting=button==1 || button==2;
        if(button==0){
            if(terrainMode){painting=true;strokeSet=false;lastStamp=0;stamp(x,y);return true;}
            handle=AquariumGizmo.pick(x,y,partial);
            if(handle==null)AquariumOrbit.selected=AquariumOrbit.pick(x,y,partial);
            original=piece();dragging=original!=null;startMouseX=x;startMouseY=y;
            if(dragging){
                var t=AquariumOrbit.tank();var b=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.bounds(original,t.data);
                start=AquariumOrbit.plane(x,y,partial,t.getBlockPos().getY()+b.y());
                if(handle!=null && handle.rotate()){
                    rotationLast=AquariumGizmo.rotationVector(handle,x,y,partial);rotationDelta=0;dragging=rotationLast!=null;
                }else if(handle!=null){var a=AquariumGizmo.project(handle.end(),partial);var e=AquariumGizmo.project(handle.end().add(handle.direction()),partial);
                    if(a==null || e==null){dragging=false;handle=null;}
                    else{screenAxisX=e.x()-a.x();screenAxisY=e.y()-a.y();if(screenAxisX*screenAxisX+screenAxisY*screenAxisY<4){dragging=false;handle=null;}}
                }
            }
        }
        return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(orbiting){AquariumOrbit.yaw+=(float)(dx*.4);AquariumOrbit.pitch=Math.clamp(AquariumOrbit.pitch+(float)(dy*.4),-70,80);return true;}
        if(painting){stamp(x,y);return true;}
        var p=piece();var t=AquariumOrbit.tank();if(!dragging || p==null || t==null)return true;
        if(handle!=null && handle.rotate()){
            var next=AquariumGizmo.rotationVector(handle,x,y,partial);if(next!=null && rotationLast!=null){
                rotationDelta+=Math.toDegrees(Math.atan2(handle.direction().dot(rotationLast.cross(next)),Math.clamp(rotationLast.dot(next),-1,1)));rotationLast=next;
                double delta=hasShiftDown()?Math.round(rotationDelta/15)*15:rotationDelta;
                t.data.scape.angles(p.id(),t.data.size,Math.IEEEremainder(original.rotation()*90+original.yaw()+(handle.axis()==1?delta:0),360),Math.IEEEremainder(original.pitch()+(handle.axis()==0?delta:0),360),Math.IEEEremainder(original.roll()+(handle.axis()==2?delta:0),360));
                io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(t.data);
            }
        }else if(handle!=null){
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
            var b=io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.bounds(original,t.data);var hit=AquariumOrbit.plane(x,y,partial,t.getBlockPos().getY()+b.y());
            if(hit!=null && start!=null){var delta=hit.subtract(start);change(p,original.x()+delta.x/(t.data.size.blocksWide()-.16),p.y(),original.z()+delta.z/(t.data.size.blocksDeep()-.16),p.rotation(),p.scaleX(),p.scaleY(),p.scaleZ());}
        }
        return true;
    }
    private void stamp(double x,double y){
        if(net.minecraft.Util.getMillis()-lastStamp<110)return;lastStamp=net.minecraft.Util.getMillis();var t=AquariumOrbit.tank();var hit=AquariumOrbit.terrain(x,y,partial);
        if(t==null)return;
        boolean anchored=strokeSet && Math.hypot(x-strokeMouseX,y-strokeMouseY)<1;
        if(!anchored && hit==null){notice="Point at the substrate to sculpt.";return;}
        double px=anchored?strokeX:Math.clamp((hit.x-.08)/(t.data.size.blocksWide()-.16),0,1),pz=anchored?strokeZ:Math.clamp((hit.z-.08)/(t.data.size.blocksDeep()-.16),0,1);int mode=hasShiftDown()?1:brushMode;
        strokeSet=true;strokeX=px;strokeZ=pz;strokeMouseX=x;strokeMouseY=y;
        t.data.terrarium.terrain.brush(t.data.terrarium,px,pz,brushRadius,mode);io.github.jakediscord.hobbymod.terrarium.TerrariumTerrain.fitDecor(t.data);
        NetworkManager.sendToServer(new AquariumNetworking.Terrain(t.getBlockPos(),px,pz,brushRadius,mode));notice="Terrain saved";
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(dragging){var p=piece();var t=AquariumOrbit.tank();if(handle!=null && handle.rotate() && p!=null && t!=null){NetworkManager.sendToServer(new AquariumNetworking.Angles(t.getBlockPos(),p.id(),p.rotation()*90+p.yaw(),p.pitch(),p.roll()));notice="Rotation saved";}else send();}
        painting=false;dragging=false;orbiting=false;original=null;handle=null;return true;
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(terrainMode && hasShiftDown())brushRadius=Math.clamp(brushRadius+v*.03,.12,.5);else AquariumOrbit.distance=Math.clamp(AquariumOrbit.distance*Math.pow(.9,v),1,12);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void onClose(){
        var t=AquariumOrbit.tank();if(dragging && original!=null && t!=null){if(handle!=null && handle.rotate())t.data.scape.angles(original.id(),t.data.size,original.rotation()*90+original.yaw(),original.pitch(),original.roll());change(original,original.x(),original.y(),original.z(),handle!=null && handle.rotate()?0:original.rotation(),original.scaleX(),original.scaleY(),original.scaleZ());}
        AquariumOrbit.close();minecraft.setScreen(t==null?null:AquariumScreen.care(t.getBlockPos()));
    }
    @Override public void removed(){AquariumOrbit.close();}
}
