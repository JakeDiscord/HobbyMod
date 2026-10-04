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
    @Inject(method="getProjectionMatrix",at=@At("RETURN"),cancellable=true)
    private void hobby$eyepieceProjection(double fov,CallbackInfoReturnable<org.joml.Matrix4f> ci){
        var mc=net.minecraft.client.Minecraft.getInstance();var scope=io.github.jakediscord.hobbymod.astronomy.client.AstronomyClient.scope();
        if(scope!=null){var matrix=new org.joml.Matrix4f(ci.getReturnValue());matrix.m20(matrix.m20()+138f/mc.getWindow().getGuiScaledWidth());matrix.m21(matrix.m21()-10f/mc.getWindow().getGuiScaledHeight());ci.setReturnValue(matrix);}
    }
}
