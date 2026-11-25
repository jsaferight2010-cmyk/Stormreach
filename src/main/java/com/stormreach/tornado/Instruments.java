package com.stormreach.tornado;

import com.stormreach.sim.EFScale;
import com.stormreach.weather.StormWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.Vec3;

/**
 * The mod's weather model as seen by instruments: wind, pressure, temperature, humidity, dew point, storm risk
 * and a simple forecast from the server's weather timers. Values use real units.
 */
public final class Instruments {
    private Instruments() {}

    /** Sustained wind and gust at a position (mph): background weather + any tornado or downburst. */
    public static double[] windMph(ServerLevel level, BlockPos pos) {
        long hour = level.getGameTime() / 1000;
        double n = noise(hour, pos.getX() >> 6, pos.getZ() >> 6);
        double base = level.isThundering() ? 28 + 14 * n : level.isRaining() ? 16 + 12 * n : 3 + 9 * n;
        if (level.dimension() != Level.OVERWORLD) base = com.stormreach.realm.TempestRealm.isRealm(level) ? 22 + 18 * n : 2 + 4 * n;
        double elevation = 1.0 + Mth.clamp((pos.getY() - 63) / 120.0, -0.3, 0.6);
        if (!level.canSeeSky(pos.above())) elevation *= 0.35;
        double sustained = base * elevation;
        double gust = sustained * (level.isThundering() ? 1.6 : 1.35);
        double tornado = TornadoManager.tornadoWindMph(level, Vec3.atCenterOf(pos));
        double burst = WeatherEvents.downburstMph(level, Vec3.atCenterOf(pos));
        return new double[]{Math.max(sustained, Math.max(tornado, burst) * 0.75), Math.max(gust, Math.max(tornado, burst))};
    }

    /** Background wind speed (mph) at a column without touching any blocks: the radar uses this for every gate. */
    public static double ambientMph(ServerLevel level, double x, double z) {
        long hour = level.getGameTime() / 1000;
        double n = noise(hour, Mth.floor(x) >> 6, Mth.floor(z) >> 6);
        double base = level.isThundering() ? 28 + 14 * n : level.isRaining() ? 16 + 12 * n : 3 + 9 * n;
        if (level.dimension() != Level.OVERWORLD) base = com.stormreach.realm.TempestRealm.isRealm(level) ? 22 + 18 * n : 2 + 4 * n;
        return base;
    }

    /** Direction the wind blows FROM (meteorological convention), as a 16-point compass label. */
    public static String windFrom() {
        Vec3 w = StormWeather.windVector();
        // bearing the air moves toward is atan2(x, -z) (0 = north, 90 = east); it comes FROM the opposite side.
        // (5.x had both signs flipped, so a wind blowing east read "from E".)
        return compass(Math.toDegrees(Math.atan2(w.x, -w.z)) + 180);
    }

    /** Station pressure in hPa (altitude-corrected), with any tornado's pressure deficit. */
    public static double pressureHpa(ServerLevel level, BlockPos pos) {
        double slp = 1013.25 + (level.isThundering() ? -14 : level.isRaining() ? -7 : 4) + 3 * Math.sin(level.getGameTime() / 6000.0);
        double station = slp * Math.exp(-(pos.getY() - 63) / 8434.0);
        TornadoEntity t = TornadoManager.nearest(level, Vec3.atCenterOf(pos), 600);
        if (t != null) {
            double r = Math.hypot(pos.getX() - t.getX(), pos.getZ() - t.getZ());
            station -= t.windField().pressureDeficit(r) / 100.0;
        }
        return station;
    }

    /** Air temperature in degrees Celsius from biome climate, altitude, time of day and rain. */
    public static double temperatureC(ServerLevel level, BlockPos pos) {
        float t = level.getBiome(pos).value().getBaseTemperature();
        double c = 13.6 * t + 4.0;
        double day = (level.getOverworldClockTime() % 24000) / 24000.0; // 0 = sunrise
        c += 5.0 * Math.sin((day - 0.1) * Math.PI * 2);
        if (level.isRainingAt(pos)) c -= 3;
        c -= Math.max(0, pos.getY() - 63) * 0.0065; // standard lapse rate 6.5 C/km
        if (level.dimension() == Level.NETHER) c = 45 + 5 * Math.sin(day * Math.PI * 2);
        if (level.dimension() == Level.END) c = -12;
        return c;
    }

    public static double humidity(ServerLevel level, BlockPos pos) {
        double rh = 35 + 55 * Mth.clamp(downfall(level, pos), 0, 1);
        if (level.isRaining()) rh = Math.max(rh, 92);
        return Mth.clamp(rh, 5, 100);
    }

    private static double downfall(ServerLevel level, BlockPos pos) {
        // the biome's downfall isn't exposed; approximate from precipitation and temperature
        var b = level.getBiome(pos).value();
        if (!b.hasPrecipitation()) return 0.0;
        return b.getBaseTemperature() > 0.9 ? 0.85 : 0.55;
    }

    /** Dew point (Magnus formula), degrees Celsius. */
    public static double dewPointC(double tempC, double rh) {
        double a = 17.62, b = 243.12;
        double g = Math.log(Math.max(1, rh) / 100.0) + a * tempC / (b + tempC);
        return b * g / (a - g);
    }

    /** SPC-style severe risk category, from moisture, warmth and whether storms are ongoing. */
    public static String stormRisk(ServerLevel level, BlockPos pos) {
        double t = temperatureC(level, pos), td = dewPointC(t, humidity(level, pos));
        int score = 0;
        if (td > 10) score++;
        if (td > 16) score++;
        if (t > 22) score++;
        if (level.isRaining()) score++;
        if (level.isThundering()) score += 2;
        if (!TornadoManager.live(level).isEmpty()) score++;
        String[] names = {"none", "general thunder", "marginal", "slight", "enhanced", "moderate", "high"};
        return names[Mth.clamp(score, 0, names.length - 1)];
    }

    /** Minutes until the next weather change according to the server's weather timers. */
    public static String forecast(ServerLevel level) {
        WeatherData w = level.getServer().getWeatherData();
        if (w.getClearWeatherTime() > 0) return "clear for about " + w.getClearWeatherTime() / 1200 + " more minutes";
        if (w.isRaining()) {
            String s = "rain ending in about " + Math.max(1, w.getRainTime() / 1200) + " minutes";
            if (w.isThundering()) s += "; thunder ending in about " + Math.max(1, w.getThunderTime() / 1200) + " minutes";
            else s += "; thunderstorms possible in about " + Math.max(1, w.getThunderTime() / 1200) + " minutes";
            return s;
        }
        return "rain expected in about " + Math.max(1, w.getRainTime() / 1200) + " minutes";
    }

    public static String beaufort(double mph) {
        double[] limits = {1, 3, 7, 12, 18, 24, 31, 38, 46, 54, 63, 72};
        String[] names = {"calm", "light air", "light breeze", "gentle breeze", "moderate breeze", "fresh breeze", "strong breeze",
                "near gale", "gale", "strong gale", "storm", "violent storm", "hurricane force"};
        for (int i = 0; i < limits.length; i++) if (mph < limits[i]) return "Beaufort " + i + " (" + names[i] + ")";
        return "Beaufort 12 (" + names[12] + ")";
    }

    public static String compass(double deg) {
        String[] pts = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int i = (int) Math.round(((deg % 360) + 360) % 360 / 22.5) % 16;
        return pts[i];
    }

    /** Compass bearing from one position to another (Minecraft north = -z). */
    public static String bearing(Vec3 from, Vec3 to) {
        double deg = Math.toDegrees(Math.atan2(to.x - from.x, -(to.z - from.z)));
        return compass(deg);
    }

    public static boolean hasWarningDevice(ServerPlayer p) {
        if (p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(com.stormreach.story.StoryRegistry.STORM_CHASER_CAP)) return true;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(WeatherItems.STORM_RADAR) || s.is(WeatherItems.WEATHER_RADIO) || s.is(com.stormreach.story.StoryRegistry.CHASERS_BADGE)) return true;
        }
        return false;
    }

    private static double noise(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        return ((h >>> 11) & 0xFFFF) / 65535.0;
    }

    public static String efLabel(double mph) {
        EFScale ef = EFScale.fromGustMph(mph);
        return ef == null ? "below EF0" : ef.name() + "-equivalent";
    }

}
