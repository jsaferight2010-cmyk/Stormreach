package com.stormreach.weather.cloud;

import com.stormreach.StormreachConfig;
import com.stormreach.weather.precip.PrecipContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Keeps a few developing storm clouds around each player during thunderstorms (and the odd shower cloud in rain),
 * and makes them collapse when the weather clears.
 */
public final class StormCells {
    private StormCells() {}

    public static void tick(ServerLevel level) {
        if (!StormreachConfig.stormClouds || level.getGameTime() % 100 != 37) return;
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return;
        RandomSource r = level.getRandom();
        boolean storm = level.isThundering(), rain = level.isRaining();
        for (ServerPlayer p : level.players()) {
            List<StormCellEntity> near = level.getEntitiesOfClass(StormCellEntity.class, new AABB(p.blockPosition()).inflate(500, 400, 500));
            if (!rain) {
                for (StormCellEntity c : near) c.dissipate();
                continue;
            }
            int want = storm ? 3 : 1;
            if (near.size() < want && r.nextInt(storm ? 2 : 6) == 0) spawnNear(level, p.position(), r, storm && r.nextInt(4) == 0, false);
        }
    }

    /**
     * 6.0: storm cloud bases sit well above the land (they were only 70-100 blocks over the ground, low enough to look
     * like fog banks from a hill): at least y=165 and 90+ blocks over the terrain, leaving room for the tower.
     */
    public static double cloudBase(ServerLevel level, int ground, RandomSource r) {
        return Math.min(level.getMaxY() - 70, Math.max(165, ground + 90) + r.nextDouble() * 20);
    }

    /** Spawns a cell 150–350 blocks from {@code around} (or right there if {@code here}). */
    public static StormCellEntity spawnNear(ServerLevel level, Vec3 around, RandomSource r, boolean severe, boolean here) {
        double a = r.nextDouble() * Math.PI * 2, d = here ? 0 : 150 + r.nextDouble() * 200;
        double x = around.x + Math.cos(a) * d, z = around.z + Math.sin(a) * d;
        int fx = (int) Math.floor(x), fz = (int) Math.floor(z);   // 6.0: 150-350 blocks out is often unloaded: don't load it to look
        int ground = Math.max(level.getSeaLevel(), level.hasChunk(fx >> 4, fz >> 4) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, fx, fz) : level.getSeaLevel());
        double base = cloudBase(level, ground, r);
        float depth = (float) Math.min(level.getMaxY() - 6 - base, (severe ? 150 : 90) + r.nextDouble() * 60);
        if (depth < 30) return null;
        StormCellEntity c = PrecipContent.STORM_CELL.create(level, EntitySpawnReason.EVENT);
        if (c == null) return null;
        double steer = Math.toRadians(com.stormreach.tornado.TornadoManager.steeringHeadingDeg());
        double speed = 0.06 + r.nextDouble() * 0.08;
        c.setup((float) ((severe ? 70 : 40) + r.nextDouble() * 40), depth, severe, (severe ? 12000 : 7200) + r.nextInt(4800),
                Math.cos(steer) * speed, Math.sin(steer) * speed);
        c.setPos(x, base, z);
        level.addFreshEntity(c);
        return c;
    }
}
