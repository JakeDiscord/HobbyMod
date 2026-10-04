package io.github.jakediscord.hobbymod.aquarium;

import java.util.*;

/** Item-backed decorations with stable identities and bounded 3D transforms. */
public final class AquariumScape {
    public enum Material { SEAGRASS,KELP,ROCK,WOOD,BLOCK }
    public record Piece(Material material,double x,double z,int rotation,double y,double scaleX,double scaleY,double scaleZ,UUID id,String block,double yaw,double pitch,double roll){
        public Piece(Material material,double x,double z,int rotation,double y,double sx,double sy,double sz,UUID id,String block){this(material,x,z,rotation,y,sx,sy,sz,id,block,0,0,0);}
        public Piece(Material material,double x,double z,int rotation){this(material,x,z,rotation,0,1,1,1,UUID.randomUUID(),"");}
    }
    public record Bounds(double x,double y,double z,double X,double Y,double Z){}
    private final List<Piece> pieces=new ArrayList<>();
    private List<Piece> snapshot;
    public List<Piece> pieces(){if(snapshot==null)snapshot=List.copyOf(pieces);return snapshot;}
    public boolean add(Material material,double x,double z,int rotation){return add(new Piece(material,x,z,Math.floorMod(rotation,4)));}
    public boolean add(Piece p){
        if(!valid(p) || pieces.size()>=28 || pieces.stream().anyMatch(a->a.id.equals(p.id)))return false;
        long plants=pieces.stream().filter(a->a.material==Material.SEAGRASS || a.material==Material.KELP).count();
        long count=pieces.stream().filter(a->a.material==p.material).count();
        if((p.material==Material.SEAGRASS || p.material==Material.KELP)?plants>=16:count>=(p.material==Material.ROCK?8:p.material==Material.WOOD?4:28))return false;
        snapshot=null;pieces.add(p);return true;
    }
    public static boolean valid(Piece p){
        return p!=null && p.material!=null && p.id!=null && p.block!=null && p.block.length()<=128
                && finite(p.x,p.y,p.z,p.scaleX,p.scaleY,p.scaleZ,p.yaw,p.pitch,p.roll) && Math.abs(p.yaw)<=360 && Math.abs(p.pitch)<=360 && Math.abs(p.roll)<=360 && p.x>=.08 && p.x<=.92 && p.z>=.08 && p.z<=.92
                && p.y>=-.45 && p.y<=.92 && p.scaleX>=.15 && p.scaleX<=4 && p.scaleY>=.15 && p.scaleY<=4 && p.scaleZ>=.15 && p.scaleZ<=4 && p.rotation>=0 && p.rotation<4;
    }
    private static boolean finite(double... values){for(double v:values)if(!Double.isFinite(v))return false;return true;}
    public static double baseWidth(Material m){return switch(m){case ROCK->.5;case WOOD->.8;case SEAGRASS,KELP->.44;case BLOCK->.5;};}
    public static double baseDepth(Material m){return m==Material.ROCK?.46:m==Material.WOOD?.2:baseWidth(m);}
    public static double baseHeight(Material m,AquariumData.Size s){return switch(m){case ROCK->.45;case WOOD,BLOCK->.5;case SEAGRASS->.35*(s.height-2);case KELP->.7*(s.height-2);};}
    public static Piece fit(Piece p,AquariumData.Size s){
        if(precise(p))return fitPrecise(p,s);
        double w=s.width-2,h=s.height-2,d=s.depth-2;
        double sx=Math.clamp(p.scaleX,.15,Math.min(4,(p.rotation%2==0?w:d)*.8/baseWidth(p.material)));
        double sz=Math.clamp(p.scaleZ,.15,Math.min(4,(p.rotation%2==0?d:w)*.8/baseDepth(p.material)));
        double sy=Math.clamp(p.scaleY,.15,Math.min(4,(h-.18)/baseHeight(p.material,s)));
        double rx=(p.rotation%2==0?baseWidth(p.material)*sx:baseDepth(p.material)*sz)/2/w+.01;
        double rz=(p.rotation%2==0?baseDepth(p.material)*sz:baseWidth(p.material)*sx)/2/d+.01;
        return new Piece(p.material,Math.clamp(p.x,Math.max(.08,rx),Math.min(.92,1-rx)),Math.clamp(p.z,Math.max(.08,rz),Math.min(.92,1-rz)),p.rotation,
                Math.clamp(p.y,-.45,Math.max(0,Math.min(.92,(h-.18-baseHeight(p.material,s)*sy)/h))),sx,sy,sz,p.id,p.block);
    }
    public boolean transform(UUID id,AquariumData.Size size,double x,double y,double z,int rotation,double sx,double sy,double sz){
        for(int i=0;i<pieces.size();i++)if(pieces.get(i).id.equals(id)){
            var old=pieces.get(i);var p=new Piece(old.material,x,z,rotation,y,sx,sy,sz,id,old.block,old.yaw,old.pitch,old.roll);
            if(!valid(p))return false;snapshot=null;pieces.set(i,fit(p,size));return true;
        }
        return false;
    }
    public void fitAll(AquariumData.Size s){snapshot=null;for(int i=0;i<pieces.size();i++)pieces.set(i,fit(pieces.get(i),s));}
    public static Bounds bounds(Piece p,AquariumData.Size s){
        if(precise(p))return preciseBounds(p,s);
        double ax=(s.blocksWide()-.16)/(s.width-2),ay=(s.blocksHigh()*.88-.24)/(s.height-2),az=(s.blocksDeep()-.16)/(s.depth-2);
        double cx=.08+p.x*(s.blocksWide()-.16),cz=.08+p.z*(s.blocksDeep()-.16),y=.1+(.08+p.y*(s.height-2))*ay;
        double rx=(p.rotation%2==0?baseWidth(p.material)*p.scaleX:baseDepth(p.material)*p.scaleZ)*ax/2;
        double rz=(p.rotation%2==0?baseDepth(p.material)*p.scaleZ:baseWidth(p.material)*p.scaleX)*az/2;
        return new Bounds(cx-rx,y,cz-rz,cx+rx,y+baseHeight(p.material,s)*p.scaleY*ay,cz+rz);
    }
    public static boolean precise(Piece p){return p.yaw!=0 || p.pitch!=0 || p.roll!=0;}
    /** Apply the same Z, X, Y rotation order used by the model pose stack. */
    public static double[] rotate(Piece p,double x,double y,double z){
        double r=Math.toRadians(p.roll),c=Math.cos(r),n=Math.sin(r),a=x*c-y*n,b=x*n+y*c;
        r=Math.toRadians(p.pitch);c=Math.cos(r);n=Math.sin(r);double q=b*c-z*n,v=b*n+z*c;
        r=Math.toRadians(p.rotation*90+p.yaw);c=Math.cos(r);n=Math.sin(r);return new double[]{a*c+v*n,q,-a*n+v*c};
    }
    public static Bounds extents(Piece p,AquariumData.Size s){
        double w=baseWidth(p.material)*p.scaleX/2,h=baseHeight(p.material,s)*p.scaleY,d=baseDepth(p.material)*p.scaleZ/2;
        double x=Double.POSITIVE_INFINITY,y=x,z=x,X=-x,Y=-x,Z=-x;
        for(int i=0;i<8;i++){var a=rotate(p,(i&1)==0?-w:w,(i&2)==0?0:h,(i&4)==0?-d:d);x=Math.min(x,a[0]);y=Math.min(y,a[1]);z=Math.min(z,a[2]);X=Math.max(X,a[0]);Y=Math.max(Y,a[1]);Z=Math.max(Z,a[2]);}
        return new Bounds(x,y,z,X,Y,Z);
    }
    private static Piece fitPrecise(Piece p,AquariumData.Size s){
        double w=s.width-2,h=s.height-2,d=s.depth-2;var e=extents(p,s);
        double factor=Math.min(1,Math.min(w*.8/(e.X-e.x),Math.min((h-.18)/(e.Y-e.y),d*.8/(e.Z-e.z))));
        var q=new Piece(p.material,p.x,p.z,p.rotation,p.y,Math.max(.15,p.scaleX*factor),Math.max(.15,p.scaleY*factor),Math.max(.15,p.scaleZ*factor),p.id,p.block,p.yaw,p.pitch,p.roll);
        e=extents(q,s);
        return new Piece(p.material,Math.clamp(p.x,Math.max(.08,-e.x/w+.01),Math.min(.92,1-e.X/w-.01)),Math.clamp(p.z,Math.max(.08,-e.z/d+.01),Math.min(.92,1-e.Z/d-.01)),p.rotation,
                Math.clamp(p.y,-.45,Math.max(0,Math.min(.92,(h-.18-(e.Y-e.y))/h))),q.scaleX,q.scaleY,q.scaleZ,p.id,p.block,p.yaw,p.pitch,p.roll);
    }
    private static Bounds preciseBounds(Piece p,AquariumData.Size s){
        var e=extents(p,s);double ax=(s.blocksWide()-.16)/(s.width-2),ay=(s.blocksHigh()*.88-.24)/(s.height-2),az=(s.blocksDeep()-.16)/(s.depth-2);
        double x=.08+p.x*(s.blocksWide()-.16),z=.08+p.z*(s.blocksDeep()-.16),y=.1+(.08+p.y*(s.height-2))*ay;
        return new Bounds(x+e.x*ax,y,z+e.z*az,x+e.X*ax,y+(e.Y-e.y)*ay,z+e.Z*az);
    }
    public boolean angles(UUID id,AquariumData.Size size,double yaw,double pitch,double roll){
        for(int i=0;i<pieces.size();i++)if(pieces.get(i).id.equals(id)){
            var old=pieces.get(i);var p=new Piece(old.material,old.x,old.z,0,old.y,old.scaleX,old.scaleY,old.scaleZ,id,old.block,yaw,pitch,roll);
            if(!valid(p))return false;snapshot=null;pieces.set(i,fit(p,size));return true;
        }return false;
    }
    public Piece remove(int index){if(index<0 || index>=pieces.size())return null;snapshot=null;return pieces.remove(index);}
    public void clear(){pieces.clear();snapshot=null;}
    public int count(Material m){return (int)pieces.stream().filter(p->p.material==m).count();}
}
