package io.github.jakediscord.hobbymod.rendering;
import io.github.jakediscord.hobbymod.pottery.PotteryShape;
import io.github.jakediscord.hobbymod.sculpting.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RenderingGeometryTest {
    @Test void potteryLodIsCachedButEveryChangedShapeInvalidatesAllDetails(){
        var p=new PotteryShape();p.openCenter();var hi=p.mesh();var mid=p.mesh(1);var low=p.mesh(2);
        assertSame(hi,p.mesh());assertSame(mid,p.mesh(1));assertSame(low,p.mesh(2));
        assertTrue(mid.quads().size()<hi.quads().size()/3);assertTrue(low.quads().size()<hi.quads().size()/10);
        assertTrue(p.throwClay(.8,.01,.01,false));assertNotSame(hi,p.mesh());assertNotSame(mid,p.mesh(1));assertNotSame(low,p.mesh(2));
        assertEquals(p.height(),p.mesh(2).quads().stream().flatMap(q->java.util.Arrays.stream(q.vertices())).mapToDouble(MarbleMesh.Vertex::y).max().orElseThrow()-p.wall()/2,1e-9);
    }
    @Test void compactionPreservesSurfaceAreaNormalsFinishAndBounds(){
        var v=new MarbleVolume();var original=v.mesh();var compact=PlanarMesh.compact(original);
        assertTrue(compact.quads().size()<original.quads().size()/4);
        assertEquals(area(original),area(compact),1e-9);
        for(var q:compact.quads())for(int i=0;i<4;i++){var p=q.vertex(i);assertTrue(p.x()>=0 && p.x()<=1 && p.y()>=0 && p.y()<=1 && p.z()>=0 && p.z()<=1);}
        var a=new MarbleMesh.Vertex(.015625,0,.015625,0,-1,0,true);var b=new MarbleMesh.Vertex(.046875,0,.015625,0,-1,0,true);var c=new MarbleMesh.Vertex(.046875,0,.046875,0,-1,0,true);var d=new MarbleMesh.Vertex(.015625,0,.046875,0,-1,0,true);
        var polished=PlanarMesh.compact(new MarbleMesh(java.util.List.of(new MarbleMesh.Quad(d,c,b,a))));assertTrue(polished.quads().getFirst().a().polished());assertEquals(-1,polished.quads().getFirst().a().ny());
    }
    @Test void preparedAttributesAndFastGlazePowerMatchTheExpectedMath(){
        var p=new PotteryShape();p.openCenter();var data=MeshVertexData.pottery(p.mesh(2));assertEquals(p.mesh(2).quads().size()*4,data.vertices());
        for(float x:new float[]{0,.1F,.5F,.9F,1})assertEquals(.38*Math.pow(x,24),MeshVertexData.shine(x),1e-6);
        assertEquals(0,MeshVertexData.shine(-1));
    }
    private static double area(MarbleMesh m){double sum=0;for(var q:m.quads())sum+=tri(q.a(),q.b(),q.c())+tri(q.a(),q.c(),q.d());return sum;}
    private static double tri(MarbleMesh.Vertex a,MarbleMesh.Vertex b,MarbleMesh.Vertex c){double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z(),X=c.x()-a.x(),Y=c.y()-a.y(),Z=c.z()-a.z();return Math.sqrt(Math.pow(y*Z-z*Y,2)+Math.pow(z*X-x*Z,2)+Math.pow(x*Y-y*X,2))/2;}
}
