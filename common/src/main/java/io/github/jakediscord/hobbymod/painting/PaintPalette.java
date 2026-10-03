package io.github.jakediscord.hobbymod.painting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
public final class PaintPalette {
    public static final int PER_DYE=512,MAX=4096;
    public static ItemStack find(Player p){for(var stack:p.getInventory().items)if(stack.is(PaintingContent.PALETTE.get()))return stack;for(var stack:p.getInventory().offhand)if(stack.is(PaintingContent.PALETTE.get()))return stack;return ItemStack.EMPTY;}
    public static int[] amounts(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);int[] old=data==null?new int[0]:data.copyTag().getIntArray("Pigments"),a=new int[16];for(int i=0;i<Math.min(16,old.length);i++)a[i]=Math.clamp(old[i],0,MAX);return a;}
    private static void write(ItemStack stack,int[] a){var old=stack.get(DataComponents.CUSTOM_DATA);CompoundTag t=old==null?new CompoundTag():old.copyTag();t.putIntArray("Pigments",a);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static boolean load(Player p,int color){if(color<0 || color>15)return false;var palette=find(p);if(palette.isEmpty())return false;var a=amounts(palette);if(a[color]>MAX-PER_DYE)return false;
        for(var stack:java.util.stream.Stream.concat(p.getInventory().items.stream(),p.getInventory().offhand.stream()).toList())if(stack.getItem() instanceof DyeItem dye && dye.getDyeColor().getId()==color){a[color]+=PER_DYE;write(palette,a);if(!p.getAbilities().instabuild)stack.shrink(1);return true;}return false;}
    public static boolean pay(Player p,int first,int second,int mix,int cost){if(first<0 || first>15 || second<0 || second>15 || mix<0 || mix>100 || cost<1)return false;var palette=find(p);if(palette.isEmpty())return false;var a=amounts(palette);boolean useA=mix<100 || first==second,useB=mix>0 && first!=second;if((useA && a[first]<cost) || (useB && a[second]<cost))return false;if(!p.getAbilities().instabuild){if(useA)a[first]-=cost;if(useB)a[second]-=cost;write(palette,a);}return true;}
    public static int color(int a,int b,int mix){return PaintingData.mix(DyeColor.byId(a).getTextureDiffuseColor(),DyeColor.byId(b).getTextureDiffuseColor(),mix);}
    private PaintPalette(){}
}
