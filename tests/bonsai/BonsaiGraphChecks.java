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
                if(g.water<45)g.water();if(g.soilAge>=30)g.repot();if(g.rootAge>=40)g.rootPrune();g.advance(true,false);
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
            edited.advance(true,false);
            if(t%11==0 && edited.nodes().size()>3)edited.prune(edited.nodes().get(edited.nodes().size()-1).id);
            for(var n:edited.nodes())check(n.parent==0 || edited.node(n.parent)!=null,"growth/pruning interleave orphaned child");
        }
        BonsaiGraph wired=new BonsaiGraph();wired.plant(BonsaiGraph.Species.OAK,42);
        for(int i=0;i<6;i++)check(wired.wire(2,false),"safe bend broke early");
        check(!wired.wire(2,false) && wired.node(2)==null,"severe bend must break");
        BonsaiGraph stressed=new BonsaiGraph();stressed.plant(BonsaiGraph.Species.CHERRY,0);
        for(int i=0;i<40;i++)stressed.advance(false,false);
        check(stressed.health==0,"neglect should kill foliage");
        stressed.health=100;stressed.water=80;stressed.water();check(stressed.health==88,"overwatering not penalized");
        check(smallest>=20,"some trees matured with fewer than 20 meaningful branches: "+smallest);
        System.out.println("BONSAI_GRAPH_PASS: 3000 aged trees; mature nodes "+smallest+".."+largest+"; pruning, wiring, stress, corrupt and deep graphs");
    }
}
