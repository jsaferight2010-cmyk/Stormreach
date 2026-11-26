package com.stormreach.weather.precip;

import com.stormreach.Stormreach;
import com.stormreach.sim.HailSize;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 6.0: hail without entities. 5.x spawned up to 600 synced HailstoneEntities, each raycasting every tick and
 * streaming position packets to every client, which is what made hailstorms lag. Now the server only keeps a list of
 * scheduled impacts (where and when each stone lands, from its size's terminal speed and the drift) and resolves them
 * when they are due: one heightmap lookup and one small box query per stone, nothing synced. Clients draw the falling
 * stones themselves as particles from a once-a-second description of the hail field ({@link FieldPayload}).
 */
public final class HailSim {
    private HailSim() {}

    /** Most impacts kept per level (anything beyond is simply not simulated; the visuals are client-side anyway). */
    public static final int MAX_PENDING = 2000;

    record Stone(double x, double z, HailSize size, long landAt) {}

    private static final java.util.Map<ServerLevel, List<Stone>> PENDING = new java.util.WeakHashMap<>();

    public static int pending(ServerLevel level) {
        List<Stone> l = PENDING.get(level);
        return l == null ? 0 : l.size();
    }

    /**
     * Schedules one stone released at (x, fromY, z), drifting (dx, dz) blocks per tick. Returns false at the cap or when
     * the column is not loaded. Landing is computed now, so it costs nothing while it "falls".
     */
    public static boolean schedule(ServerLevel level, double x, double fromY, double z, HailSize size, double dx, double dz) {
        List<Stone> list = PENDING.computeIfAbsent(level, k -> new ArrayList<>());
        if (list.size() >= MAX_PENDING) return false;
        double vt = size.terminalMs() * 0.05;                     // blocks per tick
        int gx = Mth.floor(x), gz = Mth.floor(z);
        if (!level.hasChunkAt(new BlockPos(gx, 0, gz))) return false;
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz);
        double fall = Math.max(1, fromY - top);
        int ticks = (int) Math.ceil(fall / vt);
        list.add(new Stone(x + dx * ticks, z + dz * ticks, size, level.getGameTime() + ticks));
        return true;
    }

    /** 6.0 (Dispel Weather): drops every hailstone still falling and tells clients the hail field is gone. */
    public static int clear(ServerLevel level) {
        List<Stone> list = PENDING.remove(level);
        FieldPayload none = new FieldPayload(0, 0, 0, 0, 0, 0, 0);
        for (ServerPlayer pl : level.players()) if (ServerPlayNetworking.canSend(pl, FieldPayload.TYPE)) ServerPlayNetworking.send(pl, none);
        return list == null ? 0 : list.size();
    }

    public static void tick(ServerLevel level) {
        List<Stone> list = PENDING.get(level);
        if (list == null || list.isEmpty()) return;
        long now = level.getGameTime();
        int budget = 400;                                       // impacts resolved per tick, at most
        for (Iterator<Stone> it = list.iterator(); it.hasNext() && budget > 0; ) {
            Stone s = it.next();
            if (s.landAt > now) continue;
            it.remove();
            budget--;
            land(level, s);
        }
    }

    private static void land(ServerLevel level, Stone s) {
        int gx = Mth.floor(s.x), gz = Mth.floor(s.z);
        if (!level.hasChunkAt(new BlockPos(gx, 0, gz))) return;
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz);
        // anything alive standing in the open column where it comes down (a roof over you shields you)
        AABB column = new AABB(s.x - 0.35, top, s.z - 0.35, s.x + 0.35, top + 2.6, s.z + 0.35);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, column, e -> e.isAlive() && !e.isSpectator())) {
            if (e instanceof Player p && p.isCreative()) continue;
            HailstoneEntity.strikeEntity(level, e, s.size, new Vec3(s.x, e.getY() + e.getBbHeight(), s.z));
            return;
        }
        BlockPos ground = new BlockPos(gx, top - 1, gz);
        boolean showy = s.size.ordinal() >= HailSize.QUARTER.ordinal() || level.getRandom().nextInt(4) == 0;
        HailstoneEntity.strikeBlock(level, ground, Direction.UP, new Vec3(s.x, top, s.z), s.size, showy);
    }

    // ------------------------------------------------------------------ the hail field, for client visuals

    /** Where hail is falling: centre, radius, severity (size mix), drift and how long this description holds. */
    public record FieldPayload(double x, double z, float radius, float severity, float driftX, float driftZ, int ttl) implements CustomPacketPayload {
        public static final Type<FieldPayload> TYPE = new Type<>(Stormreach.id("hail_field"));
        public static final StreamCodec<FriendlyByteBuf, FieldPayload> CODEC = StreamCodec.ofMember((p, b) -> {
            b.writeDouble(p.x); b.writeDouble(p.z); b.writeFloat(p.radius); b.writeFloat(p.severity); b.writeFloat(p.driftX); b.writeFloat(p.driftZ); b.writeVarInt(p.ttl);
        }, b -> new FieldPayload(b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(FieldPayload.TYPE, FieldPayload.CODEC);
    }

    /** Tells players near a hail core what is falling there (sent about once a second by the hail event). */
    public static void broadcast(ServerLevel level, Vec3 centre, double radius, double severity, double driftX, double driftZ) {
        FieldPayload p = new FieldPayload(centre.x, centre.z, (float) radius, (float) severity, (float) driftX, (float) driftZ, 30);
        for (ServerPlayer pl : level.players())
            if (Math.hypot(pl.getX() - centre.x, pl.getZ() - centre.z) < radius + 96 && ServerPlayNetworking.canSend(pl, FieldPayload.TYPE))
                ServerPlayNetworking.send(pl, p);
    }
}
