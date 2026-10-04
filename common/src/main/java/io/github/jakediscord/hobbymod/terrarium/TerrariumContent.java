package io.github.jakediscord.hobbymod.terrarium;

import dev.architectury.registry.registries.*;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.aquarium.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class TerrariumContent {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(HobbyMod.MOD_ID,Registries.ITEM);
    public static final RegistrySupplier<Item> KIT=ITEMS.register("terrarium",()->new AquariumKitItem(AquariumData.Size.MEDIUM,true));
    public static final RegistrySupplier<Item> MISTER=ITEMS.register("terrarium_mister",()->named("Terrarium Mister",new Item.Properties().durability(96)));
    public static final RegistrySupplier<Item> HEAT_LAMP=ITEMS.register("terrarium_heat_lamp",()->named("Terrarium Heat Lamp",new Item.Properties()));
    public static final RegistrySupplier<Item> FOOD=ITEMS.register("leaf_litter",()->named("Leaf Litter",new Item.Properties()));
    public static final RegistrySupplier<Item> SPRINGTAILS=ITEMS.register("springtail_colony",()->named("Springtail Colony",new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> ISOPODS=ITEMS.register("isopod_colony",()->named("Isopod Colony",new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> SNAILS=ITEMS.register("snail",()->named("Terrarium Snail",new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> FROGS=ITEMS.register("tree_frog",()->named("Terrarium Tree Frog",new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> GECKOS=ITEMS.register("gecko",()->named("Terrarium Gecko",new Item.Properties().stacksTo(16)));
    private static Item named(String name,Item.Properties properties){return new Item(properties){@Override public Component getName(ItemStack stack){return Component.literal(name);}
        @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){
            if(name.endsWith("Colony") || name.equals("Terrarium Snail") || name.equals("Terrarium Tree Frog") || name.equals("Terrarium Gecko")){var saved=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);if(saved==null)lines.add(Component.literal(name.endsWith("Colony")?"Introduces 3 tiny residents":"Introduces one resident"));else{
                var list=saved.copyTag().getList("Colony",net.minecraft.nbt.Tag.TAG_COMPOUND);lines.add(Component.literal(Math.min(3,list.size())+" collected residents"));
                var variants=new java.util.LinkedHashSet<String>();String[] names={"slate","cream","blue","copper"};for(int i=0;i<Math.min(3,list.size());i++)variants.add(names[Math.clamp(list.getCompound(i).getInt("Variant"),0,3)]);lines.add(Component.literal("Variants: "+String.join(", ",variants)));
            }}
            else if(name.equals("Terrarium Mister"))lines.add(Component.literal("Mist your habitat to raise moisture and humidity"));
            else if(name.equals("Terrarium Heat Lamp"))lines.add(Component.literal("Warms the enclosure and emits indoor light"));
            else if(name.equals("Leaf Litter"))lines.add(Component.literal("Food for terrarium inhabitants"));
        }};}
    public static Item colony(TerrariumData.Species species){return switch(species){case SPRINGTAIL->SPRINGTAILS.get();case ISOPOD->ISOPODS.get();case SNAIL->SNAILS.get();case TREE_FROG->FROGS.get();case GECKO->GECKOS.get();};}
    public static TerrariumData.Species species(ItemStack stack){for(var species:TerrariumData.Species.values())if(stack.is(colony(species)))return species;return null;}
    public static void register(){ITEMS.register();}
    private TerrariumContent(){}
}
