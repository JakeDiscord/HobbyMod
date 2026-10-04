package io.github.jakediscord.hobbymod.astronomy;

import java.util.*;

/** Deterministic simplified celestial sphere, shared by server and client. Not an Earth ephemeris. */
public final class SkyCatalog {
    public enum Kind { STAR, PLANET, NEBULA, GALAXY, CLUSTER }
    public record Vector(double x,double y,double z){
        public double dot(Vector b){return x*b.x+y*b.y+z*b.z;}
        public Vector scale(double f){return new Vector(x*f,y*f,z*f);}
        public Vector cross(Vector b){return new Vector(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        public Vector unit(){return scale(1/Math.sqrt(dot(this)));}
    }
    public record Object(int id,String name,Kind kind,double ra,double dec,double magnitude,int color,double period,double phase,double radius){
        public Vector direction(double ticks){
            double days=ticks/24000.0,longitude=ra+(period>0?days/period*Math.PI*2+phase:0);
            double declination=dec+(period>0?Math.sin(longitude)*.08:0);
            double hour=longitude-ticks/24000.0*Math.PI*2+Math.floor(days)/32*Math.PI*2,lat=Math.PI/4;
            double east=-Math.cos(declination)*Math.sin(hour);
            double up=Math.sin(lat)*Math.sin(declination)+Math.cos(lat)*Math.cos(declination)*Math.cos(hour);
            double south=Math.sin(lat)*Math.cos(declination)*Math.cos(hour)-Math.cos(lat)*Math.sin(declination);
            return new Vector(east,up,south);
        }
        public boolean deep(){return kind==Kind.NEBULA || kind==Kind.GALAXY || kind==Kind.CLUSTER;}
        public String description(){return switch(kind){case STAR->"A bright reference star for the constellation chart.";case PLANET->"A moving solar-system target. Compare its position on another night.";case NEBULA->"A faint cloud of glowing gas. A larger aperture reveals its structure.";case GALAXY->"A distant island of stars. Dark skies matter more than magnification.";case CLUSTER->"A gathering of stars. Repeated observations resolve more members.";};}
    }
    public record Constellation(String name,int[] stars,int[][] edges){}
    public final List<Object> targets,stars;
    public final List<Constellation> constellations;
    public SkyCatalog(long seed){
        var t=new ArrayList<Object>();
        // Recognizable patterns at a fixed 45-degree observing latitude.
        named(t,"Betelgeuse",5.92,7.4,.5,0xF4B184);named(t,"Bellatrix",5.42,6.3,1.6,0xB7D8FF);
        named(t,"Alnitak",5.68,-1.9,1.7,0xC6DCFF);named(t,"Alnilam",5.60,-1.2,1.7,0xC6DCFF);named(t,"Mintaka",5.53,-.3,2.2,0xD9E8FF);
        named(t,"Rigel",5.24,-8.2,.1,0xADCFFF);named(t,"Saiph",5.80,-9.7,2.1,0xADCFFF);
        named(t,"Dubhe",11.06,61.8,1.8,0xFFC58B);named(t,"Merak",11.03,56.4,2.4,0xDAEAFF);named(t,"Phecda",11.90,53.7,2.4,0xDAEAFF);
        named(t,"Megrez",12.26,57.0,3.3,0xDAEAFF);named(t,"Alioth",12.90,55.9,1.8,0xDAEAFF);named(t,"Mizar",13.40,54.9,2.2,0xDAEAFF);named(t,"Alkaid",13.79,49.3,1.9,0xADCFFF);
        named(t,"Caph",.15,59.1,2.3,0xFFE9BE);named(t,"Schedar",.68,56.5,2.2,0xFFC58B);named(t,"Gamma Cassiopeiae",.95,60.7,2.5,0xADCFFF);named(t,"Ruchbah",1.43,60.2,2.7,0xDAEAFF);named(t,"Segin",1.91,63.7,3.3,0xADCFFF);
        named(t,"Vega",18.62,38.8,0.0,0xBCD7FF);named(t,"Sheliak",18.83,33.4,3.5,0xDAEAFF);named(t,"Sulafat",18.98,32.7,3.2,0xDAEAFF);
        constellations=List.of(new Constellation("Orion",new int[]{0,1,2,3,4,5,6},new int[][]{{0,1},{0,2},{1,4},{2,3},{3,4},{2,6},{4,5},{5,6}}),new Constellation("Ursa Major",new int[]{7,8,9,10,11,12,13},new int[][]{{7,8},{8,9},{9,10},{10,7},{10,11},{11,12},{12,13}}),new Constellation("Cassiopeia",new int[]{14,15,16,17,18},new int[][]{{14,15},{15,16},{16,17},{17,18}}),new Constellation("Lyra",new int[]{19,20,21},new int[][]{{19,20},{20,21},{21,19}}));
        String[] planets={"Mercury","Venus","Mars","Jupiter","Saturn","Uranus","Neptune"};double[] periods={8,12,24,60,100,160,220};
        int[] colors={0xC7BBB0,0xFFF0B0,0xE68E63,0xD9B181,0xD6C091,0x94D5D0,0x7294DE};
        for(int i=0;i<7;i++)t.add(new Object(t.size(),planets[i],Kind.PLANET,i*.83,-.1,-2+i*.65,colors[i],periods[i],i*.59,.1+i*.018));
        var random=new Random(seed^0x4A5354524F4E4F4DL);
        for(int i=0;i<20;i++){var kind=Kind.values()[2+i%3];t.add(new Object(t.size(),switch(kind){case NEBULA->"Nebula ";case GALAXY->"Galaxy ";default->"Cluster ";}+String.format(Locale.ROOT,"%03d",100+random.nextInt(900)),kind,random.nextDouble()*Math.PI*2,Math.asin(random.nextDouble()*1.6-.8),5+random.nextDouble()*3,new int[]{0xC792D2,0x9CBFE0,0xCDE1FF}[i%3],0,0,.3+random.nextDouble()*.5));}
        targets=List.copyOf(t);var field=new ArrayList<Object>(t);
        for(int i=0;i<1100;i++)field.add(new Object(-1,"",Kind.STAR,random.nextDouble()*Math.PI*2,Math.asin(random.nextDouble()*2-1),2.5+random.nextDouble()*4.5,new int[]{0xCADFFF,0xFFEAC6,0xEAF1FF,0xF0C3A2}[random.nextInt(4)],0,0,0));stars=List.copyOf(field);
    }
    private static void named(List<Object> a,String name,double ra,double dec,double mag,int color){a.add(new Object(a.size(),name,Kind.STAR,ra/24*Math.PI*2,Math.toRadians(dec),mag,color,0,0,0));}
    public Object target(int id){return id>=0 && id<targets.size()?targets.get(id):null;}
    public Object nearest(Vector aim,double ticks,double degrees){Object result=null;double best=Math.cos(Math.toRadians(degrees));for(var o:targets){double d=aim.dot(o.direction(ticks));if(d>best){best=d;result=o;}}return result;}
    public static Vector aim(double yaw,double pitch){double y=Math.toRadians(yaw),p=Math.toRadians(pitch);return new Vector(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));}
    public static double night(double ticks){return Math.clamp((Math.cos((ticks/24000.0-.75)*Math.PI*2)-.1)*1.8,0,1);}
    public static double quality(Object o,double ticks,double focus,double magnification,double aperture,double rain,double blockLight,boolean clear){
        if(o==null || !clear || o.direction(ticks).y()<.08 || rain>.7 || night(ticks)<.4)return 0;
        if(o.deep() && aperture<60)return 0;
        double sharp=Math.max(0,1-Math.abs(focus-.72)*3.5),altitude=Math.clamp(o.direction(ticks).y()*1.4,.2,1);
        double moon=(1+Math.cos(Math.floor(ticks/24000.0)%8/8*Math.PI*2))*.5;
        double darkness=1-rain*.75-blockLight/15*.35-(o.deep()?moon*.25:moon*.08);
        double resolved=o.deep()?Math.clamp(aperture/120,.35,1):.85;
        double excessive=Math.clamp(1-Math.max(0,magnification-aperture*.8)/100,.25,1);
        return Math.clamp(sharp*altitude*darkness*resolved*excessive*night(ticks),0,1);
    }
    /** Sparse meteor streaks, stable across reconnects and independent of frame rate. */
    public static double meteor(long seed,double ticks){long day=(long)Math.floor(ticks/24000);Random r=new Random(seed^day*7919);if(r.nextInt(4)!=0)return -1;double start=14000+r.nextInt(7500),local=ticks-day*24000;return local>=start && local<start+70?(local-start)/70:-1;}
}
