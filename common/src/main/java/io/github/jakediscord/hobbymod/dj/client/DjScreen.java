package io.github.jakediscord.hobbymod.dj.client;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.dj.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/** Native widgets and crisp pixel grids; project edits are local until acknowledged by the authoritative workstation. */
public final class DjScreen extends Screen {
    public final BlockPos pos;
    private MusicProject project;
    private int revision,deck,tab,track,pattern,songPage,songLane,octave=60,length=2,velocity=100,x,y,w,h,age;
    private boolean dirty,waiting,closing,dragging;
    private long dirtyAt,sentAt;
    private int dragStart,dragLength,dragMode,originalStep,originalPitch;
    private boolean stepPaint;
    private int pendingCross;
    private MusicProject.Note dragged;
    private EditBox title;
    private String notice="",confirm="";
    private final ArrayDeque<int[]> commands=new ArrayDeque<>();
    private DjClient.Playback audition,monitor;
    private int monitorDeck=-1;
    private final ArrayDeque<byte[]> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private byte[] baseline;
    private boolean wavBusy;
    private long auditionAt;
    private static final int[] COLORS={0xffbd7652,0xffbc9860,0xffb6b8a3,0xffb69aaf,0xff779ec1,0xff91af80,0xffc2aa69,0xff9c8bbb};
    private static final String[] TABS={"Patterns","Piano","Song","Mixer","DJ","Files"};
    private int gridX(){return x+94;}private int gridY(){return y+82;}private int stepW(){return Math.max(12,(w-106)/16);}private int gridW(){return stepW()*16;}
    public DjScreen(BlockPos p){super(Component.literal("DJ Studio"));pos=p;}
    private DjBlockEntity block(){return minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(pos) instanceof DjBlockEntity b?b:null;}
    private void read(){var b=block();if(b!=null){project=b.projects[deck].copy();revision=b.revision;if(baseline==null)baseline=project.encode();}}
    @Override protected void init(){if(project==null)read();if(project==null)return;w=Math.min(460,width-8);h=Math.min(236,height-8);x=(width-w)/2;y=(height-h)/2;build();}
    private Button button(String text,int bx,int by,int bw,Runnable action){return addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x+bx,y+by,bw,16).build());}
    private void build(){if(project==null)return;boolean titleFocused=title!=null && title.isFocused();int titleCursor=title==null?0:title.getCursorPosition();clearWidgets();
        title=new EditBox(font,x+86,y+8,w-194,16,Component.literal("Song title"));title.setMaxLength(32);title.setValue(project.title);title.setResponder(s->{project.title=MusicProject.clean(s);changed();});addRenderableWidget(title);if(titleFocused){setFocused(title);title.setFocused(true);title.moveCursorTo(Math.min(titleCursor,title.getValue().length()),false);}
        button(deck==0?"[A]":"A",w-102,8,44,()->switchDeck(0));button(deck==1?"[B]":"B",w-54,8,44,()->switchDeck(1));
        int tw=(w-16)/6;for(int t=0;t<6;t++){int selected=t;button((tab==t?"> ":"")+TABS[t],8+t*tw,30,tw-2,()->{tab=selected;confirm="";build();});}
        switch(tab){
            case 0->{int cw=(w-16)/5;button("Pattern "+(char)('A'+pattern),8,55,cw-2,()->{pattern=(pattern+1)%8;build();});button(project.tracks[track].instrument.label(),8+cw,55,cw-2,()->minecraft.setScreen(new DjInstrumentScreen(this,project.tracks[track].instrument)));button("Copy >",8+cw*2,55,cw-2,()->{project.copyPattern(pattern,(pattern+1)%8);pattern=(pattern+1)%8;changed();build();});button("Clear",8+cw*3,55,cw-2,()->confirm("clear",()->{project.clearPattern(pattern);changed();}));button("Swing "+project.swing+"%",8+cw*4,55,cw-2,()->{project.swing=(project.swing+5)%80;changed();build();});}
            case 1->{int cw=(w-16)/6;button(project.tracks[track].name,8,55,cw-2,()->{track=(track+1)%8;build();});button("Pat "+(char)('A'+pattern),8+cw,55,cw-2,()->{pattern=(pattern+1)%8;build();});button("Oct -",8+cw*2,55,cw-2,()->{octave=Math.max(36,octave-12);});button("Oct +",8+cw*3,55,cw-2,()->{octave=Math.min(73,octave+12);});button("Len "+length,8+cw*4,55,cw-2,()->{length=length>=16?1:length*2;build();});button("Vel "+velocity,8+cw*5,55,cw-2,()->{velocity=velocity>=127?32:Math.min(127,velocity+16);build();});}
            case 2->{int cw=(w-16)/5;button("Pat "+(char)('A'+pattern),8,55,cw-2,()->{pattern=(pattern+1)%8;build();});button("Bars "+project.bars,8+cw,55,cw-2,()->{project.bars=project.bars>=64?1:project.bars+1;changed();build();});button("Repeat L"+(songLane+1),8+cw*2,55,cw-2,()->{project.repeatPattern(songLane,pattern);changed();});button("Clear song",8+cw*3,55,cw-2,()->confirm("song",()->{project.clearSong();changed();}));button("Page "+(songPage+1),8+cw*4,55,cw-2,()->{songPage=(songPage+1)%4;build();});}
            case 3->mixerWidgets();
            case 4->deckWidgets();
            case 5->fileWidgets();
        }
        var b=block();boolean compact=w<400;button(b!=null && b.playing[deck]?"Stop":"Play",8,h-38,compact?40:48,()->command(b!=null && b.playing[deck]?DjNetworking.STOP:DjNetworking.PLAY,0));
        button(b!=null && b.pattern[deck]>=0?"Pat "+(char)('A'+b.pattern[deck]):"Song",compact?52:60,h-38,compact?42:48,()->command(DjNetworking.MODE,b!=null && b.pattern[deck]<0?pattern:-1));
        button(b!=null && b.loop[deck]?"Loop +":"Loop -",compact?98:112,h-38,compact?48:56,()->command(DjNetworking.LOOP,0));
        button("-",compact?150:174,h-38,compact?16:18,()->{project.bpm=Math.max(60,project.bpm-1);changed();build();});button("+",compact?216:244,h-38,compact?16:18,()->{project.bpm=Math.min(200,project.bpm+1);changed();build();});
        if(!compact)button("Disc",w-118,h-38,48,()->command(DjNetworking.BURN,0));button("Close",w-66,h-38,56,this::onClose);
    }
    private void mixerWidgets(){int cw=(w-16)/8;for(int i=0;i<8;i++){int index=i,bx=8+i*cw;var t=project.tracks[i];button(t.mute?"M+":"M",bx,148,cw/2-1,()->{t.mute=!t.mute;changed();build();});button(t.solo?"S+":"S",bx+cw/2,148,cw/2-1,()->{t.solo=!t.solo;changed();build();});button("P"+t.pan,bx,166,cw-2,()->{t.pan=t.pan>=100?-100:Math.min(100,t.pan+25);changed();build();});button("F"+t.cutoff,bx,55,cw-2,()->{t.cutoff=t.cutoff<=0?100:Math.max(0,t.cutoff-25);changed();build();});button("D"+t.delay,bx,74,cw-2,()->{t.delay=t.delay>=100?0:Math.min(100,t.delay+25);changed();build();});button("R"+t.reverb,bx,93,cw-2,()->{t.reverb=t.reverb>=100?0:Math.min(100,t.reverb+25);changed();build();});}}
    private void deckWidgets(){var b=block();if(b==null)return;int half=(w-24)/2;for(int d=0;d<2;d++){int target=d,bx=8+d*(half+8),cw=(half-6)/4;button(b.playing[d]?"Stop":"Play",bx,108,cw,()->deckCommand(target,b.playing[target]?DjNetworking.STOP:DjNetworking.PLAY,0));button("Cue",bx+cw+2,108,cw,()->deckCommand(target,DjNetworking.CUE,0));button("Sync",bx+cw*2+4,108,cw,()->deckCommand(target,DjNetworking.SYNC,0));button(b.loop[d]?"Loop +":"Loop -",bx+cw*3+6,108,cw,()->deckCommand(target,DjNetworking.LOOP,0));button("Load disc",bx,128,half/2-2,()->deckCommand(target,DjNetworking.LOAD,0));button(monitorDeck==d?"Monitor +":"Monitor",bx+half/2+2,128,half/2-2,()->monitor(target));}
        button("A",8,166,28,()->command(DjNetworking.CROSS,0));button("Center",w/2-26,146,52,()->command(DjNetworking.CROSS,50));button("B",w-36,166,28,()->command(DjNetworking.CROSS,100));}
    private void fileWidgets(){int cw=(w-32)/3;button("Export project",12,58,cw,this::exportProject);button("Import project",20+cw,58,cw,this::importProject);button("Export WAV",28+cw*2,58,cw,this::exportWav);button("Load held disc",12,88,cw,()->command(DjNetworking.LOAD,0));button("Write music disc",20+cw,88,cw,()->command(DjNetworking.BURN,0));button("Demo",28+cw*2,88,cw,()->confirm("demo",()->command(DjNetworking.DEMO,0)));button("New empty project",12,118,cw,()->confirm("new",()->command(DjNetworking.NEW,0)));button("Master "+project.master+"%",20+cw,118,cw,()->{project.master=project.master>=100?0:Math.min(100,project.master+5);changed();build();});}
    private void confirm(String key,Runnable action){if(confirm.equals(key)){confirm="";action.run();build();}else{confirm=key;notice="Click again to confirm "+key+".";}}
    private void changed(){byte[] current=project.encode();if(Arrays.equals(baseline,current))return;if(baseline!=null && !Arrays.equals(baseline,current)){undo.addLast(baseline);while(undo.size()>32)undo.removeFirst();redo.clear();}baseline=current;dirty=true;dirtyAt=System.currentTimeMillis();confirm="";}
    private void send(int kind,int target,int value,byte[] data){waiting=true;sentAt=System.currentTimeMillis();NetworkManager.sendToServer(new DjNetworking.Action(pos,revision,kind,target,value,data));}
    private void command(int kind,int value){deckCommand(deck,kind,value);}
    private void deckCommand(int target,int kind,int value){var b=block();var song=target==deck?project:b==null?null:b.projects[target];if(kind==DjNetworking.PLAY && b!=null && b.pattern[target]<0 && song!=null && song.songEmpty())commands.add(new int[]{DjNetworking.MODE,target,pattern});commands.add(new int[]{kind,target,value});flush();}
    private void flush(){if(waiting || project==null || dragging)return;if(dirty){dirty=false;send(DjNetworking.SAVE,deck,0,project.encode());}else if(!commands.isEmpty()){var c=commands.remove();send(c[0],c[1],c[2],new byte[0]);}}
    public void ack(String message){waiting=false;notice=message;if(message.startsWith("The set changed") || message.startsWith("Invalid project") || message.contains("expired")){dirty=false;commands.clear();baseline=null;undo.clear();redo.clear();}if(!dirty){read();build();}else{var b=block();if(b!=null)revision=b.revision;}if(closing){flush();if(!waiting && !dirty && commands.isEmpty())finishClose();}else flush();}
    private void switchDeck(int next){if(next==deck)return;if(dirty || waiting || !commands.isEmpty()){notice="Waiting for this deck to save before switching.";flush();return;}deck=next;baseline=null;undo.clear();redo.clear();read();build();}
    @Override public void tick(){super.tick();age++;var b=block();if(b==null || minecraft.player==null || !io.github.jakediscord.hobbymod.pottery.PotteryNetworking.permitted(minecraft.player,pos)){finishClose();return;}
        if(age%40==0)NetworkManager.sendToServer(new DjNetworking.Action(pos,revision,DjNetworking.HEARTBEAT,deck,0,new byte[0]));
        if(dirty && System.currentTimeMillis()-dirtyAt>400 || !commands.isEmpty())flush();
        if(waiting && System.currentTimeMillis()-sentAt>5000){notice="Waiting for the server to acknowledge your save…";sentAt=System.currentTimeMillis();}
        if(audition!=null && System.currentTimeMillis()-auditionAt>1500){audition.close();audition=null;}
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){}
    private void panel(GuiGraphics g,int px,int py,int pw,int ph){g.fill(px,py,px+pw,py+ph,0xff373737);g.fill(px+1,py+1,px+pw-1,py+ph-1,0xffc6c6c6);g.fill(px+2,py+2,px+pw-2,py+3,0xffeeeeee);g.fill(px+2,py+ph-3,px+pw-2,py+ph-2,0xff686868);}
    private void label(GuiGraphics g,String s,int px,int py,int color){g.drawString(font,s,px,py,color,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){if(project==null)return;panel(g,x,y,w,h);label(g,"DJ Studio",x+8,y+12,0xff353535);switch(tab){case 0->patterns(g,mx,my);case 1->piano(g,mx,my);case 2->song(g,mx,my);case 3->mixer(g,mx,my);case 4->decks(g,mx,my);case 5->files(g);}
        label(g,project.bpm+" BPM",x+(w<400?170:198),y+h-34,0xff353535);label(g,waiting?"Saving…":dirty?"Unsaved edits":notice.isEmpty()?"Saved in workstation":font.plainSubstrByWidth(notice,w-20),x+8,y+h-16,0xff353535);super.render(g,mx,my,delta);
        if(tab==2 && !hasAltDown() && mx>=gridX() && mx<gridX()+gridW() && my>=gridY() && my<gridY()+112){int lane=(my-gridY())/14,bar=songPage*16+(mx-gridX())/stepW();if(project.playlist[lane][bar]>=0 && !project.fullPattern(lane,bar))g.renderTooltip(font,Component.literal("Saved channel clip: click to use the full pattern"),mx,my);}
        if(hasAltDown() && tab<3 && mx>=gridX() && mx<gridX()+gridW() && my>=gridY() && my<gridY()+112){String hint=tab==0?"Left: toggle · Right: erase · Select a channel for Piano":tab==1?"Draw: note length · Drag: move · Shift-drag: resize · Right: erase":"Left: full pattern clip · Right: erase · Shift: cue bar";g.renderTooltip(font,Component.literal(hint),mx,my);}
    }
    private void patterns(GuiGraphics g,int mx,int my){int gy=gridY(),gx=gridX(),sw=stepW();for(int t=0;t<8;t++){int py=gy+t*14;var channel=project.tracks[t];g.fill(x+8,py,x+86,py+13,t==track?0xffa0a0a0:0xffb7b7b7);label(g,font.plainSubstrByWidth(channel.name,72),x+11,py+3,0xff303030);
            for(int s=0;s<16;s++){boolean active=false;for(var n:channel.notes(pattern))if(n.step()==s){active=true;break;}g.fill(gx+s*sw,py,gx+(s+1)*sw-2,py+12,active?COLORS[t]:s%4==0?0xff757575:0xff949494);if(active)g.fill(gx+s*sw+2,py+2,gx+(s+1)*sw-4,py+4,0xffeeeecc);}}
        playhead(g,gy,112,sw,false);}
    private boolean black(int pitch){int k=pitch%12;return k==1 || k==3 || k==6 || k==8 || k==10;}
    private static String noteName(int pitch){return new String[]{"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"}[pitch%12]+(pitch/12-1);}
    private void piano(GuiGraphics g,int mx,int my){int gy=gridY(),gx=gridX(),sw=stepW();g.enableScissor(x+8,gy,x+w-8,gy+108);
        for(int r=0;r<12;r++){int pitch=octave+11-r,py=gy+r*9;g.fill(x+8,py,x+86,py+8,black(pitch)?0xff545454:0xffededdf);label(g,noteName(pitch),x+12,py,black(pitch)?0xffeeeeee:0xff303030);for(int s=0;s<16;s++)g.fill(gx+s*sw,py,gx+(s+1)*sw-1,py+8,black(pitch)?(s%4==0?0xff444444:0xff535353):(s%4==0?0xff737373:0xff858585));}
        for(var n:project.tracks[track].notes(pattern)){int row=octave+11-n.pitch();if(row<0 || row>=12)continue;int px=gx+n.step()*sw,py=gy+row*9,nw=n.length()*sw-2;g.fill(px+1,py+1,px+nw,py+8,COLORS[track]);g.fill(px+2,py+2,px+Math.max(3,(int)(nw*n.velocity()/127.0)),py+3,0xffeeeed4);}
        playhead(g,gy,108,sw,false);g.disableScissor();}
    private void song(GuiGraphics g,int mx,int my){int gy=gridY(),gx=gridX(),sw=stepW();
        for(int cell=0;cell<16;cell++)g.drawCenteredString(font,Integer.toString(songPage*16+cell+1),gx+cell*sw+sw/2,gy-9,0xff404040);
        for(int lane=0;lane<MusicProject.LANES;lane++){int py=gy+lane*14;
            g.fill(x+8,py,x+86,py+13,lane==songLane?0xffa0a0a0:0xffb7b7b7);label(g,"Lane "+(lane+1),x+11,py+3,0xff303030);
            for(int cell=0;cell<16;cell++){int bar=songPage*16+cell,pat=project.playlist[lane][bar];
                g.fill(gx+cell*sw,py,gx+(cell+1)*sw-2,py+12,bar>=project.bars?0xffaaaaaa:pat<0?0xff8a8a8a:COLORS[pat]);
                if(pat>=0){label(g,String.valueOf((char)('A'+pat)),gx+cell*sw+3,py+2,0xff242424);if(!project.fullPattern(lane,bar))g.fill(gx+(cell+1)*sw-5,py+2,gx+(cell+1)*sw-3,py+4,0xfff3edc4);}
            }
        }playhead(g,gy,112,sw,true);
    }
    private void playhead(GuiGraphics g,int gy,int gh,int sw,boolean arrangement){var b=block();if(b==null || !b.playing[deck])return;double position=b.positionFrames(deck,minecraft.level.getGameTime())/(double)b.projects[deck].barFrames();int px=gridX()+(int)((arrangement?position-songPage*16:position%1*16)*sw);if(px>=gridX() && px<gridX()+gridW())g.fill(px,gy,px+1,gy+gh,0xffece8b1);}
    private void mixer(GuiGraphics g,int mx,int my){int cw=(w-16)/8;for(int i=0;i<8;i++){var t=project.tracks[i];int px=x+8+i*cw;label(g,font.plainSubstrByWidth(t.name,cw-2),px,y+113,0xff303030);g.fill(px+4,y+126,px+cw-6,y+144,0xff656565);g.fill(px+5,y+127,px+5+(cw-12)*t.volume/100,y+143,COLORS[i]);label(g,Integer.toString(t.volume),px+cw/2-8,y+131,0xff202020);} }
    private void decks(GuiGraphics g,int mx,int my){var b=block();if(b==null)return;int half=(w-24)/2;for(int d=0;d<2;d++){int px=x+8+d*(half+8);g.fill(px,y+55,px+half,y+104,0xff5f5f5f);label(g,"Deck "+(d==0?"A":"B")+" · "+b.projects[d].bpm+" BPM",px+5,y+59,0xffe4e4d5);label(g,font.plainSubstrByWidth(b.projects[d].title,half-10),px+5,y+72,0xffe4e4d5);
            double beat=b.positionFrames(d,minecraft.level.getGameTime())/(double)b.projects[d].barFrames();label(g,(b.playing[d]?"Playing":"Ready")+" · bar "+(1+(int)beat),px+5,y+86,0xffe4e4d5);for(int i=0;i<16;i++)g.fill(px+6+i*(half-12)/16,y+98,px+6+(i+1)*(half-12)/16-2,y+101,((int)(beat%1*16)==i && b.playing[d])?0xffeeeeaa:0xff949494);}
        int start=x+44,end=x+w-44;g.fill(start,y+171,end,y+175,0xff4f4f4f);int knob=start+(end-start)*b.crossfade/100;g.fill(knob-3,y+164,knob+4,y+182,0xffeeeecc);label(g,"Equal-power crossfader",x+w/2-65,y+184,0xff303030);}
    private void files(GuiGraphics g){label(g,font.plainSubstrByWidth("Projects: .hobbytrack   Audio: stereo WAV",w-24),x+12,y+146,0xff353535);label(g,font.plainSubstrByWidth("Minecraft folder / hobbymod/music",w-24),x+12,y+158,0xff353535);label(g,font.plainSubstrByWidth("Demo / New need two clicks. Import opens a file list.",w-24),x+12,y+174,0xff353535);}
    @Override public boolean mouseClicked(double mx,double my,int button){if(project==null || closing)return false;if(super.mouseClicked(mx,my,button))return true;
        int gx=gridX(),gy=gridY(),sw=stepW();if(tab==0 && my>=gy && my<gy+112){int t=(int)(my-gy)/14;if(mx>=x+8 && mx<x+86){track=t;build();return true;}if(mx>=gx && mx<gx+gridW()){int step=(int)(mx-gx)/sw;var channel=project.tracks[t];var old=channel.notes(pattern).stream().filter(n->n.step()==step).findFirst().orElse(null);if(old!=null){for(var note:new ArrayList<>(channel.notes(pattern)))if(note.step()==step)channel.remove(pattern,note.step(),note.pitch());}else if(button==0){channel.put(pattern,new MusicProject.Note(step,channel.instrument.drum()?60:60,1,velocity));preview(t,60);}dragging=true;dragMode=5;stepPaint=old==null && button==0;return true;}}
        if(tab==1 && my>=gy && my<gy+108){int pitch=octave+11-(int)(my-gy)/9;if(mx>=x+8 && mx<x+86){preview(track,pitch);return true;}if(mx>=gx && mx<gx+gridW()){int step=(int)(mx-gx)/sw;var channel=project.tracks[track];var old=channel.notes(pattern).stream().filter(n->n.pitch()==pitch && step>=n.step() && step<n.step()+n.length()).findFirst().orElse(null);if(button==1){if(old!=null){channel.remove(pattern,old.step(),old.pitch());changed();}return true;}if(button!=0)return false;dragging=true;dragStart=step;dragLength=old==null?Math.min(length,16-step):old.length();dragged=old;dragMode=old==null?0:hasShiftDown()?2:1;originalStep=old==null?step:old.step();originalPitch=pitch;if(old==null){dragged=new MusicProject.Note(step,pitch,Math.min(length,16-step),velocity);channel.put(pattern,dragged);}preview(track,pitch);return true;}}
        if(tab==2 && mx>=x+8 && mx<x+86 && my>=gy && my<gy+112 && button==0){songLane=(int)(my-gy)/14;build();return true;}
        if(tab==2 && mx>=gx && mx<gx+gridW() && my>=gy && my<gy+112){int bar=songPage*16+(int)(mx-gx)/sw,t=(int)(my-gy)/14;if(hasShiftDown()){command(DjNetworking.CUE,Math.min(bar,project.bars-1));return true;}if(button!=0 && button!=1)return false;songLane=t;project.placeClip(t,bar,button==1?-1:pattern);build();project.bars=Math.max(project.bars,bar+1);dragging=true;dragMode=6;stepPaint=button!=1;return true;}
        if(tab==3 && my>=y+126 && my<y+145 && mx>=x+8 && mx<x+w-8){int cw=(w-16)/8;track=Math.min(7,(int)(mx-x-8)/cw);project.tracks[track].volume=Math.clamp((int)((mx-(x+8+track*cw+5))*100/(cw-12)),0,100);dragging=true;dragMode=3;return true;}
        if(tab==4 && mx>=x+44 && mx<x+w-44 && my>=y+160 && my<y+185){pendingCross=Math.clamp((int)((mx-x-44)*100/(w-88)),0,100);var b=block();if(b!=null)b.crossfade=pendingCross;dragging=true;dragMode=7;return true;}return false;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(!dragging)return super.mouseDragged(mx,my,button,dx,dy);if(dragMode==7){pendingCross=Math.clamp((int)((mx-x-44)*100/(w-88)),0,100);var b=block();if(b!=null)b.crossfade=pendingCross;return true;}if(dragMode==5 || dragMode==6){int step=Math.clamp((int)Math.floor((mx-gridX())/stepW()),0,15),t=Math.clamp((int)Math.floor((my-gridY())/14),0,7);var channel=project.tracks[t];if(dragMode==6){int bar=songPage*16+step;songLane=t;project.placeClip(t,bar,stepPaint?pattern:-1);project.bars=Math.max(project.bars,bar+1);}else if(stepPaint){if(channel.notes(pattern).stream().noneMatch(n->n.step()==step))channel.put(pattern,new MusicProject.Note(step,60,1,velocity));}else{for(var note:new ArrayList<>(channel.notes(pattern)))if(note.step()==step)channel.remove(pattern,note.step(),note.pitch());}return true;}if(dragMode==3){int cw=(w-16)/8;project.tracks[track].volume=Math.clamp((int)((mx-(x+8+track*cw+5))*100/(cw-12)),0,100);return true;}
        int step=Math.clamp((int)Math.floor((mx-gridX())/stepW()),0,15),pitch=Math.clamp(octave+11-(int)Math.floor((my-gridY())/9),octave,octave+11);var channel=project.tracks[track];channel.remove(pattern,dragged.step(),dragged.pitch());
        if(dragMode==1){int start=Math.clamp(originalStep+step-dragStart,0,15);dragged=new MusicProject.Note(start,pitch,Math.min(dragLength,16-start),dragged.velocity());}else{int start=originalStep;dragged=new MusicProject.Note(start,originalPitch,Math.clamp(step-start+1,1,16-start),dragged.velocity());}channel.put(pattern,dragged);return true;
    }
    @Override public boolean mouseReleased(double mx,double my,int button){if(dragging){dragging=false;if(dragMode==7)command(DjNetworking.CROSS,pendingCross);else changed();return true;}return super.mouseReleased(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double sx,double sy){if(tab==3 && mx>=x+8 && mx<x+w-8){int cw=(w-16)/8,index=Math.min(7,(int)(mx-x-8)/cw),sign=sy>0?1:-1;var t=project.tracks[index];if(my>=y+55 && my<y+71)t.cutoff=Math.clamp(t.cutoff+sign,0,100);else if(my>=y+74 && my<y+90)t.delay=Math.clamp(t.delay+sign,0,100);else if(my>=y+93 && my<y+109)t.reverb=Math.clamp(t.reverb+sign,0,100);else if(my>=y+126 && my<y+145)t.volume=Math.clamp(t.volume+sign,0,100);else if(my>=y+166 && my<y+182)t.pan=Math.clamp(t.pan+sign*5,-100,100);else return super.mouseScrolled(mx,my,sx,sy);changed();build();return true;}if(tab==1 && my>=gridY() && my<gridY()+108){if(mx>=gridX() && mx<gridX()+gridW()){int step=Math.clamp((int)(mx-gridX())/stepW(),0,15),pitch=octave+11-(int)(my-gridY())/9;var t=project.tracks[track];var note=t.notes(pattern).stream().filter(n->n.pitch()==pitch && step>=n.step() && step<n.step()+n.length()).findFirst().orElse(null);if(note!=null){t.put(pattern,new MusicProject.Note(note.step(),note.pitch(),note.length(),Math.clamp(note.velocity()+(sy>0?8:-8),1,127)));changed();return true;}}octave=Math.clamp(octave+(sy>0?1:-1),36,73);return true;}if(my>=y+h-40 && mx>=x+174 && mx<x+264){project.bpm=Math.clamp(project.bpm+(sy>0?1:-1),60,200);changed();build();return true;}return super.mouseScrolled(mx,my,sx,sy);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(title!=null && title.isFocused())return super.keyPressed(key,scan,modifiers);if(key==32){var b=block();command(b!=null && b.playing[deck]?DjNetworking.STOP:DjNetworking.PLAY,0);return true;}if(hasControlDown() && key==90){history(hasShiftDown());return true;}if(hasControlDown() && key==89){history(true);return true;}if(hasControlDown() && key==83){flush();notice="Saving project";return true;}return super.keyPressed(key,scan,modifiers);}
    private void history(boolean forward){var from=forward?redo:undo;var to=forward?undo:redo;if(from.isEmpty())return;try{to.addLast(project.encode());project=MusicProject.decode(from.removeLast());baseline=project.encode();dirty=true;dirtyAt=System.currentTimeMillis();build();}catch(java.io.IOException e){notice="Cannot restore edit.";}}
    private void monitor(int selected){if(monitor!=null){monitor.close();monitor=null;if(monitorDeck==selected){monitorDeck=-1;build();return;}}if(minecraft.player==null || !minecraft.player.getInventory().contains(new net.minecraft.world.item.ItemStack(DjContent.HEADPHONES.get())) && !minecraft.player.getOffhandItem().is(DjContent.HEADPHONES.get())){monitorDeck=-1;notice="Carry studio headphones to privately monitor a deck.";return;}var b=block();if(b==null || DjClient.backend==null)return;monitorDeck=selected;monitor=DjClient.backend.play(b.projects[selected],b.pattern[selected],true,b.positionFrames(selected,minecraft.level.getGameTime()));monitor.gain(.65F);build();}
    public void selectInstrument(MusicProject.Instrument instrument){var t=project.tracks[track];t.instrument=instrument;t.name=instrument.label();changed();build();preview(track,60);}
    private void preview(int channel,int pitch){if(DjClient.backend==null)return;if(audition!=null)audition.close();var p=new MusicProject();p.bpm=project.bpm;p.master=60;var t=p.tracks[channel];var source=project.tracks[channel];t.instrument=source.instrument;t.volume=source.volume;t.cutoff=source.cutoff;t.pan=source.pan;t.reverb=source.reverb;t.put(0,new MusicProject.Note(0,pitch,1,velocity));audition=DjClient.backend.play(p,0,false,0);auditionAt=System.currentTimeMillis();}
    private Path directory()throws java.io.IOException{Path p=minecraft.gameDirectory.toPath().resolve("hobbymod/music");Files.createDirectories(p);return p;}
    private String basename(){String name=project.title.replaceAll("[^a-zA-Z0-9_-]","_");return (name.isBlank()?"Untitled":name)+"-"+project.id.toString().substring(0,8);}
    private void exportProject(){try{Path p=directory().resolve(basename()+".hobbytrack");Files.write(p,project.encode());notice="Exported "+p.getFileName();}catch(Exception e){notice="Export failed: "+e.getMessage();}}
    private void importProject(){if(waiting || dirty){flush();notice="Save current edits before importing.";return;}try{var dir=directory();try(var paths=Files.list(dir)){var files=paths.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS) && p.toString().endsWith(".hobbytrack")).sorted().limit(32).toList();minecraft.setScreen(new DjFilesScreen(this,files));}}catch(Exception e){notice="Import failed: "+e.getMessage();}}
    public void imported(Path file){try{if(Files.size(file)>MusicProject.MAX_BYTES)throw new java.io.IOException("Project exceeds size limit");try(var input=Files.newInputStream(file)){project=MusicProject.decode(input.readNBytes(MusicProject.MAX_BYTES+1));}changed();notice="Imported "+file.getFileName();build();flush();}catch(Exception e){notice="Import rejected: "+e.getMessage();}}
    private void exportWav(){if(wavBusy){notice="WAV rendering is already running.";return;}try{wavBusy=true;var copy=project.copy();var file=directory().resolve(basename()+".wav");notice="Rendering WAV…";CompletableFuture.runAsync(()->{try(var out=Files.newOutputStream(file)){MusicSynth.wav(copy,out);}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}}).whenComplete((v,error)->minecraft.execute(()->{wavBusy=false;notice=error==null?"Exported "+file.getFileName():"WAV export failed.";}));}catch(Exception e){wavBusy=false;notice="Export failed: "+e.getMessage();}}
    @Override public void onClose(){closing=true;dragging=false;flush();if(!waiting && !dirty && commands.isEmpty())finishClose();}
    private void finishClose(){if(monitor!=null){monitor.close();monitor=null;}if(audition!=null)audition.close();NetworkManager.sendToServer(new DjNetworking.Action(pos,revision,DjNetworking.RELEASE,deck,0,new byte[0]));minecraft.setScreen(null);}
}
