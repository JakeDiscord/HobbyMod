package io.github.jakediscord.hobbymod.habitats;

import io.github.jakediscord.hobbymod.aquarium.*;
import io.github.jakediscord.hobbymod.terrarium.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

/** Capture one real wild individual; never conjure residents from repeated clicks on terrain. */
public final class HabitatNet extends Item {
    public HabitatNet(){super(new Item.Properties().durability(128));}
    @Override public Component getName(ItemStack stack){return Component.literal("Habitat Collection Net");}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.literal("Right-click wild critters to collect them"));lines.add(Component.literal("Fish need a water bucket in your inventory"));}
    public static ItemStack critter(TerrariumData.Resident resident){var stack=new ItemStack(TerrariumContent.colony(resident.species));var tag=new CompoundTag();var list=new ListTag();list.add(TerrariumNbt.resident(resident));tag.put("Colony",list);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
    @Override public InteractionResult interactLivingEntity(ItemStack tool,Player player,LivingEntity target,InteractionHand hand){
        if(!(target instanceof WildCritter) && !(target instanceof AbstractFish))return InteractionResult.PASS;
        if(!target.isAlive() || target.isRemoved() || player.distanceToSqr(target)>25)return InteractionResult.FAIL;
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        ItemStack caught;
        if(target instanceof WildCritter critter)caught=critter(critter.resident());
        else{
            int water=-1;for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(Items.WATER_BUCKET)){water=i;break;}
            if(water<0 && !player.getAbilities().instabuild){player.displayClientMessage(Component.literal("Bring a water bucket to carry a fish safely."),true);return InteractionResult.FAIL;}
            AquariumData.Fish fish;
            if(target instanceof WildAquariumFish wild)fish=wild.resident();
            else{var data=new AquariumData();data.seed=target.getUUID().getMostSignificantBits();var kind=target instanceof Cod?AquariumData.Species.CORYDORAS:target instanceof Salmon?AquariumData.Species.ZEBRA_DANIO:AquariumData.Species.GUPPY;fish=data.newFish(kind);var exact=new AquariumData.Fish(target.getUUID(),kind);exact.colorA=fish.colorA;exact.colorB=fish.colorB;exact.formA=fish.formA;exact.formB=fish.formB;exact.female=fish.female;exact.age=12;fish=exact;}
            caught=AquariumFishItem.capture(fish);if(!player.getAbilities().instabuild)player.getInventory().getItem(water).shrink(1);
        }
        var message=Component.literal("Collected "+caught.getHoverName().getString());
        if(!player.addItem(caught))player.drop(caught,false);target.discard();
        if(!player.getAbilities().instabuild)tool.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));
        player.displayClientMessage(message,true);player.inventoryMenu.broadcastChanges();return InteractionResult.SUCCESS;
    }
}
