package io.github.jakediscord.hobbymod.neoforge.bonsai;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.bonsai.BonsaiBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Read synchronized care every frame, without overwriting interaction feedback in the action bar. */
@EventBusSubscriber(modid=HobbyMod.MOD_ID,value=Dist.CLIENT)
public final class BonsaiCareOverlay {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null || client.level==null || client.screen!=null || client.options.hideGui)return;
        if(!(client.hitResult instanceof BlockHitResult hit)
                || !(client.level.getBlockEntity(hit.getBlockPos()) instanceof BonsaiBlockEntity tree)
                || !tree.graph.planted())return;
        var graph=tree.graph;
        java.util.List<String> lines=new java.util.ArrayList<>();
        lines.add(graph.species+" · "+graph.age+" min");
        lines.add("Water "+graph.water+"% · Health "+graph.health+"%");
        if(client.player.isShiftKeyDown())lines.add("Soil "+graph.soilAge+" min · Roots "+graph.rootAge+" min");
        String care=graph.careStatus(client.level.getMaxLocalRawBrightness(hit.getBlockPos().above())>=9);
        if(!care.equals("Healthy"))lines.add(care);
        var graphics=event.getGuiGraphics();
        int y=graphics.guiHeight()-64-lines.size()*12;
        for(String line:lines) {
            for(var wrapped:client.font.split(net.minecraft.network.chat.Component.literal(line),Math.max(80,graphics.guiWidth()-24))) {
                int width=client.font.width(wrapped),x=(graphics.guiWidth()-width)/2;
                graphics.fill(x-3,y-2,x+width+3,y+10,0xA0000000);
                graphics.drawString(client.font,wrapped,x,y,0xFFFFFF,true);
                y+=12;
            }
        }
    }
}
