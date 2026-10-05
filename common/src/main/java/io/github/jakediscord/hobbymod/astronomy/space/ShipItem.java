package io.github.jakediscord.hobbymod.astronomy.space;

import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ShipItem extends Item {
    public ShipItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext c){
        var p=c.getPlayer();if(p==null || c.getClickedFace()!=Direction.UP)return InteractionResult.FAIL;
        if(p.getVehicle() instanceof PrototypeShip){if(p instanceof ServerPlayer s)FlightNetworking.open(s);return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
        var pos=c.getClickedPos().above();if(!io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(p,pos))return InteractionResult.FAIL;
        var ship=new PrototypeShip(FlightContent.SHIP.get(),c.getLevel());ship.moveTo(pos.getX()+.5,pos.getY()+.05,pos.getZ()+.5,p.getYRot(),0);
        if(!c.getLevel().noCollision(ship,ship.getBoundingBox().inflate(.1,0,.1))){p.displayClientMessage(Component.literal("The ship needs a clear 4 × 4 × 3 launch area"),true);return InteractionResult.FAIL;}
        if(!c.getLevel().isClientSide){var data=c.getItemInHand().get(DataComponents.CUSTOM_DATA);if(data!=null)ship.readPortable(data.copyTag());ship.owner=p.getUUID();
            if(!ship.hasHome()){var home=c.getLevel().dimension()==Level.OVERWORLD?pos:c.getLevel().getServer().overworld().getSharedSpawnPos();ship.setHome(home);}
            if(!c.getLevel().addFreshEntity(ship))return InteractionResult.FAIL;
            if(!p.getAbilities().instabuild)c.getItemInHand().shrink(1);ship.playSound(net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN,.6f,.8f);
        }return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(!(p.getVehicle() instanceof PrototypeShip))return InteractionResultHolder.pass(p.getItemInHand(h));if(p instanceof ServerPlayer s)FlightNetworking.open(s);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    public static ItemStack packed(PrototypeShip ship){var item=new ItemStack(FlightContent.ITEM.get());var tag=new net.minecraft.nbt.CompoundTag();ship.writePortable(tag);item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return item;}
}
