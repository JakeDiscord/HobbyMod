package io.github.jakediscord.hobbymod.neoforge.aquarium;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.aquarium.AquariumBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=HobbyMod.MOD_ID)
public final class AquariumInteractions {
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity().isShiftKeyDown() && event.getLevel().getBlockEntity(event.getPos()) instanceof AquariumBlockEntity)event.setUseBlock(TriState.TRUE);
    }
}
