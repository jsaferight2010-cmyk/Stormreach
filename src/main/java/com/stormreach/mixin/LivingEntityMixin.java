package com.stormreach.mixin;

import com.stormreach.Stormreach;
import com.stormreach.storm.StormPerks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Storm mob abilities and stormwater/stormlava effects. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void stormreach$stormTick(CallbackInfo ci) {
        try {
            com.stormreach.realm.RealmGear.tick((LivingEntity) (Object) this);
            StormPerks.tick((LivingEntity) (Object) this);
        } catch (RuntimeException e) {
            Stormreach.LOGGER.error("Stormreach mob tick failed", e);
        }
    }
}
