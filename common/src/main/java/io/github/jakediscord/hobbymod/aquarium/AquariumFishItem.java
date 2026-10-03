package io.github.jakediscord.hobbymod.aquarium;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

public final class AquariumFishItem extends Item {
    public final AquariumData.Species species;
    public AquariumFishItem(AquariumData.Species species){super(new Item.Properties().stacksTo(1));this.species=species;}
    public AquariumData.Fish resident(ItemStack stack,AquariumData tank){
        var saved=stack.get(DataComponents.CUSTOM_DATA);
        if(saved!=null){var fish=AquariumNbt.fish(saved.copyTag());return fish!=null && fish.species==species?fish:null;}
        return tank.newFish(species);
    }
    public static ItemStack capture(AquariumData.Fish fish){
        ItemStack stack=new ItemStack(AquariumContent.FISH.get(fish.species).get());
        var tag=AquariumNbt.fish(fish);tag.putBoolean("CapturedBucket",true);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
    }
    @Override public Component getName(ItemStack stack){var saved=stack.get(DataComponents.CUSTOM_DATA);
        return Component.literal(species.label+(saved!=null && saved.copyTag().getBoolean("CapturedBucket")?" in a Bucket":" Bag"));}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal(species.warm?"Warm water":"Cool water"));
        var saved=stack.get(DataComponents.CUSTOM_DATA);var fish=saved==null?null:AquariumNbt.fish(saved.copyTag());
        if(fish!=null){
            lines.add(Component.literal((fish.female?"Female":"Male")+" · Health "+fish.health+"% · Age "+fish.age+" min"));
            lines.add(Component.literal("Color "+fish.colorA+"/"+fish.colorB+" · Form "+fish.formA+"/"+fish.formB));
            if(flag.isAdvanced()){
                lines.add(Component.literal("ID "+fish.id));
                if(fish.mother!=null)lines.add(Component.literal("Mother "+fish.mother));
                if(fish.father!=null)lines.add(Component.literal("Father "+fish.father));
            }
        }
    }
}
