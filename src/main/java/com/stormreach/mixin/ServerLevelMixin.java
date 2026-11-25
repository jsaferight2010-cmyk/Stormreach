package com.stormreach.mixin;

import com.stormreach.Stormreach;
import com.stormreach.weather.StormWeather;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/** Runs the Stormreach weather once per world tick. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("TAIL"))
    private void stormreach$tickWeather(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        try {
            StormWeather.tick((ServerLevel) (Object) this);
            ServerLevel self = (ServerLevel) (Object) this;
            com.stormreach.realm.TempestRealm.tick(self);
            com.stormreach.tornado.TornadoManager.tick(self);
            com.stormreach.weather.precip.RainAccumulation.tick(self);
            com.stormreach.weather.cloud.StormCells.tick(self);
            com.stormreach.labyrinth.LabyrinthTracker.tick(self);
            com.stormreach.story.StrangerDirector.tick(self);
            com.stormreach.story.Cinematics.tick(self);
        } catch (RuntimeException e) {
            // Never let a weather bug take the server down.
            Stormreach.LOGGER.error("Stormreach weather tick failed", e);
        }
    }
}
