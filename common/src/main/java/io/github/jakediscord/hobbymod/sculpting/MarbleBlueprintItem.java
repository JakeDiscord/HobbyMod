package io.github.jakediscord.hobbymod.sculpting;

import dev.architectury.networking.NetworkManager;
import io.github.jakediscord.hobbymod.registry.HobbyContent;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class MarbleBlueprintItem extends Item {
    public MarbleBlueprintItem(){super(new Properties().stacksTo(1));}
    public static byte[] bytes(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);return data==null?new byte[0]:data.copyTag().getByteArray("marble_blueprint");}
    public static void store(ItemStack stack,MarbleBlueprint design){var tag=new CompoundTag();tag.putByteArray("marble_blueprint",design.compressed());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();var origin=context.getClickedPos();
        if(player==null || !MarbleColumn.marble(level,origin))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!allowed(player,origin,context.getItemInHand(),true))return InteractionResult.FAIL;
        try{
            if(player.isShiftKeyDown() || bytes(context.getItemInHand()).length==0){
                List<MarbleBlueprint.Section> sections=new ArrayList<>();Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(origin);
                while(!queue.isEmpty()){
                    var pos=queue.remove();if(!seen.add(pos))continue;
                    if(seen.size()>MarbleBlueprint.MAX_SECTIONS || Math.abs(pos.getX()-origin.getX())>15 || Math.abs(pos.getY()-origin.getY())>15 || Math.abs(pos.getZ()-origin.getZ())>15)throw new IllegalArgumentException("Structure too large: capture at most 64 connected blocks within 15 blocks of the anchor.");
                    if(!allowed(player,pos,context.getItemInHand(),false))throw new IllegalArgumentException("Cannot read protected marble.");
                    var volume=level.getBlockEntity(pos) instanceof SculptureBlockEntity s?s.volume().copy():new MarbleVolume();
                    sections.add(new MarbleBlueprint.Section(pos.subtract(origin),volume));
                    for(var d:Direction.values()){var next=pos.relative(d);if(!seen.contains(next) && MarbleColumn.marble(level,next))queue.add(next);}
                }
                store(context.getItemInHand(),new MarbleBlueprint(sections));message(player,"Captured "+sections.size()+" blocks. This clicked block is the blueprint anchor.");
            }else apply(player,origin,context.getItemInHand());
        }catch(IllegalArgumentException e){message(player,e.getMessage());return InteractionResult.FAIL;}
        return InteractionResult.CONSUME;
    }
    public static void apply(Player player,BlockPos origin,ItemStack stack){
        MarbleBlueprint blueprint=MarbleBlueprint.decode(bytes(stack));var level=player.level();
        // Validate the entire transaction before changing any block. Existing marble supplies all material.
        for(var section:blueprint.sections()){var pos=origin.offset(section.offset());if(!MarbleColumn.marble(level,pos) || !allowed(player,pos,stack,false))throw new IllegalArgumentException("Place matching marble blocks for every blueprint section before applying.");}
        Map<BlockPos,MarbleVolume> designs=new LinkedHashMap<>();for(var section:blueprint.sections())designs.put(origin.offset(section.offset()),section.volume().copy());
        // Keep each vertical group connected, matching ordinary sculpture support rules.
        Set<BlockPos> processed=new HashSet<>();for(var pos:designs.keySet())if(!processed.contains(pos)){
            var bottom=pos;while(designs.containsKey(bottom.below()))bottom=bottom.below();List<MarbleVolume> column=new ArrayList<>();
            for(var at=bottom;designs.containsKey(at);at=at.above()){processed.add(at);column.add(designs.get(at));}MarbleColumn.prune(column);
        }
        for(var entry:designs.entrySet()){
            var pos=entry.getKey();if(entry.getValue().count()==0){level.removeBlock(pos,false);continue;}
            if(!level.getBlockState(pos).is(HobbyContent.SCULPTURE.get()))level.setBlock(pos,HobbyContent.SCULPTURE.get().defaultBlockState(),3);
            if(level.getBlockEntity(pos) instanceof SculptureBlockEntity s)s.applyBlueprint(entry.getValue());
        }
        message(player,"Applied marble blueprint to "+designs.size()+" blocks.");
    }
    private static boolean allowed(Player p,BlockPos pos,ItemStack stack,boolean reach){return p.isAlive() && !p.isSpectator() && p.level().hasChunkAt(pos) && p.level().mayInteract(p,pos) && p.mayUseItemAt(pos,Direction.UP,stack) && (!reach || p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=36);}
    private static void message(Player player,String text){player.displayClientMessage(Component.literal(text),true);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer server)NetworkManager.sendToPlayer(server,new BlueprintNetworking.OpenFiles(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag flags){
        lines.add(Component.literal(bytes(s).length==0?"Blank · use on marble to capture":"Use on marble to apply · sneak-use to recapture"));
        lines.add(Component.literal("Use in air to import/export a blueprint file"));
    }
}
