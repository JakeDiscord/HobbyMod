package io.github.jakediscord.hobbymod.painting.client;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.jakediscord.hobbymod.painting.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
/** Small shared LRU: GUI and placed paintings reuse nearest-filtered raster textures. */
public final class PaintingTextures {
    private record Entry(int revision,int width,int height,ResourceLocation location,DynamicTexture texture){}
    private static final Map<PaintingData.Shape,PaintingData> BLANKS=new EnumMap<>(PaintingData.Shape.class);
    public static PaintingData blank(PaintingData.Shape shape){return BLANKS.computeIfAbsent(shape,s->new PaintingData(s,32));}
    private static final Map<UUID,Entry> CACHE=new LinkedHashMap<>(128,.75F,true);
    public static NativeImage image(PaintingData d){var image=new NativeImage(d.width(),d.height(),false);write(image,d);return image;}
    private static void write(NativeImage image,PaintingData d){for(int y=0;y<d.height();y++)for(int x=0;x<d.width();x++){int rgb=d.pixel(x,y);image.setPixelRGBA(x,y,d.inside(x,y)?0xff000000|(rgb&255)<<16|(rgb&0xff00)|(rgb>>16)&255:0);}}
    public static ResourceLocation texture(PaintingData d){var old=CACHE.get(d.id);if(old!=null && old.revision==d.revision && old.width==d.width() && old.height==d.height())return old.location;
        if(old!=null && old.width==d.width() && old.height==d.height()){
            write(old.texture.getPixels(),d);old.texture.upload();CACHE.put(d.id,new Entry(d.revision,d.width(),d.height(),old.location,old.texture));return old.location;
        }
        if(old!=null)Minecraft.getInstance().getTextureManager().release(old.location);var texture=new DynamicTexture(image(d));texture.setFilter(false,false);var loc=Minecraft.getInstance().getTextureManager().register("hobbymod_painting_"+d.id,texture);CACHE.put(d.id,new Entry(d.revision,d.width(),d.height(),loc,texture));
        if(CACHE.size()>128){var it=CACHE.entrySet().iterator();var oldest=it.next();Minecraft.getInstance().getTextureManager().release(oldest.getValue().location);it.remove();}return loc;
    }
    public static void invalidate(UUID id){var e=CACHE.get(id);if(e!=null)CACHE.put(id,new Entry(-1,e.width,e.height,e.location,e.texture));}
    private PaintingTextures(){}
}
