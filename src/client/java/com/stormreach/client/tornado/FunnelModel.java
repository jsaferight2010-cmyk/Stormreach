package com.stormreach.client.tornado;

import com.stormreach.sim.TornadoMorphology;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * A condensation funnel built from stacked rings. Each frame the rings are resized from the tornado's
 * morphology profile (rope, cone, stovepipe, wedge...), spun, and wobbled so the funnel looks alive.
 * The model is drawn in "height units": ring i spans one unit; the renderer scales units to blocks.
 */
public class FunnelModel extends EntityModel<TornadoRenderState> {
    public static final int RINGS = 24;
    private final ModelPart[] rings = new ModelPart[RINGS];
    /** Width multiplier for the current pass (1 = main funnel, smaller for subvortices). */
    public float widthScale = 1f;
    public float phase = 0f;

    public FunnelModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        for (int i = 0; i < RINGS; i++) rings[i] = root.getChild("ring_" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < RINGS; i++) {
            // a 16x16x16 box = one block; its texture strip repeats round the ring
            root.addOrReplaceChild("ring_" + i, CubeListBuilder.create().texOffs(0, (i % 2) * 32).addBox(-8, -16, -8, 16, 16, 16), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TornadoRenderState s) {
        super.setupAnim(s);
        TornadoMorphology morph = TornadoMorphology.values()[Mth.clamp(s.morph, 0, TornadoMorphology.values().length - 1)];
        float segH = s.cloudBase / RINGS;
        float t = s.ageInTicks + phase;
        for (int i = 0; i < RINGS; i++) {
            ModelPart r = rings[i];
            float h = (i + 0.5f) / RINGS;                          // 0 at ground, 1 at cloud base
            boolean visible = h >= 1 - s.reach;                     // the funnel lowers from the cloud as it forms
            float radius = (float) (s.width * 0.5 * morph.radiusProfile(h)) * widthScale;
            if (!visible) radius = 0.001f;
            // ropes and needles snake about; big funnels only sway a little
            float sway = (morph == TornadoMorphology.ROPE ? 3.5f : morph == TornadoMorphology.NEEDLE ? 1.5f : 0.6f) * (1 - h) * (1 - h);
            r.x = Mth.sin(t * 0.05f + i * 0.35f) * sway * 16;
            r.z = Mth.cos(t * 0.043f + i * 0.31f) * sway * 16;
            r.y = -i * 16 * segH;                                    // translation is applied before the ring's scale
            r.xScale = radius * 2;                                   // the box is one block across; scale to the diameter
            r.zScale = radius * 2;
            r.yScale = segH;
            r.yRot = s.rotation * t * (0.25f + 0.02f * i);
        }
    }
}
