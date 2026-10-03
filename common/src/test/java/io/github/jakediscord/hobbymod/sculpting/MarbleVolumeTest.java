package io.github.jakediscord.hobbymod.sculpting;

import io.github.jakediscord.hobbymod.sculpting.client.OrbitCamera;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarbleVolumeTest {
    @Test void chipsExposeDeeperMarbleWithoutChangingOtherCells() {
        MarbleVolume volume = new MarbleVolume();
        double[] origin={0.51,0.51,2}, direction={0,0,-1};
        var first=volume.pick(origin,direction);
        assertEquals(31,MarbleVolume.z(first.cell()));
        assertEquals(1,volume.stroke(first.cell(),CarvingTool.DETAIL));
        var deeper=volume.pick(origin,direction);
        assertEquals(30,MarbleVolume.z(deeper.cell()));
        assertEquals(MarbleVolume.CELLS-1,volume.count());
        assertTrue(volume.has(15,16,31));
        assertEquals(0,volume.stroke(MarbleVolume.index(16,16,16),CarvingTool.ROUGH));
    }
    @Test void differentToolsAndPolishingChangeDifferentAmounts() {
        int cell=MarbleVolume.index(16,16,31);
        MarbleVolume fine=new MarbleVolume(), point=new MarbleVolume(), rough=new MarbleVolume();
        int f=fine.stroke(cell,CarvingTool.DETAIL), p=point.stroke(cell,CarvingTool.POINT), r=rough.stroke(cell,CarvingTool.ROUGH);
        assertTrue(f<p && p<r);
        MarbleVolume polished=new MarbleVolume();
        int count=polished.count();
        assertTrue(polished.stroke(cell,CarvingTool.POLISH)>0);
        assertEquals(count,polished.count());
        assertEquals(0,polished.stroke(cell,CarvingTool.POLISH));
    }
    @Test void savedGeometryAndFinishRoundTripAndRemainIndependent() {
        MarbleVolume volume=new MarbleVolume();
        volume.stroke(MarbleVolume.index(16,16,31),CarvingTool.ROUGH);
        volume.stroke(MarbleVolume.index(0,10,10),CarvingTool.POLISH);
        MarbleVolume restored=MarbleVolume.read(volume.marbleBits(),volume.polishBits());
        assertArrayEquals(volume.marbleBits(),restored.marbleBits());
        assertArrayEquals(volume.polishBits(),restored.polishBits());
        restored.stroke(MarbleVolume.index(0,10,10),CarvingTool.DETAIL);
        assertNotEquals(volume.count(),restored.count());
    }
    @Test void finalCellAndInvalidTargetsAreProtected() {
        MarbleVolume one=MarbleVolume.read(new long[]{1},new long[0]);
        assertEquals(0,one.stroke(0,CarvingTool.DETAIL));
        assertEquals(1,one.count());
        assertEquals(0,one.stroke(-1,CarvingTool.ROUGH));
        assertEquals(0,one.stroke(MarbleVolume.CELLS,CarvingTool.POLISH));
        assertEquals(MarbleVolume.CELLS,MarbleVolume.read(new long[MarbleVolume.WORDS+1],new long[0]).count());
    }
    @Test void greedyMeshPreservesSurfaceAreaAndWinding() {
        MarbleVolume volume=new MarbleVolume();
        assertEquals(6,volume.faces().size());
        assertEquals(6*32*32,volume.faces().stream().mapToInt(f->f.width()*f.height()).sum());
        volume.stroke(MarbleVolume.index(16,16,31),CarvingTool.DETAIL);
        // One face removed, five newly exposed: exterior area increases by four cells.
        assertEquals(6*32*32+4,volume.faces().stream().mapToInt(f->f.width()*f.height()).sum());
        for (var face:volume.faces()) {
            var p=face.vertices();
            double[] a={p[1][0]-p[0][0],p[1][1]-p[0][1],p[1][2]-p[0][2]};
            double[] b={p[2][0]-p[0][0],p[2][1]-p[0][1],p[2][2]-p[0][2]};
            double[] normal={a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
            assertTrue(normal[face.side()/2]*(face.side()%2==1?1:-1)>0);
        }
    }
    @Test void orbitProjectionAndPickingAgreeFromEverySide() {
        MarbleVolume volume=new MarbleVolume();
        OrbitCamera camera=new OrbitCamera();
        for (int angle=0;angle<360;angle+=30) {
            camera.yaw=Math.toRadians(angle);
            for (double pitch:new double[]{-1.1,0,1.1}) {
                camera.pitch=pitch;
                double[] p=camera.view(0.27,0.71,0.82);
                double[] restored=camera.inverse(p[0],p[1],p[2]);
                assertArrayEquals(new double[]{-0.23,0.21,0.32},restored,1e-9);
                assertNotNull(volume.pick(camera.rayOrigin(0,0),camera.rayDirection()));
            }
        }
        assertNull(volume.pick(new double[]{2,2,2},new double[]{0,0,-1}));
    }
}
