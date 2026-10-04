package io.github.jakediscord.hobbymod.winery;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;

public final class WineBottleItem extends Item {
    public WineBottleItem(){super(new Item.Properties().stacksTo(16));}
    public static ItemStack create(WineBatch b,int count){var s=new ItemStack(WineryContent.BOTTLE.get(),count);var t=new CompoundTag();t.putUUID("BatchId",b.id);t.putString("Label",WineBatch.clean(b.label));t.putInt("Vintage",b.vintage);t.putInt("Quality",b.quality());t.putInt("Color",b.kind().ordinal());s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));return s;}
    public static CompoundTag descriptor(ItemStack s){var d=s.get(DataComponents.CUSTOM_DATA);return d==null?new CompoundTag():d.copyTag();}
    public static UUID batchId(ItemStack s){var t=descriptor(s);return t.hasUUID("BatchId")?t.getUUID("BatchId"):null;}
    public static int color(ItemStack s){return Math.clamp(descriptor(s).getInt("Color"),0,2);}
    @Override public Component getName(ItemStack s){String label=WineBatch.clean(descriptor(s).getString("Label"));return Component.literal(label.isEmpty()?"Wine bottle":label);}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){var t=descriptor(s);if(batchId(s)==null){lines.add(Component.literal("Bottle wine from a fermentation barrel"));return;}lines.add(Component.literal(WineBatch.Kind.values()[color(s)].label()+" · Vintage "+Math.max(0,t.getInt("Vintage"))));lines.add(Component.literal("Quality "+Math.clamp(t.getInt("Quality"),0,100)+"/100"));lines.add(Component.literal("Drink to taste · place in a wine rack"));if(f.isAdvanced())lines.add(Component.literal("Batch "+batchId(s).toString().substring(0,8)));}
    @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.DRINK;}
    @Override public int getUseDuration(ItemStack s,LivingEntity e){return 32;}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){return batchId(p.getItemInHand(hand))==null?InteractionResultHolder.pass(p.getItemInHand(hand)):ItemUtils.startUsingInstantly(l,p,hand);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(entity instanceof Player p && level instanceof ServerLevel server){var id=batchId(stack);var entry=id==null?null:WineCellarData.get(server).ledger.get(id);int quality=50;
            if(entry!=null){var wine=entry.wine();quality=wine.quality();p.displayClientMessage(Component.literal(wine.label+" · "+wine.tastingNotes()+" · quality "+quality),true);}else p.displayClientMessage(Component.literal("Imported vintage · original tasting record unavailable"),true);
            p.getFoodData().eat(2,.25F);if(quality>=80)p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,600,0));
            level.playSound(null,p.blockPosition(),SoundEvents.BOTTLE_EMPTY,net.minecraft.sounds.SoundSource.PLAYERS,.7F,1);
            if(!p.getAbilities().instabuild){stack.shrink(1);if(stack.isEmpty())return new ItemStack(Items.GLASS_BOTTLE);var glass=new ItemStack(Items.GLASS_BOTTLE);if(!p.addItem(glass))p.drop(glass,false);}
        }return stack;
    }
}
