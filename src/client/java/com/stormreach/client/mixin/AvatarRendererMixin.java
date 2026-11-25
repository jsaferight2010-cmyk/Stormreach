package com.stormreach.client.mixin;

import com.stormreach.backpack.BackpackEquipment;
import com.stormreach.backpack.BackpackItem;
import com.stormreach.client.backpack.BackpackLayer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the worn-backpack layer to player renderers and passes the worn tier into the render state. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
    protected AvatarRendererMixin(EntityRendererProvider.Context ctx, PlayerModel model, float shadow) {
        super(ctx, model, shadow);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void stormreach$addBackpackLayer(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.addLayer(new BackpackLayer((net.minecraft.client.renderer.entity.RenderLayerParent) this));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void stormreach$backpackTier(Avatar entity, AvatarRenderState state, float partial, CallbackInfo ci) {
        ItemStack worn = entity.getItemBySlot(BackpackEquipment.SLOT);
        int tier = -1;
        if (worn.getItem() instanceof BackpackItem bp) tier = bp.tier == null ? 7 : bp.tier.ordinal();
        ((BackpackLayer.BackpackRenderData) state).stormreach$setBackpackTier(tier);
    }
}
