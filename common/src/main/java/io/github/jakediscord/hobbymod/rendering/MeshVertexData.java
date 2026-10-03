package io.github.jakediscord.hobbymod.rendering;

import io.github.jakediscord.hobbymod.sculpting.MarbleMesh;

/** Immutable, atlas-independent attributes prepared once per geometry change. */
public final class MeshVertexData {
    public static final int STRIDE=11;
    private final float[] data;
    private MeshVertexData(float[] data){this.data=data;}
    public float[] data(){return data;} // Renderer reads only; no per-frame copy.
    public int vertices(){return data.length/STRIDE;}
    public static MeshVertexData pottery(MarbleMesh mesh){return prepare(mesh,false);}
    public static MeshVertexData marble(MarbleMesh mesh){return prepare(PlanarMesh.compact(mesh),true);}
    private static MeshVertexData prepare(MarbleMesh mesh,boolean marble){
        float[] out=new float[mesh.quads().size()*4*STRIDE];int i=0;
        for(var face:mesh.quads()){
            double nx=face.a().nx()+face.b().nx()+face.c().nx()+face.d().nx(),ny=face.a().ny()+face.b().ny()+face.c().ny()+face.d().ny(),nz=face.a().nz()+face.b().nz()+face.c().nz()+face.d().nz();
            int axis=Math.abs(ny)>Math.abs(nx)?1:0;if(Math.abs(nz)>Math.abs(axis==0?nx:ny))axis=2;
            int polished=(face.a().polished()?1:0)+(face.b().polished()?1:0)+(face.c().polished()?1:0)+(face.d().polished()?1:0);
            for(int n=0;n<4;n++){
                var v=face.vertex(n);
                out[i++]=(float)v.x();out[i++]=(float)v.y();out[i++]=(float)v.z();
                out[i++]=(float)v.nx();out[i++]=(float)v.ny();out[i++]=(float)v.nz();
                out[i++]=(float)(marble?Math.clamp(axis==0?v.z():v.x(),0,1):v.x());
                out[i++]=(float)(marble?Math.clamp(axis==1?v.z():1-v.y(),0,1):v.z());
                out[i++]=(float)(marble?.72+.18*v.ny()+.08*v.nx()+.04*v.nz():.72+.23*v.ny()+.12*v.nx()-.06*v.nz());
                double angle=Math.atan2(v.z()-.5,v.x()-.5);
                out[i++]=marble?1:1+(float)(.035*Math.sin(angle*3)+.018*Math.cos(angle*7));
                out[i++]=marble && polished>=2?1:0;
            }
        }
        return new MeshVertexData(out);
    }
    /** x^24 without a transcendental pow call, for the moving glaze highlight. */
    public static float shine(float dot){float x=Math.max(0,dot),x2=x*x,x4=x2*x2,x8=x4*x4;return .38F*x8*x8*x8;}
}
