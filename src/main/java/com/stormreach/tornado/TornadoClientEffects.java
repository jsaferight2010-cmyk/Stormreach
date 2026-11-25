package com.stormreach.tornado;

import com.stormreach.StormreachConfig;
import com.stormreach.sim.FunnelShape;
import com.stormreach.sim.TornadoFormation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Client-side particles at the base of a tornado (no world changes): the dust whirl, the debris cloud, waterspout spray. */
public final class TornadoClientEffects {
    private TornadoClientEffects() {}

    /** Set by the client each tick: the Creative see-inside view is on, so thin the particles right out. */
    public static volatile boolean seeThrough;

    static void tick(TornadoEntity t) {
        // spin: angular speed ~ v / r, so narrow funnels whirl and broad wedges turn slowly
        t.clientSpinO = t.clientSpin;
        float half = Math.max(1.5f, t.visualWidth() * 0.5f);
        t.clientSpin += Math.max(0.015f, Math.min(0.45f, 2.2f * (0.4f + 0.6f * t.intensity()) / half));
        Level level = t.level();
        RandomSource r = level.getRandom();
        float density = StormreachConfig.particleDensity / 100f * (seeThrough ? 0.15f : 1f);
        float reach = t.funnelReach();
        if (!FunnelShape.dustWhirl(reach) || density <= 0) return;
        boolean down = FunnelShape.touchedDown(reach);
        boolean water = t.formation() == TornadoFormation.WATERSPOUT;
        double rad = Math.max(2, t.visualWidth() * 0.5);
        int rot = t.rotationSign();
        // the circulation reaches the ground before the funnel does: a ring of whirling dust (or spray) comes first
        int n = (int) ((down ? 2 + t.visualWidth() * 0.15f : 3) * Math.max(0.3f, t.intensity()) * density);
        for (int i = 0; i < Math.min(40, n); i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = rad * (down ? 0.6 + r.nextDouble() * 0.9 : 0.8 + r.nextDouble() * 0.4);
            double x = t.getX() + Math.cos(a) * d, z = t.getZ() + Math.sin(a) * d;
            double y = t.getY() + r.nextDouble() * (down ? Math.max(3, rad * 0.4) : 1.5);
            // tangential motion following the rotation, with inflow and a lift
            double vx = (Math.sin(a) * rot - Math.cos(a) * 0.15) * 0.45, vz = (-Math.cos(a) * rot - Math.sin(a) * 0.15) * 0.45;
            if (water) {
                level.addParticle(r.nextInt(3) == 0 ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, x, y, z, vx, 0.12 + r.nextDouble() * 0.25, vz);
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.FALLING_WATER, x, y + 2 + r.nextDouble() * 4, z, 0, 0, 0);
            } else {
                level.addParticle(!down ? ParticleTypes.POOF : r.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        x, y, z, vx, 0.08 + r.nextDouble() * 0.2, vz);
            }
        }
    }
}
