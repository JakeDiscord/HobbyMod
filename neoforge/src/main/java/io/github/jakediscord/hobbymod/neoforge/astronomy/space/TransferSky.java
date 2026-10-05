package io.github.jakediscord.hobbymod.neoforge.astronomy.space;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.github.jakediscord.hobbymod.astronomy.*;
import io.github.jakediscord.hobbymod.astronomy.space.*;
import io.github.jakediscord.hobbymod.neoforge.astronomy.AstronomySky;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="hobbymod",value=Dist.CLIENT)
public final class TransferSky {
    private static final SkyCatalog CATALOG=new SkyCatalog(0);
    @SubscribeEvent public static void effects(RegisterDimensionSpecialEffectsEvent e){e.register(ResourceLocation.parse("hobbymod:space"),new DimensionSpecialEffects(Float.NaN,false,DimensionSpecialEffects.SkyType.NONE,true,true){
        @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float brightness){return Vec3.ZERO;}
        @Override public boolean isFoggyAt(int x,int z){return false;}
    });}
    private static void vertex(BufferBuilder b,org.joml.Matrix4f m,SkyCatalog.Vector d,int c){b.addVertex(m,(float)(d.x()*100),(float)(d.y()*100),(float)(d.z()*100)).setColor(c);}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(mc.level==null || e.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY || !SolarMap.voidSpace(mc.level.dimension().location().toString()))return;
        String dim=mc.level.dimension().location().toString();boolean transfer=dim.equals(SolarMap.TRANSFER);double time=mc.level.getGameTime();var m=e.getModelViewMatrix();var camera=e.getCamera().getPosition();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(var star:CATALOG.stars)if(star.kind()==SkyCatalog.Kind.STAR){var d=star.direction(18000);var r=d.cross(new SkyCatalog.Vector(0,1,0)).unit().scale(.0008);var u=r.unit().cross(d).scale(.0008);
            for(int[] c:new int[][]{{-1,-1},{-1,1},{1,1},{1,-1}})vertex(b,m,new SkyCatalog.Vector(d.x()+r.x()*c[0]+u.x()*c[1],d.y()+r.y()*c[0]+u.y()*c[1],d.z()+r.z()*c[0]+u.z()*c[1]),0xFFB6C8DD);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        record Body(int target,Vec3 relative,double radius){}
        var bodies=new java.util.ArrayList<Body>();
        for(var planet:CelestialBodies.PLANETS)if(transfer || SolarMap.body(dim)==planet.target())bodies.add(new Body(planet.target(),transfer?FlightTravel.point(planet.target()).subtract(camera):new Vec3(240,80,280),transfer?32:140));
        if(transfer)bodies.add(new Body(-1,FlightTravel.point(-1).subtract(camera),32));
        // Sky geometry sits on a normalized sphere: paint distant bodies first for correct mutual occlusion.
        bodies.sort(java.util.Comparator.comparingDouble((Body body)->body.relative().lengthSqr()).reversed());
        for(var body:bodies){
            RenderSystem.setShaderTexture(0,ResourceLocation.parse(body.target()==-1?"hobbymod:textures/environment/earth_cube.png":"hobbymod:textures/environment/planet_cubes.png"));
            b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            var relative=body.relative();double distance=relative.length();if(distance<.001)relative=new Vec3(0,0,1);var d=new SkyCatalog.Vector(relative.x,relative.y,relative.z).unit();
            var planet=body.target()==-1?new SkyCatalog.Object(22,"Earth",SkyCatalog.Kind.PLANET,0,0,-1,0x4383C4,0,0,1):CATALOG.target(body.target());
            AstronomySky.cube(b,m,planet,d,body.radius()/Math.max(1,distance),255,time);BufferUploader.drawWithShader(b.buildOrThrow());
        }
        RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();
    }
}
