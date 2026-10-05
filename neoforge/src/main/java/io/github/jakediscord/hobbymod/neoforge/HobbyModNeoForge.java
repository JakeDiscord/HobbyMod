package io.github.jakediscord.hobbymod.neoforge;

import io.github.jakediscord.hobbymod.HobbyMod;
import net.neoforged.fml.common.Mod;

@Mod(HobbyMod.MOD_ID)
public final class HobbyModNeoForge {
    public HobbyModNeoForge() {
        HobbyMod.init();
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))io.github.jakediscord.hobbymod.neoforge.astronomy.space.SableNavigation.install();
    }
}
