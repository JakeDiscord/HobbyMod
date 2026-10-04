package io.github.jakediscord.hobbymod.neoforge.aquarium;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.aquarium.AquariumContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid=HobbyMod.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AquariumClientEvents {
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
        for(var entry:AquariumContent.FISH.entrySet())event.register((stack,tint)->{
            if(tint!=1)return 0xFFFFFFFF;
            var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            var fish=data==null?null:io.github.jakediscord.hobbymod.aquarium.AquariumNbt.fish(data.copyTag());
            return 0xFF000000|(fish==null?entry.getKey().color:fish.color());
        },entry.getValue().get());
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(AquariumContent.TANK_ENTITY.get(),NeoForgeAquariumRenderer::new);event.registerEntityRenderer(io.github.jakediscord.hobbymod.habitats.HabitatWildlife.CRITTER.get(),io.github.jakediscord.hobbymod.habitats.client.WildCritterRenderer::new);event.registerEntityRenderer(io.github.jakediscord.hobbymod.habitats.HabitatWildlife.FISH.get(),net.minecraft.client.renderer.entity.TropicalFishRenderer::new);}
}
