package io.github.jakediscord.hobbymod.aquarium;

import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.DyeColor;

/** The same native pixel-texture phenotype in the wild and inside an aquarium. */
public final class AquariumFishAppearance {
    public static int variant(AquariumData.Fish fish){
        var pattern=switch(fish.species){case BETTA->TropicalFish.Pattern.BETTY;case ANGELFISH->TropicalFish.Pattern.BLOCKFISH;case GOLDFISH->TropicalFish.Pattern.FLOPPER;case NEON_TETRA,ZEBRA_DANIO->TropicalFish.Pattern.SUNSTREAK;case CHERRY_BARB->TropicalFish.Pattern.BRINELY;default->fish.formA%2==0?TropicalFish.Pattern.KOB:TropicalFish.Pattern.SPOTTY;};
        DyeColor base=closest(fish.color());DyeColor marking=switch(fish.species){case NEON_TETRA->DyeColor.LIGHT_BLUE;case ZEBRA_DANIO,ANGELFISH->DyeColor.BLACK;case GOLDFISH->DyeColor.YELLOW;default->DyeColor.byId((base.getId()+fish.colorB)%16);};
        return new TropicalFish.Variant(pattern,base,marking).getPackedId();
    }
    private static DyeColor closest(int rgb){DyeColor best=DyeColor.WHITE;long distance=Long.MAX_VALUE;for(var color:DyeColor.values()){int c=color.getTextureDiffuseColor();long r=((rgb>>16)&255)-((c>>16)&255),g=((rgb>>8)&255)-((c>>8)&255),b=(rgb&255)-(c&255),d=r*r+g*g+b*b;if(d<distance){distance=d;best=color;}}return best;}
    private AquariumFishAppearance(){}
}
