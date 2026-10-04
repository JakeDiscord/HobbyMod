package io.github.jakediscord.hobbymod.dj.client;

import io.github.jakediscord.hobbymod.dj.MusicProject.Instrument;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native, paged instrument picker; keep the workstation's edit lease alive while choosing. */
public final class DjInstrumentScreen extends Screen implements DjEditorOverlay {
    private final DjScreen parent;
    private final Instrument selected;
    private int page,x,y,w,h,capacity;

    public DjInstrumentScreen(DjScreen parent,Instrument selected){
        super(Component.literal("Choose instrument"));this.parent=parent;this.selected=selected;
    }

    @Override protected void init(){
        clearWidgets();w=Math.min(420,width-8);h=Math.min(236,height-8);x=(width-w)/2;y=(height-h)/2;
        int rows=Math.max(1,(h-54)/18),cw=(w-16)/3;capacity=rows*3;
        var instruments=Instrument.values();page=Math.min(page,(instruments.length-1)/capacity);
        for(int cell=0;cell<capacity && page*capacity+cell<instruments.length;cell++){
            var instrument=instruments[page*capacity+cell];
            var button=Button.builder(Component.literal(font.plainSubstrByWidth((selected==instrument?"> ":"")+instrument.label(),cw-8)),b->{minecraft.setScreen(parent);parent.selectInstrument(instrument);})
                .bounds(x+8+cell%3*cw,y+26+cell/3*18,cw-2,16).build();
            button.setTooltip(Tooltip.create(Component.literal(instrument.label()+(instrument.drum()?" · percussion":" · pitched instrument"))));addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(x+8,y+h-24,cw-2,16).build());
        var previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;init();}).bounds(x+8+cw,y+h-24,cw-2,16).build());previous.active=page>0;
        var next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;init();}).bounds(x+8+cw*2,y+h-24,cw-2,16).build());next.active=(page+1)*capacity<instruments.length;
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public DjScreen editor(){return parent;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float d){}
    @Override public void tick(){parent.tick();}
    @Override public void render(GuiGraphics g,int mx,int my,float d){
        g.fill(x,y,x+w,y+h,0xff373737);g.fill(x+1,y+1,x+w-1,y+h-1,0xffc6c6c6);
        g.drawString(font,"Choose instrument · "+(page+1)+"/"+((Instrument.values().length-1)/capacity+1),x+8,y+10,0xff353535,false);
        super.render(g,mx,my,d);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
