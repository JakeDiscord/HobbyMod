package io.github.jakediscord.hobbymod.sculpting;

import java.util.BitSet;

/** A quantized signed-distance field, not a collection of rendered cubes. */
public final class MarbleVolume {
    public static final int SIZE = 32, CELLS = SIZE * SIZE * SIZE, WORDS = CELLS / 64;
    public static final int GRID = SIZE + 3, NODES = GRID * GRID * GRID;
    private final byte[] density;
    private final BitSet polished;
    private MarbleMesh mesh;
    private int count = -1;

    public MarbleVolume() {
        density = new byte[NODES]; polished = new BitSet(NODES);
        for (int z=0;z<GRID;z++) for (int y=0;y<GRID;y++) for (int x=0;x<GRID;x++) {
            double px=(x-1)/(double)SIZE, py=(y-1)/(double)SIZE, pz=(z-1)/(double)SIZE;
            density[nodeIndex(x,y,z)]=quantize(Math.min(Math.min(Math.min(px,1-px),Math.min(py,1-py)),Math.min(pz,1-pz)));
        }
    }
    private MarbleVolume(byte[] density, BitSet polished) { this.density=density; this.polished=polished; }
    public MarbleVolume copy() { return new MarbleVolume(density.clone(),(BitSet)polished.clone()); }
    public byte[] densityBytes() { return density.clone(); }
    public long[] polishBits() { return polished.toLongArray(); }
    public int polishedCount() { return polished.cardinality(); }
    public static int nodeIndex(int x,int y,int z) { return x+GRID*(y+GRID*z); }
    public int node(int x,int y,int z) { return density[nodeIndex(x,y,z)]; }
    public static int index(int x,int y,int z) { return x+SIZE*(y+SIZE*z); }
    public static int x(int i) { return i%SIZE; }
    public static int y(int i) { return i/SIZE%SIZE; }
    public static int z(int i) { return i/(SIZE*SIZE); }
    private static byte quantize(double value) { return (byte)Math.clamp(Math.round(value*1024),-127,127); }
    private void invalidate() { mesh=null; count=-1; }

    public static MarbleVolume read(byte[] data,long[] finish) {
        if (data.length!=NODES || finish.length>(NODES+63)/64) return new MarbleVolume();
        MarbleVolume v=new MarbleVolume(data.clone(),BitSet.valueOf(finish));
        // Enforce a negative outside shell even for malformed item/chunk data.
        for (int z=0;z<GRID;z++) for (int y=0;y<GRID;y++) for (int x=0;x<GRID;x++)
            if (x==0 || y==0 || z==0 || x==GRID-1 || y==GRID-1 || z==GRID-1) v.density[nodeIndex(x,y,z)]=-32;
        v.pruneDetached();
        return v.count()==0 ? new MarbleVolume() : v;
    }

    /** Migrate old voxel saves to an interpolated field without resetting the carving. */
    public static MarbleVolume read(long[] cells,long[] finish) {
        if (cells.length>WORDS || finish.length>WORDS) return new MarbleVolume();
        BitSet stone=BitSet.valueOf(cells), oldFinish=BitSet.valueOf(finish);
        if (stone.isEmpty()) return new MarbleVolume();
        if (stone.cardinality()==CELLS && oldFinish.isEmpty()) return new MarbleVolume();
        MarbleVolume v=new MarbleVolume();
        for (int gz=1;gz<GRID-1;gz++) for (int gy=1;gy<GRID-1;gy++) for (int gx=1;gx<GRID-1;gx++) {
            int solid=0, total=0; boolean smooth=false;
            for (int dz=-1;dz<=0;dz++) for (int dy=-1;dy<=0;dy++) for (int dx=-1;dx<=0;dx++) {
                int x=gx-1+dx,y=gy-1+dy,z=gz-1+dz;
                if (x<0 || y<0 || z<0 || x>=SIZE || y>=SIZE || z>=SIZE) continue;
                total++; if (stone.get(index(x,y,z))) solid++;
                smooth |= oldFinish.get(index(x,y,z));
            }
            int i=nodeIndex(gx,gy,gz);
            v.density[i]=(byte)Math.min(v.density[i],Math.round((solid/(double)total-0.5)*32));
            if (smooth) v.polished.set(i);
        }
        v.pruneDetached(); return v.count()==0 ? new MarbleVolume() : v;
    }

    public double field(double x,double y,double z) {
        double gx=Math.clamp(x*SIZE+1,0,GRID-1.000001),gy=Math.clamp(y*SIZE+1,0,GRID-1.000001),gz=Math.clamp(z*SIZE+1,0,GRID-1.000001);
        int ix=(int)gx,iy=(int)gy,iz=(int)gz;
        double fx=gx-ix,fy=gy-iy,fz=gz-iz, result=0;
        for (int dz=0;dz<2;dz++) for (int dy=0;dy<2;dy++) for (int dx=0;dx<2;dx++)
            result+=node(ix+dx,iy+dy,iz+dz)*(dx==0?1-fx:fx)*(dy==0?1-fy:fy)*(dz==0?1-fz:fz);
        return result/1024;
    }
    public double[] normal(double x,double y,double z) {
        double e=1.0/64;
        double nx=field(x-e,y,z)-field(x+e,y,z),ny=field(x,y-e,z)-field(x,y+e,z),nz=field(x,y,z-e)-field(x,y,z+e);
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        return length<1e-10 ? new double[]{0,1,0} : new double[]{nx/length,ny/length,nz/length};
    }
    public boolean polishedAt(double x,double y,double z) {
        return polished.get(nodeIndex(Math.clamp((int)Math.round(x*SIZE)+1,1,GRID-2),Math.clamp((int)Math.round(y*SIZE)+1,1,GRID-2),Math.clamp((int)Math.round(z*SIZE)+1,1,GRID-2)));
    }
    public boolean has(int x,int y,int z) {
        return x>=0 && y>=0 && z>=0 && x<SIZE && y<SIZE && z<SIZE && field((x+0.5)/SIZE,(y+0.5)/SIZE,(z+0.5)/SIZE)>0;
    }
    public int count() {
        if (count<0) { count=0; for (int z=0;z<SIZE;z++) for (int y=0;y<SIZE;y++) for (int x=0;x<SIZE;x++) if (has(x,y,z)) count++; }
        return count;
    }
    public boolean exposed(double x,double y,double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && x>=0 && y>=0 && z>=0 && x<=1 && y<=1 && z<=1 && Math.abs(field(x,y,z))<=1.0/SIZE;
    }

    /** Rounded subtractive cuts; a rasp only removes high spots, never adds material. */
    public int stroke(double x,double y,double z,CarvingTool tool) { return stroke(x,y,z,tool,false); }
    public int stroke(double x,double y,double z,CarvingTool tool,boolean mirror) {
        if (!exposed(x,y,z)) return 0;
        boolean mirrored=mirror && Math.abs(x-0.5)>0.001 && exposed(1-x,y,z);
        double[] n=normal(x,y,z), reflected=mirrored?normal(1-x,y,z):null;
        int changed=applyBrush(x,y,z,tool,n);
        if (mirrored) changed+=applyBrush(1-x,y,z,tool,reflected);
        return changed;
    }
    private int applyBrush(double x,double y,double z,CarvingTool tool,double[] n) {
        double radius=tool.cutRadius;
        double cx=x-n[0]*radius*0.15, cy=y-n[1]*radius*0.15, cz=z-n[2]*radius*0.15;
        byte[] before=tool.polishes ? density.clone() : null;
        int changed=0;
        for (int gz=Math.max(1,(int)((cz-radius)*SIZE)+1);gz<=Math.min(GRID-2,(int)((cz+radius)*SIZE)+2);gz++)
            for (int gy=Math.max(1,(int)((cy-radius)*SIZE)+1);gy<=Math.min(GRID-2,(int)((cy+radius)*SIZE)+2);gy++)
                for (int gx=Math.max(1,(int)((cx-radius)*SIZE)+1);gx<=Math.min(GRID-2,(int)((cx+radius)*SIZE)+2);gx++) {
                    double dx=(gx-1)/(double)SIZE-cx,dy=(gy-1)/(double)SIZE-cy,dz=(gz-1)/(double)SIZE-cz;
                    double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
                    if (distance>radius) continue;
                    int i=nodeIndex(gx,gy,gz); byte old=density[i], next;
                    if (tool.polishes) {
                        int sum=before[i-1]+before[i+1]+before[i-GRID]+before[i+GRID]+before[i-GRID*GRID]+before[i+GRID*GRID];
                        // Limit erosion per pass; smooth the local high spots in the actual geometry.
                        next=(byte)Math.min(old,Math.max(old-3,Math.round(old*0.75F+sum/24F)));
                        if (!polished.get(i) && Math.abs(old)<=48) { polished.set(i); changed++; }
                    } else next=(byte)Math.min(old,quantize(distance-radius));
                    if (next!=old) { density[i]=next; changed++; }
                }
        if (changed>0) invalidate();
        return changed;
    }

    /** Keep the largest component touching the lowest surviving stone: detached chips disappear. */
    public int pruneDetached() {
        BitSet visited=new BitSet(NODES), keep=new BitSet(NODES);
        int[] queue=new int[NODES]; int bestY=GRID, bestSize=0;
        for (int seed=0;seed<NODES;seed++) {
            if (density[seed]<0 || visited.get(seed)) continue;
            int head=0,tail=1,minY=GRID; queue[0]=seed; visited.set(seed);
            while (head<tail) {
                int i=queue[head++],x=i%GRID,y=i/GRID%GRID,z=i/(GRID*GRID); minY=Math.min(minY,y);
                int[] neighbors={x>0?i-1:-1,x<GRID-1?i+1:-1,y>0?i-GRID:-1,y<GRID-1?i+GRID:-1,z>0?i-GRID*GRID:-1,z<GRID-1?i+GRID*GRID:-1};
                for (int next:neighbors) if (next>=0 && density[next]>=0 && !visited.get(next)) { visited.set(next);queue[tail++]=next; }
            }
            if (minY<bestY || (minY==bestY && tail>bestSize)) {
                bestY=minY;bestSize=tail;keep.clear();for (int j=0;j<tail;j++) keep.set(queue[j]);
            }
        }
        int removed=0;
        for (int i=0;i<NODES;i++) if (density[i]>=0 && !keep.get(i)) { density[i]=-32;polished.clear(i);removed++; }
        if (removed>0) invalidate();
        return removed;
    }
    public MarbleMesh mesh() { if (mesh==null) mesh=MarbleMesh.build(this); return mesh; }
    public MarbleMesh.Hit pick(double[] origin,double[] direction) { return mesh().pick(origin,direction); }
}
