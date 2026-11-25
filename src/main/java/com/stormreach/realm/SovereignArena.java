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
 */
public final class SovereignArena {
    private SovereignArena() {}

    public static final int RADIUS = 20, HEIGHT_ABOVE = 70, TIMEOUT = 15 * 60 * 20;
    private record Return(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot) {}
    private record Fight(ServerLevel level, BlockPos centre, UUID boss, long started) {}
    private static final Map<UUID, Return> RETURNS = new HashMap<>();
    private static final Map<UUID, BlockPos> IN_ARENA = new HashMap<>();
    private static final java.util.List<Fight> FIGHTS = new java.util.ArrayList<>();

    /** The arena centre (floor level) for an altar. */
    public static BlockPos centreFor(ServerLevel level, BlockPos altar) {
        int y = Math.min(level.getMaxY() - 24, altar.getY() + HEIGHT_ABOVE);
        return new BlockPos(altar.getX(), y, altar.getZ());
    }

    /** Builds the platform if it isn't there yet: a disc of tempest bricks with a rim, four pillars of charged stone. */
    public static void build(ServerLevel level, BlockPos c) {
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x += 16) for (int z = -RADIUS - 1; z <= RADIUS + 1; z += 16) level.getChunk(c.offset(x, 0, z)); // load first
        BlockState floor = block("tempest_bricks"), rim = block("chiseled_tempest_bricks"), wall = block("tempest_brick_wall"), stone = block("charged_stormstone");
        for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > RADIUS + 0.5) continue;
            BlockPos p = c.offset(x, 0, z);
            level.setBlock(p.below(), floor, 2);
            level.setBlock(p, d > RADIUS - 1.2 ? rim : floor, 2);
            for (int y = 1; y <= 6; y++) if (!level.getBlockState(p.above(y)).isAir()) level.setBlock(p.above(y), Blocks.AIR.defaultBlockState(), 2);
            if (d > RADIUS - 1.2) level.setBlock(p.above(), wall, 2);                     // a low parapet
        }
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            BlockPos p = c.offset((int) Math.round(Math.cos(a) * (RADIUS - 4)), 1, (int) Math.round(Math.sin(a) * (RADIUS - 4)));
            for (int y = 0; y < 5; y++) level.setBlock(p.above(y), y == 4 ? stone : rim, 2);
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
        BlockPos c = centreFor(level, altar);
        build(level, c);
        int n = 0;
        for (ServerPlayer p : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(altar)) < 32 * 32)) {
            double a = n++ * 0.9;
            BlockPos feet = c.offset((int) Math.round(Math.cos(a) * 12), 1, (int) Math.round(Math.sin(a) * 12));
            if (!safeSpot(level, feet)) feet = c.above();
            RETURNS.putIfAbsent(p.getUUID(), new Return(level.dimension(), p.position(), p.getYRot(), p.getXRot()));
            IN_ARENA.put(p.getUUID(), c);
            moveTo(p, level, Vec3.atBottomCenterOf(feet));
            p.sendSystemMessage(Component.translatable("block.stormreach.sovereign_altar.arena").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return Vec3.atBottomCenterOf(c.above(4));
    }

    public static void track(ServerLevel level, BlockPos centre, UUID boss) {
        FIGHTS.add(new Fight(level, centre, boss, level.getGameTime()));
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
            if (p.getY() < c.getY() - 12 || Math.hypot(p.getX() - c.getX() - 0.5, p.getZ() - c.getZ() - 0.5) > RADIUS + 24) {
                moveTo(p, level, Vec3.atBottomCenterOf(c.above()));   // fell off: back onto the platform
            }
        }
        for (Iterator<Fight> it = FIGHTS.iterator(); it.hasNext(); ) {
            Fight f = it.next();
            if (f.level() != level) continue;
            boolean bossGone = !(level.getEntity(f.boss()) instanceof net.minecraft.world.entity.LivingEntity le) || !le.isAlive();
            boolean timedOut = level.getGameTime() - f.started() > TIMEOUT;
            if (!bossGone && !timedOut) continue;
            if (bossGone && level.getGameTime() % 20 != 0) continue;
            it.remove();
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (f.centre().equals(IN_ARENA.get(p.getUUID()))) sendBack(level.getServer(), p);
            }
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
