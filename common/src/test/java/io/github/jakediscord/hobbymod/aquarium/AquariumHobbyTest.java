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
    @Test void transformsAreBoundedPersistentAndKeepIdentityAfterRemoval(){
        var d=new AquariumData();d.size=AquariumData.Size.SMALL;
        d.scape.add(AquariumScape.Material.ROCK,.5,.5,0);d.scape.add(AquariumScape.Material.WOOD,.6,.6,0);
        var id=d.scape.pieces().getLast().id();d.scape.remove(0);
        assertTrue(d.scape.transform(id,d.size,.92,.92,.92,1,4,4,4));
        var p=d.scape.pieces().getFirst();var bounds=AquariumScape.bounds(p,d.size);
        assertTrue(bounds.x()>=.05 && bounds.z()>=.05 && bounds.X()<=.95 && bounds.Z()<=.95);
        assertTrue(bounds.Y()<d.size.blocksHigh()*.88-.13,"Stretching must stay under the water surface");
        assertFalse(d.scape.transform(id,d.size,.5,Double.NaN,.5,0,1,1,1));assertEquals(p,d.scape.pieces().getFirst());
        assertFalse(d.scape.transform(java.util.UUID.randomUUID(),d.size,.5,0,.5,0,1,1,1));
        d.syncScape();var copy=new AquariumData();AquariumNbt.load(copy,AquariumNbt.save(d));assertEquals(d.scape.pieces(),copy.scape.pieces());
    }
    @Test void arbitraryBlockDecorKeepsItsMaterialAndTransform(){
        var d=new AquariumData();var id=java.util.UUID.randomUUID();
        assertTrue(d.scape.add(new AquariumScape.Piece(AquariumScape.Material.BLOCK,.5,.5,2,.2,1.3,.7,.5,id,"minecraft:oak_stairs")));
        var copy=new AquariumData();AquariumNbt.load(copy,AquariumNbt.save(d));assertEquals(d.scape.pieces(),copy.scape.pieces());
        assertEquals("minecraft:oak_stairs",copy.scape.pieces().getFirst().block());
    }
    @Test void wanderingIsContinuousDistinctAndStaysInsideEveryTank(){
        var a=new AquariumData.Fish(new java.util.UUID(7,1),AquariumData.Species.GUPPY);
        var b=new AquariumData.Fish(new java.util.UUID(7,2),AquariumData.Species.GUPPY);
        assertNotEquals(AquariumMotion.pose(a,AquariumData.Size.SMALL,10,true),AquariumMotion.pose(b,AquariumData.Size.SMALL,10,true));
        for(var size:AquariumData.Size.values())for(int tick=-200;tick<4000;tick++){
            var p=AquariumMotion.pose(a,size,tick,true);var next=AquariumMotion.pose(a,size,tick+.01,true);
            assertTrue(p.x()>1.3 && p.x()<size.width-1.3 && p.z()>1.3 && p.z()<size.depth-1.3);
            assertTrue(p.y()>1.1 && p.y()<size.height-1.1 && Double.isFinite(p.yaw()));
            assertTrue(Math.abs(next.x()-p.x())<.01 && Math.abs(next.z()-p.z())<.01,"Waypoint boundaries must not teleport fish");
        }
    }

    @Test void deadFishNeverReviveBreedOrEnterCarryingBuckets(){
        var d=new AquariumData();d.filled=true;d.cycle=5;d.filter=true;var fish=d.newFish(AquariumData.Species.GUPPY);assertTrue(d.add(fish));
        for(int i=0;i<20;i++)d.advance(true,false);assertFalse(fish.alive());assertEquals(1,d.bodies());assertNull(d.capture());assertEquals(1,d.fish().size());assertEquals(0,d.load());
        d.food=100;d.plants=5;d.quality=100;for(int i=0;i<20;i++)d.advance(true,false);assertFalse(fish.alive());assertEquals(0,d.births);assertFalse(d.canAdd(fish).isEmpty());
        var saved=new AquariumData();AquariumNbt.load(saved,AquariumNbt.save(d));assertEquals(1,saved.bodies());assertEquals(1,saved.removeBodies());assertEquals(0,saved.removeBodies());assertTrue(saved.fish().isEmpty());
    }

}
