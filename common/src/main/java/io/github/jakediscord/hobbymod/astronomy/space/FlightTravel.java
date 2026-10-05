package io.github.jakediscord.hobbymod.astronomy.space;

import io.github.jakediscord.hobbymod.astronomy.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** Shared arrival boundary: flight reaches a gate before any dimension change. Never generates routes from clients. */
public final class FlightTravel {
    public static ServerLevel level(ServerPlayer p,String name){return p.server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(name)));}
    public static Vec3 point(int target){var p=SolarMap.position(target);return new Vec3(p.x(),p.y(),p.z());}
    public static boolean advance(PrototypeShip ship,ServerPlayer pilot,int vertical){
        if(!ship.readyToTransfer())return false;
        String dimension=ship.level().dimension().location().toString();
        if(dimension.equals(SolarMap.TRANSFER)){
            int target=ship.destination();var center=point(target);double distance=ship.position().distanceTo(center);
            if(target!=-1 && !ObservationAccess.discovered(AstronomyData.get(pilot.server).journal(pilot.getUUID()),target))return false;
            if(FlightDynamics.canArrive(distance,ship.getDeltaMovement().length(),vertical)){
                var destination=level(pilot,SolarMap.arrival(target));if(destination==null)return false;
                Vec3 landing=target==-1?homePosition(ship,pilot):arrivalPosition(destination,pilot.getUUID());
                return transfer(ship,pilot,destination,landing);
            }
            // Soft collision shell prevents flying through the visible planetary body.
            if(distance<78){var outward=ship.position().subtract(center);if(outward.lengthSqr()<.001)outward=new Vec3(1,0,0);ship.setPos(center.add(outward.normalize().scale(80)));ship.setDeltaMovement(ship.getDeltaMovement().scale(.3));}
        }else if((ship.level().dimension()==Level.OVERWORLD || SolarMap.habitat(dimension)) && ship.getY()>ship.level().getMaxBuildHeight()+12){
            var space=level(pilot,SolarMap.TRANSFER);if(space==null)return false;
            int body=SolarMap.body(dimension);var center=point(body);var facing=SkyCatalog.aim(pilot.getYRot(),0);
            return transfer(ship,pilot,space,center.add(facing.x()*160,0,facing.z()*160));
        }
        return false;
    }
    public static Vec3 arrivalPosition(ServerLevel level,java.util.UUID owner){
        int x=Math.floorMod(owner.hashCode(),16)*48,z=Math.floorMod(owner.hashCode()>>>8,16)*48;
        if(SolarMap.voidSpace(level.dimension().location().toString())){
            for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++){var pos=new BlockPos(x+dx,96,z+dz);if(level.isEmptyBlock(pos))level.setBlockAndUpdate(pos,Math.abs(dx)==5 || Math.abs(dz)==5?Blocks.GRAY_CONCRETE.defaultBlockState():Blocks.WHITE_CONCRETE.defaultBlockState());}
            var beacon=new BlockPos(x+4,97,z+4);if(level.isEmptyBlock(beacon))level.setBlockAndUpdate(beacon,Blocks.SEA_LANTERN.defaultBlockState());
            return new Vec3(x+.5,safeHeight(level,x,z)+4,z+.5);
        }
        return new Vec3(x+.5,Math.max(safeHeight(level,x,z)+8,48),z+.5);
    }
    private static int safeHeight(ServerLevel level,int x,int z){
        for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2})level.getChunkAt(new BlockPos(x+dx,0,z+dz));
        int height=level.getMinBuildHeight();for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)height=Math.max(height,level.getHeight(Heightmap.Types.MOTION_BLOCKING,x+dx,z+dz));return height;
    }
    public static Vec3 homePosition(PrototypeShip ship,ServerPlayer p){
        var home=ship.home()==null?p.server.overworld().getSharedSpawnPos():ship.home();var earth=p.server.overworld();int y=safeHeight(earth,home.getX(),home.getZ());
        return new Vec3(home.getX()+.5,Math.max(y+4,home.getY()+2),home.getZ()+.5);
    }
    private static boolean transfer(PrototypeShip ship,ServerPlayer pilot,ServerLevel destination,Vec3 position){
        if(!destination.getWorldBorder().isWithinBounds(BlockPos.containing(position)))return false;
        destination.getChunkAt(BlockPos.containing(position));
        boolean clear=false;for(int attempt=0;attempt<16;attempt++){var box=net.minecraft.world.phys.AABB.ofSize(position.add(0,ship.getBbHeight()/2,0),ship.getBbWidth(),ship.getBbHeight(),ship.getBbWidth());if(destination.noCollision(ship,box)){clear=true;break;}position=position.add(0,3,0);}if(!clear)return false;
        pilot.stopRiding();var moved=ship.changeDimension(new DimensionTransition(destination,position,Vec3.ZERO,pilot.getYRot(),0,DimensionTransition.DO_NOTHING));
        if(!(moved instanceof PrototypeShip replacement)){pilot.startRiding(ship,true);return false;}
        replacement.setDeltaMovement(Vec3.ZERO);replacement.cooldown(destination.getGameTime());
        pilot.changeDimension(new DimensionTransition(destination,position.add(0,1,0),Vec3.ZERO,pilot.getYRot(),0,DimensionTransition.DO_NOTHING));pilot.fallDistance=0;
        // Riding immediately can race the client entity spawn during dimension loading.
        FlightNetworking.remount(pilot,replacement);
        pilot.displayClientMessage(net.minecraft.network.chat.Component.literal(destination.dimension().location().toString().equals(SolarMap.TRANSFER)?"Transfer space · N navigation":"Arrived · C descend · Shift dismount when landed"),false);
        return true;
    }
    public static void recover(PrototypeShip ship,ServerPlayer pilot){transfer(ship,pilot,pilot.server.overworld(),homePosition(ship,pilot));}
    private FlightTravel(){}
}
