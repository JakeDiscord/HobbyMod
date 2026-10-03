package io.github.jakediscord.hobbymod.pottery.client;

import io.github.jakediscord.hobbymod.pottery.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class KilnScreen extends AbstractContainerScreen<KilnMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    public KilnScreen(KilnMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int burn=menu.progress.get(0),firing=menu.progress.get(1),cooling=menu.progress.get(2);
        if(burn>0 && firing>0 && cooling==0 && menu.progress.get(3)==0){int h=Math.max(1,burn*13/KilnBlockEntity.FUEL_TICKS);g.blitSprite(ResourceLocation.withDefaultNamespace("container/furnace/lit_progress"),14,14,0,14-h,leftPos+56,topPos+36+14-h,14,h);}
        int w=menu.progress.get(3)==1?24:firing*24/KilnBlockEntity.FIRING_TICKS;
        if(w>0)g.blitSprite(ResourceLocation.withDefaultNamespace("container/furnace/burn_progress"),24,16,0,0,leftPos+79,topPos+34,w,16);
        String status=cooling>0?"Cooling: "+((cooling+19)/20)+"s":menu.progress.get(3)==1?"Ready":menu.progress.get(4)==0?"":burn==0?"Add fuel":"Firing: "+firing*100/KilnBlockEntity.FIRING_TICKS+"%";
        if(!status.isEmpty())g.drawString(font,status,leftPos+79,topPos+58,cooling>0?0x397ca2:0x404040,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
