package io.github.jakediscord.hobbymod.rendering.client;
import io.github.jakediscord.hobbymod.rendering.MeshVertexData;
import io.github.jakediscord.hobbymod.sculpting.MarbleMesh;
import java.util.*;
import java.util.function.Function;
/** Bounded client LRU. Keys use owner identity, never a mesh record's expensive deep hash. */
public final class GeometryRenderCache<K> {
    private record Entry(MarbleMesh mesh,MeshVertexData data){}
    private final LinkedHashMap<K,Entry> entries=new LinkedHashMap<>(64,.75F,true);
    private int vertices;
    private Object world;
    public void world(Object next){if(world!=next){entries.clear();vertices=0;world=next;}}
    public MeshVertexData get(K key,MarbleMesh mesh,Function<MarbleMesh,MeshVertexData> prepare){
        var old=entries.get(key);if(old!=null && old.mesh==mesh)return old.data;
        if(old!=null)vertices-=old.data.vertices();
        var data=prepare.apply(mesh);entries.put(key,new Entry(mesh,data));vertices+=data.vertices();
        var it=entries.entrySet().iterator();while((vertices>300_000 || entries.size()>256) && entries.size()>1 && it.hasNext()){var removed=it.next();vertices-=removed.getValue().data.vertices();it.remove();}
        return data;
    }
}
