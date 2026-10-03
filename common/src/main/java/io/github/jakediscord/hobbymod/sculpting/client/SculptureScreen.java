package io.github.jakediscord.hobbymod.sculpting.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.sculpting.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/** A dedicated orbiting sculpting workspace; clients request strokes, never submit shape data. */
@Environment(EnvType.CLIENT)
public final class SculptureScreen extends Screen {
    private final BlockPos pos;
    private final OrbitCamera camera = new OrbitCamera();
    private final List<Button> tools = new ArrayList<>();
    private Button mirrorButton;
    private CarvingTool selected = CarvingTool.DETAIL;
    private boolean mirror, leftHeld, orbiting;
    private int lastRevision = -1, pendingRevision = -1, pendingTicks, tick, lastStroke = -10;
    private int beforeCount, beforePolish;
    private double mouseX, mouseY;
    private String status = "Choose a tool, then chip the marble.";
    private final List<Chip> chips = new ArrayList<>();
    private record Quad(double[][] points, double depth, int color) {}
    private static final class Chip {
        double x, y, vx, vy; int age;
        Chip(double x, double y, double vx, double vy) { this.x=x; this.y=y; this.vx=vx; this.vy=vy; }
    }

    public SculptureScreen(BlockPos pos) { super(Component.translatable("screen.hobbymod.sculpture")); this.pos = pos.immutable(); }
    private SculptureBlockEntity sculpture() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof SculptureBlockEntity s ? s : null;
    }
    private int sidebar() { return width < 400 ? 104 : 124; }
    private int left() { return sidebar() + 8; }
    private int top() { return 52; }
    private int right() { return width - 12; }
    private int bottom() { return height - 52; }
    private double cx() { return (left() + right()) / 2.0 + camera.panX; }
    private double cy() { return (top() + bottom()) / 2.0 + camera.panY; }
    private double scale() { return Math.max(30, Math.min(right() - left(), bottom() - top()) * 0.62) * camera.zoom; }
    private boolean inView(double x, double y) { return x >= left() && x < right() && y >= top() && y < bottom(); }

    @Override protected void init() {
        tools.clear();
        if (minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof ChiselItem item) selected = item.tool();
        String[] names = {"1  Detail", "2  Point", "3  Mallet", "4  Rasp"};
        for (CarvingTool tool : CarvingTool.values()) {
            tools.add(addRenderableWidget(Button.builder(Component.literal(names[tool.ordinal()]), b -> selected = tool)
                    .bounds(10, 54 + tool.ordinal() * 22, sidebar() - 20, 20).build()));
        }
        mirrorButton = addRenderableWidget(Button.builder(Component.literal("Mirror X: off"), b -> {
            mirror = !mirror; b.setMessage(Component.literal("Mirror X: " + (mirror ? "on" : "off")));
        }).bounds(12, height - 25, 80, 20).build());
        mirrorButton.setMessage(Component.literal("Mirror X: " + (mirror ? "on" : "off")));
        addRenderableWidget(Button.builder(Component.literal("Undo"), b -> undo()).bounds(100, height - 25, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Frame"), b -> camera.frame()).bounds(168, height - 25, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width - 70, height - 25, 58, 20).build());
    }

    @Override public void tick() {
        tick++;
        SculptureBlockEntity s = sculpture();
        if (s == null || minecraft.player == null || minecraft.player.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) > 36) { onClose(); return; }
        if (s.revision() != lastRevision) {
            if (pendingRevision >= 0) {
                int removed = beforeCount - s.volume().count();
                int polished = s.volume().polishedCount() - beforePolish;
                status = removed > 0 ? "Chipped away " + removed + " pieces." : polished > 0 ? "Finished " + polished + " surfaces." : "Previous stroke restored.";
                if (removed > 0) for (int i=0; i<10; i++) chips.add(new Chip(mouseX, mouseY, (Math.random()-0.5)*3, -1-Math.random()*2));
                pendingRevision = -1;
            }
            lastRevision = s.revision();
        }
        if (pendingRevision >= 0 && ++pendingTicks > 20) { pendingRevision = -1; status = "No change. Select exposed marble and an available tool."; }
        for (int i=0; i<tools.size(); i++) {
            tools.get(i).active = !SculptureNetworking.findTool(minecraft.player, i).isEmpty();
        }
        chips.removeIf(c -> ++c.age > 16);
        for (Chip c : chips) { c.x+=c.vx; c.y+=c.vy; c.vy+=0.18; }
        if (leftHeld && !orbiting && !hasAltDown()) stroke(mouseX, mouseY);
    }

    private MarbleVolume.Hit hit(double x, double y) {
        SculptureBlockEntity s = sculpture();
        if (s == null || !inView(x,y)) return null;
        return s.volume().pick(camera.rayOrigin((x-cx())/scale(), (cy()-y)/scale()), camera.rayDirection());
    }
    private void stroke(double x, double y) {
        SculptureBlockEntity s = sculpture();
        MarbleVolume.Hit hit = hit(x,y);
        if (s == null || hit == null || pendingRevision >= 0 || tick-lastStroke < 4) return;
        ItemStack tool = SculptureNetworking.findTool(minecraft.player, selected.ordinal());
        if (tool.isEmpty()) { status = "Put this tool in your inventory to use it."; return; }
        send(s, hit.cell(), false);
    }
    private void undo() {
        SculptureBlockEntity s = sculpture();
        if (s != null && pendingRevision < 0) send(s, 0, true);
    }
    private void send(SculptureBlockEntity s, int cell, boolean undo) {
        pendingRevision = s.revision(); pendingTicks = 0; lastStroke = tick;
        beforeCount = s.volume().count(); beforePolish = s.volume().polishedCount();
        NetworkManager.sendToServer(new SculptureNetworking.Stroke(pos, s.revision(), cell, selected.ordinal(), mirror, undo));
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x,y,button)) return true;
        if (!inView(x,y)) return false;
        mouseX=x; mouseY=y;
        orbiting = button == 1 || button == 2 || (button == 0 && hasAltDown());
        leftHeld = button == 0;
        if (button == 0 && !orbiting) stroke(x,y);
        return true;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        mouseX=x; mouseY=y;
        if (orbiting) {
            if (hasShiftDown()) { camera.panX+=dx; camera.panY+=dy; }
            else camera.orbit(dx,dy);
            return true;
        }
        if (leftHeld) { stroke(x,y); return true; }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        leftHeld=false; orbiting=false;
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (inView(x,y)) { camera.zoom = Math.clamp(camera.zoom * Math.pow(1.12, vertical), 0.45, 3); return true; }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) { selected = CarvingTool.values()[key-GLFW.GLFW_KEY_1]; return true; }
        if (key == GLFW.GLFW_KEY_Z && hasControlDown()) { undo(); return true; }
        if (key == GLFW.GLFW_KEY_F) { camera.frame(); return true; }
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }

    // The workspace draws its own background. Screen.render otherwise blurs
    // everything already drawn, including the editable sculpture and labels.
    @Override public void renderBackground(GuiGraphics g, int x, int y, float partialTick) {}

    @Override public void render(GuiGraphics g, int x, int y, float partialTick) {
        mouseX=x; mouseY=y;
        g.fill(0,0,width,height,0xff171a20);
        g.fill(0,0,width,36,0xff242933);
        g.drawString(font,title,12,8,0xffeedcc0,false);
        g.drawString(font,"CARVING WORKSPACE",12,23,0xff8e98a8,false);
        g.fill(8,42,sidebar()-2,height-50,0xff20252e);
        g.fill(left(),top(),right(),bottom(),0xff11151b);
        SculptureBlockEntity s = sculpture();
        if (s != null) {
            double remaining = s.volume().count() * 100.0 / MarbleVolume.CELLS;
            g.drawString(font, String.format(java.util.Locale.ROOT,"Marble remaining: %.1f%%",remaining),left(),40,0xffaeb8c8,false);
            renderVolume(g,s.volume(),x,y);
            if (height > 210) {
                g.drawString(font,selected.title,12,149,0xffeedcc0,false);
                String[] help = {"Fine detail", "Small chips", "Broad cuts", "Surface finish"};
                g.drawString(font,help[selected.ordinal()],12,161,0xff8e98a8,false);
            }
            for (int i=0; i<tools.size(); i++) if (i==selected.ordinal()) g.fill(6,56+i*22,8,72+i*22,0xffffc776);
            String text = status;
            if (font.width(text) > right()-left()) text = font.plainSubstrByWidth(text,right()-left());
            g.drawString(font,text,left()+4,bottom()-12,0xffb6c1d0,false);
        }
        g.drawString(font,"LMB carve  |  RMB orbit  |  Wheel zoom",12,height-42,0xffaab5c5,false);
        super.render(g,x,y,partialTick);
        for (Chip c : chips) {
            if (inView(c.x,c.y)) g.fill((int)c.x,(int)c.y,(int)c.x+2,(int)c.y+2,0xffe8dfcf);
        }
    }

    private void renderVolume(GuiGraphics g, MarbleVolume volume, int mx, int my) {
        List<Quad> quads = new ArrayList<>();
        for (MarbleVolume.Face face : volume.faces()) {
            double[][] points = face.vertices();
            int axis=face.side()/2;
            double[] normal=new double[3]; normal[axis]=face.side()%2==1 ? 1 : -1;
            double[] transformed=camera.view(normal[0]+0.5,normal[1]+0.5,normal[2]+0.5);
            if (transformed[2] <= 0) continue;
            double depth=0;
            double[][] projected=new double[4][2];
            for (int i=0;i<4;i++) {
                double[] p=camera.view(points[i][0],points[i][1],points[i][2]); depth+=p[2];
                projected[i][0]=cx()+p[0]*scale(); projected[i][1]=cy()-p[1]*scale();
            }
            double light=Math.clamp(0.68+0.3*(normal[0]*-0.4+normal[1]*0.8+normal[2]*0.45),0.38,1);
            // Recessed surfaces need depth shading to keep deep cuts readable.
            double inset=(face.side()%2==1 ? MarbleVolume.SIZE-face.plane() : face.plane())/(double)MarbleVolume.SIZE;
            light *= 1 - inset * 0.45;
            int r=(int)((face.polished()?246:219)*light), green=(int)((face.polished()?240:214)*light), b=(int)((face.polished()?225:202)*light);
            quads.add(new Quad(projected,depth/4,0xff000000|(r<<16)|(green<<8)|b));
        }
        quads.sort(Comparator.comparingDouble(Quad::depth));
        g.flush();
        g.enableScissor(left(),top(),right(),bottom());
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferBuilder buffer=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose=g.pose().last().pose();
        // A studio floor grid provides spatial orientation while orbiting.
        for (int i=-5;i<=5;i++) {
            double n=i/5.0;
            line(buffer,pose,project(-0.5,-0.035,n+0.5),project(1.5,-0.035,n+0.5),0xff343c48,0.5);
            line(buffer,pose,project(n+0.5,-0.035,-0.5),project(n+0.5,-0.035,1.5),0xff343c48,0.5);
        }
        for (Quad quad:quads) drawQuad(buffer,pose,quad.points,quad.color);
        MarbleVolume.Hit hit=hit(mx,my);
        if (hit!=null) {
            drawSelection(buffer,pose,hit.cell(),0xffffc776);
            if (mirror) drawSelection(buffer,pose,MarbleVolume.index(31-MarbleVolume.x(hit.cell()),MarbleVolume.y(hit.cell()),MarbleVolume.z(hit.cell())),0xff77cbd5);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull(); RenderSystem.enableDepthTest();
        g.disableScissor();
    }
    private double[] project(double x,double y,double z) {
        double[] p=camera.view(x,y,z);
        return new double[]{cx()+p[0]*scale(),cy()-p[1]*scale()};
    }
    private void drawSelection(BufferBuilder buffer,Matrix4f pose,int cell,int color) {
        double x=MarbleVolume.x(cell)/32.0, y=MarbleVolume.y(cell)/32.0, z=MarbleVolume.z(cell)/32.0, step=1/32.0;
        double[][] p=new double[8][];
        for (int i=0;i<8;i++) p[i]=project(x+((i&1)!=0?step:0),y+((i&2)!=0?step:0),z+((i&4)!=0?step:0));
        for (int i=0;i<8;i++) for (int bit=1;bit<=4;bit*=2) if ((i&bit)==0) line(buffer,pose,p[i],p[i|bit],color,0.8);
    }
    private static void line(BufferBuilder buffer,Matrix4f pose,double[] a,double[] b,int color,double width) {
        double dx=b[0]-a[0],dy=b[1]-a[1],length=Math.hypot(dx,dy);
        if (length<1e-5) return;
        double nx=-dy/length*width,ny=dx/length*width;
        drawQuad(buffer,pose,new double[][]{{a[0]+nx,a[1]+ny},{b[0]+nx,b[1]+ny},{b[0]-nx,b[1]-ny},{a[0]-nx,a[1]-ny}},color);
    }
    private static void drawQuad(BufferBuilder buffer,Matrix4f pose,double[][] points,int color) {
        for (double[] p:points) buffer.addVertex(pose,(float)p[0],(float)p[1],0).setColor(color);
    }
}
