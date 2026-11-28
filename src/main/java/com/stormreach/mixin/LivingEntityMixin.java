package com.stormreach.mixin;

import com.stormreach.Stormreach;
import com.stormreach.storm.StormPerks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.stormreach.anim.AnimPulse;

/** Storm mob abilities and stormwater/stormlava effects. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void stormreach$stormTick(CallbackInfo ci) {
        try {
            com.stormreach.realm.RealmGear.tick((LivingEntity) (Object) this);
            StormPerks.tick((LivingEntity) (Object) this);
            com.stormreach.wild.Stormkite.tick((LivingEntity) (Object) this);
            com.stormreach.front.UmbrellaItem.tick((LivingEntity) (Object) this);
        } catch (RuntimeException e) {
            Stormreach.LOGGER.error("Stormreach mob tick failed", e);
        }
    }

    /** Client: animation pulses from {@link AnimPulse}. */
    @Inject(method = "handleEntityEvent(B)V", at = @At("HEAD"), cancellable = true)
    private void stormreach$animPulse(byte id, CallbackInfo ci) {
        if (id == AnimPulse.ATTACK || id == AnimPulse.CAST) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (AnimPulse.isOurs(self)) {
                AnimPulse.record(self, id);
                ci.cancel();
            }
        }
    }
}
