package io.github.jakediscord.hobbymod.pottery;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotteryShapeTest {
    @Test void openingAndThrowingRedistributeClayInsteadOfCreatingIt() {
        var p=new PotteryShape();double mass=p.mass();assertTrue(p.openCenter());assertFalse(p.openCenter());
        assertEquals(mass,p.volume(p.wall()),1e-8);
        double old=p.radius(.5);assertTrue(p.throwClay(.5,.018,0,false));assertTrue(p.radius(.5)>old);
        assertEquals(mass,p.volume(p.wall()),1e-8);
        assertTrue(p.throwClay(.9,-.01,.02,false));assertEquals(mass,p.volume(p.wall()),1e-8);
        assertTrue(p.wall()>=PotteryShape.MIN_WALL);assertTrue(p.radius(.5)>p.wall());
    }
    @Test void smoothMeshHasACavityRoundedRimAndClosedEdges() {
        var p=new PotteryShape();p.openCenter();p.throwClay(.55,.02,0,false);var mesh=p.mesh();assertSame(mesh,p.mesh());
        var floor=mesh.pick(new double[]{.5,2,.5},new double[]{0,-1,0});assertNotNull(floor);
        assertEquals(p.height()*PotteryShape.FLOOR,floor.y(),1e-8,"Ray enters the open pot and hits its floor");
        assertTrue(mesh.quads().stream().flatMap(q->Arrays.stream(q.vertices())).anyMatch(v->v.y()>p.height()+.005),"Lip is rounded above the wall");
        var edges=new HashMap<String,Integer>();
        for(var q:mesh.quads()){var vertices=q.vertices();for(int i=0;i<4;i++){
            String a=key(vertices[i]),b=key(vertices[(i+1)%4]);if(a.equals(b))continue;
            edges.merge(a.compareTo(b)<0?a+"|"+b:b+"|"+a,1,Integer::sum);
        }}
        assertTrue(edges.values().stream().allMatch(n->n==2),"Outer wall, rim, inner wall and floors form one closed solid");
    }
    private static String key(io.github.jakediscord.hobbymod.sculpting.MarbleMesh.Vertex v){return Math.round(v.x()*1e6)+","+Math.round(v.y()*1e6)+","+Math.round(v.z()*1e6);}
    @Test void spongeActuallySmoothsTheProfileWhileTrimmingRemovesClay() {
        var p=new PotteryShape();p.openCenter();for(int i=0;i<5;i++)p.throwClay(.5,.008,0,false);
        double mass=p.mass(),rough=roughness(p);for(int i=0;i<15;i++)p.throwClay(.5,0,0,true);
        assertTrue(roughness(p)<rough);assertEquals(mass,p.mass(),1e-8);
        double bottom=p.radius(.1);assertTrue(p.trim(.1));assertTrue(p.radius(.1)<bottom);assertTrue(p.mass()<mass);
        assertFalse(p.trim(.9));
    }
    private static double roughness(PotteryShape p){double[] a=p.data();double sum=0;for(int i=5;i<a.length-1;i++)sum+=Math.pow(a[i-1]-2*a[i]+a[i+1],2);return sum;}
    @Test void BoundsAndMalformedDataCannotBreakTheWalls() {
        var p=new PotteryShape();p.openCenter();double[] before=p.data();
        assertFalse(p.throwClay(Double.NaN,.01,0,false));assertFalse(p.throwClay(.5,1,0,false));assertArrayEquals(before,p.data());
        var random=new Random(42);for(int i=0;i<1500;i++)p.throwClay(random.nextDouble(),(random.nextDouble()-.5)*.04,(random.nextDouble()-.5)*.04,i%5==0);
        for(int i=0;i<100;i++)assertTrue(p.radius(i/99.0)>p.wall()+.008);
        assertEquals(p.mass(),p.volume(p.wall()),1e-8);
        assertArrayEquals(p.data(),PotteryShape.read(p.data()).data(),1e-8);
        assertFalse(PotteryShape.read(new double[]{Double.NaN}).open());
    }
    @Test void closedClayCanBePickedAndFiringShrinkageKeepsTheDesign() {
        var p=new PotteryShape();assertEquals(p.height(),p.mesh().pick(new double[]{.5,2,.5},new double[]{0,-1,0}).y(),1e-8);
        p.openCenter();p.throwClay(.6,.02,0,false);double ratio=p.radius(.6)/p.radius(.15),height=p.height();
        p.shrink(.94);assertEquals(height*.94,p.height(),1e-8);assertEquals(ratio,p.radius(.6)/p.radius(.15),1e-8);
    }
}
