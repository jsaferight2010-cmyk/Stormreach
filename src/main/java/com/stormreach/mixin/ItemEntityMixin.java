package com.stormreach.mixin;

import com.stormreach.backpack.Backpacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resilience: dropped backpacks ignore all damage and are lifted back out of the void. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void stormreach$resilient(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Backpacks.indestructible((ItemEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void stormreach$outOfVoid(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level() instanceof ServerLevel level && self.getY() < level.getMinY() + 2 && Backpacks.indestructible(self)) {
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, self.getBlockX(), self.getBlockZ());
            self.setPos(self.getX(), Math.max(top, level.getSeaLevel()) + 1, self.getZ());
            self.setDeltaMovement(0, 0, 0);
            self.setNoGravity(top <= level.getMinY());
            self.setRemainingFireTicks(0);
        }
    }
}
