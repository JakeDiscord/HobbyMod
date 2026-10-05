package io.github.jakediscord.hobbymod.astronomy;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class TelescopeBlockEntity extends BlockEntity {
    public float yaw,pitch=-35,focus=.72f,magnification=8;
    public TelescopeBlockEntity(BlockPos p,BlockState s){super(AstronomyContent.ENTITY.get(),p,s);}
    public boolean large(){return getBlockState().is(AstronomyContent.OBSERVATORY.get());}
    public double aperture(){return large()?180:80;}
    public net.minecraft.world.phys.Vec3 eyepiece(){return net.minecraft.world.phys.Vec3.atBottomCenterOf(worldPosition).add(0,large()?1.55:1.12,0);}
    public net.minecraft.world.phys.Vec3 lens(float yaw,float pitch){var d=SkyCatalog.aim(yaw,pitch);return eyepiece().add(d.x()*(large()?1.0:.75),d.y()*(large()?1.0:.75),d.z()*(large()?1.0:.75));}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putFloat("Yaw",yaw);t.putFloat("Pitch",pitch);t.putFloat("Focus",focus);t.putFloat("Magnification",magnification);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);yaw=finite(t.getFloat("Yaw"),0);pitch=Math.clamp(finite(t.getFloat("Pitch"),-35),-89,15);focus=.72f; // Legacy saves and packets retain the field; optics now focus automatically.
magnification=Math.clamp(finite(t.getFloat("Magnification"),8),large()?12:6,large()?100:40);}
    private float finite(float f,float fallback){return Float.isFinite(f)?f:fallback;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
