package com.stormreach.client.story;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * The player model, posed like something that has stood in the storm for a long time: slumped shoulders, arms hanging
 * dead and swaying, a slow breath, the head tilting, and now and then a sudden sideways snap of the head.
 */
public class WeatheredModel extends PlayerModel {
    public WeatheredModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AvatarRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float breath = Mth.sin(t * 0.045f);
        body.xRot += 0.1f;
        head.z -= 1.2f;
        head.y += 0.6f;
        head.xRot += 0.12f;
        head.zRot += Mth.sin(t * 0.021f) * 0.1f;
        float snap = (t + s.id * 37) % 160f;
        if (snap > 140f) {
            float p = (snap - 140f) / 20f;
            head.zRot += (p < 0.12f ? p / 0.12f : 1f - Math.max(0, p - 0.6f) / 0.4f) * 0.5f;
        }
        rightArm.z -= 1f; leftArm.z -= 1f;
        rightArm.y += 0.8f + breath * 0.2f;
        leftArm.y += 0.8f + breath * 0.2f;
        rightArm.xRot = 0.06f + Mth.sin(t * 0.031f) * 0.05f;
        leftArm.xRot = 0.04f + Mth.sin(t * 0.027f + 1.3f) * 0.05f;
        rightArm.zRot = 0.05f + Mth.sin(t * 0.019f) * 0.02f;
        leftArm.zRot = -0.05f - Mth.sin(t * 0.023f) * 0.02f;
        rightArm.yRot = 0f;
        leftArm.yRot = 0f;
    }
}
