package com.stormreach.client.anim;

import com.stormreach.anim.AnimPulse;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.WeakHashMap;

/** Base renderer for the rigged mobs: fills a {@link RigState}, smoothing the values that would otherwise snap. */
public abstract class RigRenderer<T extends Mob, M extends EntityModel<? super RigState>> extends MobRenderer<T, RigState, M> {
    /** Per entity: last time, air, speed, turn. */
    private final Map<Entity, float[]> memory = new WeakHashMap<>();

    protected RigRenderer(EntityRendererProvider.Context ctx, M model, float shadow) {
        super(ctx, model, shadow);
    }

    @Override
    public RigState createRenderState() {
        return new RigState();
    }

    @Override
    public void extractRenderState(T e, RigState s, float partial) {
        super.extractRenderState(e, s, partial);
        s.attackAge = AnimPulse.age(e, partial);
        s.attackKind = AnimPulse.kind(e);
        s.hurtAge = e.hurtTime > 0 ? Math.max(0, e.hurtDuration - e.hurtTime) + partial : 1e6f;
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        float now = e.tickCount + partial;
        float[] m = memory.computeIfAbsent(e, k -> new float[] {now, e.onGround() ? 0 : 1, 0, 0});
        float dt = Mth.clamp(now - m[0], 0f, 5f);
        m[0] = now;
        float k = 1f - (float) Math.exp(-0.25f * dt);
        m[1] += ((e.onGround() || e.isPassenger() ? 0f : 1f) - m[1]) * k;
        m[2] += ((float) Math.sqrt(dx * dx + dz * dz) - m[2]) * k;
        m[3] += (Mth.wrapDegrees(e.yBodyRot - e.yBodyRotO) - m[3]) * k;
        s.air = m[1];
        s.speed = m[2];
        s.turn = m[3];
        s.vy = (float) (e.getY() - e.yo);
        s.seed = ((e.getId() * 0.618034f) % 1f + 1f) % 1f;
    }
}
