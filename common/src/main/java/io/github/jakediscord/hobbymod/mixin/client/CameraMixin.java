package io.github.jakediscord.hobbymod.mixin.client;

import io.github.jakediscord.hobbymod.sculpting.client.SculptureOrbit;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="setup",at=@At("TAIL"))
    private void hobby$orbit(BlockGetter level,Entity entity,boolean detached,boolean mirrored,float partialTick,CallbackInfo ci) {
        var pose=SculptureOrbit.cameraPose();if(pose==null)return;
        // Keep the local player's body out of the orbit view when zooming out.
        this.detached=false;
        setRotation(pose.yaw(),pose.pitch());
        setPosition(pose.position().x,pose.position().y,pose.position().z);
    }
}
