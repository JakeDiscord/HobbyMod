package io.github.jakediscord.hobbymod.neoforge.aquarium;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.aquarium.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid=HobbyMod.MOD_ID,value=Dist.CLIENT)
public final class AquariumOverlay {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var c=Minecraft.getInstance();
        if(c.level==null || c.player==null || c.screen!=null || c.options.hideGui || !(c.hitResult instanceof BlockHitResult hit))return;
        var tank=AquariumPartBlock.find(c.level,hit.getBlockPos());if(tank==null)return;
        String text=tank.data.terrarium==null?"Right-click to manage aquarium":"Right-click to manage terrarium";var g=event.getGuiGraphics();int x=(g.guiWidth()-c.font.width(text))/2,y=g.guiHeight()-65;
        g.fill(x-4,y-3,x+c.font.width(text)+4,y+12,0xA0000000);g.drawString(c.font,text,x,y,0xFFFFFF,true);
    }
}
