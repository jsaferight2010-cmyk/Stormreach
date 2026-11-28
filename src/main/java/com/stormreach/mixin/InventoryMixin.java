package com.stormreach.mixin;

import com.stormreach.backpack.Backpacks;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soulbound backpacks are set aside before a dead player's inventory spills. */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Inject(method = "dropAll()V", at = @At("HEAD"))
    private void stormreach$keepSoulbound(CallbackInfo ci) {
        Backpacks.keepSoulbound((Inventory) (Object) this);
    }
}
