package io.github.jakediscord.hobbymod.dj.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.List;
public final class DjFilesScreen extends Screen implements DjEditorOverlay {
    private final DjScreen parent;private final List<Path> files;private int page;
    public DjFilesScreen(DjScreen parent,List<Path> files){super(Component.literal("Music projects"));this.parent=parent;this.files=files;}
    @Override protected void init(){clearWidgets();int x=width/2-150,y=height/2-100;for(int i=0;i<7;i++){int index=page*7+i;if(index>=files.size())break;var file=files.get(index);addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(file.getFileName().toString(),280)),b->{minecraft.setScreen(parent);parent.imported(file);}).bounds(x,y+24+i*19,300,18).build());}addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(x,y+166,94,18).build());addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page=Math.max(0,page-1);init();}).bounds(x+100,y+166,94,18).build());addRenderableWidget(Button.builder(Component.literal("Next"),b->{if((page+1)*7<files.size())page++;init();}).bounds(x+200,y+166,100,18).build());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public DjScreen editor(){return parent;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float d){}
    @Override public void tick(){parent.tick();}
    @Override public void render(GuiGraphics g,int mx,int my,float d){int x=width/2-154,y=height/2-104;g.fill(x,y,x+308,y+196,0xffc6c6c6);g.drawString(font,files.isEmpty()?"No .hobbytrack files in hobbymod/music":"Choose project · page "+(page+1),x+8,y+10,0xff353535,false);super.render(g,mx,my,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
