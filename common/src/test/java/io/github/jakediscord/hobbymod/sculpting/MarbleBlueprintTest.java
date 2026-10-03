package io.github.jakediscord.hobbymod.sculpting;
import net.minecraft.core.BlockPos;
import java.util.*;
import java.io.*;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MarbleBlueprintTest {
    @Test void filesAndNetworkPreserveCarvedGeometry(){
        MarbleVolume lower=new MarbleVolume();lower.stroke(.5,.5,1,CarvingTool.DETAIL);
        MarbleBlueprint b=new MarbleBlueprint(List.of(new MarbleBlueprint.Section(BlockPos.ZERO,lower),new MarbleBlueprint.Section(new BlockPos(0,1,0),new MarbleVolume())));
        var restored=MarbleBlueprint.decode(b.compressed());assertEquals(2,restored.sections().size());
        for(int i=0;i<2;i++){assertArrayEquals(b.sections().get(i).volume().densityBytes(),restored.sections().get(i).volume().densityBytes());assertArrayEquals(b.sections().get(i).volume().polishBits(),restored.sections().get(i).volume().polishBits());}
        assertTrue(b.compressed().length<MarbleBlueprint.MAX_COMPRESSED);
    }
    @Test void malformedAndDisconnectedStructuresAreRejected(){
        var v=new MarbleVolume();assertThrows(IllegalArgumentException.class,()->new MarbleBlueprint(List.of()));
        assertThrows(IllegalArgumentException.class,()->new MarbleBlueprint(List.of(new MarbleBlueprint.Section(BlockPos.ZERO,v),new MarbleBlueprint.Section(new BlockPos(2,0,0),v))));
        assertThrows(IllegalArgumentException.class,()->new MarbleBlueprint(List.of(new MarbleBlueprint.Section(BlockPos.ZERO,v),new MarbleBlueprint.Section(BlockPos.ZERO,v))));
        String json=new MarbleBlueprint(List.of(new MarbleBlueprint.Section(BlockPos.ZERO,v))).json();
        assertThrows(IllegalArgumentException.class,()->MarbleBlueprint.fromJson(json.replace("\"version\": 1","\"version\": 99")));
        assertThrows(IllegalArgumentException.class,()->MarbleBlueprint.fromJson(json.replace("\"x\": 0","\"x\": 0.5")));
        assertThrows(IllegalArgumentException.class,()->MarbleBlueprint.fromJson("{}"));
    }
    @Test void compressedAndExpandedInputsAreBounded()throws IOException{
        assertThrows(IllegalArgumentException.class,()->MarbleBlueprint.decode(new byte[MarbleBlueprint.MAX_COMPRESSED+1]));
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(var zip=new GZIPOutputStream(out)){zip.write(new byte[MarbleBlueprint.MAX_JSON+1]);}
        assertThrows(IllegalArgumentException.class,()->MarbleBlueprint.decode(out.toByteArray()));
        assertThrows(IllegalArgumentException.class,()->MarbleBlueprint.decode(new byte[]{1,2,3}));
    }
}
