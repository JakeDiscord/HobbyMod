import io.github.jakediscord.hobbymod.pottery.PotteryShape;
import io.github.jakediscord.hobbymod.sculpting.*;
import io.github.jakediscord.hobbymod.rendering.*;
/** Pure CPU attribute benchmark, not an FPS or GPU benchmark. Uses actual generated hobby meshes. */
public final class RenderingBenchmark {
    private static volatile double sink;
    private static double legacy(MarbleMesh mesh){double value=0;for(var q:mesh.quads())for(var v:q.vertices()){
        
        double vx=2-v.x(),vy=2-v.y(),vz=4-v.z(),len=Math.sqrt(vx*vx+vy*vy+vz*vz),hx=vx/len-.3,hy=vy/len+.8,hz=vz/len-.5,hl=Math.sqrt(hx*hx+hy*hy+hz*hz);
        value+=Math.clamp(.72+.23*v.ny()+.12*v.nx()-.06*v.nz(),.35,1)+.38*Math.pow(Math.max(0,(v.nx()*hx+v.ny()*hy+v.nz()*hz)/hl),24);
    }return value;}
    private static double cached(MeshVertexData mesh){double value=0;float[] p=mesh.data();for(int i=0;i<p.length;i+=MeshVertexData.STRIDE)value+=Math.clamp(p[i+8],.35F,1)+MeshVertexData.shine(p[i+3]*.2F+p[i+4]*.9F+p[i+5]*.3F);return value;}
    public static void main(String[] args){var pot=new PotteryShape();pot.openCenter();var full=pot.mesh();var prepared=MeshVertexData.pottery(full);for(int i=0;i<100;i++){sink=legacy(full);sink=cached(prepared);}
        int runs=300;long start=System.nanoTime();for(int i=0;i<runs;i++)sink=legacy(full);long legacy=System.nanoTime()-start;
        start=System.nanoTime();for(int i=0;i<runs;i++)sink=cached(prepared);long cached=System.nanoTime()-start;
        var marble=new MarbleVolume().mesh();
        System.out.printf("Pottery quads full/medium/distant: %d / %d / %d%n",full.quads().size(),pot.mesh(1).quads().size(),pot.mesh(2).quads().size());
        System.out.printf("Solid marble quads original/compacted: %d / %d%n",marble.quads().size(),PlanarMesh.compact(marble).quads().size());
        System.out.printf("CPU attributes per full-pot frame: old %.3f ms, cached %.3f ms (%.1fx)%n",legacy/1e6/runs,cached/1e6/runs,(double)legacy/cached);
    }
}
