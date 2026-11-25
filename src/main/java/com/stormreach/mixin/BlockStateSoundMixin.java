package com.stormreach.mixin;

import com.stormreach.registry.ModSoundTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stormreach blocks sound like Stormreach (see ModSoundTypes); everything else is left alone. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateSoundMixin {
    @Inject(method = "getSoundType()Lnet/minecraft/world/level/block/SoundType;", at = @At("RETURN"), cancellable = true)
    private void stormreach$ownSounds(CallbackInfoReturnable<SoundType> cir) {
        Block block = ((BlockBehaviour.BlockStateBase) (Object) this).getBlock();
        SoundType ours = ModSoundTypes.forBlock(block, cir.getReturnValue());
        if (ours != null) cir.setReturnValue(ours);
    }
}
