package io.github.jakediscord.hobbymod.painting;

import java.util.*;
import java.util.zip.*;
import java.io.*;

/** Portable bounded raster, shape mask and deterministic brushes. Colors are RGB. */
public final class PaintingData {
    public enum Shape {
        SQUARE("Square",1,1),LANDSCAPE("Landscape",3,2),PORTRAIT("Portrait",2,3),PANORAMA("Panoramic",2,1),ROUND("Round",1,1);
        public final String label;public final int x,y;
        Shape(String label,int x,int y){this.label=label;this.x=x;this.y=y;}
    }
    public static final int LINEN=0xF1E9D2,MAX_PIXELS=32768;
    public static final int[] RESOLUTIONS={16,32,64,128};
    public UUID id=UUID.randomUUID();public final Shape shape;
    public int resolution,revision;public boolean signed;public String title="",author="";
    private int[] pixels;
    public PaintingData(Shape shape,int resolution){this.shape=Objects.requireNonNull(shape);this.resolution=validResolution(resolution)?resolution:32;pixels=new int[width()*height()];Arrays.fill(pixels,LINEN);}
    public static boolean validResolution(int r){return r==16 || r==32 || r==64 || r==128;}
    public int width(){return resolution*shape.x/Math.min(shape.x,shape.y);}public int height(){return resolution*shape.y/Math.min(shape.x,shape.y);}
    public int pixel(int x,int y){return pixels[y*width()+x];}
    public int[] pixels(){return pixels.clone();}
    public boolean inside(int x,int y){if(x<0 || y<0 || x>=width() || y>=height())return false;if(shape!=Shape.ROUND)return true;double a=(x+.5)/width()*2-1,b=(y+.5)/height()*2-1;return a*a+b*b<=1;}
    public PaintingData copy(){var d=new PaintingData(shape,resolution);d.id=id;d.revision=revision;d.signed=signed;d.title=title;d.author=author;d.pixels=pixels.clone();return d;}
    public boolean resize(int r){if(!validResolution(r) || r==resolution || signed)return false;int oldW=width(),oldH=height();var old=pixels;resolution=r;pixels=new int[width()*height()];for(int y=0;y<height();y++)for(int x=0;x<width();x++)pixels[y*width()+x]=old[Math.min(oldH-1,y*oldH/height())*oldW+Math.min(oldW-1,x*oldW/width())];revision++;return true;}
    public static int mix(int a,int b,int percent){int out=0;for(int shift:new int[]{0,8,16})out|=(((a>>shift)&255)*(100-percent)+((b>>shift)&255)*percent)/100<<shift;return out;}
    public boolean validPoints(float[] xy){if(xy==null || xy.length<2 || xy.length>64 || xy.length%2!=0)return false;for(int i=0;i<xy.length;i++)if(!Float.isFinite(xy[i]) || xy[i]<0 || xy[i]>((i%2==0?width():height())-1))return false;return true;}
    /** tool 0 round brush, 1 square brush, 2 pencil, 3 bounded flood fill, 4 soft brush. */
    public int paint(float[] xy,int color,int diameter,int opacity,int tool){
        if(signed || !validPoints(xy) || diameter<1 || diameter>16 || opacity<1 || opacity>100 || tool<0 || tool>4)return 0;
        color&=0xFFFFFF;int changed=0;
        if(tool==3){int sx=Math.round(xy[0]),sy=Math.round(xy[1]);if(!inside(sx,sy))return 0;int original=pixel(sx,sy),replacement=mix(original,color,opacity);if(original==replacement)return 0;
            int[] queue=new int[pixels.length];int read=0,write=0;int start=sy*width()+sx;queue[write++]=start;pixels[start]=replacement;
            while(read<write){int index=queue[read++],x=index%width(),y=index/width();changed++;for(int next:new int[]{index-1,index+1,index-width(),index+width()}){int nx=next%width(),ny=next/width();if(next<0 || next>=pixels.length || Math.abs(nx-x)+Math.abs(ny-y)!=1 || !inside(nx,ny) || pixels[next]!=original)continue;pixels[next]=replacement;queue[write++]=next;}}
        }else{
            int[] coverage=new int[pixels.length];double radius=(tool==2?1:diameter)/2.0;
            int count=Math.max(1,xy.length/2-1);
            for(int i=0;i<count;i++){double ax=xy[i*2],ay=xy[i*2+1],bx=xy.length==2?ax:xy[i*2+2],by=xy.length==2?ay:xy[i*2+3],dx=bx-ax,dy=by-ay,l2=dx*dx+dy*dy;
                for(int y=Math.max(0,(int)Math.floor(Math.min(ay,by)-radius));y<=Math.min(height()-1,(int)Math.ceil(Math.max(ay,by)+radius));y++)for(int x=Math.max(0,(int)Math.floor(Math.min(ax,bx)-radius));x<=Math.min(width()-1,(int)Math.ceil(Math.max(ax,bx)+radius));x++){
                    int index=y*width()+x;if(!inside(x,y))continue;double t=l2==0?0:Math.clamp(((x-ax)*dx+(y-ay)*dy)/l2,0,1),ex=x-ax-t*dx,ey=y-ay-t*dy;
                    if(tool==1?Math.max(Math.abs(ex),Math.abs(ey))>radius:ex*ex+ey*ey>radius*radius)continue;
                    int strength=tool==4?(int)Math.round(opacity*Math.max(0,1-Math.sqrt(ex*ex+ey*ey)/radius)):opacity;coverage[index]=Math.max(coverage[index],strength);
                }
            }
            for(int i=0;i<pixels.length;i++)if(coverage[i]>0){int value=mix(pixels[i],color,coverage[i]);if(value!=pixels[i]){pixels[i]=value;changed++;}}
        }
        if(changed>0)revision++;return changed;
    }
    public byte[] compressed(){try{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(new DeflaterOutputStream(bytes))){for(int value:pixels)out.writeInt(value);}return bytes.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
    public boolean restore(byte[] bytes){if(bytes.length==0 || bytes.length>MAX_PIXELS*4+1024)return false;try(var in=new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)))){var restored=new int[pixels.length];for(int i=0;i<restored.length;i++)restored[i]=in.readInt()&0xFFFFFF;if(in.read()!=-1)return false;pixels=restored;return true;}catch(IOException e){return false;}}
}
