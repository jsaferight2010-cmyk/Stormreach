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
        Vec3 out = LockerClient.exitingFrom();
        if (out != null) {   // stepping out: glide from the locker's eye point to the player's own view
            Vec3 now = ((net.minecraft.client.Camera) (Object) this).position();
            Vec3 p = out.lerp(now, LockerClient.transition());
            setPosition(p.x, p.y, p.z);
            return;
        }
        if (!LockerClient.hidden()) return;
        Vec3 eye = LockerClient.eye();
        Vec3 in = LockerClient.enteringFrom();
        if (in != null) eye = in.lerp(eye, LockerClient.transition());   // stepping in: glide into the locker
        float front = LockerClient.facing().toYRot();
        // look around freely within a wide window through the grate (it was a narrow 70 x 35 degree box)
        float yaw = front + Mth.clamp(Mth.wrapDegrees(yRot() - front), -85f, 85f);
        float pitch = Mth.clamp(xRot(), -50f, 50f);
        setRotation(yaw, pitch);
        setPosition(eye.x, eye.y, eye.z);
        detached = false; // always first person from inside the locker (so your own body isn't drawn in front of you)
    }
}
