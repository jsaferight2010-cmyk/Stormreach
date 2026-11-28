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
        // 6.0: the Cyrrus intro camera takes over completely while it plays (third person, so you see yourself)
        if (com.stormreach.client.story.CinematicCamera.active()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            Vec3 eye = mc.player == null ? ((net.minecraft.client.Camera) (Object) this).position() : mc.player.getEyePosition(partialTick);
            double[] c = com.stormreach.client.story.CinematicCamera.pose(partialTick, eye,
                    mc.player == null ? yRot() : mc.player.getViewYRot(partialTick), mc.player == null ? xRot() : mc.player.getViewXRot(partialTick));
            if (c != null) {
                setRotation((float) c[3], (float) c[4]);
                setPosition(c[0], c[1], c[2]);
                detached = true;
                return;
            }
        }
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
