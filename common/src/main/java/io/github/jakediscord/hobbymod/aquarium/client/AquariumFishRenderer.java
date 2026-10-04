package io.github.jakediscord.hobbymod.aquarium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jakediscord.hobbymod.aquarium.AquariumData;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/** Vanilla fish models, pixel textures and animated tails. Visual proxies never enter the world. */
public final class AquariumFishRenderer {
    private final Map<UUID,AbstractFish> models=new LinkedHashMap<>(256,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<UUID,AbstractFish> entry){if(size()>256){styles.remove(entry.getKey());return true;}return false;}
    };
    private record Style(AquariumData.Species species,int colorA,int colorB,int formA){}
    private final Map<UUID,Style> styles=new HashMap<>();
    private Level world;
    public void render(AquariumData.Fish resident,Level level,double ticks,float partial,float yaw,PoseStack poses,MultiBufferSource buffers,int light){
        if(world!=level){models.clear();styles.clear();world=level;}
        AbstractFish fish=models.computeIfAbsent(resident.id,id->resident.species==AquariumData.Species.CORYDORAS
                ?new Cod(EntityType.COD,level){@Override public boolean isInWater(){return true;}}
                :new TropicalFish(EntityType.TROPICAL_FISH,level){@Override public boolean isInWater(){return true;}});
        fish.setNoAi(true);fish.tickCount=(int)Math.floor(ticks);fish.setYRot(yaw);fish.yRotO=yaw;fish.yBodyRot=yaw;fish.yBodyRotO=yaw;fish.yHeadRot=yaw;fish.yHeadRotO=yaw;
        var style=styles.get(resident.id);
        if(fish instanceof TropicalFish tropical && (style==null || style.species!=resident.species || style.colorA!=resident.colorA || style.colorB!=resident.colorB || style.formA!=resident.formA)){
            styles.put(resident.id,new Style(resident.species,resident.colorA,resident.colorB,resident.formA));
            var tag=new CompoundTag();tag.putInt("Variant",io.github.jakediscord.hobbymod.aquarium.AquariumFishAppearance.variant(resident));tropical.readAdditionalSaveData(tag);
        }
        float scale=(float)(resident.size()*(resident.species==AquariumData.Species.GOLDFISH?.7:.55));
        poses.pushPose();poses.scale(scale,scale,scale);
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fish).render(fish,yaw,partial,poses,buffers,light);
        poses.popPose();
    }
}
