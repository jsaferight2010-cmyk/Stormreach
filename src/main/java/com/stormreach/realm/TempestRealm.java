package com.stormreach.realm;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** The Tempest Realm dimension: travel through Stormgates, falling back to the Overworld, and the realm's storm. */
public final class TempestRealm {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Stormreach.id("tempest_realm"));
    private static final Set<Entity> PENDING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final int FALL_OUT_DEPTH = 8;

    private TempestRealm() {}

    public static boolean isRealm(Level level) {
        return level.dimension() == LEVEL;
    }

    /** Portal blocks call this from inside an entity's movement; the actual jump happens at the end of the level tick. */
    public static void queueTravel(Entity entity) {
        entity.setPortalCooldown(200);
        PENDING.add(entity);
    }

    /** Runs at the end of every level tick. */
    public static void tick(ServerLevel level) {
        if (!PENDING.isEmpty()) {
            List<Entity> now = new ArrayList<>();
            for (Entity e : PENDING) if (e.level() == level) now.add(e);
            for (Entity e : now) {
                PENDING.remove(e);
                if (e.isAlive() && !e.isRemoved()) {
                    try { travel(level, e); } catch (RuntimeException ex) { Stormreach.LOGGER.error("Stormgate travel failed", ex); }
                }
            }
        }
        if (!isRealm(level)) return;
        RandomSource random = level.getRandom();
        for (ServerPlayer player : new ArrayList<>(level.players())) {
            if (StormreachConfig.realmFallToOverworld && player.getY() < level.getMinY() - FALL_OUT_DEPTH && !player.isSpectator()) {
                fallOut(level, player);
                continue;
            }
            if (StormreachConfig.realmLightning && random.nextInt(500) == 0) {
                BlockPos at = player.blockPosition().offset(random.nextInt(81) - 40, 0, random.nextInt(81) - 40);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, at);
                if (top.getY() > level.getMinY() + 1) {
                    if (random.nextInt(4) == 0) StormWeather.strike(level, top, null);
                    else StormWeather.strikeVisual(level, top);
                } else {
                    level.playSound(null, player.blockPosition(), com.stormreach.registry.ModSfx.WEATHER_THUNDER, SoundSource.WEATHER, 2.0f, 0.6f + random.nextFloat() * 0.3f);
                }
            }
            if (random.nextInt(8) == 0) {
                level.sendParticles(player, com.stormreach.particle.ModParticles.STORM_SPARK, false, false,
                        player.getX() + random.nextGaussian() * 8, player.getY() + random.nextDouble() * 6, player.getZ() + random.nextGaussian() * 8,
                        1, 0, 0, 0, 0.05);
            }
        }
    }

    private static void travel(ServerLevel from, Entity entity) {
        boolean goingIn = !isRealm(from);
        ServerLevel to = goingIn ? from.getServer().getLevel(LEVEL) : from.getServer().overworld();
        if (to == null) {
            Stormreach.LOGGER.warn("The Tempest Realm dimension is missing (is a datapack overriding it?)");
            return;
        }
        int x = entity.getBlockX(), z = entity.getBlockZ();
        BlockPos gate = findGate(to, x, z);
        if (gate == null && goingIn && StormreachConfig.returnGateBuilt) {
            gate = gateSiteFor(to, x, z);
            Stormgate.build(to, gate);
        }
        Vec3 dest;
        if (gate != null) {
            dest = safeSpot(to, gate.offset(3, 0, 0));
        } else {
            BlockPos top = to.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            dest = Vec3.atBottomCenterOf(top.getY() <= to.getMinY() + 1 ? new BlockPos(x, 100, z) : top);
        }
        Entity moved = entity.teleport(new TeleportTransition(to, dest, Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));
        if (moved != null) {
            moved.setPortalCooldown(300);
            if (moved instanceof ServerPlayer p && goingIn) {
                p.sendSystemMessage(Component.translatable("dimension.stormreach.tempest_realm.enter").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private static void fallOut(ServerLevel realm, ServerPlayer player) {
        ServerLevel overworld = realm.getServer().overworld();
        Vec3 dest = new Vec3(player.getX(), overworld.getMaxY() - 8, player.getZ());
        ServerPlayer moved = (ServerPlayer) player.teleport(new TeleportTransition(overworld, dest, Vec3.ZERO, player.getYRot(), player.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved != null) {
            moved.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30 * 20, 0));
            moved.resetFallDistance();
            moved.sendSystemMessage(Component.translatable("dimension.stormreach.tempest_realm.fell").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Looks for an open Stormgate near x/z. Returns its centre. */
    private static BlockPos findGate(ServerLevel level, int x, int z) {
        int r = 20;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = (x - r) >> 4; cx <= (x + r) >> 4; cx++) {
            for (int cz = (z - r) >> 4; cz <= (z + r) >> 4; cz++) {
                level.getChunk(cx, cz); // make sure it exists
            }
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x + dx, z + dz);
                for (int y = Math.min(top, level.getMaxY()); y >= level.getMinY(); y--) {
                    m.set(x + dx, y, z + dz);
                    if (level.getBlockState(m).is(RealmBlocks.STORMGATE)) {
                        double d = dx * dx + dz * dz;
                        if (d < bestDist && Stormgate.check(level, m).open()) {
                            best = m.immutable();
                            bestDist = d;
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }

    /** Where to build a return gate: on the island below, or on a platform in the sky if there's only void. */
    private static BlockPos gateSiteFor(ServerLevel realm, int x, int z) {
        realm.getChunk(x >> 4, z >> 4);
        int top = realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = top <= realm.getMinY() + 1 ? 120 : top;
        return new BlockPos(x, Math.min(y, realm.getMaxY() - 8), z);
    }

    private static Vec3 safeSpot(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos p = start.mutable();
        for (int i = 0; i < 16; i++) {
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) break;
            p.move(0, 1, 0);
        }
        return Vec3.atBottomCenterOf(p);
    }

    public static boolean protectFromFall(LivingEntity entity) {
        return isRealm(entity.level());
    }
}
