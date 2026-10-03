package io.github.jakediscord.hobbymod.pottery;

import io.github.jakediscord.hobbymod.sculpting.MarbleMesh;
import java.util.ArrayList;
import java.util.Arrays;

/** A smooth lathe profile. Throwing redistributes a fixed amount of clay into continuous walls. */
public final class PotteryShape {
    public static final int SAMPLES=32;
    public static final double FLOOR=.12, MIN_WALL=.018;
    private final double[] radius=new double[SAMPLES];
    private double height=.24,wall=.06,mass;
    private boolean open;
    private MarbleMesh mesh;
    public PotteryShape() {
        for(int i=0;i<SAMPLES;i++)radius[i]=.22-.025*Math.pow(i/(double)(SAMPLES-1)-.45,2)*4;
        mass=volume(wall);
    }
    public double height(){return height;}
    public double wall(){return wall;}
    public double mass(){return mass;}
    public boolean open(){return open;}
    public double radius(double t) {
        double s=Math.clamp(t,0,1)*(SAMPLES-1);int i=Math.min(SAMPLES-2,(int)s);double u=s-i;
        double a=radius[i],b=radius[i+1],m0=slope(i),m1=slope(i+1);
        return (2*u*u*u-3*u*u+1)*a+(u*u*u-2*u*u+u)*m0+(-2*u*u*u+3*u*u)*b+(u*u*u-u*u)*m1;
    }
    private double slope(int i) {
        if(i==0 || i==SAMPLES-1)return 0;
        double a=radius[i]-radius[i-1],b=radius[i+1]-radius[i];
        return a*b<=0?0:2*a*b/(a+b);
    }
    public double slopeAt(double t){double e=.0001;return (radius(t+e)-radius(t-e))/(2*e*height);}
    public PotteryShape copy(){return read(data());}
    public double[] data() {
        double[] data=new double[SAMPLES+4];data[0]=height;data[1]=wall;data[2]=mass;data[3]=open?1:0;
        System.arraycopy(radius,0,data,4,SAMPLES);return data;
    }
    public static PotteryShape read(double[] data) {
        PotteryShape p=new PotteryShape();
        if(data.length!=SAMPLES+4 || Arrays.stream(data).anyMatch(v->!Double.isFinite(v)))return p;
        if(data[0]<.16 || data[0]>1.15 || data[1]<MIN_WALL || data[1]>.2 || data[2]<=0 || data[2]>.6
                || (data[3]!=0 && data[3]!=1))return p;
        for(int i=4;i<data.length;i++)if(data[i]<.075 || data[i]>.43)return p;
        p.height=data[0];p.wall=data[1];p.mass=data[2];p.open=data[3]==1;
        System.arraycopy(data,4,p.radius,0,SAMPLES);
        if(p.open && Arrays.stream(p.radius).min().orElse(0)<=p.wall+.008)return new PotteryShape();
        // Clay volume is derived from geometry; item data cannot invent mass.
        p.mass=p.volume(p.wall);return p;
    }
    public boolean openCenter() {
        if(open)return false;
        height*=1.4;for(int i=0;i<SAMPLES;i++)radius[i]*=1.15;open=true;
        if(!balance())throw new IllegalStateException("Initial clay cannot form a hollow pot");
        mesh=null;return true;
    }
    /** Wet shaping conserves clay; an impossible, too-thin wall rejects the whole stroke. */
    public boolean throwClay(double t,double push,double lift,boolean smooth) {
        if(!open || !Double.isFinite(t) || !Double.isFinite(push) || !Double.isFinite(lift) || t<0 || t>1
                || Math.abs(push)>.025 || Math.abs(lift)>.025)return false;
        var before=data();
        double[] old=radius.clone();
        for(int i=0;i<SAMPLES;i++) {
            double w=Math.exp(-Math.pow((i/(double)(SAMPLES-1)-t)/.13,2));
            double next=smooth && i>0 && i<SAMPLES-1?old[i]+w*(old[i-1]+old[i+1]-2*old[i])*.3:old[i]+push*w;
            radius[i]=Math.clamp(next,.075,.43);
        }
        if(t>.65)height=Math.clamp(height+lift,.16,1.15);
        if(!balance()){restore(before);return false;}
        double[] after=data();boolean changed=false;
        for(int i=0;i<after.length;i++)if(i!=2 && i!=3 && Math.abs(after[i]-before[i])>=.000001){changed=true;break;}
        if(changed)mesh=null;else restore(before);return changed;
    }
    public boolean trim(double t) {
        if(!open || !Double.isFinite(t) || t<0 || t>.28)return false;
        boolean changed=false;
        for(int i=0;i<SAMPLES;i++) {
            double f=i/(double)(SAMPLES-1);if(f>.3)continue;
            double next=Math.max(wall+.025,radius[i]-.003*Math.exp(-Math.pow((f-t)/.07,2)));
            if(next<radius[i]){radius[i]=next;changed=true;}
        }
        if(changed){mass=volume(wall);mesh=null;}return changed;
    }
    public void shrink(double amount) {
        if(!Double.isFinite(amount) || amount<.9 || amount>1)throw new IllegalArgumentException("Invalid shrinkage");
        height*=amount;wall*=amount;for(int i=0;i<SAMPLES;i++)radius[i]*=amount;
        // Keep saved geometry within its supported range even after both firings.
        height=Math.max(.16,height);wall=Math.max(MIN_WALL,wall);
        for(int i=0;i<SAMPLES;i++)radius[i]=Math.max(.075,radius[i]);
        mass=volume(wall);mesh=null;
    }
    private void restore(double[] d){height=d[0];wall=d[1];mass=d[2];System.arraycopy(d,4,radius,0,SAMPLES);}
    private boolean balance() {
        double lo=MIN_WALL,hi=Math.min(.2,Arrays.stream(radius).min().orElse(.075)-.009);
        if(volume(lo)>mass || volume(hi)<mass)return false;
        for(int i=0;i<32;i++){double mid=(lo+hi)/2;if(volume(mid)<mass)lo=mid;else hi=mid;}
        wall=(lo+hi)/2;return true;
    }
    public double volume(double thickness) {
        double result=0;
        for(int i=0;i<256;i++) {
            double t=(i+.5)/256,r=radius(t),inner=open && t>=FLOOR?Math.max(0,r-thickness):0;
            result+=Math.PI*(r*r-inner*inner)*height/256;
        }
        // Rounded lip contains additional clay.
        if(open)result+=2*Math.PI*(radius(1)-thickness/2)*Math.PI*thickness*thickness/8;
        return result;
    }
    public MarbleMesh mesh() {
        if(mesh!=null)return mesh;
        var faces=new ArrayList<MarbleMesh.Quad>();int rings=48,sides=64;
        for(int j=0;j<rings;j++)for(int i=0;i<sides;i++) {
            double t=j/(double)rings,u=(j+1)/(double)rings,a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides;
            faces.add(new MarbleMesh.Quad(side(t,a,false),side(u,a,false),side(u,b,false),side(t,b,false)));
            if(open) {
                double it=FLOOR+(1-FLOOR)*t,iu=FLOOR+(1-FLOOR)*u;
                faces.add(new MarbleMesh.Quad(side(it,b,true),side(iu,b,true),side(iu,a,true),side(it,a,true)));
            }
        }
        for(int i=0;i<sides;i++) {
            double a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides;
            var bottom=new MarbleMesh.Vertex(.5,0,.5,0,-1,0,false);
            faces.add(new MarbleMesh.Quad(flat(0,b,radius(0),-1),bottom,bottom,flat(0,a,radius(0),-1)));
            if(open) {
                var floor=new MarbleMesh.Vertex(.5,FLOOR*height,.5,0,1,0,false);
                faces.add(new MarbleMesh.Quad(flat(FLOOR,a,radius(FLOOR)-wall,1),floor,floor,flat(FLOOR,b,radius(FLOOR)-wall,1)));
                for(int k=0;k<8;k++) {
                    double c=k*Math.PI/8,d=(k+1)*Math.PI/8;
                    faces.add(new MarbleMesh.Quad(lip(a,c),lip(a,d),lip(b,d),lip(b,c)));
                }
            }else {
                var top=new MarbleMesh.Vertex(.5,height,.5,0,1,0,false);
                faces.add(new MarbleMesh.Quad(flat(1,a,radius(1),1),top,top,flat(1,b,radius(1),1)));
            }
        }
        return mesh=new MarbleMesh(java.util.List.copyOf(faces));
    }
    private MarbleMesh.Vertex flat(double t,double angle,double r,double ny){return new MarbleMesh.Vertex(.5+r*Math.cos(angle),height*t,.5+r*Math.sin(angle),0,ny,0,false);}
    private MarbleMesh.Vertex side(double t,double a,boolean inside) {
        double r=radius(t)-(inside?wall:0),s=slopeAt(t),length=Math.sqrt(1+s*s),sign=inside?-1:1;
        return new MarbleMesh.Vertex(.5+r*Math.cos(a),height*t,.5+r*Math.sin(a),sign*Math.cos(a)/length,-sign*s/length,sign*Math.sin(a)/length,false);
    }
    private MarbleMesh.Vertex lip(double angle,double arc) {
        double r=radius(1)-wall/2+wall/2*Math.cos(arc);
        return new MarbleMesh.Vertex(.5+r*Math.cos(angle),height+wall/2*Math.sin(arc),.5+r*Math.sin(angle),Math.cos(angle)*Math.cos(arc),Math.sin(arc),Math.sin(angle)*Math.cos(arc),false);
    }
}
