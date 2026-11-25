package com.stormreach.client.mixin;

import com.stormreach.client.labyrinth.LockerClient;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hiding in a Rusted Locker: the camera sits just behind the grate and looks out through its slats, whatever the
 * perspective setting. You can look around inside a 70° x 35° window around the locker's front.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow public abstract float yRot();
    @Shadow public abstract float xRot();
    @Shadow private boolean detached;

    @Inject(method = "alignWithEntity(F)V", at = @At("TAIL"))
    private void stormreach$lockerView(float partialTick, CallbackInfo ci) {
        if (!LockerClient.hidden()) return;
        Vec3 eye = LockerClient.eye();
        float front = LockerClient.facing().toYRot();
        float yaw = front + Mth.clamp(Mth.wrapDegrees(yRot() - front), -70f, 70f);
        float pitch = Mth.clamp(xRot(), -35f, 35f);
        setRotation(yaw, pitch);
        setPosition(eye.x, eye.y, eye.z);
        detached = false; // always first person from inside the locker (so your own body isn't drawn in front of you)
    }
}
