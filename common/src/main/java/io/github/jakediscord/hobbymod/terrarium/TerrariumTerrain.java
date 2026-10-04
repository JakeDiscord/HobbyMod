package io.github.jakediscord.hobbymod.terrarium;

import io.github.jakediscord.hobbymod.aquarium.*;

/** Small saved height field shared by terrain rendering, brush picking and resident grounding. */
public final class TerrariumTerrain {
    public static final int X=8,Z=4,COUNT=(X+1)*(Z+1);
    private final int[] heights=new int[COUNT]; // Millimetres of model height relative to the substrate bed.
    public int[] saved(){return heights.clone();}
    public void load(int[] saved){java.util.Arrays.fill(heights,0);for(int i=0;i<Math.min(COUNT,saved.length);i++)heights[i]=Math.clamp(saved[i],-220,800);}
    public static double drainage(TerrariumData d){return d.drainage?.16:0;}
    public static double bed(TerrariumData d){return d.substrate==TerrariumData.Substrate.NONE?drainage(d):drainage(d)+switch(d.substrate){case SAND->.26;case SOIL->.22;case MOSS->.18;default->0;};}
    public double vertex(TerrariumData d,int x,int z){return Math.clamp(bed(d)+heights[z*(X+1)+x]/1000.0,Math.max(.08,drainage(d)+.04),1.2);}
    public double sample(TerrariumData d,double x,double z){
        if(d.substrate==TerrariumData.Substrate.NONE)return Math.max(.04,drainage(d));
        double px=Math.clamp(x,0,1)*X,pz=Math.clamp(z,0,1)*Z;int ix=Math.min(X-1,(int)px),iz=Math.min(Z-1,(int)pz);double a=px-ix,b=pz-iz;
        double h00=vertex(d,ix,iz),h10=vertex(d,ix+1,iz),h01=vertex(d,ix,iz+1),h11=vertex(d,ix+1,iz+1);
        return b>=a?h00+(h11-h01)*a+(h01-h00)*b:h00+(h10-h00)*a+(h11-h10)*b;
    }
    /** Mode 0 raises, 1 lowers, 2 smooths. A packet is one small brush stamp, never an arbitrary mesh. */
    public boolean brush(TerrariumData d,double x,double z,double radius,int mode){
        if(d.substrate==TerrariumData.Substrate.NONE || !Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(radius) || x<0 || x>1 || z<0 || z>1 || radius<.12 || radius>.5 || mode<0 || mode>2)return false;
        int[] before=heights.clone();boolean changed=false;
        for(int iz=0;iz<=Z;iz++)for(int ix=0;ix<=X;ix++){
            double distance=Math.hypot(ix/(double)X-x,iz/(double)Z-z);if(distance>=radius)continue;
            double weight=1-distance/radius;weight=weight*weight*(3-2*weight);int i=iz*(X+1)+ix;
            int next;if(mode==2){int sum=0,n=0;for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)if(ix+dx>=0 && ix+dx<=X && iz+dz>=0 && iz+dz<=Z){sum+=before[(iz+dz)*(X+1)+ix+dx];n++;}next=(int)Math.round(before[i]+(sum/(double)n-before[i])*weight*.4);}
            else next=before[i]+(int)Math.round((mode==0?60:-60)*weight);
            next=Math.clamp(next,-220,800);if(next!=heights[i]){heights[i]=next;changed=true;}
        }return changed;
    }
    public static AquariumScape.Bounds bounds(AquariumScape.Piece p,AquariumData d){
        var b=AquariumScape.bounds(p,d.size);if(d.terrarium==null)return b;
        double lift=(d.terrarium.terrain.sample(d.terrarium,p.x(),p.z())-.08)*(d.size.blocksHigh()*.88-.24)/(d.size.height-2);
        return new AquariumScape.Bounds(b.x(),b.y()+lift,b.z(),b.X(),b.Y()+lift,b.Z());
    }
    public static void fitDecor(AquariumData d){
        if(d.terrarium==null)return;var s=d.size;double ay=(s.blocksHigh()*.88-.24)/(s.height-2);
        for(var p:d.scape.pieces()){
            var b=bounds(p,d);double floor=b.y()-p.y()*(s.height-2)*ay,room=Math.max(.05,(s.blocksHigh()*.88-.14-floor)/ay);
            double tall=(b.Y()-b.y())/ay,factor=Math.min(1,room/tall),y=Math.max(0,Math.min(p.y(),(room-tall*factor)/(s.height-2)));
            if(factor<1 || y<p.y())d.scape.transform(p.id(),s,p.x(),y,p.z(),p.rotation(),Math.max(.15,p.scaleX()*factor),Math.max(.15,p.scaleY()*factor),Math.max(.15,p.scaleZ()*factor));
        }
    }
    public TerrariumTerrain(){}
}
