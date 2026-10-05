package io.github.jakediscord.hobbymod.neoforge.astronomy.space;

import dev.ryanhcode.sable.companion.SableCompanion;
import io.github.jakediscord.hobbymod.astronomy.space.*;
import java.util.Optional;

/** Uses Sable's recommended Companion API; loaded only when the real Sable mod is present. */
public final class SableNavigation {
    public static void install(){SpaceflightNavigation.install("sable",player->{
        var helper=SableCompanion.INSTANCE;var ship=helper.getTrackingOrVehicleSubLevel(player);if(ship==null)return Optional.empty();
        var pose=ship.logicalPose();var p=pose.position();var q=pose.orientation();var velocity=helper.getVelocity(player.level(),ship,player.position());
        return Optional.of(new SpacecraftSnapshot(ship.getUniqueId(),player.level().dimension().location().toString(),p.x(),p.y(),p.z(),q.x(),q.y(),q.z(),q.w(),velocity.x,velocity.y,velocity.z));
    });}
    private SableNavigation(){}
}
