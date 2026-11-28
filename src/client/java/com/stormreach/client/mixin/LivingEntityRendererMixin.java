package com.stormreach.client.mixin;

import com.stormreach.storm.StormMobs;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Draws storm mobs with a cold storm-blue tint over their normal texture. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final int STORM_TINT = 0xFF8FB3E8;

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void stormreach$stormTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (state.entityType != null && StormMobs.isStorm(state.entityType)) {
            cir.setReturnValue(multiply(cir.getReturnValue(), STORM_TINT));
        }
    }

    private static int multiply(int a, int b) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24);
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (aa << 24) | ((ar * br / 255) << 16) | ((ag * bg / 255) << 8) | (ab * bb / 255);
    }
}
