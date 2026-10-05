package io.github.jakediscord.hobbymod.astronomy.space.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.astronomy.*;
import io.github.jakediscord.hobbymod.astronomy.client.AstronomyClient;
import io.github.jakediscord.hobbymod.astronomy.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;

public final class FlightClient {
    private static boolean navigationDown;
    private static int pendingOpen;
    public static void init(){
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,FlightNetworking.Open.TYPE,FlightNetworking.Open.CODEC,(p,c)->c.queue(()->{pendingOpen=20;}));
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(mc->{
            if(pendingOpen>0){pendingOpen--;if(mc.player!=null && mc.player.getVehicle() instanceof PrototypeShip && mc.screen==null){mc.setScreen(new NavigationScreen());pendingOpen=0;}}
            if(mc.player==null || !(mc.player.getVehicle() instanceof PrototypeShip ship)){navigationDown=false;return;}
            boolean active=mc.screen==null && mc.isWindowActive();boolean n=active && GLFW.glfwGetKey(mc.getWindow().getWindow(),GLFW.GLFW_KEY_N)==GLFW.GLFW_PRESS;
            if(n && !navigationDown)NetworkManager.sendToServer(new FlightNetworking.Destination(ship.getId(),-2));navigationDown=n;
            if(mc.level.getGameTime()%3!=0)return;
            int forward=active?(mc.options.keyUp.isDown()?1:mc.options.keyDown.isDown()?-1:0):0;
            int strafe=active?(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0):0;
            int vertical=active?(mc.options.keyJump.isDown()?1:0)-(GLFW.glfwGetKey(mc.getWindow().getWindow(),GLFW.GLFW_KEY_C)==GLFW.GLFW_PRESS?1:0):0;
            NetworkManager.sendToServer(new FlightNetworking.Input(ship.getId(),mc.player.getYRot(),mc.player.getXRot(),forward,strafe,vertical));
        });
    }
    public static void hud(GuiGraphics g){
        var mc=Minecraft.getInstance();if(mc.player==null || mc.screen!=null || mc.options.hideGui || !(mc.player.getVehicle() instanceof PrototypeShip ship))return;
        int w=g.guiWidth(),h=g.guiHeight();var dimension=mc.level.dimension().location().toString();boolean transfer=dimension.equals(SolarMap.TRANSFER);
        g.fill(8,8,Math.min(w-8,285),77,0xD0102130);g.fill(8,8,10,77,0xFF83DFE4);
        String name=SolarMap.name(ship.destination());double speed=ship.getDeltaMovement().length()*20;
        g.drawString(mc.font,"FLIGHT / "+(transfer?"TRANSFER":dimension.equals("minecraft:overworld")?"EARTH":SolarMap.name(SolarMap.body(dimension)).toUpperCase(Locale.ROOT)),16,15,0xBEEAEF,false);
        g.drawString(mc.font,String.format(Locale.ROOT,"%s   %.0f m/s   ALT %.0f",name,speed,ship.getY()),16,29,0xE7F0F3,false);
        String status;
        if(transfer){var target=FlightTravel.point(ship.destination());var delta=target.subtract(ship.position());double distance=delta.length();double az=(Math.toDegrees(Math.atan2(-delta.x,delta.z))+360)%360,alt=Math.toDegrees(Math.asin(delta.y/Math.max(.001,distance)));
            g.drawString(mc.font,String.format(Locale.ROOT,"%.0f m   AZ %.0f°   ALT %+.0f°",distance,az,alt),16,43,0x9CC2CD,false);
            status=distance<=100?(speed<15?"SPACE: "+(ship.destination()==-1?"return to Earth":"descend / dock"):"S: brake for arrival"):"Follow the course marker";
            double aim=SkyCatalog.aim(mc.player.getYRot(),mc.player.getXRot()).dot(new SkyCatalog.Vector(delta.x,delta.y,delta.z).unit());
            double side=-delta.dot(new net.minecraft.world.phys.Vec3(Math.cos(Math.toRadians(mc.player.getYRot())),0,Math.sin(Math.toRadians(mc.player.getYRot()))));
            int mx=(int)Math.clamp(w/2.0+side/Math.max(1,distance)*w*.6,18,w-18),my=(int)Math.clamp(h/2.0-(alt+mc.player.getXRot())*h/90,90,h-55);
            if(aim<0){mx=side<0?18:w-18;my=h/2;}
            g.drawCenteredString(mc.font,aim<0?(side<0?"◀ ":"▶ ")+name:"◇ "+name,mx,my,0x83DFE4);
        }else {g.drawString(mc.font,"Launch altitude "+(mc.level.getMaxBuildHeight()+13)+" m",16,43,0x9CC2CD,false);status=ship.onGround()?"SPACE: lift off · Shift: leave":"SPACE rise · C descend";}
        g.drawString(mc.font,status,16,60,0x9EE9C8,false);
        g.fill(8,h-50,w-8,h-35,0xB0102130);g.drawCenteredString(mc.font,"W thrust · S brake · A/D strafe · Space/C altitude · N course",w/2,h-46,0xC0D4DE);
    }
    private FlightClient(){}
}
