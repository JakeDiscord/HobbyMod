package io.github.jakediscord.hobbymod.rendering;
import io.github.jakediscord.hobbymod.sculpting.*;
import java.util.*;
/** Merge only exact, axis-aligned grid rectangles with constant normals and finish. Curved faces stay intact. */
public final class PlanarMesh {
    private record Plane(int axis,int sign,boolean polished,double coordinate){}
    public static MarbleMesh compact(MarbleMesh mesh){
        var rest=new ArrayList<MarbleMesh.Quad>();var planes=new LinkedHashMap<Plane,MarbleMesh.Quad[]>();int size=MarbleVolume.SIZE;
        for(var q:mesh.quads()){
            int axis=axis(q.a());if(axis<0){rest.add(q);continue;}
            int u=(axis+1)%3,v=(axis+2)%3;double minU=2,minV=2,maxU=-1,maxV=-1,plane=coord(q.a(),axis);boolean ok=true;
            for(int i=0;i<4;i++){var p=q.vertex(i);ok&=axis(p)==axis && p.nx()==q.a().nx() && p.ny()==q.a().ny() && p.nz()==q.a().nz() && p.polished()==q.a().polished() && coord(p,axis)==plane;minU=Math.min(minU,coord(p,u));maxU=Math.max(maxU,coord(p,u));minV=Math.min(minV,coord(p,v));maxV=Math.max(maxV,coord(p,v));}
            int x=(int)Math.round(minU*size-.5),y=(int)Math.round(minV*size-.5);
            ok&=x>=0 && y>=0 && x<size && y<size && Math.abs(minU-(x+.5)/size)<1e-10 && Math.abs(minV-(y+.5)/size)<1e-10 && Math.abs(maxU-minU-1.0/size)<1e-10 && Math.abs(maxV-minV-1.0/size)<1e-10 && maxU<=1 && maxV<=1;
            if(!ok){rest.add(q);continue;}
            int sign=normal(q.a(),axis)>0?1:-1;var key=new Plane(axis,sign,q.a().polished(),plane);var grid=planes.computeIfAbsent(key,k->new MarbleMesh.Quad[size*size]);
            if(grid[x+size*y]!=null){rest.add(q);continue;}grid[x+size*y]=q;
        }
        for(var entry:planes.entrySet()){
            var plane=entry.getKey();var grid=entry.getValue();int u=(plane.axis+1)%3,v=(plane.axis+2)%3;
            for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                if(grid[x+y*size]==null)continue;int w=1,h=1;
                while(x+w<size && grid[x+w+y*size]!=null)w++;
                outer:while(y+h<size){for(int dx=0;dx<w;dx++)if(grid[x+dx+(y+h)*size]==null)break outer;h++;}
                double U=(x+.5)/size,V=(y+.5)/size,X=(x+w+.5)/size,Y=(y+h+.5)/size;
                var a=vertex(plane,u,v,U,V);var b=vertex(plane,u,v,X,V);var c=vertex(plane,u,v,X,Y);var d=vertex(plane,u,v,U,Y);
                rest.add(plane.sign>0?new MarbleMesh.Quad(a,b,c,d):new MarbleMesh.Quad(d,c,b,a));
                for(int dy=0;dy<h;dy++)for(int dx=0;dx<w;dx++)grid[x+dx+(y+dy)*size]=null;
            }
        }
        return new MarbleMesh(List.copyOf(rest));
    }
    private static MarbleMesh.Vertex vertex(Plane p,int u,int v,double U,double V){double[] xyz=new double[3];xyz[p.axis]=p.coordinate;xyz[u]=U;xyz[v]=V;return new MarbleMesh.Vertex(xyz[0],xyz[1],xyz[2],p.axis==0?p.sign:0,p.axis==1?p.sign:0,p.axis==2?p.sign:0,p.polished);}
    private static int axis(MarbleMesh.Vertex p){for(int i=0;i<3;i++)if(Math.abs(normal(p,i))==1 && normal(p,(i+1)%3)==0 && normal(p,(i+2)%3)==0)return i;return -1;}
    private static double normal(MarbleMesh.Vertex p,int i){return i==0?p.nx():i==1?p.ny():p.nz();}
    private static double coord(MarbleMesh.Vertex p,int i){return i==0?p.x():i==1?p.y():p.z();}
    private PlanarMesh(){}
}
