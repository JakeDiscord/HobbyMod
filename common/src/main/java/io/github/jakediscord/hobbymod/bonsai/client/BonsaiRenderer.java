package io.github.jakediscord.hobbymod.bonsai.client;

import com.mojang.blaze3d.vertex.*;
import io.github.jakediscord.hobbymod.bonsai.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Small tapered branch cylinders and crossed leaf planes; no extra entities. */
public final class BonsaiRenderer implements BlockEntityRenderer<BonsaiBlockEntity> {
    public BonsaiRenderer(BlockEntityRendererProvider.Context context){}
    private static TextureAtlasSprite sprite(String name){return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/"+name));}
    @Override public void render(BonsaiBlockEntity tree,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        BonsaiGraph graph=tree.graph;if(!graph.planted())return;
        boolean distant=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(Vec3.atCenterOf(tree.getBlockPos()))>24*24;
        VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite bark=sprite(switch(graph.species){case OAK->"oak_log";case BIRCH->"birch_log";case CHERRY->"cherry_log";});
        TextureAtlasSprite leaves=sprite(switch(graph.species){case OAK->"oak_leaves";case BIRCH->"birch_leaves";case CHERRY->"cherry_leaves";});
        var nodes=graph.nodes();
        java.util.Set<Integer> parents=new java.util.HashSet<>();
        for(BonsaiGraph.Node node:nodes)parents.add(node.parent);
        for(BonsaiGraph.Node n:nodes){
            BonsaiGraph.Point a=graph.start(n),b=graph.end(n);
            Vec3 start=new Vec3(a.x(),a.y(),a.z()),end=new Vec3(b.x(),b.y(),b.z()),axis=end.subtract(start).normalize();
            Vec3 u=axis.cross(Math.abs(axis.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),v=axis.cross(u).normalize();
            int sides=distant?4:6;
            for(int i=0;i<sides;i++){
                double t=i*Math.PI*2/sides,t2=(i+1)*Math.PI*2/sides;
                Vec3 r=u.scale(Math.cos(t)).add(v.scale(Math.sin(t))),r2=u.scale(Math.cos(t2)).add(v.scale(Math.sin(t2)));
                quad(out,poses.last(),bark,start.add(r.scale(n.radius)),start.add(r2.scale(n.radius)),end.add(r2.scale(n.radius*.65)),end.add(r.scale(n.radius*.65)),0xFFFFFF,light,overlay);
            }
            if(n.wired && !distant){
                TextureAtlasSprite copper=sprite("copper_block");
                for(int i=0;i<3;i++){
                    Vec3 c=start.lerp(end,(i+1)/4.0),side=u.scale(n.radius*1.1),up=axis.scale(.006);
                    quad(out,poses.last(),copper,c.subtract(side).subtract(up),c.add(side).subtract(up),c.add(side).add(up),c.subtract(side).add(up),0xFFFFFF,light,overlay);
                }
            }
            if(n.health>0 && (n.bud || !parents.contains(n.id))){
                double size=distant?.09:.075;
                int color=graph.species==BonsaiGraph.Species.CHERRY?0xFFFFFF:graph.species==BonsaiGraph.Species.BIRCH?0x80A755:0x659846;
                if(n.health<35)color=0xA58A45;
                quad(out,poses.last(),leaves,end.add(-size,-size*.4,0),end.add(size,-size*.4,0),end.add(size,size,0),end.add(-size,size,0),color,light,overlay);
                if(!distant)quad(out,poses.last(),leaves,end.add(0,-size*.4,-size),end.add(0,-size*.4,size),end.add(0,size,size),end.add(0,size,-size),color,light,overlay);
            }
        }
    }
    private static void quad(VertexConsumer out,PoseStack.Pose pose,TextureAtlasSprite sprite,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,int light,int overlay){
        Vec3 normal=b.subtract(a).cross(c.subtract(a)).normalize();
        vertex(out,pose,a,sprite.getU0(),sprite.getV1(),color,normal,light,overlay);
        vertex(out,pose,b,sprite.getU1(),sprite.getV1(),color,normal,light,overlay);
        vertex(out,pose,c,sprite.getU1(),sprite.getV0(),color,normal,light,overlay);
        vertex(out,pose,d,sprite.getU0(),sprite.getV0(),color,normal,light,overlay);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,float u,float v,int color,Vec3 normal,int light,int overlay){
        out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z).setColor((color>>16)&255,(color>>8)&255,color&255,255).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z);
    }
}
