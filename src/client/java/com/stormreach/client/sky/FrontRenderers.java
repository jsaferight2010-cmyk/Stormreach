package com.stormreach.client.sky;

import com.stormreach.Stormreach;
import com.stormreach.client.anim.RigRenderer;
import com.stormreach.client.anim.RigState;
import com.stormreach.client.model.gen.*;
import com.stormreach.front.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Renderers for the 5.4 Thunderfront creatures and the thrown Bottled Dust Devil. */
public final class FrontRenderers {
    private FrontRenderers() {}

    private static Identifier tex(String name) {
        return Stormreach.id("textures/entity/" + name + ".png");
    }

    public static void register() {
        EntityRendererRegistry.register(FrontRegistry.RAINFROG, FrogRenderer::new);
        EntityRendererRegistry.register(FrontRegistry.STORM_PETREL, PetrelRenderer::new);
        EntityRendererRegistry.register(FrontRegistry.DUST_DEVIL, DustDevilRenderer::new);
        EntityRendererRegistry.register(FrontRegistry.BOTTLED_DUST_DEVIL_ENTITY, ThrownItemRenderer::new);
    }

    public static class FrogRenderer extends RigRenderer<Rainfrog, RainfrogModel> {
        private static final Identifier TEXTURE = tex("rainfrog");
        public FrogRenderer(EntityRendererProvider.Context c) { super(c, new RainfrogModel(RainfrogModel.createBodyLayer().bakeRoot()), 0.25f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected void scale(RigState s, com.mojang.blaze3d.vertex.PoseStack pose) { if (s.isBaby) pose.scale(0.5f, 0.5f, 0.5f); }
    }

    public static class PetrelRenderer extends RigRenderer<StormPetrel, StormPetrelModel> {
        private static final Identifier TEXTURE = tex("storm_petrel");
        public PetrelRenderer(EntityRendererProvider.Context c) { super(c, new StormPetrelModel(StormPetrelModel.createBodyLayer().bakeRoot()), 0.2f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
    }

    public static class DustDevilRenderer extends RigRenderer<DustDevil, DustDevilModel> {
        private static final Identifier TEXTURE = tex("dust_devil");
        public DustDevilRenderer(EntityRendererProvider.Context c) { super(c, new DustDevilModel(DustDevilModel.createBodyLayer().bakeRoot()), 0.6f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected int getBlockLightLevel(DustDevil e, BlockPos p) { return Math.max(8, super.getBlockLightLevel(e, p)); }
    }
}
