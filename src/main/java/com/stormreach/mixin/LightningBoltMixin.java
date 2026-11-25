package com.stormreach.mixin;

import com.stormreach.Stormreach;
import com.stormreach.weather.StormWeather;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets every lightning strike reshape Stormreach blocks (crystals from stormstone, fulgurite from sand). */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void stormreach$onStrike(CallbackInfo ci) {
        LightningBolt self = (LightningBolt) (Object) this;
        if (self.tickCount == 1 && self.level() instanceof ServerLevel server) {
            try {
                StormWeather.onLightning(server, self.blockPosition().below());
            } catch (RuntimeException e) {
                Stormreach.LOGGER.error("Stormreach lightning hook failed", e);
            }
        }
    }
}
