package io.github.jakediscord.hobbymod.gametest;
import io.github.jakediscord.hobbymod.HobbyMod;
import io.github.jakediscord.hobbymod.painting.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("hobbymod") @PrefixGameTestTemplate(false)
public final class PaintingGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static PaintingBlockEntity easel(GameTestHelper h){h.setBlock(POS.below(),Blocks.STONE);h.setBlock(POS,PaintingContent.EASEL.get());return (PaintingBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static Player player(GameTestHelper h){Player p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().mayBuild=true;p.moveTo(Vec3.atCenterOf(h.absolutePos(POS.offset(0,0,-2))));p.getInventory().setItem(0,new ItemStack(PaintingContent.PALETTE.get()));p.getInventory().setItem(1,new ItemStack(PaintingContent.BRUSH.get()));p.getInventory().setItem(2,new ItemStack(Items.RED_DYE,3));return p;}
    private static PaintingNetworking.Action action(PaintingBlockEntity b,int kind,float[] xy){return new PaintingNetworking.Action(b.getBlockPos(),b.painting.id,b.painting.revision,kind,DyeColor.RED.getId(),DyeColor.WHITE.getId(),0,4,100,0,-1,64,"Sunset",xy);}
    private static void pass(GameTestHelper h,String name){HobbyMod.LOGGER.info("PAINTING_TEST_PASS {}",name);h.succeed();}
    @GameTest(template="empty") public static void dyeLoadingRealBrushAndStaleStrokeProtection(GameTestHelper h){var b=easel(h);var p=player(h);b.painting=new PaintingData(PaintingData.Shape.LANDSCAPE,32);
        h.assertTrue(PaintPalette.load(p,DyeColor.RED.getId()) && p.getInventory().getItem(2).getCount()==2,"Loading consumes exactly one dye");var stroke=action(b,0,new float[]{1,10,40,10});h.assertTrue(PaintingNetworking.work(p,b,stroke).isEmpty(),"Real brush must paint");int ink=PaintPalette.amounts(p.getInventory().getItem(0))[DyeColor.RED.getId()];h.assertTrue(ink<512 && b.painting.pixel(20,10)==(DyeColor.RED.getTextureDiffuseColor()&0xFFFFFF),"Painting consumes loaded pigment and writes real pixels");int rev=b.painting.revision;PaintingNetworking.work(p,b,stroke);h.assertTrue(b.painting.revision==rev && PaintPalette.amounts(p.getInventory().getItem(0))[DyeColor.RED.getId()]==ink,"Stale strokes cannot consume supplies or alter artwork");pass(h,"dyeLoadingRealBrushAndStaleStrokeProtection");}
    @GameTest(template="empty") public static void missingSuppliesAndMalformedStrokesPreserveCanvas(GameTestHelper h){var b=easel(h);var p=player(h);b.painting=new PaintingData(PaintingData.Shape.ROUND,32);
        PaintingNetworking.work(p,b,action(b,0,new float[]{16,16}));h.assertTrue(b.painting.revision==0,"Empty palette cannot paint");PaintPalette.load(p,DyeColor.RED.getId());PaintingNetworking.work(p,b,action(b,0,new float[]{Float.NaN,16}));h.assertTrue(b.painting.revision==0 && PaintPalette.amounts(p.getInventory().getItem(0))[DyeColor.RED.getId()]==512,"Malformed strokes preserve canvas and pigment");p.getInventory().setItem(1,ItemStack.EMPTY);PaintingNetworking.work(p,b,action(b,0,new float[]{16,16}));h.assertTrue(b.painting.revision==0,"Paintbrush is required");pass(h,"missingSuppliesAndMalformedStrokesPreserveCanvas");}
    @GameTest(template="empty") public static void resizingSigningTakingAndPersistence(GameTestHelper h){var b=easel(h);var p=player(h);b.painting=new PaintingData(PaintingData.Shape.PORTRAIT,32);b.painting.paint(new float[]{12,12},0x336699,8,100,1);PaintingNetworking.work(p,b,action(b,2,new float[0]));h.assertTrue(b.painting.resolution==64 && b.painting.pixel(24,24)==0x336699,"Resolution changes preserve artwork");PaintingNetworking.work(p,b,action(b,4,new float[0]));var copy=(PaintingBlockEntity)BlockEntity.loadStatic(b.getBlockPos(),b.getBlockState(),b.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.painting.signed && copy.painting.title.equals("Sunset") && copy.painting.pixel(24,24)==0x336699,"Signed raster survives block save/load");PaintingNetworking.work(p,b,action(b,3,new float[0]));var stack=p.getInventory().items.stream().filter(s->s.getItem() instanceof CanvasItem).findFirst().orElse(ItemStack.EMPTY);h.assertTrue(b.getUpdateTag(h.getLevel().registryAccess()).contains("HasPainting") && !b.getUpdateTag(h.getLevel().registryAccess()).getBoolean("HasPainting"),"Empty-easel updates must have a nonempty tag so clients clear the old painting");h.assertTrue(b.painting==null && !stack.isEmpty() && CanvasItem.read(stack).title.equals("Sunset"),"Taking returns one complete canvas and clears the easel");pass(h,"resizingSigningTakingAndPersistence");}
    @GameTest(template="empty") public static void breakingEaselReturnsFrameAndArtwork(GameTestHelper h){var b=easel(h);b.painting=new PaintingData(PaintingData.Shape.SQUARE,128);b.painting.paint(new float[]{50,50},0x00ff00,16,100,0);var drops=Block.getDrops(b.getBlockState(),h.getLevel(),b.getBlockPos(),b);h.assertTrue(drops.size()==2 && drops.stream().anyMatch(s->s.is(PaintingContent.EASEL_ITEM.get())) && drops.stream().filter(s->s.getItem() instanceof CanvasItem).anyMatch(s->CanvasItem.read(s).pixel(50,50)==0x00ff00),"Breaking preserves both easel and painted pixels");pass(h,"breakingEaselReturnsFrameAndArtwork");}
    @GameTest(template="empty") public static void upperEaselRoutesCanvasMountingAndDismantles(GameTestHelper h){h.setBlock(POS.below(),Blocks.STONE);var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PaintingContent.EASEL_ITEM.get()));var floor=h.absolutePos(POS.below());var context=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false)));
        h.assertTrue(((BlockItem)p.getMainHandItem().getItem()).place(context).consumesAction(),"Easel item must place");var upper=h.absolutePos(POS.above());h.assertTrue(h.getLevel().getBlockState(upper).is(PaintingContent.EASEL_TOP.get()),"Tall easel reserves an upper interaction section");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PaintingContent.CANVASES.get(PaintingData.Shape.ROUND).get()));var hit=new BlockHitResult(Vec3.atCenterOf(upper),Direction.NORTH,upper,false);h.getLevel().getBlockState(upper).useItemOn(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,hit);var b=(PaintingBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));h.assertTrue(b.painting!=null && b.painting.shape==PaintingData.Shape.ROUND && p.getMainHandItem().isEmpty(),"Clicking the upper half mounts and consumes one canvas");h.getLevel().destroyBlock(h.absolutePos(POS),false);h.assertTrue(h.getLevel().getBlockState(upper).isAir(),"Removing the easel clears its upper section");pass(h,"upperEaselRoutesCanvasMountingAndDismantles");}

    @GameTest(template="empty",timeoutTicks=160) public static void wallPlacementAndLostSupportPreserveArtwork(GameTestHelper h){
        var p=player(h);var wall=h.absolutePos(POS.offset(0,0,1));
        for(int x=-1;x<=1;x++)for(int y=0;y<2;y++)h.getLevel().setBlock(wall.offset(x,y,0),Blocks.STONE.defaultBlockState(),3);
        var art=new PaintingData(PaintingData.Shape.LANDSCAPE,32);art.title="Gallery";art.author="Artist";art.paint(new float[]{16,16},0xff0000,8,100,0);art.signed=true;
        p.setItemInHand(InteractionHand.MAIN_HAND,CanvasItem.create(art));
        var c=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(wall).add(0,0,-.5),Direction.NORTH,wall,false)));
        h.assertTrue(((CanvasItem)p.getMainHandItem().getItem()).place(c).consumesAction(),"Canvas hangs against a complete wall");
        var entities=h.getLevel().getEntitiesOfClass(PaintedCanvasEntity.class,new AABB(h.absolutePos(POS)).inflate(2));
        h.assertTrue(entities.size()==1 && p.getMainHandItem().isEmpty(),"Placement creates one hanging entity and consumes the canvas");
        var canvas=entities.getFirst();h.assertTrue(canvas.painting().id.equals(art.id) && canvas.painting().signed && canvas.painting().pixel(16,16)==0xff0000,"Hanging painting retains the original signed raster");
        var saved=new net.minecraft.nbt.CompoundTag();canvas.saveWithoutId(saved);var restored=new PaintedCanvasEntity(PaintingContent.HANGING.get(),h.getLevel());restored.load(saved);
        h.assertTrue(restored.painting().id.equals(art.id) && restored.getDirection()==Direction.NORTH && restored.canvasWidth()==3,"Hanging entity save preserves artwork, direction and wall size");
        h.getLevel().destroyBlock(wall,false);
        h.runAfterDelay(110,()->{
            h.assertTrue(canvas.isRemoved(),"Vanilla support checks remove a painting with a missing backing block");
            var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(3));
            h.assertTrue(items.stream().anyMatch(e->e.getItem().getItem() instanceof CanvasItem && CanvasItem.read(e.getItem()).pixel(16,16)==0xff0000),"Lost support drops the original painting");pass(h,"wallPlacementAndLostSupportPreserveArtwork");
        });
    }
}
