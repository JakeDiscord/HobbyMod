package io.github.jakediscord.hobbymod.sculpting.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.sculpting.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public final class BlueprintFilesScreen extends Screen {
    private final InteractionHand hand;
    private EditBox filename;
    private String status="",files="";
    public BlueprintFilesScreen(InteractionHand hand){super(Component.literal("Marble blueprints"));this.hand=hand;}
    private Path folder(){return minecraft.gameDirectory.toPath().resolve("hobbymod/blueprints");}
    @Override protected void init(){
        filename=addRenderableWidget(new EditBox(font,width/2-140,height/2-38,280,20,Component.literal("Filename")));filename.setMaxLength(64);filename.setValue("my-sculpture");setInitialFocus(filename);
        addRenderableWidget(Button.builder(Component.literal("Import file"),b->transfer(false)).bounds(width/2-140,height/2-8,135,20).build());
        addRenderableWidget(Button.builder(Component.literal("Export file"),b->transfer(true)).bounds(width/2+5,height/2-8,135,20).build());
        addRenderableWidget(Button.builder(Component.literal("Open folder"),b->{try{Files.createDirectories(folder());net.minecraft.Util.getPlatform().openPath(folder());}catch(IOException e){status="Could not open blueprint folder.";}}).bounds(width/2-140,height/2+22,135,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2+5,height/2+22,135,20).build());
        refresh();
    }
    private void refresh(){try{Files.createDirectories(folder());try(var paths=Files.list(folder())){files=paths.filter(p->p.getFileName().toString().endsWith(".marble.json")).map(p->p.getFileName().toString().replace(".marble.json","")).sorted().limit(4).collect(java.util.stream.Collectors.joining(", "));}}catch(IOException e){files="Folder unavailable";}}
    private void transfer(boolean export){
        String name=filename.getValue().trim();if(!name.matches("[A-Za-z0-9_-]{1,64}")){status="Use letters, numbers, underscores or hyphens.";return;}
        var stack=minecraft.player.getItemInHand(hand);if(!(stack.getItem() instanceof MarbleBlueprintItem)){status="Hold your marble blueprint.";return;}
        Path file=folder().resolve(name+".marble.json");
        try{
            Files.createDirectories(folder());
            if(export){var blueprint=MarbleBlueprint.decode(MarbleBlueprintItem.bytes(stack));Files.writeString(file,blueprint.json(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);status="Exported "+file.getFileName();refresh();}
            else{if(Files.isSymbolicLink(file) || Files.size(file)>MarbleBlueprint.MAX_JSON)throw new IllegalArgumentException("Blueprint file is too large or is a link.");
                String text;try(var in=Files.newInputStream(file)){byte[] bytes=in.readNBytes(MarbleBlueprint.MAX_JSON+1);if(bytes.length>MarbleBlueprint.MAX_JSON)throw new IllegalArgumentException("Blueprint file is too large.");text=new String(bytes,StandardCharsets.UTF_8);}
                var blueprint=MarbleBlueprint.fromJson(text);NetworkManager.sendToServer(new BlueprintNetworking.Import(hand,blueprint.compressed()));status="Sent import · server confirmation appears in chat.";
            }
        }catch(FileAlreadyExistsException e){status="File exists. Choose a new export name.";}catch(NoSuchFileException e){status="File not found. Put it in the blueprints folder.";}catch(IOException | IllegalArgumentException e){status=e instanceof IOException?"Could not read/write blueprint file.":e.getMessage();}
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        super.render(g,x,y,partial);g.drawCenteredString(font,title,width/2,height/2-90,0xffffff);
        g.drawCenteredString(font,"Filename (without .marble.json)",width/2,height/2-55,0xc9c0b1);
        g.drawCenteredString(font,status,width/2,height/2+56,0xeee0ac);
        g.drawCenteredString(font,"Files: "+files,width/2,height/2+76,0xc9c0b1);
        g.drawCenteredString(font,"Saved in your Minecraft folder / hobbymod / blueprints",width/2,height/2+94,0xc9c0b1);
    }
    @Override public boolean isPauseScreen(){return false;}
}
