package io.github.jakediscord.hobbymod.mixin.client;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GameRenderer.class)
abstract class AstronomyFovMixin {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void hobby$optics(Camera c,float partial,boolean setting,CallbackInfoReturnable<Double> ci){var scope=io.github.jakediscord.hobbymod.astronomy.client.AstronomyClient.scope();if(scope!=null)ci.setReturnValue(70.0/scope.mag);}
}
