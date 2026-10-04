package io.github.jakediscord.hobbymod.terrarium;

import io.github.jakediscord.hobbymod.aquarium.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/** Actual inventory-backed habitat actions; no fish/water actions enter land enclosures. */
public final class TerrariumActions {
    public static final TagKey<Block> PLANTS=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("hobbymod","terrarium_plants"));
    public static boolean plant(ItemStack stack){return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(PLANTS);}
    public static int plants(AquariumData d){int count=0;for(var p:d.scape.pieces())if(p.material()==AquariumScape.Material.BLOCK){var id=ResourceLocation.tryParse(p.block());if(id!=null && BuiltInRegistries.BLOCK.get(id).defaultBlockState().is(PLANTS))count++;}return count;}
    public static Item substrate(TerrariumData.Substrate kind){return switch(kind){case SOIL->Items.DIRT;case SAND->Items.SAND;case MOSS->Items.MOSS_BLOCK;case NONE->Items.AIR;};}
    private static void consume(ItemStack s,Player p){if(!p.getAbilities().instabuild)s.shrink(1);}
    private static void refund(Player p,ItemStack stack){if(!p.getAbilities().instabuild && !p.addItem(stack))p.drop(stack,false);}
    private static void notice(Player p,String text){p.displayClientMessage(Component.literal(text),true);if(p instanceof net.minecraft.server.level.ServerPlayer server)dev.architectury.networking.NetworkManager.sendToPlayer(server,new AquariumNetworking.Notice(text));}
    public static ItemInteractionResult apply(ItemStack stack,AquariumBlockEntity tank,Player player,InteractionHand hand){
        var d=tank.data.terrarium;boolean remove=player.isShiftKeyDown(),changed=false;
        if(tank.condition==AquariumBlockEntity.Condition.UNLOADED || tank.condition==AquariumBlockEntity.Condition.BROKEN_GLASS){notice(player,"Load and repair the entire enclosure first.");return ItemInteractionResult.CONSUME;}
        if(stack.is(TerrariumContent.MISTER.get())){
            d.mist();tank.fedAt=player.level().getGameTime();if(!player.getAbilities().instabuild)stack.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));
            player.level().playSound(null,tank.getBlockPos(),net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,.35f,1.3f);changed=true;notice(player,"Mist added · moisture "+d.moisture+"%");
        }else if(stack.is(Items.GRAVEL)){
            if(remove && d.drainage){d.drainage=false;refund(player,new ItemStack(Items.GRAVEL));changed=true;}
            else if(!remove && !d.drainage){d.drainage=true;consume(stack,player);changed=true;}
        }else if(stack.is(Items.DIRT) || stack.is(Items.SAND) || stack.is(Items.MOSS_BLOCK)){
            var kind=stack.is(Items.DIRT)?TerrariumData.Substrate.SOIL:stack.is(Items.SAND)?TerrariumData.Substrate.SAND:TerrariumData.Substrate.MOSS;
            if(remove && d.substrate==kind){if(!d.residents().isEmpty()){notice(player,"Collect the colonies before removing their substrate.");return ItemInteractionResult.CONSUME;}refund(player,new ItemStack(substrate(d.substrate)));d.substrate=TerrariumData.Substrate.NONE;changed=true;}
            else if(!remove && d.substrate!=kind){if(d.substrate!=TerrariumData.Substrate.NONE)refund(player,new ItemStack(substrate(d.substrate)));d.substrate=kind;consume(stack,player);changed=true;}
        }else if(stack.is(TerrariumContent.FOOD.get())){
            if(d.food<=80){d.food=Math.min(100,d.food+20);consume(stack,player);changed=true;}else notice(player,"There is plenty of leaf litter already.");
        }else if(TerrariumContent.species(stack)!=null){
            var species=TerrariumContent.species(stack);
            if(remove){var taken=d.take(species);if(!taken.isEmpty()){var returned=new ItemStack(TerrariumContent.colony(species));var tag=new CompoundTag();var list=new ListTag();for(var r:taken)list.add(TerrariumNbt.resident(r));tag.put("Colony",list);returned.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));if(!player.addItem(returned))player.drop(returned,false);changed=true;}}
            else if(d.substrate==TerrariumData.Substrate.NONE)notice(player,"Lay substrate before adding a colony.");
            else{
                var saved=stack.get(DataComponents.CUSTOM_DATA);var colony=saved==null?null:saved.copyTag().getList("Colony",Tag.TAG_COMPOUND);
                if(colony==null || colony.isEmpty()){changed=d.introduce(species,tank.data.seed);}
                else if(colony.size()<=3 && d.residents().size()+colony.size()<=TerrariumData.MAX_RESIDENTS){
                    var residents=new java.util.ArrayList<TerrariumData.Resident>();boolean valid=true;
                    for(int i=0;i<colony.size();i++){var r=TerrariumNbt.resident(colony.getCompound(i));if(r==null || r.species!=species || d.residents().stream().anyMatch(a->a.id.equals(r.id)) || residents.stream().anyMatch(a->a.id.equals(r.id))){valid=false;break;}residents.add(r);}
                    if(valid){for(var r:residents)d.accept(r);changed=true;}
                }
                if(changed)consume(stack,player);else notice(player,"Colony cannot be added: duplicate residents or enclosure full.");
            }
        }else if(stack.is(TerrariumContent.HEAT_LAMP.get())){
            if(remove && d.heatLamp){d.heatLamp=false;refund(player,new ItemStack(TerrariumContent.HEAT_LAMP.get()));changed=true;}else if(!remove && !d.heatLamp){d.heatLamp=true;d.warm=true;d.light=14;consume(stack,player);changed=true;}
        }else if(stack.is(Items.GLOWSTONE_DUST)){
            if(remove && d.lamp){d.lamp=false;refund(player,new ItemStack(Items.GLOWSTONE_DUST));changed=true;}else if(!remove && !d.lamp){d.lamp=true;d.light=12;consume(stack,player);changed=true;}
        }else if(stack.is(Items.MAGMA_CREAM) || stack.is(Items.SNOWBALL)){
            boolean warm=stack.is(Items.MAGMA_CREAM);if(d.warm!=warm){d.warm=warm;consume(stack,player);changed=true;}
        }else if(stack.is(Items.SHEARS)){
            for(int i=tank.data.scape.pieces().size()-1;i>=0;i--){var p=tank.data.scape.pieces().get(i);if(plant(new ItemStack(AquariumControllerBlock.decorItem(p))) && p.scaleY()>.2){
                changed=tank.data.scape.transform(p.id(),tank.data.size,p.x(),p.y(),p.z(),p.rotation(),p.scaleX(),Math.max(.15,p.scaleY()*.9),p.scaleZ());break;}}
            if(changed){if(!player.getAbilities().instabuild)stack.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));notice(player,"Plant trimmed.");}else notice(player,"No plant needs trimming.");
        }
        else notice(player,"Choose a habitat supply, or place a block in Layout.");
        if(changed){TerrariumTerrain.fitDecor(tank.data);tank.data.substrate=d.substrate!=TerrariumData.Substrate.NONE;tank.changed();}
        return ItemInteractionResult.CONSUME;
    }
    public static boolean add(Player player,AquariumBlockEntity tank,ItemStack stack,double x,double z,int rotation){
        if(plant(stack) && tank.data.terrarium.substrate==TerrariumData.Substrate.NONE){notice(player,"Lay a substrate before planting.");return false;}
        boolean plant=plant(stack),moss=stack.is(Items.MOSS_BLOCK);
        if(!AquariumControllerBlock.addDecor(player,tank,stack,x,z,rotation))return false;
        if(plant){var p=tank.data.scape.pieces().getLast();tank.data.scape.transform(p.id(),tank.data.size,p.x(),p.y(),p.z(),p.rotation(),moss?1.6:2,moss?.28:2.5,moss?1.6:2);TerrariumTerrain.fitDecor(tank.data);tank.changed();}
        return true;
    }
    private TerrariumActions(){}
}
