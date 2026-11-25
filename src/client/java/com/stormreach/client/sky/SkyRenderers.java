package com.stormreach.client.sky;

import com.stormreach.Stormreach;
import com.stormreach.client.anim.RigRenderer;
import com.stormreach.client.anim.RigState;
import com.stormreach.client.model.gen.*;
import com.stormreach.sky.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Renderers for the 5.3 Skyward creatures, the Bottled Lightning and the Wind Turbine's rotor. */
public final class SkyRenderers {
    private SkyRenderers() {}

    private static Identifier tex(String name) {
        return Stormreach.id("textures/entity/" + name + ".png");
    }

    public static void register() {
        EntityRendererRegistry.register(SkyRegistry.SQUALL_SERPENT, SerpentRenderer::new);
        EntityRendererRegistry.register(SkyRegistry.HAILSTONE_GOLEM, GolemRenderer::new);
        EntityRendererRegistry.register(SkyRegistry.AURORA_MOTH, MothRenderer::new);
        EntityRendererRegistry.register(SkyRegistry.STORMFLY, StormflyRenderer::new);
        EntityRendererRegistry.register(SkyRegistry.BOTTLED_LIGHTNING_ENTITY, ThrownItemRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(SkyRegistry.TURBINE_ENTITY, TurbineRenderer::new);
    }

    public static class SerpentRenderer extends RigRenderer<SquallSerpent, SquallSerpentModel> {
        private static final Identifier TEXTURE = tex("squall_serpent");
        public SerpentRenderer(EntityRendererProvider.Context c) { super(c, new SquallSerpentModel(SquallSerpentModel.createBodyLayer().bakeRoot()), 1.1f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected int getBlockLightLevel(SquallSerpent e, BlockPos p) { return Math.max(8, super.getBlockLightLevel(e, p)); }
    }

    public static class GolemRenderer extends RigRenderer<HailstoneGolem, HailstoneGolemModel> {
        private static final Identifier TEXTURE = tex("hailstone_golem");
        public GolemRenderer(EntityRendererProvider.Context c) { super(c, new HailstoneGolemModel(HailstoneGolemModel.createBodyLayer().bakeRoot()), 0.9f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
    }

    public static class MothRenderer extends RigRenderer<AuroraMoth, AuroraMothModel> {
        private static final Identifier TEXTURE = tex("aurora_moth");
        public MothRenderer(EntityRendererProvider.Context c) { super(c, new AuroraMothModel(AuroraMothModel.createBodyLayer().bakeRoot()), 0.2f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected int getBlockLightLevel(AuroraMoth e, BlockPos p) { return Math.max(9, super.getBlockLightLevel(e, p)); }
    }

    public static class StormflyRenderer extends RigRenderer<Stormfly, StormflyModel> {
        private static final Identifier TEXTURE = tex("stormfly");
        public StormflyRenderer(EntityRendererProvider.Context c) { super(c, new StormflyModel(StormflyModel.createBodyLayer().bakeRoot()), 0.0f); }
        @Override public Identifier getTextureLocation(RigState s) { return TEXTURE; }
        @Override protected int getBlockLightLevel(Stormfly e, BlockPos p) { return 15; }
        @Override protected void scale(RigState s, com.mojang.blaze3d.vertex.PoseStack pose) { pose.scale(0.6f, 0.6f, 0.6f); }
    }
}
