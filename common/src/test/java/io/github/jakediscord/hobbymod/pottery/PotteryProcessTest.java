package io.github.jakediscord.hobbymod.pottery;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PotteryProcessTest {
    @Test void fullProcessRequiresDryingBisqueGlazeAndSecondFiring(){
        var p=new PotteryPiece();p.shape.openCenter();assertFalse(p.fire());assertFalse(p.glaze(DyeColor.BLUE));
        for(int i=0;i<PotteryPiece.DRY_INTERVAL;i++)p.airDry();assertEquals(PotteryPiece.Stage.LEATHER_HARD,p.stage);assertFalse(p.canFire());
        assertTrue(p.shape.trim(.1));for(int i=0;i<PotteryPiece.DRY_INTERVAL;i++)p.airDry();
        assertEquals(PotteryPiece.Stage.DRY,p.stage);assertTrue(p.fire());assertEquals(PotteryPiece.Stage.BISQUE,p.stage);
        assertFalse(p.fire());assertTrue(p.glaze(DyeColor.BLUE));assertFalse(p.glaze(DyeColor.RED));assertTrue(p.fire());
        assertEquals(PotteryPiece.Stage.FINISHED,p.stage);assertEquals(DyeColor.BLUE,p.glaze);assertFalse(p.fire());assertFalse(p.rewet());
    }
    @Test void rewettingAndSerializationPreserveIndividualGeometryAndStage(){
        var p=new PotteryPiece();p.shape.openCenter();p.shape.throwClay(.7,.018,.01,false);
        for(int i=0;i<PotteryPiece.DRY_INTERVAL;i++)p.airDry();assertTrue(p.rewet());assertEquals(PotteryPiece.Stage.WET,p.stage);
        p.moisture=53;p.drying=333;var restored=PotteryPiece.read(p.save());
        assertEquals(53,restored.moisture);assertEquals(333,restored.drying);assertEquals(p.stage,restored.stage);
        assertArrayEquals(p.shape.data(),restored.shape.data(),.000002);
    }
}
