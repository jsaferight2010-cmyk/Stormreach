package com.stormreach.client.particle;

import com.stormreach.particle.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** One class for all of Stormreach's particles; each kind sets its own life, size, motion, fade and animation. */
public class StormParticle extends SingleQuadParticle {
    enum Kind { SPARK, SPLASH, SHARD, DUST, STREAK, VAULT, HAIL_S, HAIL_M, HAIL_L }

    private final Kind kind;
    private final SpriteSet sprites;
    private final float baseSize;
    private boolean bounced;

    StormParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.kind = kind;
        this.sprites = sprites;
        this.xd = xd; this.yd = yd; this.zd = zd;
        float r = random.nextFloat();
        switch (kind) {
            case SPARK -> { lifetime = 4 + random.nextInt(5); gravity = 0; friction = 0.7f; quadSize = 0.06f + r * 0.05f; hasPhysics = false; }
            case SPLASH -> { lifetime = 6 + random.nextInt(4); gravity = 0.9f; friction = 0.9f; quadSize = 0.08f + r * 0.05f; }
            case SHARD -> { lifetime = 12 + random.nextInt(10); gravity = 1.0f; friction = 0.96f; quadSize = 0.05f + r * 0.06f; roll = r * 6.28f; }
            case DUST -> { lifetime = 30 + random.nextInt(30); gravity = -0.01f; friction = 0.94f; quadSize = 0.25f + r * 0.35f; roll = r * 6.28f; }
            case STREAK -> { lifetime = 10 + random.nextInt(8); gravity = 0; friction = 0.98f; quadSize = 0.12f + r * 0.08f; hasPhysics = false; }
            case VAULT -> { lifetime = 40 + random.nextInt(40); gravity = -0.004f; friction = 0.97f; quadSize = 0.04f + r * 0.03f; }
            // 6.0 hail: falls at the stone's terminal speed (set by the spawner), bounces once, rolls a moment and melts away
            case HAIL_S -> { lifetime = 70; gravity = 0; friction = 1f; quadSize = 0.035f + r * 0.02f; roll = r * 6.28f; }
            case HAIL_M -> { lifetime = 80; gravity = 0; friction = 1f; quadSize = 0.07f + r * 0.035f; roll = r * 6.28f; }
            case HAIL_L -> { lifetime = 90; gravity = 0; friction = 1f; quadSize = 0.14f + r * 0.07f; roll = r * 6.28f; }
        }
        this.baseSize = quadSize;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSpriteFromAge(sprites);
        float f = age / (float) lifetime;
        switch (kind) {
            case SPARK -> { alpha = random.nextFloat() < 0.3f ? 0.4f : 1f; quadSize = baseSize * (1 - f * 0.5f); }   // flickers
            case DUST -> { quadSize = baseSize * (1 + f * 1.5f); alpha = 0.75f * (1 - f); oRoll = roll; roll += 0.02f; }
            case STREAK -> alpha = Mth.sin((float) Math.PI * f);
            case VAULT -> alpha = 0.9f * Mth.sin((float) Math.PI * f);
            case SHARD -> { oRoll = roll; roll += 0.25f; if (onGround) alpha *= 0.9f; }
            case HAIL_S, HAIL_M, HAIL_L -> {
                oRoll = roll; roll += 0.2f;
                if (onGround) {
                    if (!bounced) { bounced = true; yd = 0.05 + quadSize * 0.4; xd *= 0.3; zd *= 0.3; gravity = 0.9f; friction = 0.8f;
                        if (lifetime - age > 25) lifetime = age + 25; }
                    else alpha = Math.max(0, alpha - 0.05f);
                }
            }
            default -> alpha = 1 - f * f;
        }
    }

    @Override
    protected Layer getLayer() {
        return kind == Kind.SHARD || kind == Kind.HAIL_S || kind == Kind.HAIL_M || kind == Kind.HAIL_L ? Layer.OPAQUE : Layer.TRANSLUCENT;
    }

    record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public net.minecraft.client.particle.Particle createParticle(SimpleParticleType t, ClientLevel level, double x, double y, double z,
                                                                     double xd, double yd, double zd, net.minecraft.util.RandomSource random) {
            return new StormParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }

    public static void register() {
        ParticleProviderRegistry reg = ParticleProviderRegistry.getInstance();
        reg.register(ModParticles.STORM_SPARK, s -> new Provider(s, Kind.SPARK));
        reg.register(ModParticles.RAIN_SPLASH, s -> new Provider(s, Kind.SPLASH));
        reg.register(ModParticles.HAIL_SHARD, s -> new Provider(s, Kind.SHARD));
        reg.register(ModParticles.DEBRIS_DUST, s -> new Provider(s, Kind.DUST));
        reg.register(ModParticles.WIND_STREAK, s -> new Provider(s, Kind.STREAK));
        reg.register(ModParticles.VAULT_DUST, s -> new Provider(s, Kind.VAULT));
        reg.register(ModParticles.HAIL_SMALL, s -> new Provider(s, Kind.HAIL_S));
        reg.register(ModParticles.HAIL_MEDIUM, s -> new Provider(s, Kind.HAIL_M));
        reg.register(ModParticles.HAIL_LARGE, s -> new Provider(s, Kind.HAIL_L));
    }
}
