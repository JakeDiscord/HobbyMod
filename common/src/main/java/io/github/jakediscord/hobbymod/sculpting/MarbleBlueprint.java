package io.github.jakediscord.hobbymod.sculpting;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import net.minecraft.core.BlockPos;

/** Portable, versioned geometry only: no block entities, commands, or arbitrary registry data. */
public record MarbleBlueprint(List<Section> sections) {
    public static final int MAX_SECTIONS=64,MAX_COMPRESSED=28000,MAX_JSON=4_000_000;
    public record Section(BlockPos offset,MarbleVolume volume){}
    public MarbleBlueprint {
        sections=List.copyOf(sections);
        if(sections.isEmpty() || sections.size()>MAX_SECTIONS)throw new IllegalArgumentException("Blueprints need 1–64 marble blocks.");
        Set<BlockPos> seen=new HashSet<>();
        for(var s:sections){var p=s.offset();if(Math.abs(p.getX())>15 || Math.abs(p.getY())>15 || Math.abs(p.getZ())>15 || !seen.add(p) || s.volume().count()==0)throw new IllegalArgumentException("Invalid blueprint block or offset.");}
        if(!seen.contains(BlockPos.ZERO))throw new IllegalArgumentException("Blueprint has no anchor block.");
        Set<BlockPos> reached=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(BlockPos.ZERO);
        while(!queue.isEmpty()){var p=queue.remove();if(!reached.add(p))continue;for(var d:net.minecraft.core.Direction.values())if(seen.contains(p.relative(d)) && !reached.contains(p.relative(d)))queue.add(p.relative(d));}
        if(reached.size()!=seen.size())throw new IllegalArgumentException("Blueprint blocks must form one connected structure.");
    }
    public String json(){
        JsonObject root=new JsonObject();root.addProperty("format","hobbymod:marble_blueprint");root.addProperty("version",1);JsonArray array=new JsonArray();
        for(var s:sections){JsonObject o=new JsonObject();o.addProperty("x",s.offset.getX());o.addProperty("y",s.offset.getY());o.addProperty("z",s.offset.getZ());
            o.addProperty("density",Base64.getEncoder().encodeToString(s.volume.densityBytes()));JsonArray polish=new JsonArray();for(long word:s.volume.polishBits())polish.add(Long.toString(word));o.add("polish",polish);array.add(o);}
        root.add("blocks",array);return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
    public static MarbleBlueprint fromJson(String text){
        if(text.length()>MAX_JSON)throw new IllegalArgumentException("Blueprint file is too large.");
        try{
            JsonObject root=JsonParser.parseString(text).getAsJsonObject();if(!"hobbymod:marble_blueprint".equals(root.get("format").getAsString()) || root.get("version").getAsInt()!=1)throw new IllegalArgumentException("Unsupported blueprint format.");
            JsonArray array=root.getAsJsonArray("blocks");if(array.isEmpty() || array.size()>MAX_SECTIONS)throw new IllegalArgumentException("Blueprints need 1–64 blocks.");
            List<Section> blocks=new ArrayList<>();
            for(var value:array){var o=value.getAsJsonObject();String encoded=o.get("density").getAsString();if(encoded.length()>((MarbleVolume.NODES+2)/3)*4)throw new IllegalArgumentException("Invalid density length.");
                byte[] density=Base64.getDecoder().decode(encoded);if(density.length!=MarbleVolume.NODES)throw new IllegalArgumentException("Invalid density length.");
                JsonArray bits=o.getAsJsonArray("polish");if(bits.size()>(MarbleVolume.NODES+63)/64)throw new IllegalArgumentException("Invalid polish data.");long[] polish=new long[bits.size()];for(int i=0;i<polish.length;i++)polish[i]=Long.parseLong(bits.get(i).getAsString());
                blocks.add(new Section(new BlockPos(coordinate(o,"x"),coordinate(o,"y"),coordinate(o,"z")),MarbleVolume.read(density,polish,false)));
            }
            return new MarbleBlueprint(blocks);
        }catch(IllegalArgumentException e){throw e;}catch(RuntimeException e){throw new IllegalArgumentException("Invalid marble blueprint file.",e);}
    }
    private static int coordinate(JsonObject o,String key){double n=o.get(key).getAsDouble();if(!Double.isFinite(n) || n!=Math.rint(n) || Math.abs(n)>15)throw new IllegalArgumentException("Invalid block offset.");return (int)n;}
    public byte[] compressed(){
        try{byte[] json=json().getBytes(StandardCharsets.UTF_8);if(json.length>MAX_JSON)throw new IllegalArgumentException("Design is too large; capture smaller sections.");ByteArrayOutputStream out=new ByteArrayOutputStream();try(GZIPOutputStream zip=new GZIPOutputStream(out)){zip.write(json);}
            byte[] bytes=out.toByteArray();if(bytes.length>MAX_COMPRESSED)throw new IllegalArgumentException("Design is too detailed to fit one blueprint; capture smaller sections.");return bytes;
        }catch(IOException e){throw new IllegalArgumentException("Could not encode blueprint.",e);}
    }
    public static MarbleBlueprint decode(byte[] bytes){
        if(bytes.length==0 || bytes.length>MAX_COMPRESSED)throw new IllegalArgumentException("Invalid blueprint size.");
        try(GZIPInputStream in=new GZIPInputStream(new ByteArrayInputStream(bytes))){byte[] json=in.readNBytes(MAX_JSON+1);if(json.length>MAX_JSON)throw new IllegalArgumentException("Blueprint expands beyond the size limit.");return fromJson(new String(json,StandardCharsets.UTF_8));}
        catch(IOException e){throw new IllegalArgumentException("Invalid compressed blueprint.",e);}
    }
}
