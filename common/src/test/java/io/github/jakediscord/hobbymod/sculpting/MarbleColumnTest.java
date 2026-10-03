package io.github.jakediscord.hobbymod.sculpting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarbleColumnTest {
    @Test void joinedSectionsFormOneClosedSurfaceWithoutInternalCaps() {
        var bottom=new MarbleVolume();var top=new MarbleVolume();
        assertClosed(bottom,top);
        bottom.applyBrush(0.5,1,1,CarvingTool.ROUGH,new double[]{0,0,1});
        top.applyBrush(0.5,0,1,CarvingTool.ROUGH,new double[]{0,0,1});
        assertClosed(bottom,top);
    }
    private static void assertClosed(MarbleVolume bottom,MarbleVolume top) {
        var meshes=List.of(MarbleMesh.build(MarbleColumn.field(bottom,null,top),false,true),
                MarbleMesh.build(MarbleColumn.field(top,bottom,null),true,false));
        var edges=new HashMap<String,Integer>();
        for(int section=0;section<2;section++)for(var quad:meshes.get(section).quads()) {
            var vertices=quad.vertices();final int offset=section;
            assertFalse(Arrays.stream(vertices).allMatch(p->Math.abs(p.y()+offset-1)<1e-7)
                    && Math.abs(quad.a().ny())>0.9,"No horizontal internal caps");
            for(int i=0;i<4;i++) {
                String a=key(vertices[i],section),b=key(vertices[(i+1)%4],section);
                edges.merge(a.compareTo(b)<0?a+"|"+b:b+"|"+a,1,Integer::sum);
            }
        }
        assertTrue(edges.values().stream().allMatch(n->n==2),"Each joined edge has exactly two faces");
        var lowerHit=meshes.getFirst().pick(new double[]{0.5,1,2},new double[]{0,0,-1});
        var upperHit=meshes.getLast().pick(new double[]{0.5,0,2},new double[]{0,0,-1});
        assertTrue(lowerHit!=null || upperHit!=null,"The join has no picking gap");
    }
    private static String key(MarbleMesh.Vertex p,int section) {
        return Math.round(p.x()*1e8)+","+Math.round((p.y()+section)*1e8)+","+Math.round(p.z()*1e8);
    }
    @Test void pruningUsesConnectionsAcrossTheSeamAndRemovesSeveredTop() {
        var bottom=new MarbleVolume();var top=new MarbleVolume();
        var volumes=List.of(bottom,top);MarbleColumn.prune(volumes);
        assertEquals(MarbleVolume.CELLS,bottom.count());assertEquals(MarbleVolume.CELLS,top.count());
        for(int z=1;z<MarbleVolume.GRID-1;z++)for(int x=1;x<MarbleVolume.GRID-1;x++)
            bottom.removeNode(MarbleVolume.nodeIndex(x,20,z));
        MarbleColumn.prune(volumes);
        assertEquals(0,top.count(),"Upper block cannot float after lower stone is severed");
        assertTrue(bottom.count()>0 && bottom.field(0.5,0.8,0.5)<0);
    }
    @Test void seamBrushCutsBothSectionsAndRaspOnlyRemovesMaterial() {
        var bottom=new MarbleVolume();var top=new MarbleVolume();
        bottom.applyBrush(0.5,1,1,CarvingTool.ROUGH,new double[]{0,0,1});
        top.applyBrush(0.5,0,1,CarvingTool.ROUGH,new double[]{0,0,1});
        MarbleColumn.prune(List.of(bottom,top));
        assertTrue(bottom.field(0.5,0.97,0.98)<0 && top.field(0.5,0.03,0.98)<0);
        byte[] before=bottom.densityBytes();bottom.stroke(0.6,0.9,1,CarvingTool.POLISH);
        byte[] after=bottom.densityBytes();for(int i=0;i<before.length;i++)assertTrue(after[i]<=before[i]);
    }
    @Test void sectionsSupportedThroughAnotherBlockSurviveReloadAndPruning() {
        byte[] a=new byte[MarbleVolume.NODES],b=new byte[MarbleVolume.NODES];
        Arrays.fill(a,(byte)-32);Arrays.fill(b,(byte)-32);
        for(int z=10;z<=20;z++) {
            for(int y=2;y<=33;y++)for(int x=6;x<=24;x++)if(x<=10 || x>=20)a[MarbleVolume.nodeIndex(x,y,z)]=32;
            for(int y=1;y<=10;y++)for(int x=6;x<=24;x++)b[MarbleVolume.nodeIndex(x,y,z)]=32;
        }
        var bottom=MarbleVolume.read(a,new long[0],false);var top=MarbleVolume.read(b,new long[0],false);
        MarbleColumn.prune(List.of(bottom,top));
        assertTrue(bottom.field(8.0/32,0.5,14.0/32)>0 && bottom.field(22.0/32,0.5,14.0/32)>0,
                "Both legs remain attached through the bridge in the upper section");
        assertArrayEquals(bottom.densityBytes(),MarbleVolume.read(bottom.densityBytes(),bottom.polishBits(),false).densityBytes(),
                "Loading an individual section must not discard stone connected through its neighbor");
    }
}
