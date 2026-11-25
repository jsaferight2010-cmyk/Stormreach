package com.stormreach.labyrinth;

import com.stormreach.progress.StormAdvancements;
import com.stormreach.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks players inside Labyrinths. The Hollow Keeper is spawned only when someone enters (deep inside, away
 * from them) and removed when everyone has been gone for a minute, so it can never be met or seen outside.
 */
public final class LabyrinthTracker {
    private static final Map<Long, UUID> GUARDIANS = new HashMap<>();
    private static final Map<Long, Long> LAST_OCCUPIED = new HashMap<>();

    private LabyrinthTracker() {}

    private static boolean isLabyrinth(Holder<Structure> h) {
        return h.is(LabyrinthRegistry.STRUCTURE_KEY);
    }

    public static StructureStart at(ServerLevel level, BlockPos pos) {
        StructureStart s = level.structureManager().getStructureWithPieceAt(pos, LabyrinthTracker::isLabyrinth);
        return s != null && s.isValid() ? s : null;
    }

    public static void tick(ServerLevel level) {
        VaultDoor.tick(level);
        HidingTracker.tick(level);
        if (level.getGameTime() % 20 != 0) return;
        long now = level.getGameTime();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            StructureStart s = at(level, p.blockPosition());
            if (s == null) continue;
            long key = s.getChunkPos().pack();
            LAST_OCCUPIED.put(key, now);
            p.addEffect(new MobEffectInstance(ModEffects.LABYRINTH, 60, 0, true, false, false));
            StormAdvancements.award(p, "enter_labyrinth");
            UUID id = GUARDIANS.get(key);
            Entity g = id == null ? null : level.getEntity(id);
            if (g == null || !g.isAlive()) spawnGuardian(level, s, p, key);
        }
        // remove guardians of labyrinths nobody has been in for a minute
        GUARDIANS.entrySet().removeIf(e -> {
            if (now - LAST_OCCUPIED.getOrDefault(e.getKey(), 0L) < 1200) return false;
            Entity g = level.getEntity(e.getValue());
            if (g != null) g.discard();
            return g != null || now - LAST_OCCUPIED.getOrDefault(e.getKey(), 0L) > 6000;
        });
    }

    private static void spawnGuardian(ServerLevel level, StructureStart s, ServerPlayer p, long key) {
        BoundingBox box = s.getBoundingBox();
        BoundingBox maze = new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.minY() + LabyrinthStructure.HEIGHT, box.maxZ());
        HollowKeeper k = LabyrinthRegistry.HOLLOW_KEEPER.create(level, EntitySpawnReason.STRUCTURE);
        if (k == null) return;
        // spawn in the cell centre farthest from the player
        int bestX = box.minX() + 3, bestZ = box.minZ() + 3;
        double best = -1;
        for (int cx = 0; cx < LabyrinthStructure.SIZE; cx += 4) {
            for (int cz = 0; cz < LabyrinthStructure.SIZE; cz += 4) {
                int x = box.minX() + cx * LabyrinthStructure.CELL + 3, z = box.minZ() + cz * LabyrinthStructure.CELL + 3;
                double d = p.distanceToSqr(x, p.getY(), z);
                if (d > best) { best = d; bestX = x; bestZ = z; }
            }
        }
        k.setHome(maze);
        k.snapTo(bestX + 0.5, box.minY() + 1, bestZ + 0.5, 0, 0);
        level.addFreshEntity(k);
        GUARDIANS.put(key, k.getUUID());
    }

    public static HollowKeeper nearestGuardian(ServerLevel level, Vec3 pos, double r) {
        HollowKeeper best = null;
        double bd = r * r;
        for (HollowKeeper k : level.getEntitiesOfClass(HollowKeeper.class, new AABB(BlockPos.containing(pos)).inflate(r))) {
            double d = k.distanceToSqr(pos);
            if (d < bd) { bd = d; best = k; }
        }
        return best;
    }
}
