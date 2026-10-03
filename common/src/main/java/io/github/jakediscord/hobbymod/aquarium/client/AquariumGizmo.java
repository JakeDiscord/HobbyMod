package io.github.jakediscord.hobbymod.aquarium.client;

import io.github.jakediscord.hobbymod.aquarium.*;
import io.github.jakediscord.hobbymod.sculpting.client.GameRendererAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Camera-projected 3D handles anchored to the selected piece, drawn over glass. */
final class AquariumGizmo {
    record Point(double x,double y) {}
    record Handle(int axis,boolean scale,Vec3 direction,Vec3 center,Vec3 end) {}
    static AquariumScape.Piece piece(){var t=AquariumOrbit.tank();return t==null || AquariumOrbit.selected==null?null:t.data.scape.pieces().stream().filter(p->p.id().equals(AquariumOrbit.selected)).findFirst().orElse(null);}
    static Point project(Vec3 local,float partial){
        var mc=Minecraft.getInstance();var t=AquariumOrbit.tank();if(t==null)return null;
        var camera=mc.gameRenderer.getMainCamera();var p=t.toWorld(local).subtract(camera.getPosition());
        var v=new Vector3f((float)p.x,(float)p.y,(float)p.z).rotate(new org.joml.Quaternionf(camera.rotation()).conjugate());
        if(v.z>=-.01)return null;
        double tan=Math.tan(Math.toRadians(((GameRendererAccess)mc.gameRenderer).hobby$getFov(camera,partial,true))/2);
        double aspect=mc.getWindow().getWidth()/(double)mc.getWindow().getHeight();
        return new Point((1+v.x/(-v.z*tan*aspect))*mc.getWindow().getGuiScaledWidth()/2.0,
                (1-v.y/(-v.z*tan))*mc.getWindow().getGuiScaledHeight()/2.0);
    }
    static java.util.List<Handle> handles(){
        var p=piece();var t=AquariumOrbit.tank();if(p==null || t==null)return java.util.List.of();
        var b=AquariumScape.bounds(p,t.data.size);var center=new Vec3((b.x()+b.X())/2,(b.y()+b.Y())/2,(b.z()+b.Z())/2);
        double angle=p.rotation()*Math.PI/2,c=Math.cos(angle),s=Math.sin(angle);
        Vec3[] axes={new Vec3(c,0,-s),new Vec3(0,1,0),new Vec3(s,0,c)};
        double[] ext={p.rotation()%2==0?b.X()-b.x():b.Z()-b.z(),b.Y()-b.y(),p.rotation()%2==0?b.Z()-b.z():b.X()-b.x()};
        var result=new java.util.ArrayList<Handle>();
        for(int axis=0;axis<3;axis++){
            double length=ext[axis]/2+.22;
            result.add(new Handle(axis,false,axes[axis],center,center.add(axes[axis].scale(length))));
            result.add(new Handle(axis,true,axes[axis].scale(-1),center,center.subtract(axes[axis].scale(length))));
        }
        return result;
    }
    static Handle pick(double x,double y,float partial){
        Handle found=null;double best=100;
        for(var h:handles()){var p=project(h.end,partial);if(p==null)continue;double distance=(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y);if(distance<best){best=distance;found=h;}}
        return found;
    }
    static void draw(GuiGraphics g,double mouseX,double mouseY,float partial,Handle active){
        var hovered=pick(mouseX,mouseY,partial);
        for(var h:handles()){
            var start=project(h.center,partial);var end=project(h.end,partial);if(start==null || end==null)continue;
            int color=h.axis==0?0xFFFF6262:h.axis==1?0xFF6CE989:0xFF669DFF;
            if((active!=null && active.axis==h.axis && active.scale==h.scale) || h.equals(hovered))color=0xFFFFE69B;
            line(g,start,end,color,h.scale?1:2);
            if(h.scale){
                double r=Math.clamp(AquariumOrbit.distance*.018,.035,.14);var corners=new Point[8];
                for(int i=0;i<8;i++)corners[i]=project(h.end.add((i&1)==0?-r:r,(i&2)==0?-r:r,(i&4)==0?-r:r),partial);
                var camera=AquariumOrbit.tank().toLocal(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
                int[][] faces={camera.x>h.end.x?new int[]{1,3,7,5}:new int[]{0,4,6,2},
                        camera.y>h.end.y?new int[]{2,6,7,3}:new int[]{0,1,5,4},
                        camera.z>h.end.z?new int[]{4,5,7,6}:new int[]{0,2,3,1}};
                for(int face=0;face<3;face++){
                    Point[] polygon=new Point[4];boolean visible=true;for(int i=0;i<4;i++){polygon[i]=corners[faces[face][i]];visible&=polygon[i]!=null;}
                    if(visible)fill(g,polygon,shade(color,face==0?.7:face==1?1:.85));
                }
                for(int i=0;i<8;i++)for(int bit=1;bit<=4;bit*=2)if((i&bit)==0 && corners[i]!=null && corners[i|bit]!=null)line(g,corners[i],corners[i|bit],color,1);

            }else{
                double dx=end.x-start.x,dy=end.y-start.y,n=Math.hypot(dx,dy);if(n<1)continue;dx/=n;dy/=n;
                line(g,end,new Point(end.x-dx*8+dy*4,end.y-dy*8-dx*4),color,2);
                line(g,end,new Point(end.x-dx*8-dy*4,end.y-dy*8+dx*4),color,2);
            }
            g.drawString(Minecraft.getInstance().font,"XYZ".substring(h.axis,h.axis+1),(int)end.x+7,(int)end.y-4,color,false);
        }
        if(hovered!=null)g.drawString(Minecraft.getInstance().font,(hovered.scale?"Scale ":"Move ")+"XYZ".charAt(hovered.axis),(int)mouseX+12,(int)mouseY+10,0xFFFFFFFF);
    }
    private static int shade(int color,double factor){return 0xFF000000|((int)(((color>>16)&255)*factor)<<16)|((int)(((color>>8)&255)*factor)<<8)|(int)((color&255)*factor);}
    private static void fill(GuiGraphics g,Point[] polygon,int color){
        int low=(int)Math.floor(java.util.Arrays.stream(polygon).mapToDouble(Point::y).min().orElse(0));
        int high=(int)Math.ceil(java.util.Arrays.stream(polygon).mapToDouble(Point::y).max().orElse(0));
        if(high-low>200)return;
        for(int y=low;y<high;y++){
            double left=Double.POSITIVE_INFINITY,right=Double.NEGATIVE_INFINITY;
            for(int i=0;i<4;i++){var a=polygon[i];var b=polygon[(i+1)%4];double row=y+.5;
                if((a.y<=row && b.y>row)||(b.y<=row && a.y>row)){double x=a.x+(row-a.y)*(b.x-a.x)/(b.y-a.y);left=Math.min(left,x);right=Math.max(right,x);}}
            if(left<=right)g.fill((int)Math.floor(left),y,(int)Math.ceil(right),y+1,color);
        }
    }
    private static void line(GuiGraphics g,Point a,Point b,int color,int width){
        int steps=(int)Math.ceil(Math.max(Math.abs(b.x-a.x),Math.abs(b.y-a.y)));if(steps>2000)return;
        for(int i=0;i<=steps;i++){double t=steps==0?0:i/(double)steps;int x=(int)Math.round(a.x+(b.x-a.x)*t),y=(int)Math.round(a.y+(b.y-a.y)*t);g.fill(x,y,x+width,y+width,color);}
    }
    private AquariumGizmo(){}
}
