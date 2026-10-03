package io.github.jakediscord.hobbymod.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Bounded, parent-before-child graph. No recursive traversal, including corrupt saves. */
public final class BonsaiGraph {
    public static final int MAX_NODES = 28;
    public static final int GROWTH_TICKS = 1200;
    public enum Species { OAK, BIRCH, CHERRY }
    public static final class Node {
        public final int id, parent;
        public double length, radius, yaw, pitch;
        public int age, health = 100, bend;
        public int growthTicks = GROWTH_TICKS;
        public boolean bud = true, wired, leavesRemoved;
        public Node(int id, int parent, double length, double radius, double yaw, double pitch) {
            this.id = id; this.parent = parent; this.length = length; this.radius = radius;
            this.yaw = yaw; this.pitch = pitch;
        }
    }
    public record Point(double x, double y, double z) {
        public double distanceSquared(Point b) { return Math.pow(x-b.x,2)+Math.pow(y-b.y,2)+Math.pow(z-b.z,2); }
    }
    private final List<Node> nodes = new ArrayList<>();
    public Species species = Species.OAK;
    public int age, water = 55, health = 100, soilAge, rootAge, nextId = 1;
    public long seed;
    public List<Node> nodes() { return List.copyOf(nodes); }
    public boolean planted() { return !nodes.isEmpty(); }
    public void plant(Species species, long seed) {
        if (planted()) return;
        this.species = species; this.seed = seed;
        nodes.add(new Node(nextId++, 0, .24, .075, 0, 0));
        nodes.add(new Node(nextId++, 1, .18, .036, 0, .15));
        for(Node n:nodes)n.growthTicks=0;
    }
    public Node node(int id) { for (Node n : nodes) if (n.id == id) return n; return null; }
    public boolean grow(int ticks) {
        int elapsed=Math.max(0,Math.min(GROWTH_TICKS,ticks));
        if(elapsed==0)return false;
        boolean changed=false;
        for(Node n:nodes) if(n.growthTicks<GROWTH_TICKS){n.growthTicks=Math.min(GROWTH_TICKS,n.growthTicks+elapsed);changed=true;}
        return changed;
    }
    public double growth(Node n, double partial) {
        return Math.max(0,Math.min(1,(n.growthTicks+partial)/GROWTH_TICKS));
    }
    public double visibleLength(Node n, double partial) {
        return n.length*growth(n,partial);
    }
    public Point start(Node n) { return start(n,0); }
    public Point start(Node n, double partial) {
        Point p = new Point(.5, .19, .5);
        // Parent IDs decrease and the list is bounded; invalid links cannot recurse forever.
        int parent = n.parent;
        for (int i = 0; parent != 0 && i < MAX_NODES; i++) {
            Node a = node(parent); if (a == null) break;
            Point v = vector(a,partial); p = new Point(p.x+v.x, p.y+v.y, p.z+v.z); parent = a.parent;
        }
        return p;
    }
    private Point vector(Node n, double partial) {
        double length=visibleLength(n,partial);
        return new Point(Math.sin(n.pitch)*Math.cos(n.yaw)*length,
                Math.cos(n.pitch)*length, Math.sin(n.pitch)*Math.sin(n.yaw)*length);
    }
    public Point end(Node n) { return end(n,0); }
    public Point end(Node n,double partial) { Point a=start(n,partial), b=vector(n,partial); return new Point(a.x+b.x,a.y+b.y,a.z+b.z); }
    public int nearest(Point hit) {
        int id=0; double best=.12*.12;
        for (Node n : nodes) {
            if(visibleLength(n,0)==0)continue;
            Point a=start(n), b=end(n);
            double dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z;
            double t=Math.max(0,Math.min(1,((hit.x-a.x)*dx+(hit.y-a.y)*dy+(hit.z-a.z)*dz)/(visibleLength(n,0)*visibleLength(n,0))));
            double d=hit.distanceSquared(new Point(a.x+t*dx,a.y+t*dy,a.z+t*dz));
            if(d<best){best=d;id=n.id;}
        }
        return id;
    }
    public record ShearResult(boolean leavesRemoved,int branchesCut) {
        public boolean changed(){return leavesRemoved || branchesCut>0;}
    }
    /** Defoliation is saved on the node, so a second click cuts the same branch. */
    public ShearResult shear(int id) {
        Node n=node(id);
        if(n==null)return new ShearResult(false,0);
        if(!n.leavesRemoved){
            List<Integer> ids=subtree(id);
            for(Node child:nodes)if(ids.contains(child.id))child.leavesRemoved=true;
            damage(2);
            return new ShearResult(true,0);
        }
        return new ShearResult(false,prune(id));
    }
    public boolean hasLeaves(Node n) {
        return !n.leavesRemoved && n.health>0
                && (n.bud || nodes.stream().noneMatch(child->child.parent==n.id));
    }
    /** Return the remaining foliage size, including growth of later-generation buds. */
    public double foliageGrowth(Node n,double partial) {
        return hasLeaves(n)?growth(n,partial):0;
    }
    /** Darken the existing dirt texture continuously as water content rises. */
    public int soilColor() {
        int shade=planted()?255-(int)Math.round(Math.max(0,Math.min(100,water))*1.2):255;
        return (shade<<16)|(shade<<8)|shade;
    }
    private List<Integer> subtree(int id) {
        List<Integer> ids=new ArrayList<>();ids.add(id);
        for(Node n:nodes)if(ids.contains(n.parent))ids.add(n.id);
        return ids;
    }
    public int prune(int id) {
        if(id<=1 || node(id)==null) return 0;
        List<Integer> removed=subtree(id);
        nodes.removeIf(n -> removed.contains(n.id));
        damage(removed.size()*6);
        for(Node n:nodes) if(n.health>0) n.bud=true;
        return removed.size();
    }
    /** Each bend is 12 degrees. Repeated severe bending breaks the selected subtree. */
    public boolean wire(int id, boolean reverse) {
        Node n=node(id); if(n==null || id==1 || n.health==0) return false;
        n.wired=true; n.bend+=12;
        if(n.bend>72){prune(id); return false;}
        n.yaw += reverse ? -Math.PI/15 : Math.PI/15;
        n.pitch=Math.max(0,Math.min(1.5,n.pitch+(reverse?-.08:.08))); damage(1); return true;
    }
    private void damage(int amount){health=Math.max(0,health-amount);for(Node n:nodes)n.health=health;}
    public void water() { if(water>75) damage(12); water=Math.min(100,water+35); }
    public boolean rootPrune() {
        if(rootAge<12)return false;
        rootAge=0;damage(8);return true;
    }
    public boolean repot() {
        if(soilAge<12) return false;
        soilAge=0; water=55; damage(4); return true;
    }
    public String careStatus(boolean goodLight) {
        List<String> causes=new ArrayList<>();
        if(water<15)causes.add("dry");else if(water>85)causes.add("too wet");
        if(!goodLight)causes.add("needs light");
        if(soilAge>40)causes.add("repot needed");
        if(rootAge>60)causes.add("root pruning needed");
        return causes.isEmpty()?(health==100?"Healthy":"Recovering"):String.join(", ",causes);
    }
    public void advance(boolean goodLight, boolean rain) {
        if(!planted()) return;
        age=Math.min(1_000_000,age+1); soilAge=Math.min(1_000_000,soilAge+1); rootAge=Math.min(1_000_000,rootAge+1);
        water=Math.max(0,Math.min(100,water+(rain?3:-2)));
        boolean stressed=water<15 || water>85 || !goodLight || soilAge>40 || rootAge>60;
        health=Math.max(0,Math.min(100,health+(stressed?-4:2)));
        for(Node n:nodes){n.age=Math.min(1_000_000,n.age+1);n.health=health; n.radius=Math.min(n.id==1?.095:n.id==2?.05:.026,n.radius+.00015);}
        if(health<35 || nodes.size()>=MAX_NODES || age%2!=0 || nextId>1_000_000) return;
        Random random=new Random(seed+age*7919L);
        List<Node> candidates=nodes.stream().filter(n -> n.bud && n.health>0 && end(n).y<.78).toList();
        if(candidates.isEmpty()) return;
        Node parent=candidates.get(random.nextInt(candidates.size()));
        long children=nodes.stream().filter(n->n.parent==parent.id).count();
        if(children>=3){parent.bud=false;return;}
        double yaw=parent.yaw+children*2.399+random.nextDouble()*.8;
        double pitch=species==Species.BIRCH?.4+random.nextDouble()*.55:.65+random.nextDouble()*.65;
        Node child=new Node(nextId++,parent.id,.09+random.nextDouble()*.07,Math.max(.007,Math.min(.026,parent.radius*.65)),yaw,pitch);
        Point endpoint=end(child);
        if(endpoint.x<.08 || endpoint.x>.92 || endpoint.z<.08 || endpoint.z>.92 || endpoint.y>.94){parent.bud=false;return;}
        child.growthTicks=0;
        nodes.add(child);
    }
    public void clearForLoad(){nodes.clear();}
    public boolean acceptLoaded(Node n) {
        if(nodes.size()>=MAX_NODES || n.id<=0 || n.id>1_000_000 || node(n.id)!=null ||
                (nodes.isEmpty()?n.parent!=0 || n.id!=1:n.parent==0 || n.parent>=n.id || node(n.parent)==null) ||
                !Double.isFinite(n.length+n.radius+n.yaw+n.pitch)) return false;
        n.length=Math.max(.02,Math.min(.25,n.length));n.radius=Math.max(.004,Math.min(n.id==1?.095:.05,n.radius));
        n.pitch=Math.max(0,Math.min(1.5,n.pitch));n.yaw%=Math.PI*2;
        n.age=Math.max(0,Math.min(1_000_000,n.age));n.health=Math.max(0,Math.min(100,n.health));
        n.bend=Math.max(0,Math.min(72,n.bend));n.growthTicks=Math.max(0,Math.min(GROWTH_TICKS,n.growthTicks));
        if(n.id==1)n.radius=Math.max(.075,n.radius);
        nodes.add(n);nextId=Math.max(nextId,n.id+1);return true;
    }
}
