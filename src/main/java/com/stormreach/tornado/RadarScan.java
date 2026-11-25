package com.stormreach.tornado;

import com.stormreach.sim.EFScale;
import com.stormreach.sim.RadarMath;
import com.stormreach.sim.WindField;
import com.stormreach.weather.StormWeather;
import com.stormreach.weather.cloud.StormCellEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One full Doppler scan around a radar: reflectivity (dBZ) and radial velocity (m/s) for every gate of the polar grid
 * in {@link RadarMath}. Built entirely from the simulated weather (storm cells, hail cores, downbursts, tornado wind
 * fields and the ambient wind), never from chunk data, so it costs no block lookups and loads nothing.
 *
 * Velocity is only reported where something reflects the beam (precipitation, debris, or clear-air returns close to
 * the radar), as on a real scope; elsewhere the gate is {@link RadarMath#NO_DATA}.
 */
public final class RadarScan {
    private RadarScan() {}

    public record Grid(byte[] dbz, byte[] velocity) {
        public static Grid empty() {
            byte[] v = new byte[RadarMath.AZIMUTHS * RadarMath.GATES];
            java.util.Arrays.fill(v, RadarMath.NO_DATA);
            return new Grid(new byte[RadarMath.AZIMUTHS * RadarMath.GATES], v);
        }
    }

    /** Ambient wind at a point in m/s as {u (east), v (south)}: the weather's wind direction and a terrain-free speed. */
    public static double[] ambient(ServerLevel level, double x, double z) {
        Vec3 dir = StormWeather.windVector();
        double mph = Instruments.ambientMph(level, x, z);
        double ms = EFScale.mphToMs(mph);
        return new double[]{dir.x * ms, dir.z * ms};
    }

    public static Grid scan(ServerLevel level, Vec3 radar, double range, List<TornadoEntity> tornadoes,
                            List<StormCellEntity> cells, List<WeatherEvents.ActiveEvent> events) {
        Grid g = Grid.empty();
        boolean raining = level.isRaining();
        double clearAir = range * 0.22;
        double t = level.getGameTime() / 20.0;
        for (int az = 0; az < RadarMath.AZIMUTHS; az++) {
            for (int gate = 0; gate < RadarMath.GATES; gate++) {
                double[] off = RadarMath.gateOffset(az, gate, range);
                double x = radar.x + off[0], z = radar.z + off[1];
                double dist = Math.hypot(off[0], off[1]);
                // ---- reflectivity
                double dbz = raining && level.dimension() == Level.OVERWORLD ? 14 + 6 * Math.sin(x * 0.011 + z * 0.007 + t * 0.02) : 0;
                for (StormCellEntity c : cells) {
                    double d = Math.hypot(c.getX() - x, c.getZ() - z);
                    double peak = 30 + 25 * Math.min(1.2, c.growth()) + (c.severe() ? 10 : 0);
                    dbz = RadarMath.combineDbz(dbz, RadarMath.coreDbz(peak, Math.max(8, c.cloudWidth() * (0.35 + 0.4 * c.growth())), d));
                }
                double u = 0, v = 0;
                double[] amb = ambient(level, x, z);
                u += amb[0]; v += amb[1];
                for (WeatherEvents.ActiveEvent e : events) {
                    if (e.level != level) continue;
                    double ex = x - e.centre.x, ez = z - e.centre.z, d = Math.hypot(ex, ez);
                    if (e instanceof WeatherEvents.HailBurst h) {
                        dbz = RadarMath.combineDbz(dbz, RadarMath.coreDbz(58 + 12 * h.severity, h.radius, d));
                    } else if (e instanceof WeatherEvents.Downburst db) {
                        dbz = RadarMath.combineDbz(dbz, RadarMath.coreDbz(db.wet ? 52 : 18, db.radius, d));
                        double ms = EFScale.mphToMs(db.mphAt(new Vec3(x, e.centre.y, z)));
                        if (d > 0.5) { u += ms * ex / d; v += ms * ez / d; }   // outflow spreads straight out from the core
                    }
                }
                boolean debris = false;
                for (TornadoEntity tor : tornadoes) {
                    double tx = x - tor.getX(), tz = z - tor.getZ(), d = Math.hypot(tx, tz);
                    WindField f = tor.windField();
                    double reach = Math.max(40, f.coreRadius * 10);
                    if (d > reach * 2) continue;
                    WindField.Sample s = f.sample(tx, tz, 30, t);
                    double fade = Math.exp(-(d / reach) * (d / reach));
                    // the vortex's own circulation and inflow on top of the background flow it is embedded in
                    u += (s.u() - f.translateX) * fade;
                    v += (sampleV(s) - f.translateZ) * fade;
                    // the parent mesocyclone: a broad, weaker circulation the radar resolves even when the tornado
                    // itself is smaller than a gate (that is what a real velocity couplet usually is)
                    double rm = Math.max(70, f.coreRadius * 6), vm = Math.min(45, f.vMax * 0.45);
                    if (d < rm * 6 && d > 0.5) {
                        double vt = WindField.tangential(vm, rm, d) * Math.exp(-(d / (rm * 3)) * (d / (rm * 3)));
                        u += vt * (tz / d) * f.rotationSign;
                        v += vt * (-tx / d) * f.rotationSign;
                    }
                    if (d < Math.max(12, tor.visualWidth() * 1.5)) { debris = true; dbz = RadarMath.combineDbz(dbz, 62); }   // debris ball
                    else dbz = RadarMath.combineDbz(dbz, RadarMath.coreDbz(48, reach * 0.6, d));                           // hook echo / RFD precip
                }
                int i = RadarMath.index(az, gate);
                g.dbz()[i] = RadarMath.packDbz(dbz);
                boolean echo = dbz >= 8 || debris || dist < clearAir;
                g.velocity()[i] = echo ? RadarMath.packVelocity(RadarMath.radial(u, v, off[0], off[1])) : RadarMath.NO_DATA;
            }
        }
        return g;
    }

    /** The v (south) component of a wind sample. WindField.Sample names it w after its own convention. */
    private static double sampleV(WindField.Sample s) { return s.w(); }
}
