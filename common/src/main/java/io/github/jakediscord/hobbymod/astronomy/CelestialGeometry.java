package io.github.jakediscord.hobbymod.astronomy;

/** Full closed cubes in celestial coordinates, independent of camera yaw, pitch and position. */
public final class CelestialGeometry {
    public static final int[][] FACES={{0,2,3,1},{5,7,6,4},{4,6,2,0},{1,3,7,5},{2,6,7,3},{4,0,1,5}};
    public record Frame(SkyCatalog.Vector x,SkyCatalog.Vector y,SkyCatalog.Vector z){
        public SkyCatalog.Vector local(double X,double Y,double Z){return new SkyCatalog.Vector(x.x()*X+y.x()*Y+z.x()*Z,x.y()*X+y.y()*Y+z.y()*Z,x.z()*X+y.z()*Y+z.z()*Z);}
    }
    public static Frame frame(int id,double time){
        var planet=CelestialBodies.target(id);double spin=time/24000/(planet==null?(id==-1?1:8):planet.rotationDays())*Math.PI*2+id*.47;
        double tilt=Math.toRadians(planet==null?(id==-1?23.4:15+Math.floorMod(id,45)):planet.tilt());
        double cs=Math.cos(spin),sn=Math.sin(spin),ct=Math.cos(tilt),st=Math.sin(tilt);
        return new Frame(observer(new SkyCatalog.Vector(cs,sn*st,sn*ct),time),observer(new SkyCatalog.Vector(0,ct,-st),time),observer(new SkyCatalog.Vector(-sn,cs*st,cs*ct),time));
    }
    public static SkyCatalog.Vector observer(SkyCatalog.Vector inertial,double time){
        double days=time/24000,angle=days*Math.PI*2-Math.floor(days)/32*Math.PI*2;
        double X=inertial.x()*Math.cos(angle)+inertial.z()*Math.sin(angle),Z=-inertial.x()*Math.sin(angle)+inertial.z()*Math.cos(angle),half=Math.sqrt(.5);
        return new SkyCatalog.Vector(-Z,half*(inertial.y()+X),half*(X-inertial.y()));
    }
    public static SkyCatalog.Vector[] cube(SkyCatalog.Vector center,double halfSize,Frame frame){
        var vertices=new SkyCatalog.Vector[8];for(int i=0;i<8;i++){var p=frame.local((i&4)==0?-halfSize:halfSize,(i&2)==0?-halfSize:halfSize,(i&1)==0?-halfSize:halfSize);vertices[i]=new SkyCatalog.Vector(center.x()+p.x(),center.y()+p.y(),center.z()+p.z());}return vertices;
    }
    public static boolean front(int face,Frame frame,SkyCatalog.Vector direction){var n=normal(face,frame);return n.dot(direction)<0;}
    public static SkyCatalog.Vector normal(int face,Frame f){return switch(face){case 0->f.x().scale(-1);case 1->f.x();case 2->f.z().scale(-1);case 3->f.z();case 4->f.y();default->f.y().scale(-1);};}
    private CelestialGeometry(){}
}
