package com.stormreach.mixin;

import com.stormreach.storm.StormPerks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets storm mobs shock what they hit. */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void stormreach$shock(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        com.stormreach.anim.AnimPulse.fire((Mob) (Object) this, com.stormreach.anim.AnimPulse.ATTACK);
        if (Boolean.TRUE.equals(cir.getReturnValue())) StormPerks.onHit(level, (Mob) (Object) this, target);
    }
}
