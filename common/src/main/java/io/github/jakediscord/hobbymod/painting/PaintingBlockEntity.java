package io.github.jakediscord.hobbymod.painting;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class PaintingBlockEntity extends BlockEntity {
    public PaintingData painting;
    public PaintingBlockEntity(BlockPos p,BlockState s){super(PaintingContent.ENTITY.get(),p,s);}
    public boolean easel(){return getBlockState().is(PaintingContent.EASEL.get());}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putBoolean("HasPainting",painting!=null);if(painting!=null)t.put("Painting",PaintingNbt.save(painting));}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);painting=t.contains("Painting")?PaintingNbt.read(t.getCompound("Painting"),PaintingData.Shape.SQUARE):null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
