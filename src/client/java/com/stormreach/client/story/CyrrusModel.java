package com.stormreach.client.story;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/**
 * Cyrrus in the player's likeness. Keeps the player model's walk and attack swings and layers the storm on top: in
 * its first form it still slumps like the Stranger; once corrupted it stands tall with arms held out from its sides,
 * trembling with charge, its head cocked; in its final form the tremor and spread are stronger.
 */
public class CyrrusModel extends PlayerModel {
    public CyrrusModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AvatarRenderState s) {
        super.setupAnim(s);
        int form = s instanceof StoryRenderers.CyrrusState c ? c.form : 0;
        float t = s.ageInTicks;
        if (form == 0) {
            body.xRot += 0.1f;
            head.xRot += 0.12f;
            head.zRot += Mth.sin(t * 0.021f) * 0.1f;
            return;
        }
        float k = form == 1 ? 1f : 1.6f;
        float tremor = (Mth.sin(t * 2.7f) * 0.6f + Mth.sin(t * 4.1f) * 0.4f) * 0.025f * k;
        head.zRot += 0.18f + Mth.sin(t * 0.04f) * 0.06f + tremor;
        head.xRot -= 0.05f;
        rightArm.zRot += 0.28f * k + Mth.sin(t * 0.06f) * 0.05f + tremor;
        leftArm.zRot -= 0.28f * k + Mth.sin(t * 0.06f + 1f) * 0.05f + tremor;
        rightArm.xRot -= 0.15f * k;
        leftArm.xRot -= 0.15f * k;
        body.yRot += Mth.sin(t * 0.03f) * 0.05f;
    }
}
