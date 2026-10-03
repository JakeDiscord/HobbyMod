package io.github.jakediscord.hobbymod.sculpting.client;

import net.minecraft.client.Camera;
public interface GameRendererAccess {
    double hobby$getFov(Camera camera,float partialTick,boolean useSetting);
}
