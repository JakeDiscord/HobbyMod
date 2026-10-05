package io.github.jakediscord.hobbymod.winery.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.Util;

/** Quiet, short tasting traits; one word at a time instead of an action-bar paragraph. */
public final class TastingOverlay {
    private static List<String> notes=List.of();private static long started;
    private static final int NOTE_MS=2200;
    public static void begin(List<String> traits){notes=List.copyOf(traits);started=Util.getMillis();}
    public static void clear(){notes=List.of();}
    public static void render(GuiGraphics g){
        var mc=Minecraft.getInstance();if(mc.level==null || mc.screen!=null || mc.options.hideGui || notes.isEmpty())return;
        long elapsed=Util.getMillis()-started;int index=(int)(elapsed/NOTE_MS);if(index>=notes.size()){clear();return;}long local=elapsed%NOTE_MS;
        String full=notes.get(index)+"...";String visible=full.substring(0,(int)Math.min(full.length(),1+local/65));
        double opacity=Math.min(Math.min(1,local/200D),Math.max(0,(NOTE_MS-local)/400D));int alpha=(int)(opacity*255);if(alpha<8)return;
        float x=(g.guiWidth()-mc.font.width(full))/2F,y=g.guiHeight()-84;
        g.pose().pushPose();g.pose().translate(x+Math.sin(elapsed*.003)*.65,y+Math.cos(elapsed*.002)*.45,0);
        g.drawString(mc.font,visible,0,0,(alpha<<24)|0xf1e6cd,true);g.pose().popPose();
    }
    private TastingOverlay(){}
}
