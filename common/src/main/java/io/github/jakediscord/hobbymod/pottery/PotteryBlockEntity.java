package io.github.jakediscord.hobbymod.pottery;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PotteryBlockEntity extends BlockEntity {
    public PotteryPiece piece;
    public ItemStack flower=ItemStack.EMPTY;
    public boolean containsWater;
    public int revision;
    public double offsetX,offsetY,offsetZ;
    /** Counts received snapshots as well as changed revisions, acknowledging rejected/no-op work. */
    public int snapshots;
    public long spinUntil;
    public PotteryBlockEntity(BlockPos pos,BlockState state){super(PotteryContent.PIECE.get(),pos,state);if(!state.is(PotteryContent.WHEEL.get()))piece=new PotteryPiece();}
    public boolean wheel(){return getBlockState().is(PotteryContent.WHEEL.get());}
    public boolean spinning(){return wheel() && level!=null && level.getGameTime()<spinUntil;}
    public void spin(){if(wheel() && piece!=null && (piece.stage==PotteryPiece.Stage.WET || piece.stage==PotteryPiece.Stage.LEATHER_HARD)){spinUntil=level.getGameTime()+60;changed();}}
    public boolean work(Player player,double t,double push,double lift,int expectedRevision) {
        if(level==null || level.isClientSide || !wheel() || piece==null || revision!=expectedRevision || !PotteryNetworking.permitted(player,worldPosition)
                || !Double.isFinite(t) || t<0 || t>1 || !Double.isFinite(push) || !Double.isFinite(lift) || Math.abs(push)>.025 || Math.abs(lift)>.025)return false;
        ItemStack stack=player.getMainHandItem();
        var tool=stack.isEmpty()?PotteryToolItem.Tool.HAND:stack.getItem() instanceof PotteryToolItem item?item.tool:null;
        if(tool==null || tool==PotteryToolItem.Tool.WIRE)return false;
        boolean changed=false;
        if(piece.stage==PotteryPiece.Stage.LEATHER_HARD && tool==PotteryToolItem.Tool.LOOP)changed=piece.shape.trim(t);
        else if(piece.stage==PotteryPiece.Stage.WET && piece.moisture>0 && tool!=PotteryToolItem.Tool.LOOP) {
            if(!piece.shape.open() && tool==PotteryToolItem.Tool.HAND && t>=.8)changed=piece.shape.openCenter();
            else if(piece.shape.open())changed=piece.shape.throwClay(t,push,lift,tool==PotteryToolItem.Tool.RIB || tool==PotteryToolItem.Tool.SPONGE);
            if(changed){piece.drying=0;piece.moisture=Math.max(0,piece.moisture-1);}
        }
        if(!changed)return false;
        if(!stack.isEmpty() && !player.getAbilities().instabuild)stack.hurtAndBreak(1,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        spinUntil=level.getGameTime()+60;changed();return true;
    }
    public void changed(){revision++;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,PotteryBlockEntity pot) {
        if(level.isClientSide || pot.piece==null || pot.spinning())return;
        boolean transition=pot.piece.airDry();
        if(transition)pot.changed();else if(level.getGameTime()%20==0)pot.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);if(piece!=null)tag.put("piece",piece.save());tag.putInt("revision",revision);
        tag.putDouble("offset_x",offsetX);tag.putDouble("offset_y",offsetY);tag.putDouble("offset_z",offsetZ);
        tag.putBoolean("water",containsWater);tag.putLong("spin_until",spinUntil);if(!flower.isEmpty())tag.put("flower",flower.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);snapshots++;
        var next=tag.contains("piece")?PotteryPiece.read(tag.getCompound("piece")):wheel()?null:new PotteryPiece();
        // Pedal acknowledgements and moisture updates can reuse an unchanged surface mesh.
        if(next!=null && piece!=null && java.util.Arrays.equals(next.shape.data(),piece.shape.data()))next.shape=piece.shape;
        piece=next;
        offsetX=finiteOffset(tag.getDouble("offset_x"),-.43,.43);offsetY=finiteOffset(tag.getDouble("offset_y"),-.5,0);offsetZ=finiteOffset(tag.getDouble("offset_z"),-.43,.43);
        if(!wheel() && piece!=null){double edge=.5-piece.shape.maxRadius();offsetX=Math.clamp(offsetX,-edge,edge);offsetZ=Math.clamp(offsetZ,-edge,edge);}
        revision=Math.max(0,tag.getInt("revision"));spinUntil=tag.getLong("spin_until");flower=ItemStack.parseOptional(registries,tag.getCompound("flower"));containsWater=piece!=null && piece.stage==PotteryPiece.Stage.FINISHED && (tag.getBoolean("water") || !tag.contains("water") && !flower.isEmpty());
    }
    private static double finiteOffset(double n,double min,double max){return Double.isFinite(n)?Math.clamp(n,min,max):0;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
