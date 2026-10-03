package io.github.jakediscord.hobbymod.sculpting;

import java.util.ArrayList;
import java.util.List;

/** Surface nets interpolate sub-grid vertices; shared mesh also supplies exact triangle picking. */
public record MarbleMesh(List<Quad> quads) {
    public record Vertex(double x,double y,double z,double nx,double ny,double nz,boolean polished) {}
    public record Quad(Vertex a,Vertex b,Vertex c,Vertex d) {
        public Vertex[] vertices() { return new Vertex[]{a,b,c,d}; }
    }
    public record Hit(double x,double y,double z,double distance) {}
    private static final int[][] EDGES={{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
    public static MarbleMesh build(MarbleVolume v) {
        int grid=MarbleVolume.GRID,cells=grid-1;
        Vertex[] vertices=new Vertex[cells*cells*cells];
        for (int z=0;z<cells;z++) for (int y=0;y<cells;y++) for (int x=0;x<cells;x++) {
            int[] values=new int[8];int mask=0;
            for (int i=0;i<8;i++) { values[i]=v.node(x+(i&1),y+((i>>1)&1),z+((i>>2)&1));if(values[i]>=0)mask|=1<<i; }
            if(mask==0 || mask==255) continue;
            double px=0,py=0,pz=0;int crossings=0;
            for(int[] edge:EDGES) {
                int a=edge[0],b=edge[1];if((values[a]>=0)==(values[b]>=0))continue;
                double t=values[a]/(double)(values[a]-values[b]);crossings++;
                px+=x-1+(a&1)+t*((b&1)-(a&1));
                py+=y-1+((a>>1)&1)+t*(((b>>1)&1)-((a>>1)&1));
                pz+=z-1+((a>>2)&1)+t*(((b>>2)&1)-((a>>2)&1));
            }
            px/=crossings*MarbleVolume.SIZE;py/=crossings*MarbleVolume.SIZE;pz/=crossings*MarbleVolume.SIZE;
            double[] n=v.normal(px,py,pz);
            vertices[x+cells*(y+cells*z)]=new Vertex(px,py,pz,n[0],n[1],n[2],v.polishedAt(px,py,pz));
        }
        List<Quad> faces=new ArrayList<>();
        for(int axis=0;axis<3;axis++) {
            int ua=(axis+1)%3,va=(axis+2)%3;
            for(int layer=0;layer<cells;layer++) for(int u=1;u<cells;u++) for(int w=1;w<cells;w++) {
                int[] p=new int[3];p[axis]=layer;p[ua]=u;p[va]=w;
                int a=v.node(p[0],p[1],p[2]);p[axis]++;int b=v.node(p[0],p[1],p[2]);p[axis]--;
                if((a>=0)==(b>=0))continue;
                Vertex[] q=new Vertex[4];int[][] offsets={{-1,-1},{0,-1},{0,0},{-1,0}};
                for(int i=0;i<4;i++) {
                    int[] c=p.clone();c[ua]+=offsets[i][0];c[va]+=offsets[i][1];
                    q[i]=vertices[c[0]+cells*(c[1]+cells*c[2])];
                }
                if(q[0]==null || q[1]==null || q[2]==null || q[3]==null)continue;
                faces.add(a>=0 ? new Quad(q[0],q[1],q[2],q[3]) : new Quad(q[3],q[2],q[1],q[0]));
            }
        }
        return new MarbleMesh(List.copyOf(faces));
    }
    public Hit pick(double[] origin,double[] direction) {
        double near=Double.POSITIVE_INFINITY;
        for(Quad q:quads) {
            near=Math.min(near,triangle(origin,direction,q.a,q.b,q.c));
            near=Math.min(near,triangle(origin,direction,q.a,q.c,q.d));
        }
        return Double.isFinite(near) ? new Hit(origin[0]+direction[0]*near,origin[1]+direction[1]*near,origin[2]+direction[2]*near,near) : null;
    }
    private static double triangle(double[] o,double[] d,Vertex a,Vertex b,Vertex c) {
        double ex=b.x-a.x,ey=b.y-a.y,ez=b.z-a.z,fx=c.x-a.x,fy=c.y-a.y,fz=c.z-a.z;
        double px=d[1]*fz-d[2]*fy,py=d[2]*fx-d[0]*fz,pz=d[0]*fy-d[1]*fx;
        double det=ex*px+ey*py+ez*pz;
        if(Math.abs(det)<1e-10)return Double.POSITIVE_INFINITY;
        double tx=o[0]-a.x,ty=o[1]-a.y,tz=o[2]-a.z;
        double u=(tx*px+ty*py+tz*pz)/det;if(u<0 || u>1)return Double.POSITIVE_INFINITY;
        double qx=ty*ez-tz*ey,qy=tz*ex-tx*ez,qz=tx*ey-ty*ex;
        double w=(d[0]*qx+d[1]*qy+d[2]*qz)/det;if(w<0 || u+w>1)return Double.POSITIVE_INFINITY;
        double t=(fx*qx+fy*qy+fz*qz)/det;return t>=0?t:Double.POSITIVE_INFINITY;
    }
}
