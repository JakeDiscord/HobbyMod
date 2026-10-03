package io.github.jakediscord.hobbymod.neoforge.aquarium;

import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.aquarium.*;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid=HobbyMod.MOD_ID,value=Dist.CLIENT)
public final class AquariumOverlay {
    private static BlockPos target,controller;
    private static long checked=-1;
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var client=Minecraft.getInstance();
        if(client.level==null || client.player==null || client.screen!=null || client.options.hideGui)return;
        if(!(client.hitResult instanceof BlockHitResult hit)){target=null;controller=null;return;}
        var position=hit.getBlockPos();long time=client.level.getGameTime();
        if(!position.equals(target) || time<checked || time-checked>=10){
            target=position;checked=time;controller=null;
            search:for(int y=0;y<5;y++)for(int x=0;x<9;x++)for(int z=0;z<6;z++){
                BlockPos p=position.offset(-x,-y,-z);
                if(client.level.getBlockEntity(p) instanceof AquariumBlockEntity tank){
                    var s=tank.data.size;
                    if(x<s.width && y<s.height && z<s.depth){controller=p;break search;}
                }
            }
        }
        if(controller==null || !(client.level.getBlockEntity(controller) instanceof AquariumBlockEntity tank))return;
        var d=tank.data;var lines=new ArrayList<String>();
        lines.add((d.warm?"Warm":"Cool")+" · Quality "+d.quality+"% · Fish "+d.fish().size());
        String warning=tank.warning();if(!warning.isEmpty())lines.add(warning);
        var fish=d.selected();if(fish!=null){
            lines.add(fish.label()+" · "+(fish.female?"Female":"Male")+" · "+fish.health+"%"+(fish.acclimation>0?" · Acclimating":""));
            if(client.player.isShiftKeyDown())lines.add("Color "+fish.colorA+"/"+fish.colorB+" · Form "+fish.formA+"/"+fish.formB);
        }
        if(client.player.isShiftKeyDown())lines.add("Food "+d.food+" · Algae "+d.algae+" · Plants "+d.plants+" · Stock "+d.load()+"/"+d.size.volume());
        var graphics=event.getGuiGraphics();int y=graphics.guiHeight()-64-lines.size()*12;
        for(String line:lines)for(var wrapped:client.font.split(Component.literal(line),Math.max(80,graphics.guiWidth()-24))){
            int width=client.font.width(wrapped),x=(graphics.guiWidth()-width)/2;
            graphics.fill(x-3,y-2,x+width+3,y+10,0xA0000000);graphics.drawString(client.font,wrapped,x,y,0xFFFFFF,true);y+=12;
        }
    }
}
