package io.github.jakediscord.hobbymod.aquarium;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AquariumHobbyTest {
    @Test void decorationsHaveRealBoundedPositionsAndMaterialLimits(){
        var scape=new AquariumScape();assertFalse(scape.add(AquariumScape.Material.KELP,Double.NaN,.5,0));assertFalse(scape.add(AquariumScape.Material.WOOD,1,.5,0));
        for(int i=0;i<16;i++)assertTrue(scape.add(i%2==0?AquariumScape.Material.KELP:AquariumScape.Material.SEAGRASS,.4,.6,i));
        assertFalse(scape.add(AquariumScape.Material.SEAGRASS,.5,.5,0));for(int i=0;i<8;i++)assertTrue(scape.add(AquariumScape.Material.ROCK,.5,.5,0));for(int i=0;i<4;i++)assertTrue(scape.add(AquariumScape.Material.WOOD,.5,.5,0));
        assertEquals(28,scape.pieces().size());assertEquals(AquariumScape.Material.KELP,scape.remove(0).material());assertNull(scape.remove(99));assertTrue(scape.add(AquariumScape.Material.KELP,.7,.3,5));assertEquals(1,scape.pieces().getLast().rotation());
    }
    @Test void ordinaryFeedingSupportsTwentyMinutesWithoutConstantMaintenance(){
        var d=new AquariumData();d.filled=true;d.cycle=5;d.filter=true;d.plants=3;d.add(d.newFish(AquariumData.Species.GUPPY));d.feed();assertEquals(20,d.foodMinutes());
        for(int i=0;i<19;i++)d.advance(true,false);assertEquals(1,d.food);assertEquals(100,d.fish().getFirst().health);assertEquals(100,d.quality);
        d.food=80;d.feed();assertEquals(92,d.quality,"Overfeeding still has a cost");
    }
    @Test void filterStarterRequiresARealFilledFilterAndDoesNotResetIt(){
        var d=new AquariumData();assertFalse(d.seedFilter());d.filled=true;assertFalse(d.seedFilter());d.filter=true;assertTrue(d.seedFilter());assertEquals(5,d.cycle);assertFalse(d.seedFilter());
    }
    @Test void layoutsAndLegacyDecorSurviveSaveAndReload(){
        var d=new AquariumData();d.substrate=true;d.gravel=true;d.births=7;d.discovered=1<<AquariumData.Species.GUPPY.ordinal();d.scape.add(AquariumScape.Material.KELP,.2,.8,3);d.scape.add(AquariumScape.Material.WOOD,.7,.3,1);d.syncScape();
        var copy=new AquariumData();AquariumNbt.load(copy,AquariumNbt.save(d));assertEquals(d.scape.pieces(),copy.scape.pieces());assertTrue(copy.gravel);assertEquals(7,copy.births);assertEquals(d.discovered,copy.discovered,"The journal keeps species after their fish move out");
        var old=AquariumNbt.save(d);old.remove("Scape");old.putInt("Plants",4);old.putInt("Rocks",2);old.putInt("Wood",1);AquariumNbt.load(copy,old);assertEquals(7,copy.scape.pieces().size());assertEquals(4,copy.plants);
    }
}
