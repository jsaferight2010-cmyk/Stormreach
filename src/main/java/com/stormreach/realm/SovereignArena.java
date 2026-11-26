package com.stormreach.realm;

import com.stormreach.StormreachConfig;
import com.stormreach.Stormreach;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Sovereign's arena (optional, sovereignArena in the properties file): a floating storm-stone platform high
 * above the spire. When the ritual ends, everyone near the altar is lifted up to it and the Sovereign descends there.
 *
 * <p>Teleporting is made safe:
 * <ul>
 * <li>the arena is built (and its chunks loaded) before anyone is moved, and arrivals land on solid floor with two
 *     blocks of air above, with Slow Falling and a moment of Resistance;</li>
 * <li>anyone who falls off is caught and put back on the platform instead of dying;</li>
 * <li>when the Sovereign dies (or after 15 minutes, or if it vanishes) everyone is taken back to exactly where they
 *     stood, and a player who logged out mid-fight is taken back when they return.</li>
 * </ul>
 * 6.0: the arena exists only for the fight: it is built when the ritual ends and taken down once everyone is back.
 * After a win there are a few seconds before the return, and the Tempest Eye waits over the altar below (it is no
 * longer dropped up here).</p>
 */
public final class SovereignArena {
    private SovereignArena() {}

    public static final int RADIUS = 20, HEIGHT_ABOVE = 70, TIMEOUT = 15 * 60 * 20;

    /**
     * 6.0: arena styles. The Sovereign's is a tempest-brick disc; Cyrrus's (the Hollow Shrine fight) is a wider ring of
     * storm-torn skyshale with broken pillars, open to the sky so its lightning and hail still fall.
     */
    public enum Style {
        SOVEREIGN(RADIUS, "tempest_bricks", "chiseled_tempest_bricks", "tempest_brick_wall", "charged_stormstone"),
        SHRINE(26, "skyshale_bricks", "chiseled_skyshale", "cobbled_skyshale", "charged_stormstone");
        final int radius; final String floor, rim, wall, crown;
        Style(int radius, String floor, String rim, String wall, String crown) { this.radius = radius; this.floor = floor; this.rim = rim; this.wall = wall; this.crown = crown; }
    }
    private record Return(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot) {}
    private record Fight(ServerLevel level, BlockPos centre, UUID boss, long started, Style style) {}
    private static final Map<UUID, Long> ENDED_AT = new HashMap<>();   // boss id -> when it was first seen gone
    private static final Map<UUID, Return> RETURNS = new HashMap<>();
    private static final Map<UUID, BlockPos> IN_ARENA = new HashMap<>();
    private static final java.util.List<Fight> FIGHTS = new java.util.ArrayList<>();

    /** The arena centre (floor level) for an altar. */
    public static BlockPos centreFor(ServerLevel level, BlockPos altar) {
        int y = Math.min(level.getMaxY() - 24, altar.getY() + HEIGHT_ABOVE);
        return new BlockPos(altar.getX(), y, altar.getZ());
    }

    /** Builds the platform if it isn't there yet: a disc of tempest bricks with a rim, four pillars of charged stone. */
    public static void build(ServerLevel level, BlockPos c) { build(level, c, Style.SOVEREIGN); }

    public static void build(ServerLevel level, BlockPos c, Style style) {
        int R = style.radius;
        for (int x = -R - 1; x <= R + 1; x += 16) for (int z = -R - 1; z <= R + 1; z += 16) level.getChunk(c.offset(x, 0, z)); // load first
        BlockState floor = block(style.floor), rim = block(style.rim), wall = block(style.wall), stone = block(style.crown);
        net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(c.asLong());
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > R + 0.5) continue;
            BlockPos p = c.offset(x, 0, z);
            level.setBlock(p.below(), floor, 2);
            level.setBlock(p, d > R - 1.2 ? rim : (style == Style.SHRINE && rnd.nextInt(7) == 0 ? rim : floor), 2);
            for (int y = 1; y <= 8; y++) if (!level.getBlockState(p.above(y)).isAir()) level.setBlock(p.above(y), Blocks.AIR.defaultBlockState(), 2);
            if (d > R - 1.2 && (style == Style.SOVEREIGN || rnd.nextInt(3) > 0)) level.setBlock(p.above(), wall, 2);   // a parapet (broken on the shrine's)
        }
        int pillars = style == Style.SHRINE ? 8 : 4;
        for (int k = 0; k < pillars; k++) {
            double a = k * Math.PI * 2 / pillars + Math.PI / 4;
            BlockPos p = c.offset((int) Math.round(Math.cos(a) * (R - 4)), 1, (int) Math.round(Math.sin(a) * (R - 4)));
            int h = style == Style.SHRINE ? 3 + rnd.nextInt(5) : 5;   // the shrine's pillars are storm-broken stumps
            for (int y = 0; y < h; y++) level.setBlock(p.above(y), y == h - 1 && style == Style.SOVEREIGN ? stone : rim, 2);
            if (style == Style.SHRINE) level.setBlock(p.above(h), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        }
    }

    /**
     * 6.0: the server is stopping mid-fight. Bring everyone in an arena back down, take the platforms away and send the
     * bosses home (a Sovereign or Cyrrus left floating over open sky would only fall). The ritual can be done again.
     */
    public static void endAllForShutdown(MinecraftServer server) {
        for (Fight f : new java.util.ArrayList<>(FIGHTS)) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) if (f.centre().equals(IN_ARENA.get(p.getUUID()))) sendBack(server, p);
            if (f.level().getEntity(f.boss()) instanceof net.minecraft.world.entity.Entity boss) boss.discard();
            demolish(f.level(), f.centre(), f.style());
        }
        FIGHTS.clear(); ENDED_AT.clear();
    }

    /** 6.0: forget everything from the world that just closed. */
    public static void resetForNewWorld() { FIGHTS.clear(); ENDED_AT.clear(); RETURNS.clear(); IN_ARENA.clear(); }

    /** Takes the platform down again (the same shape {@link #build} made), leaving open sky. */
    public static void demolish(ServerLevel level, BlockPos c) { demolish(level, c, Style.SOVEREIGN); }

    public static void demolish(ServerLevel level, BlockPos c, Style style) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int RADIUS = style.radius;
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
            if (Math.sqrt(x * x + z * z) > RADIUS + 0.5) continue;
            for (int y = -1; y <= 9; y++) {
                BlockPos p = c.offset(x, y, z);
                if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 2);
            }
        }
        for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(c).inflate(RADIUS + 2, 8, RADIUS + 2), e -> e instanceof net.minecraft.world.entity.item.ItemEntity
                        || e instanceof com.stormreach.entity.BallLightning || e instanceof net.minecraft.world.entity.ExperienceOrb)) {
            if (e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb) {
                // anything left lying on the platform comes down with the players instead of vanishing
                e.setPos(e.getX(), com.stormreach.weather.StormWeather.surfaceOr(level, e.blockPosition().below(2), 200).getY() + 1.0, e.getZ());
            } else e.discard();
        }
    }

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Stormreach.id(id)).defaultBlockState();
    }

    /** True if a player can stand here: solid floor, two blocks of air. */
    public static boolean safeSpot(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
    }

    /**
     * Starts the arena fight for this altar: builds the arena, lifts nearby players up to it and returns where the
     * Sovereign should appear. Returns null if the arena is turned off (the fight then happens at the altar).
     */
    public static Vec3 begin(ServerLevel level, BlockPos altar) {
        if (!StormreachConfig.sovereignArena) return null;
        return begin(level, altar, Style.SOVEREIGN);
    }

    /** Builds a {@code style} arena high over {@code altar}, lifts everyone within 32 blocks to it, returns the boss spot. */
    public static Vec3 begin(ServerLevel level, BlockPos altar, Style style) {
        BlockPos c = centreFor(level, altar);
        build(level, c, style);
        int n = 0;
        for (ServerPlayer p : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(altar)) < 32 * 32)) {
            double a = n++ * 0.9;
            BlockPos feet = c.offset((int) Math.round(Math.cos(a) * (style.radius - 8)), 1, (int) Math.round(Math.sin(a) * (style.radius - 8)));
            if (!safeSpot(level, feet)) feet = c.above();
            RETURNS.putIfAbsent(p.getUUID(), new Return(level.dimension(), p.position(), p.getYRot(), p.getXRot()));
            IN_ARENA.put(p.getUUID(), c);
            moveTo(p, level, Vec3.atBottomCenterOf(feet));
            p.sendSystemMessage(Component.translatable("block.stormreach.sovereign_altar.arena").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return Vec3.atBottomCenterOf(c.above(4));
    }

    public static void track(ServerLevel level, BlockPos centre, UUID boss) { track(level, centre, boss, Style.SOVEREIGN); }

    public static void track(ServerLevel level, BlockPos centre, UUID boss, Style style) {
        FIGHTS.add(new Fight(level, centre, boss, level.getGameTime(), style));
    }

    /** True while {@code centre} is a tracked arena (so its owner knows the fight is still on). */
    public static boolean active(BlockPos centre) {
        for (Fight f : FIGHTS) if (f.centre().equals(centre)) return true;
        return false;
    }

    private static void moveTo(ServerPlayer p, ServerLevel level, Vec3 to) {
        p.teleport(new TeleportTransition(level, to, Vec3.ZERO, p.getYRot(), p.getXRot(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, true, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 2, true, false, true));
    }

    /** Sends a player back to where they stood before the fight. */
    public static void sendBack(MinecraftServer server, ServerPlayer p) {
        Return r = RETURNS.remove(p.getUUID());
        IN_ARENA.remove(p.getUUID());
        if (r == null) return;
        ServerLevel to = server.getLevel(r.dim());
        if (to == null) to = server.overworld();
        p.teleport(new TeleportTransition(to, r.pos(), Vec3.ZERO, r.yRot(), r.xRot(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false, true));
        p.sendSystemMessage(Component.translatable("block.stormreach.sovereign_altar.arena_return").withStyle(ChatFormatting.AQUA));
    }

    /** Every tick: catch anyone falling off, end fights whose Sovereign is gone. */
    public static void tick(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            BlockPos c = IN_ARENA.get(p.getUUID());
            if (c == null || p.level() != level) continue;
            if (p.getY() < c.getY() - 12 || Math.hypot(p.getX() - c.getX() - 0.5, p.getZ() - c.getZ() - 0.5) > RADIUS + 30) {
                moveTo(p, level, Vec3.atBottomCenterOf(c.above()));   // fell off: back onto the platform
            }
        }
        for (Iterator<Fight> it = FIGHTS.iterator(); it.hasNext(); ) {
            Fight f = it.next();
            if (f.level() != level) continue;
            boolean bossGone = !(level.getEntity(f.boss()) instanceof net.minecraft.world.entity.LivingEntity le) || !le.isAlive();
            if (!bossGone && level.getEntity(f.boss()) instanceof net.minecraft.world.entity.LivingEntity le
                    && (le.getY() < f.centre().getY() - 12 || Math.hypot(le.getX() - f.centre().getX(), le.getZ() - f.centre().getZ()) > f.style().radius + 30))
                le.teleportTo(f.centre().getX() + 0.5, f.centre().getY() + 2, f.centre().getZ() + 0.5);   // a boss knocked off comes back too
            boolean timedOut = level.getGameTime() - f.started() > TIMEOUT;
            if (!bossGone && !timedOut) continue;
            if (bossGone) {   // a few seconds to take in the win before the trip back down
                long first = ENDED_AT.computeIfAbsent(f.boss(), k -> level.getGameTime());
                if (level.getGameTime() - first < 80) continue;
            }
            it.remove();
            ENDED_AT.remove(f.boss());
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (f.centre().equals(IN_ARENA.get(p.getUUID()))) {
                    sendBack(level.getServer(), p);
                    if (bossGone && f.style() == Style.SOVEREIGN)
                        p.sendSystemMessage(Component.translatable("block.stormreach.sovereign_altar.eye_waits").withStyle(ChatFormatting.AQUA));
                }
            }
            demolish(level, f.centre(), f.style());
        }
    }

    /** A player who left mid-fight goes home when they come back. */
    public static void onJoin(ServerPlayer p) {
        if (RETURNS.containsKey(p.getUUID())) {
            boolean ongoing = false;
            for (Fight f : FIGHTS) if (f.centre().equals(IN_ARENA.get(p.getUUID()))) ongoing = true;
            if (!ongoing) sendBack(p.level().getServer(), p);
        }
    }

    public static boolean inArena(ServerPlayer p) { return IN_ARENA.containsKey(p.getUUID()); }

    public static void init() {
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.JOIN.register(SovereignArena::onJoin);
    }
}
