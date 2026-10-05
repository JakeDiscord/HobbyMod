package io.github.jakediscord.hobbymod.neoforge.astronomy.space;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.jakediscord.hobbymod.astronomy.CelestialBodies;
import io.github.jakediscord.hobbymod.astronomy.space.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid="hobbymod")
public final class NavigationCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("astronomy").requires(s->s.hasPermission(2)).then(Commands.literal("navigation")
            .then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();var ship=SpaceflightNavigation.aboard(p);c.getSource().sendSuccess(()->Component.literal("Navigation backend: "+(p.getVehicle() instanceof PrototypeShip?"prototype":SpaceflightNavigation.backend())+"; ship: "+ship.map(s->s.ship().toString()).orElse("none")),false);return 1;}))
            .then(Commands.literal("chart").then(Commands.argument("planet",StringArgumentType.word()).suggests((c,b)->{CelestialBodies.PLANETS.forEach(p->b.suggest(p.key()));return b.buildFuture();}).executes(c->{try{var route=SpaceflightNavigation.chart(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"planet"));c.getSource().sendSuccess(()->Component.literal("Charted "+route.planet()+" for "+route.departure().ship()+"; arrival "+route.arrivalDimension()+". Launch and landing are not implemented."),false);return 1;}catch(IllegalArgumentException e){c.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}})))
            .then(Commands.literal("cancel").executes(c->{var p=c.getSource().getPlayerOrException();var ship=SpaceflightNavigation.aboard(p);return ship.isPresent() && FlightPlanData.get(p.server).cancel(ship.get().ship(),p.getUUID())?1:0;}))));
    }
}
