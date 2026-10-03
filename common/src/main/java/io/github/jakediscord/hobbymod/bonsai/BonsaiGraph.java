package io.github.jakediscord.hobbymod.bonsai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Bounded, parent-before-child graph. No recursive traversal, including corrupt saves. */
public final class BonsaiGraph {
    public static final int MAX_NODES = 28;
    public enum Species { OAK, BIRCH, CHERRY }
    public static final class Node {
        public final int id, parent;
        public double length, radius, yaw, pitch;
        public int age, health = 100, bend;
        public boolean bud = true, wired;
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
        nodes.add(new Node(nextId++, 0, .24, .035, 0, 0));
        nodes.add(new Node(nextId++, 1, .18, .023, 0, .15));
    }
    public Node node(int id) { for (Node n : nodes) if (n.id == id) return n; return null; }
    public Point start(Node n) {
        Point p = new Point(.5, .19, .5);
        // Parent IDs decrease and the list is bounded; invalid links cannot recurse forever.
        int parent = n.parent;
        for (int i = 0; parent != 0 && i < MAX_NODES; i++) {
            Node a = node(parent); if (a == null) break;
            Point v = vector(a); p = new Point(p.x+v.x, p.y+v.y, p.z+v.z); parent = a.parent;
        }
        return p;
    }
    private Point vector(Node n) {
        return new Point(Math.sin(n.pitch)*Math.cos(n.yaw)*n.length,
                Math.cos(n.pitch)*n.length, Math.sin(n.pitch)*Math.sin(n.yaw)*n.length);
    }
    public Point end(Node n) { Point a=start(n), b=vector(n); return new Point(a.x+b.x,a.y+b.y,a.z+b.z); }
    public int nearest(Point hit) {
        int id=0; double best=.12*.12;
        for (Node n : nodes) {
            Point a=start(n), b=end(n);
            double dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z;
            double t=Math.max(0,Math.min(1,((hit.x-a.x)*dx+(hit.y-a.y)*dy+(hit.z-a.z)*dz)/(n.length*n.length)));
            double d=hit.distanceSquared(new Point(a.x+t*dx,a.y+t*dy,a.z+t*dz));
            if(d<best){best=d;id=n.id;}
        }
        return id;
    }
    public int prune(int id) {
        if(id<=1 || node(id)==null) return 0;
        List<Integer> removed=new ArrayList<>(); removed.add(id);
        for(Node n:nodes) if(removed.contains(n.parent)) removed.add(n.id);
        nodes.removeIf(n -> removed.contains(n.id));
        health=Math.max(0,health-removed.size()*6);
        for(Node n:nodes) if(n.health>0) n.bud=true;
        return removed.size();
    }
    /** Each bend is 12 degrees. Repeated severe bending breaks the selected subtree. */
    public boolean wire(int id, boolean reverse) {
        Node n=node(id); if(n==null || id==1 || n.health==0) return false;
        n.wired=true; n.bend+=12;
        if(n.bend>72){prune(id); return false;}
        n.yaw += reverse ? -.21 : .21;
        n.pitch=Math.min(1.5,n.pitch+.08); health=Math.max(0,health-1); return true;
    }
    public void water() { if(water>75) health=Math.max(0,health-12); water=Math.min(100,water+35); }
    public boolean rootPrune() {
        if(rootAge<12)return false;
        rootAge=0;health=Math.max(0,health-8);return true;
    }
    public boolean repot() {
        if(soilAge<12) return false;
        soilAge=0; water=55; health=Math.max(0,health-4); return true;
    }
    public void advance(boolean goodLight, boolean rain) {
        if(!planted()) return;
        age=Math.min(1_000_000,age+1); soilAge=Math.min(1_000_000,soilAge+1); rootAge=Math.min(1_000_000,rootAge+1);
        water=Math.max(0,Math.min(100,water+(rain?3:-2)));
        boolean stressed=water<15 || water>85 || !goodLight || soilAge>40 || rootAge>60;
        health=Math.max(0,Math.min(100,health+(stressed?-4:2)));
        for(Node n:nodes){n.age=Math.min(1_000_000,n.age+1);n.health=health; n.radius=Math.min(.045,n.radius+.00015);}
        if(health<35 || nodes.size()>=MAX_NODES || age%2!=0 || nextId>1_000_000) return;
        Random random=new Random(seed+age*7919L);
        List<Node> candidates=nodes.stream().filter(n -> n.bud && n.health>0 && end(n).y<.78).toList();
        if(candidates.isEmpty()) return;
        Node parent=candidates.get(random.nextInt(candidates.size()));
        long children=nodes.stream().filter(n->n.parent==parent.id).count();
        if(children>=3){parent.bud=false;return;}
        double yaw=parent.yaw+children*2.399+random.nextDouble()*.8;
        double pitch=species==Species.BIRCH?.4+random.nextDouble()*.55:.65+random.nextDouble()*.65;
        Node child=new Node(nextId++,parent.id,.09+random.nextDouble()*.07,Math.max(.007,parent.radius*.65),yaw,pitch);
        Point endpoint=end(child);
        if(endpoint.x<.08 || endpoint.x>.92 || endpoint.z<.08 || endpoint.z>.92 || endpoint.y>.94){parent.bud=false;return;}
        nodes.add(child);
    }
    public void clearForLoad(){nodes.clear();}
    public boolean acceptLoaded(Node n) {
        if(nodes.size()>=MAX_NODES || n.id<=0 || n.id>1_000_000 || node(n.id)!=null ||
                (nodes.isEmpty()?n.parent!=0 || n.id!=1:n.parent==0 || n.parent>=n.id || node(n.parent)==null) ||
                !Double.isFinite(n.length+n.radius+n.yaw+n.pitch)) return false;
        n.length=Math.max(.02,Math.min(.25,n.length));n.radius=Math.max(.004,Math.min(.05,n.radius));
        n.pitch=Math.max(0,Math.min(1.5,n.pitch));n.yaw%=Math.PI*2;
        n.age=Math.max(0,Math.min(1_000_000,n.age));n.health=Math.max(0,Math.min(100,n.health));
        n.bend=Math.max(0,Math.min(72,n.bend));nodes.add(n);nextId=Math.max(nextId,n.id+1);return true;
    }
}
