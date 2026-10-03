import io.github.jakediscord.hobbymod.bonsai.BonsaiGraph;
import io.github.jakediscord.hobbymod.bonsai.BonsaiGrowthClock;
import java.util.HashSet;

/** Standalone Java 21 regression checks, deliberately outside the release source sets. */
public final class BonsaiGraphChecks {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        BonsaiGrowthClock clock=new BonsaiGrowthClock();
        check(!clock.poll(0),"initial tick grew");
        for(int t=1;t<1200;t++)check(!clock.poll(t),"growth before coarse interval");
        check(clock.poll(1200),"missing scheduled growth");
        check(!clock.poll(2400),"unloaded ticks caught up");
        check(!clock.poll(-100),"backwards time grew");
        check(!clock.poll(Long.MAX_VALUE),"rapid time change grew");
        clock.reset();check(!clock.poll(9000),"reload grew immediately");
        int smallest=28,largest=0;
        for(int seed=0;seed<3000;seed++){
            BonsaiGraph g=new BonsaiGraph();g.plant(BonsaiGraph.Species.values()[seed%3],seed);
            for(int tick=0;tick<200;tick++){
                if(g.water<45)g.water();if(g.soilAge>=30)g.repot();if(g.rootAge>=40)g.rootPrune();g.grow(1200);g.advance(true,false);
            }
            check(g.nodes().size()<=28,"unbounded graph");
            smallest=Math.min(smallest,g.nodes().size());largest=Math.max(largest,g.nodes().size());
            HashSet<Integer> ids=new HashSet<>();
            for(var n:g.nodes()){
                check(n.parent==0 || ids.contains(n.parent),"invalid parent ordering");ids.add(n.id);
                var end=g.end(n);check(Double.isFinite(end.x()+end.y()+end.z()),"invalid geometry");
                check(end.x()>=.08 && end.x()<=.92 && end.z()>=.08 && end.z()<=.92 && end.y()<=.94,"growth escaped pot");
            }
            check(g.prune(1)==0,"root must be protected");
            int before=g.nodes().size();int removed=g.prune(2);
            check(removed>0 && g.nodes().size()==before-removed,"subtree pruning count");
            for(var n:g.nodes())check(n.parent==0 || g.node(n.parent)!=null,"pruning orphaned child");
        }
        BonsaiGraph growing=new BonsaiGraph();growing.plant(BonsaiGraph.Species.OAK,7);
        check(growing.node(1).radius>=.075,"base radius too small");
        check(growing.node(1).growthTicks==0,"planted tree jumped to full size");
        growing.grow(1200);growing.advance(true,false);growing.advance(true,false);
        var bud=growing.nodes().get(2);double base=growing.visibleLength(bud,0);
        check(bud.growthTicks==0 && base<bud.length*.1,"new branch jumped to full length");
        BonsaiGraph resumed=new BonsaiGraph();
        for(var original:growing.nodes()){
            var copy=new BonsaiGraph.Node(original.id,original.parent,original.length,original.radius,original.yaw,original.pitch);
            copy.growthTicks=original.growthTicks;
            check(resumed.acceptLoaded(copy),"partly grown node rejected on reload");
        }
        check(resumed.visibleLength(resumed.node(bud.id),0)==base,"reload finished a partly grown branch");
        growing.grow(600);double half=growing.visibleLength(bud,0);
        check(half>base && half<bud.length && Math.abs(half-bud.length*.5)<1e-9,"not growing over the minute");
        check(growing.visibleLength(bud,.5)>half,"no partial tick interpolation");
        growing.grow(600);check(growing.visibleLength(bud,0)==bud.length,"branch failed to mature");
        growing.grow(5000);check(bud.growthTicks==1200,"growth exceeded cap");
        double yaw=growing.node(2).yaw,pitch=growing.node(2).pitch;
        growing.wire(2,false);growing.wire(2,true);
        check(Math.abs(growing.node(2).yaw-yaw)<1e-9 && Math.abs(growing.node(2).pitch-pitch)<1e-9,"reverse failed to undo direction");
        check(growing.careStatus(true).equals("Recovering") || growing.careStatus(true).equals("Healthy"),"missing healthy care feedback");
        growing.water=10;check(growing.careStatus(false).contains("dry") && growing.careStatus(false).contains("needs light"),"missing stress causes");
        BonsaiGraph leafy=new BonsaiGraph();leafy.plant(BonsaiGraph.Species.OAK,1);leafy.grow(1200);
        check(leafy.hasLeaves(leafy.node(2)),"branch should initially have foliage");
        var first=leafy.shear(2);
        check(first.leavesRemoved() && first.branchesCut()==0 && first.changed(),"first shear must remove leaves");
        check(leafy.nodes().size()==2 && !leafy.hasLeaves(leafy.node(2)) && leafy.health==98,"leaf shear must preserve wood and remove foliage");
        BonsaiGraph savedBare=new BonsaiGraph();
        for(var original:leafy.nodes()){
            var copy=new BonsaiGraph.Node(original.id,original.parent,original.length,original.radius,original.yaw,original.pitch);
            copy.leavesRemoved=original.leavesRemoved;copy.growthTicks=original.growthTicks;
            check(savedBare.acceptLoaded(copy),"defoliated node rejected on reload");
        }
        check(!savedBare.hasLeaves(savedBare.node(2)),"reload restored removed leaves");
        var second=savedBare.shear(2);
        check(!second.leavesRemoved() && second.branchesCut()==1 && savedBare.node(2)==null,"second shear must cut the branch");
        check(!leafy.shear(999).changed(),"invalid shear changed tree");
        leafy.shear(1);check(!leafy.shear(1).changed() && leafy.node(1)!=null,"two-stage shearing cut protected trunk");
        int deepest=0;
        BonsaiGraph stages=new BonsaiGraph();stages.plant(BonsaiGraph.Species.CHERRY,6);stages.grow(1200);
        for(int interval=0;interval<200;interval++){
            if(stages.water<45)stages.water();if(stages.soilAge>=30)stages.repot();if(stages.rootAge>=40)stages.rootPrune();
            var before=new HashSet<Integer>();for(var n:stages.nodes())before.add(n.id);
            stages.advance(true,false);
            for(var n:stages.nodes())if(!before.contains(n.id)){
                int depth=0;for(var parent=stages.node(n.parent);parent!=null;parent=stages.node(parent.parent))depth++;
                deepest=Math.max(deepest,depth);
                check(n.growthTicks==0 && stages.visibleLength(n,0)==0 && stages.foliageGrowth(n,0)==0,"later-stage growth appeared at a preset size");
                check(stages.end(n).distanceSquared(stages.start(n))==0,"new stem jumped away from parent");
                stages.grow(300);check(Math.abs(stages.visibleLength(n,0)-n.length*.25)<1e-9,"later-stage quarter growth wrong");
                check(Math.abs(stages.foliageGrowth(n,0)-.25)<1e-9,"later-stage foliage jumped");
                stages.grow(600);check(Math.abs(stages.visibleLength(n,0)-n.length*.75)<1e-9,"later-stage growth did not continue");
            }
            stages.grow(1200);
        }
        check(deepest>=3,"growth checks did not cover branches growing from branches");
        // Defoliating a branch removes its descendant foliage without touching wood.
        int wood=stages.nodes().size();stages.shear(1);
        check(stages.nodes().size()==wood && stages.nodes().stream().noneMatch(stages::hasLeaves),"subtree defoliation left leaves or cut wood");
        stages.water=0;int dry=stages.soilColor();stages.water=90;int wet=stages.soilColor();
        check(wet<dry,"soil did not darken with water");
        stages.water=20;check(stages.soilColor()>wet && stages.soilColor()<dry,"soil drying did not follow water level");
        check(new BonsaiGraph().soilColor()==0xFFFFFF,"empty pot should use dry soil");
        BonsaiGraph roots=new BonsaiGraph();roots.plant(BonsaiGraph.Species.OAK,1);
        check(!roots.rootPrune() && !roots.repot(),"new roots/soil should not be cut or repotted");
        for(int i=0;i<12;i++)roots.advance(true,false);
        check(roots.rootPrune() && roots.rootAge==0 && roots.soilAge==12,"root pruning must preserve soil age");
        check(roots.repot() && roots.soilAge==0 && roots.water==55,"repot must refresh soil and water");
        check(!roots.rootPrune(),"rapid root pruning accepted");
        BonsaiGraph bad=new BonsaiGraph();
        check(!bad.acceptLoaded(new BonsaiGraph.Node(2,1,.1,.01,0,0)),"orphan accepted");
        check(!bad.acceptLoaded(new BonsaiGraph.Node(2,0,.1,.01,0,0)),"noncanonical root accepted");
        check(bad.acceptLoaded(new BonsaiGraph.Node(1,0,.1,.01,0,0)),"root rejected");
        check(!bad.acceptLoaded(new BonsaiGraph.Node(1,0,.1,.01,0,0)),"duplicate ID accepted");
        check(!bad.acceptLoaded(new BonsaiGraph.Node(2,2,.1,.01,0,0)),"cycle accepted");
        check(!bad.acceptLoaded(new BonsaiGraph.Node(2,1,Double.NaN,.01,0,0)),"NaN accepted");
        check(!bad.acceptLoaded(new BonsaiGraph.Node(2,1,Double.POSITIVE_INFINITY,.01,0,0)),"infinity accepted");
        for(int id=2;id<=10000;id++)bad.acceptLoaded(new BonsaiGraph.Node(id,id-1,.1,.01,0,0));
        check(bad.nodes().size()==28,"deep graph not bounded");
        for(var n:bad.nodes())bad.end(n);
        BonsaiGraph edited=new BonsaiGraph();edited.plant(BonsaiGraph.Species.BIRCH,19);
        for(int t=0;t<1000;t++){
            if(edited.water<45)edited.water();if(edited.soilAge>=30)edited.repot();if(edited.rootAge>=40)edited.rootPrune();
            edited.grow(1200);edited.advance(true,false);
            if(t%11==0 && edited.nodes().size()>3)edited.prune(edited.nodes().get(edited.nodes().size()-1).id);
            for(var n:edited.nodes())check(n.parent==0 || edited.node(n.parent)!=null,"growth/pruning interleave orphaned child");
        }
        BonsaiGraph wired=new BonsaiGraph();wired.plant(BonsaiGraph.Species.OAK,42);
        for(int i=0;i<6;i++)check(wired.wire(2,false),"safe bend broke early");
        check(!wired.wire(2,false) && wired.node(2)==null,"severe bend must break");
        BonsaiGraph stressed=new BonsaiGraph();stressed.plant(BonsaiGraph.Species.CHERRY,0);
        for(int i=0;i<40;i++)stressed.advance(false,false);
        check(stressed.health==0,"neglect should kill foliage");
        stressed.health=100;stressed.water=80;stressed.water();check(stressed.health==88 && stressed.node(1).health==88,"overwatering/foliage health not updated immediately");
        check(smallest>=20,"some trees matured with fewer than 20 meaningful branches: "+smallest);
        System.out.println("BONSAI_GRAPH_PASS: 3000 aged trees; mature nodes "+smallest+".."+largest+"; pruning, wiring, stress, corrupt and deep graphs");
    }
}
