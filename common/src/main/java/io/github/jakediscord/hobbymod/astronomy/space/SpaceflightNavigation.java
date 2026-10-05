package io.github.jakediscord.hobbymod.astronomy.space;

import io.github.jakediscord.hobbymod.astronomy.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Backend boundary for a future helm. Charting never moves a ship or player. */
public final class SpaceflightNavigation {
    public interface Backend {Optional<SpacecraftSnapshot> aboard(ServerPlayer player);}
    private static Backend backend=player->Optional.empty();
    private static String name="unavailable";
    public static void install(String id,Backend value){name=Objects.requireNonNull(id);backend=Objects.requireNonNull(value);}
    public static String backend(){return name;}
    public static Optional<SpacecraftSnapshot> aboard(ServerPlayer player){return player.isAlive()?backend.aboard(player):Optional.empty();}
    public static FlightRoute chart(ServerPlayer player,String planet){
        var ship=aboard(player).orElseThrow(()->new IllegalArgumentException("Board a Sable structure before charting a route"));
        if(!ship.dimension().equals(player.level().dimension().location().toString()))throw new IllegalArgumentException("Ship is in another dimension");
        var route=FlightRoute.chart(player.getUUID(),ship,planet,player.server.overworld().getDayTime(),AstronomyData.get(player.server).journal(player.getUUID()));FlightPlanData.get(player.server).chart(route);return route;
    }
    private SpaceflightNavigation(){}
}
