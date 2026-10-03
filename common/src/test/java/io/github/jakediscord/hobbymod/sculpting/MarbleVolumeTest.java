package io.github.jakediscord.hobbymod.sculpting;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarbleVolumeTest {
    private static MarbleMesh.Hit front(MarbleVolume v) { return v.pick(new double[]{0.5,0.5,2},new double[]{0,0,-1}); }
    @Test void cutsExposeCurvedSubGridSurfacesAndPickingFollowsThem() {
        MarbleVolume v=new MarbleVolume();var initial=front(v);assertNotNull(initial);assertEquals(1,initial.z(),1e-8);
        assertTrue(v.stroke(initial.x(),initial.y(),initial.z(),CarvingTool.ROUGH)>0);v.pruneDetached();
        var cut=front(v);assertNotNull(cut);assertTrue(cut.z()<0.92 && cut.z()>0.75);
        assertTrue(v.mesh().quads().stream().flatMap(q->Arrays.stream(q.vertices())).anyMatch(p->p.z()>0.8 && p.z()<0.99 && Math.abs(p.z()*32-Math.rint(p.z()*32))>0.05),"Actual vertices interpolate between grid samples");
        assertTrue(v.mesh().quads().stream().flatMap(q->Arrays.stream(q.vertices())).anyMatch(p->Math.abs(p.nx())>0.1 && Math.abs(p.nz())>0.1),"Cuts have smooth oblique normals");
        assertEquals(0,v.stroke(0.5,0.5,0.5,CarvingTool.ROUGH));
        assertNull(v.pick(new double[]{2,2,2},new double[]{0,0,-1}));
    }
    @Test void toolsMakeDifferentSizeCutsAndRaspNeverAddsStone() {
        MarbleVolume detail=new MarbleVolume(),point=new MarbleVolume(),rough=new MarbleVolume();
        detail.stroke(0.5,0.5,1,CarvingTool.DETAIL);point.stroke(0.5,0.5,1,CarvingTool.POINT);rough.stroke(0.5,0.5,1,CarvingTool.ROUGH);
        assertTrue(detail.count()>point.count() && point.count()>rough.count());
        byte[] before=rough.densityBytes();var hit=front(rough);
        assertTrue(rough.stroke(hit.x(),hit.y(),hit.z(),CarvingTool.POLISH)>0);
        byte[] after=rough.densityBytes();for(int i=0;i<before.length;i++)assertTrue(after[i]<=before[i]);
        assertTrue(rough.polishedCount()>0);
    }
    @Test void overlappingMirroredCutsRemainSymmetric() {
        MarbleVolume v=new MarbleVolume();v.stroke(0.48,0.5,1,CarvingTool.ROUGH,true);
        for(int z=0;z<MarbleVolume.GRID;z++)for(int y=0;y<MarbleVolume.GRID;y++)for(int x=0;x<MarbleVolume.GRID;x++)
            assertEquals(v.node(x,y,z),v.node(MarbleVolume.GRID-1-x,y,z));
    }
    @Test void densityFinishAndMeshRoundTripWithoutSharingArrays() {
        MarbleVolume v=new MarbleVolume();v.stroke(0.5,0.5,1,CarvingTool.ROUGH);v.stroke(0,0.5,0.5,CarvingTool.POLISH);
        MarbleVolume restored=MarbleVolume.read(v.densityBytes(),v.polishBits());
        assertArrayEquals(v.densityBytes(),restored.densityBytes());assertArrayEquals(v.polishBits(),restored.polishBits());
        assertEquals(front(v).z(),front(restored).z(),1e-9);
        restored.stroke(0,0.5,0.5,CarvingTool.ROUGH);assertFalse(Arrays.equals(v.densityBytes(),restored.densityBytes()));
    }
    @Test void malformedAndNonFiniteTargetsCannotDamageStone() {
        MarbleVolume v=new MarbleVolume();byte[] original=v.densityBytes();
        assertEquals(0,v.stroke(Double.NaN,0.5,1,CarvingTool.ROUGH));
        assertEquals(0,v.stroke(Double.POSITIVE_INFINITY,0.5,1,CarvingTool.ROUGH));
        assertEquals(0,v.stroke(-1,0.5,1,CarvingTool.ROUGH));
        assertArrayEquals(original,v.densityBytes());
        assertEquals(MarbleVolume.CELLS,MarbleVolume.read(new byte[0],new long[0]).count());
        assertEquals(MarbleVolume.CELLS,MarbleVolume.read(new long[MarbleVolume.WORDS+1],new long[0]).count());
    }
    @Test void interpolatedMeshIsClosedAndHasOutwardWinding() {
        MarbleVolume v=new MarbleVolume();v.stroke(0.51,0.52,1,CarvingTool.ROUGH);
        var mesh=v.mesh();assertSame(mesh,v.mesh());
        java.util.Map<String,Integer> edges=new java.util.HashMap<>();
        for(var q:mesh.quads()) {
            var p=q.vertices();
            double ax=p[1].x()-p[0].x(),ay=p[1].y()-p[0].y(),az=p[1].z()-p[0].z();
            double bx=p[2].x()-p[0].x(),by=p[2].y()-p[0].y(),bz=p[2].z()-p[0].z();
            double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx;
            assertTrue(nx*p[0].nx()+ny*p[0].ny()+nz*p[0].nz()>=-1e-8);
            for(int i=0;i<4;i++) {
                var a=p[i];var b=p[(i+1)%4];String sa=a.x()+","+a.y()+","+a.z(),sb=b.x()+","+b.y()+","+b.z();
                String edge=sa.compareTo(sb)<0?sa+"|"+sb:sb+"|"+sa;edges.merge(edge,1,Integer::sum);
            }
        }
        assertTrue(edges.values().stream().allMatch(n->n==2),"Every mesh edge is shared by two faces");
    }
    private static void cube(byte[] data,int x0,int x1,int y0,int y1,int z0,int z1) {
        for(int z=z0;z<=z1;z++)for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++)data[MarbleVolume.nodeIndex(x,y,z)]=32;
    }
    @Test void loadingDetachedStoneKeepsTheBaseInsteadOfFloatingIslands() {
        byte[] data=new byte[MarbleVolume.NODES];Arrays.fill(data,(byte)-32);
        cube(data,5,9,2,6,5,9);cube(data,15,26,20,28,15,26);
        MarbleVolume v=MarbleVolume.read(data,new long[0]);
        assertTrue(v.field(6.0/32,3.0/32,6.0/32)>0);
        assertTrue(v.field(20.0/32,24.0/32,20.0/32)<0);
    }
    @Test void severingAThinNeckRemovesTheEntireDetachedTop() {
        byte[] data=new byte[MarbleVolume.NODES];Arrays.fill(data,(byte)-32);
        cube(data,10,20,2,10,10,20);cube(data,10,20,15,22,10,20);cube(data,15,15,11,14,15,15);
        MarbleVolume v=MarbleVolume.read(data,new long[0]);
        var hit=v.pick(new double[]{2,12.0/32,14.0/32},new double[]{-1,0,0});assertNotNull(hit);
        assertTrue(v.stroke(hit.x(),hit.y(),hit.z(),CarvingTool.POINT)>0);
        assertTrue(v.pruneDetached()>0);
        assertTrue(v.field(14.0/32,18.0/32,14.0/32)<0);
        assertTrue(v.field(14.0/32,5.0/32,14.0/32)>0);
    }
    @Test void legacyCarvingsMigrateToSmoothStoneWithoutResetting() {
        java.util.BitSet stone=new java.util.BitSet(MarbleVolume.CELLS);stone.set(0,MarbleVolume.CELLS);
        for(int z=24;z<32;z++)for(int y=12;y<20;y++)for(int x=12;x<20;x++)stone.clear(MarbleVolume.index(x,y,z));
        MarbleVolume migrated=MarbleVolume.read(stone.toLongArray(),new long[0]);
        assertTrue(migrated.count()<MarbleVolume.CELLS);assertTrue(front(migrated).z()<0.8);
    }
}
