package com.stormreach.tornado;

import com.stormreach.StormreachConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Client-side debris cloud and dust whirl at the base of a tornado (particles only; no world changes). */
public final class TornadoClientEffects {
    private TornadoClientEffects() {}

    static void tick(TornadoEntity t) {
        Level level = t.level();
        RandomSource r = level.getRandom();
        float density = StormreachConfig.particleDensity / 100f;
        int n = (int) ((2 + t.visualWidth() * 0.15f) * t.intensity() * density);
        double rad = Math.max(2, t.visualWidth() * 0.5);
        for (int i = 0; i < Math.min(40, n); i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = rad * (0.6 + r.nextDouble() * 0.9);
            double x = t.getX() + Math.cos(a) * d, z = t.getZ() + Math.sin(a) * d;
            double y = t.getY() + r.nextDouble() * Math.max(3, rad * 0.4);
            // tangential motion following the rotation
            double vx = -Math.sin(a) * 0.4 * t.rotationSign(), vz = Math.cos(a) * 0.4 * t.rotationSign();
            level.addParticle(r.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, vx, 0.08 + r.nextDouble() * 0.2, vz);
        }
        if (t.funnelReach() < 0.99 && r.nextInt(2) == 0) {
            // before the funnel reaches the ground, the circulation shows as a whirl of dust
            double a = r.nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.POOF, t.getX() + Math.cos(a) * rad, t.getY() + 0.5, t.getZ() + Math.sin(a) * rad, -Math.sin(a) * 0.3, 0.2, Math.cos(a) * 0.3);
        }
    }
}
