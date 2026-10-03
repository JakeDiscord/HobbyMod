package io.github.jakediscord.hobbymod;

import dev.architectury.platform.Platform;
import io.github.jakediscord.hobbymod.registry.HobbyContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared initialization; keep loader-specific classes in their platform module. */
public final class HobbyMod {
    public static final String MOD_ID = "hobbymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private HobbyMod() {}

    public static void init() {
        HobbyContent.register();
        LOGGER.info("HobbyMod initialized on {}", Platform.isNeoForge() ? "NeoForge" : "another loader");
    }
}
