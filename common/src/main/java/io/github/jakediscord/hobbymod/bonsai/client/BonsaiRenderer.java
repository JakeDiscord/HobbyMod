package io.github.jakediscord.hobbymod.bonsai.client;

import com.mojang.blaze3d.vertex.*;
import io.github.jakediscord.hobbymod.bonsai.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Tapered bark and irregular leaf volumes, with a fixed texture scale and distant LOD. */
public final class BonsaiRenderer implements BlockEntityRenderer<BonsaiBlockEntity> {
    public BonsaiRenderer(BlockEntityRendererProvider.Context context) {}
    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.withDefaultNamespace("block/"+name));
    }
    @Override public void render(BonsaiBlockEntity tree,float partial,PoseStack poses,
                                 MultiBufferSource buffers,int light,int overlay) {
        BonsaiGraph graph=tree.graph;
        if(!graph.planted())return;
        boolean distant=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
                .distanceToSqr(Vec3.atCenterOf(tree.getBlockPos()))>24*24;
        VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        soil(out,poses.last(),sprite("dirt"),graph.soilColor(),light,overlay);
        TextureAtlasSprite bark=sprite(switch(graph.species) {
            case OAK->"oak_log";case BIRCH->"birch_log";case CHERRY->"cherry_log";
        });
        TextureAtlasSprite leaves=sprite(switch(graph.species) {
            case OAK->"oak_leaves";case BIRCH->"birch_leaves";case CHERRY->"cherry_leaves";
        });
        var nodes=graph.nodes();
        for(BonsaiGraph.Node node:nodes) {
            double growth=graph.growth(node,partial);
            if(growth<=0)continue;
            BonsaiGraph.Point a=graph.start(node,partial),b=graph.end(node,partial);
            Vec3 start=new Vec3(a.x(),a.y(),a.z()),end=new Vec3(b.x(),b.y(),b.z());
            Vec3 axis=end.subtract(start).normalize();
            Vec3 u=axis.cross(Math.abs(axis.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
            Vec3 v=axis.cross(u).normalize();
            double radius=node.radius*Math.pow(growth,.65);
            double baseRadius=radius*(node.id==1?1.35:1);
            int sides=distant?5:8;
            for(int i=0;i<sides;i++) {
                double t=i*Math.PI*2/sides,t2=(i+1)*Math.PI*2/sides;
                Vec3 r=u.scale(Math.cos(t)).add(v.scale(Math.sin(t)));
                Vec3 r2=u.scale(Math.cos(t2)).add(v.scale(Math.sin(t2)));
                quad(out,poses.last(),bark,start.add(r.scale(baseRadius)),start.add(r2.scale(baseRadius)),
                        end.add(r2.scale(radius*.65)),end.add(r.scale(radius*.65)),0xFFFFFF,light,overlay);
            }
            if(node.wired && !distant) {
                TextureAtlasSprite copper=sprite("copper_block");
                for(int i=0;i<3;i++) {
                    Vec3 c=start.lerp(end,(i+1)/4.0),side=u.scale(radius*1.1),up=axis.scale(.006);
                    quad(out,poses.last(),copper,c.subtract(side).subtract(up),c.add(side).subtract(up),
                            c.add(side).add(up),c.subtract(side).add(up),0xFFFFFF,light,overlay);
                }
            }
            double foliage=graph.foliageGrowth(node,partial);
            if(foliage>0) {
                int color=graph.species==BonsaiGraph.Species.CHERRY?0xFFFFFF:
                        graph.species==BonsaiGraph.Species.BIRCH?0x80A755:0x659846;
                if(node.health<35)color=0xA58A45;
                double size=.08*foliage;
                int clusters=distant?1:3;
                for(int cluster=0;cluster<clusters;cluster++) {
                    double angle=node.id*2.399+cluster*2.1;
                    Vec3 center=end.add(Math.cos(angle)*size*.4,cluster*size*.2,Math.sin(angle)*size*.4);
                    foliage(out,poses.last(),leaves,center,size,angle,distant,color,light,overlay);
                }
            }
        }
    }
    private static void soil(VertexConsumer out,PoseStack.Pose pose,TextureAtlasSprite dirt,int color,int light,int overlay) {
        // Match the pot model's 3..13-pixel top face; a tiny offset avoids z-fighting.
        double min=3/16.0,max=13/16.0,y=3/16.0+.0005;
        Vec3 normal=new Vec3(0,1,0);
        vertex(out,pose,new Vec3(min,y,min),dirt.getU((float)min),dirt.getV((float)min),color,normal,light,overlay);
        vertex(out,pose,new Vec3(min,y,max),dirt.getU((float)min),dirt.getV((float)max),color,normal,light,overlay);
        vertex(out,pose,new Vec3(max,y,max),dirt.getU((float)max),dirt.getV((float)max),color,normal,light,overlay);
        vertex(out,pose,new Vec3(max,y,min),dirt.getU((float)max),dirt.getV((float)min),color,normal,light,overlay);
    }
    private static void foliage(VertexConsumer out,PoseStack.Pose pose,TextureAtlasSprite leaves,
                                Vec3 center,double size,double rotation,boolean distant,int color,int light,int overlay) {
        int sides=distant?5:7,rings=distant?2:3;
        for(int ring=0;ring<rings;ring++) {
            double lower=-Math.PI/2+Math.PI*ring/rings,upper=-Math.PI/2+Math.PI*(ring+1)/rings;
            for(int side=0;side<sides;side++) {
                double a=rotation+side*Math.PI*2/sides,b=rotation+(side+1)*Math.PI*2/sides;
                quad(out,pose,leaves,leafPoint(center,size,lower,a),leafPoint(center,size,lower,b),
                        leafPoint(center,size,upper,b),leafPoint(center,size,upper,a),color,light,overlay);
            }
        }
    }
    private static Vec3 leafPoint(Vec3 center,double size,double latitude,double longitude) {
        double irregular=1+.1*Math.sin(longitude*3+latitude*2);
        return center.add(Math.cos(latitude)*Math.cos(longitude)*size*irregular,
                Math.sin(latitude)*size*.65,Math.cos(latitude)*Math.sin(longitude)*size*irregular);
    }
    private static void quad(VertexConsumer out,PoseStack.Pose pose,TextureAtlasSprite sprite,
                             Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,int light,int overlay) {
        // One texture tile per quarter block. Crop the sprite instead of squeezing the
        // entire 16x16 tile onto every narrow face. Match UV widths to both tapered edges.
        float bottom=(float)Math.min(1,a.distanceTo(b)*4),top=(float)Math.min(1,c.distanceTo(d)*4);
        float height=(float)Math.min(1,(a.distanceTo(d)+b.distanceTo(c))*2);
        Vec3 normal=b.subtract(a).cross(c.subtract(a));
        if(normal.lengthSqr()<1e-12)normal=c.subtract(a).cross(d.subtract(a));
        normal=normal.normalize();
        vertex(out,pose,a,sprite.getU(.5F-bottom/2),sprite.getV(height),color,normal,light,overlay);
        vertex(out,pose,b,sprite.getU(.5F+bottom/2),sprite.getV(height),color,normal,light,overlay);
        vertex(out,pose,c,sprite.getU(.5F+top/2),sprite.getV(0),color,normal,light,overlay);
        vertex(out,pose,d,sprite.getU(.5F-top/2),sprite.getV(0),color,normal,light,overlay);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,float u,float v,
                               int color,Vec3 normal,int light,int overlay) {
        out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z)
                .setColor((color>>16)&255,(color>>8)&255,color&255,255).setUv(u,v)
                .setOverlay(overlay).setLight(light)
                .setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
    }
}
