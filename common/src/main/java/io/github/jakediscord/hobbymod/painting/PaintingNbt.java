package io.github.jakediscord.hobbymod.painting;
import net.minecraft.nbt.CompoundTag;
public final class PaintingNbt {
    public static CompoundTag save(PaintingData d){var t=new CompoundTag();t.putUUID("Id",d.id);t.putString("Shape",d.shape.name());t.putInt("Resolution",d.resolution);t.putInt("Revision",d.revision);t.putBoolean("Signed",d.signed);t.putString("Title",d.title);t.putString("Author",d.author);t.putByteArray("Pixels",d.compressed());return t;}
    public static PaintingData read(CompoundTag t,PaintingData.Shape fallback){PaintingData.Shape shape;try{shape=PaintingData.Shape.valueOf(t.getString("Shape"));}catch(IllegalArgumentException e){shape=fallback;}var d=new PaintingData(shape,t.getInt("Resolution"));if(t.hasUUID("Id"))d.id=t.getUUID("Id");d.revision=Math.clamp(t.getInt("Revision"),0,1_000_000_000);d.signed=t.getBoolean("Signed");d.title=clean(t.getString("Title"),48);d.author=clean(t.getString("Author"),32);d.restore(t.getByteArray("Pixels"));return d;}
    public static String clean(String value,int max){String cleaned=value.replaceAll("[\\p{Cntrl}§]","").strip();return cleaned.substring(0,Math.min(max,cleaned.length()));}
    private PaintingNbt(){}
}
