package io.github.jakediscord.hobbymod.sculpting;

import io.github.jakediscord.hobbymod.registry.HobbyContent;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** A vertical sculpture stores each section in its own block, sharing boundary geometry. */
public final class MarbleColumn {
    private MarbleColumn() {}
    public static boolean marble(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && (level.getBlockState(pos).is(HobbyContent.MARBLE.get())
                || level.getBlockState(pos).is(HobbyContent.SCULPTURE.get()));
    }
    public static List<BlockPos> positions(Level level, BlockPos pos) {
        BlockPos bottom=pos;
        while(bottom.getY()>level.getMinBuildHeight() && marble(level,bottom.below()))bottom=bottom.below();
        List<BlockPos> result=new ArrayList<>();
        for(BlockPos p=bottom; p.getY()<level.getMaxBuildHeight() && marble(level,p);p=p.above())result.add(p.immutable());
        return result;
    }
    public static List<SculptureBlockEntity> sculptures(Level level,BlockPos pos) {
        List<SculptureBlockEntity> result=new ArrayList<>();
        for(BlockPos p:positions(level,pos)) {
            if(!(level.getBlockEntity(p) instanceof SculptureBlockEntity s))break;
            result.add(s);
        }
        return result;
    }

    /** Sample neighboring stone rather than an artificial air cap at each block seam. */
    public static MarbleMesh.Field field(MarbleVolume v,MarbleVolume below,MarbleVolume above) {
        return new MarbleMesh.Field() {
            public int node(int x,int y,int z) {
                if(y==0 && below!=null)return below.node(x,MarbleVolume.SIZE,z);
                if(y==MarbleVolume.GRID-1 && above!=null)return above.node(x,2,z);
                int own=v.node(x,y,z);
                MarbleVolume other=y==1?below:y==MarbleVolume.GRID-2?above:null;
                if(other==null)return own;
                int boundary=other.node(x,y==1?MarbleVolume.GRID-2:1,z);
                if(own<0 || boundary<0)return Math.min(own,boundary);
                // Restore interior distance at joined faces while keeping side faces and cuts.
                return Math.max(Math.min(own,boundary),Math.min(v.node(x,y==1?2:MarbleVolume.SIZE,z),
                        other.node(x,y==1?MarbleVolume.SIZE:2,z)));
            }
            private double sample(double x,double y,double z) {
                double gx=Math.clamp(x*32+1,0,MarbleVolume.GRID-1.000001);
                double gy=Math.clamp(y*32+1,0,MarbleVolume.GRID-1.000001);
                double gz=Math.clamp(z*32+1,0,MarbleVolume.GRID-1.000001);
                int ix=(int)gx,iy=(int)gy,iz=(int)gz;double sum=0;
                for(int dz=0;dz<2;dz++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)
                    sum+=node(ix+dx,iy+dy,iz+dz)*(dx==0?1-(gx-ix):gx-ix)*(dy==0?1-(gy-iy):gy-iy)*(dz==0?1-(gz-iz):gz-iz);
                return sum/1024;
            }
            public double[] normal(double x,double y,double z) {
                double e=1.0/64,nx=sample(x-e,y,z)-sample(x+e,y,z),ny=sample(x,y-e,z)-sample(x,y+e,z),nz=sample(x,y,z-e)-sample(x,y,z+e);
                double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
                return length<1e-10?new double[]{0,1,0}:new double[]{nx/length,ny/length,nz/length};
            }
            public boolean polishedAt(double x,double y,double z) { return v.polishedAt(x,y,z); }
        };
    }

    /** Flood the entire pillar, so parts supported through another section remain attached. */
    public static void prune(List<MarbleVolume> volumes) {
        int nodes=MarbleVolume.NODES,grid=MarbleVolume.GRID,total=Math.multiplyExact(nodes,volumes.size());
        BitSet visited=new BitSet(total),keep=new BitSet(total);int[] queue=new int[total];
        int bestY=Integer.MAX_VALUE,bestSize=0;
        for(int seed=0;seed<total;seed++) {
            if(visited.get(seed) || value(volumes,seed)<0)continue;
            int head=0,tail=1,minY=Integer.MAX_VALUE;queue[0]=seed;visited.set(seed);
            while(head<tail) {
                int i=queue[head++],local=i%nodes,section=i/nodes,x=local%grid,y=local/grid%grid,z=local/(grid*grid);
                minY=Math.min(minY,section*MarbleVolume.SIZE+y);
                int[] next={x>1?i-1:-1,x<grid-2?i+1:-1,y>1?i-grid:-1,y<grid-2?i+grid:-1,
                        z>1?i-grid*grid:-1,z<grid-2?i+grid*grid:-1,
                        y==1 && section>0?i-nodes+MarbleVolume.SIZE*grid:-1,
                        y==grid-2 && section+1<volumes.size()?i+nodes-MarbleVolume.SIZE*grid:-1};
                for(int n:next)if(n>=0 && !visited.get(n) && value(volumes,n)>=0){visited.set(n);queue[tail++]=n;}
            }
            if(minY<bestY || (minY==bestY && tail>bestSize)) {
                bestY=minY;bestSize=tail;keep.clear();for(int i=0;i<tail;i++)keep.set(queue[i]);
            }
        }
        for(int i=0;i<total;i++)if(value(volumes,i)>=0 && !keep.get(i))volumes.get(i/nodes).removeNode(i%nodes);
        volumes.forEach(MarbleVolume::invalidate);
    }
    private static int value(List<MarbleVolume> volumes,int i) {
        int local=i%MarbleVolume.NODES,g=MarbleVolume.GRID;
        return volumes.get(i/MarbleVolume.NODES).node(local%g,local/g%g,local/(g*g));
    }
}
