package io.github.jakediscord.hobbymod.neoforge.bonsai;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.bonsai.BonsaiBlockEntity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Vanilla skips block use while sneaking with an item; bonsai tools need that gesture. */
@EventBusSubscriber(modid=HobbyMod.MOD_ID)
public final class BonsaiInteractions {
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event){
        var stack=event.getItemStack();
        if(event.getEntity().isShiftKeyDown() && (stack.is(Items.COPPER_INGOT) || stack.is(Items.SHEARS))
                && event.getLevel().getBlockEntity(event.getPos()) instanceof BonsaiBlockEntity){
            event.setUseBlock(TriState.TRUE);
        }
    }
}
