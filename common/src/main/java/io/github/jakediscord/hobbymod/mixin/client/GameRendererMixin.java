package io.github.jakediscord.hobbymod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jakediscord.hobbymod.sculpting.client.GameRendererAccess;
import io.github.jakediscord.hobbymod.sculpting.client.SculptureOrbit;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4f;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin implements GameRendererAccess {
    @Invoker("getFov") public abstract double hobby$getFov(Camera camera,float partialTick,boolean useSetting);
    @Inject(method={"bobView","bobHurt"},at=@At("HEAD"),cancellable=true)
    private void hobby$stableCamera(PoseStack poses,float partialTick,CallbackInfo ci) { if(SculptureOrbit.active() || io.github.jakediscord.hobbymod.pottery.client.PotteryOrbit.active() || io.github.jakediscord.hobbymod.aquarium.client.AquariumOrbit.active())ci.cancel(); }
    @Inject(method="shouldRenderBlockOutline",at=@At("HEAD"),cancellable=true)
    private void hobby$surfaceBrush(CallbackInfoReturnable<Boolean> ci) { if(SculptureOrbit.active() || io.github.jakediscord.hobbymod.pottery.client.PotteryOrbit.active() || io.github.jakediscord.hobbymod.aquarium.client.AquariumOrbit.active())ci.setReturnValue(false); }
    @Inject(method="renderItemInHand",at=@At("HEAD"),cancellable=true)
    private void hobby$clearView(Camera camera,float partialTick,Matrix4f projection,CallbackInfo ci) { if(SculptureOrbit.active() || io.github.jakediscord.hobbymod.pottery.client.PotteryOrbit.active() || io.github.jakediscord.hobbymod.aquarium.client.AquariumOrbit.active())ci.cancel(); }
}
