package com.stormreach.client.mixin;

import com.stormreach.client.backpack.BackpackLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements BackpackLayer.BackpackRenderData {
    @Unique
    private int stormreach$tier = -1;

    @Override
    public int stormreach$backpackTier() { return stormreach$tier; }

    @Override
    public void stormreach$setBackpackTier(int tier) { stormreach$tier = tier; }
}
