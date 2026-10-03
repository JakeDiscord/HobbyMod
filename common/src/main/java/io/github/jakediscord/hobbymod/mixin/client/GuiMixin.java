package io.github.jakediscord.hobbymod.mixin.client;

import io.github.jakediscord.hobbymod.sculpting.client.SculptureOrbit;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiMixin {
    @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
    private void hobby$cursor(GuiGraphics graphics,DeltaTracker delta,CallbackInfo ci) { if(SculptureOrbit.active() || io.github.jakediscord.hobbymod.pottery.client.PotteryOrbit.active())ci.cancel(); }
}
