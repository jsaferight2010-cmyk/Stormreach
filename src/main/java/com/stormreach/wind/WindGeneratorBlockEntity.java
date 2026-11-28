package com.stormreach.wind;

import com.stormreach.chase.InterceptVehicle;
import com.stormreach.particle.ModParticles;
import com.stormreach.tornado.BlockWind;
import com.stormreach.tornado.TornadoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 6.0: the Creative Wind Generator. Blows a steady (or gusty) wind out of its front at a set speed and reach. The wind
 * is real to the rest of the mod: instruments read it, turbines spin in it, vanes swing to it and it shoves entities
 * with a force that grows with the square of the speed. With "damage" on it also tears off blocks the way a tornado
 * does, using each block's failure wind from {@link BlockWind}. Settings are synced to clients for the streaks.
 */
public class WindGeneratorBlockEntity extends BlockEntity {
    public static final int MAX_MPH = 320, MIN_RADIUS = 8, MAX_RADIUS = 64;

    private int mph = 40, radius = 24;
    private float gustiness = 0.3f;
    private boolean on = true, damage = false;

    public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(WindRegistry.GENERATOR_ENTITY, pos, state);
    }

    public int mph() { return mph; }
    public int radius() { return radius; }
    public float gustiness() { return gustiness; }
    public boolean on() { return on; }
    public boolean damage() { return damage; }

    /** Applies settings from the screen (clamped; the server never trusts the numbers). */
    public void configure(int mph, int radius, float gustiness, boolean on, boolean damage) {
        this.mph = Mth.clamp(mph, 0, MAX_MPH);
        this.radius = Mth.clamp(radius, MIN_RADIUS, MAX_RADIUS);
        this.gustiness = Mth.clamp(gustiness, 0, 1);
        this.on = on;
        this.damage = damage;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Wind speed this tick, with gusts: a slow swell plus quick puffs scaled by gustiness. */
    public double currentMph(long time) {
        if (!on || mph <= 0) return 0;
        double swell = Math.sin(time * 0.031 + worldPosition.hashCode()) * 0.5 + Math.sin(time * 0.173) * 0.5;
        return Math.max(0, mph * (1 + gustiness * 0.35 * swell));
    }

    Direction facing() { return getBlockState().getValue(WindGeneratorBlock.FACING); }

    static void serverTick(ServerLevel level, BlockPos pos, BlockState state, WindGeneratorBlockEntity be) {
        boolean powered = level.hasNeighborSignal(pos);   // redstone turns it off
        if (!be.on || be.mph <= 0 || powered) { WindSources.remove(level, pos); return; }
        Direction f = be.facing();
        double dx = f.getStepX(), dz = f.getStepZ();
        long now = level.getGameTime();
        double mph = be.currentMph(now);
        Vec3 mouth = Vec3.atCenterOf(pos).add(dx * 0.6, 0, dz * 0.6);
        WindSources.put(level, pos, new WindSources.Source(mouth, dx, dz, mph, be.radius, be.gustiness, now));
        be.push(level, mouth, dx, dz, mph);
        if (be.damage && now % 5 == 0 && mph > 40) be.tear(level, mouth, dx, dz, mph, level.getRandom());
    }

    /** Dynamic-pressure push on everything in the cone, limited so nothing goes faster than the air itself. */
    private void push(ServerLevel level, Vec3 mouth, double dx, double dz, double mph) {
        double r = radius;
        AABB box = new AABB(mouth, mouth.add(dx * r, 0, dz * r)).inflate(2 + r * 0.35, 2 + r * 0.35, 2 + r * 0.35);
        double airSpeed = mph * 0.44704 / 20.0;   // blocks per tick
        int n = 0;
        for (Entity e : level.getEntities((Entity) null, box, e -> !e.isSpectator() && !e.noPhysics)) {
            if (++n > 96) break;
            if (e instanceof InterceptVehicle iv && iv.isAnchored()) continue;
            if (e instanceof Player p && p.getAbilities().flying && p.isCreative() && mph < 60) continue;
            Vec3 c = e.getBoundingBox().getCenter();
            double s = WindSources.strength(c.x - mouth.x, c.y - mouth.y, c.z - mouth.z, dx, dz, radius);
            if (s <= 0) continue;
            double local = mph * s;
            double force = Math.min(0.32, local * local * 0.000012);
            if (e.onGround()) force *= e instanceof Player ? 0.55 : 0.7;     // footing helps
            if (e.isShiftKeyDown()) force *= 0.5;                            // so does crouching
            if (e.isInWater()) force *= 0.3;
            Vec3 v = e.getDeltaMovement();
            double along = v.x * dx + v.z * dz;
            double room = airSpeed * s - along;
            if (room <= 0) continue;
            double add = Math.min(force, room);
            double lift = local > 110 ? add * 0.35 : local > 70 && !e.onGround() ? add * 0.1 : 0;
            e.setDeltaMovement(v.add(dx * add, lift, dz * add));
            e.needsSync = true;
            if (e instanceof ServerPlayer sp) sp.resetFallDistance();
        }
    }

    /** With damage on: a few random blocks in the cone fail when the wind there beats their failure wind. */
    private void tear(ServerLevel level, Vec3 mouth, double dx, double dz, double mph, RandomSource rnd) {
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) return;
        for (int i = 0; i < 6; i++) {
            double along = 1 + rnd.nextDouble() * (radius - 1);
            double half = 1.5 + along * 0.35;
            double lat = (rnd.nextDouble() * 2 - 1) * half, up = (rnd.nextDouble() * 2 - 1) * half;
            BlockPos p = BlockPos.containing(mouth.x + dx * along - dz * lat, mouth.y + up, mouth.z + dz * along + dx * lat);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || p.equals(worldPosition)) continue;
            double local = mph * WindSources.strength(p.getX() + 0.5 - mouth.x, p.getY() + 0.5 - mouth.y, p.getZ() + 0.5 - mouth.z, dx, dz, radius);
            com.stormreach.sim.StructuralIntegrity.MaterialClass m = BlockWind.classify(level, p, s);
            if (m == com.stormreach.sim.StructuralIntegrity.MaterialClass.UNBREAKABLE) continue;
            if (local < BlockWind.failureMph(level, p, s, m)) continue;
            var debris = TornadoEntity.takeAsDebris(level, p, s);
            if (debris != null) debris.setDeltaMovement(dx * local * 0.006, 0.15, dz * local * 0.006);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel s) WindSources.remove(s, worldPosition);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("mph", mph);
        out.putInt("radius", radius);
        out.putFloat("gustiness", gustiness);
        out.putBoolean("on", on);
        out.putBoolean("damage", damage);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        mph = Mth.clamp(in.getIntOr("mph", 40), 0, MAX_MPH);
        radius = Mth.clamp(in.getIntOr("radius", 24), MIN_RADIUS, MAX_RADIUS);
        gustiness = Mth.clamp(in.getFloatOr("gustiness", 0.3f), 0, 1);
        on = in.getBooleanOr("on", true);
        damage = in.getBooleanOr("damage", false);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    /** Client: wind streaks and dust drifting down the cone so the wind can be seen. */
    static void clientTick(Level level, BlockPos pos, BlockState state, WindGeneratorBlockEntity be) {
        if (!be.on || be.mph <= 0 || level.hasNeighborSignal(pos)) return;
        Direction f = be.facing();
        double dx = f.getStepX(), dz = f.getStepZ();
        RandomSource rnd = level.getRandom();
        double mph = be.currentMph(level.getGameTime());
        int count = 1 + (int) Math.min(6, mph / 30);
        double speed = Math.min(2.5, mph * 0.44704 / 20.0);
        for (int i = 0; i < count; i++) {
            double along = rnd.nextDouble() * 2;
            double lat = (rnd.nextDouble() * 2 - 1) * 1.2, up = (rnd.nextDouble() * 2 - 1) * 1.2;
            double x = pos.getX() + 0.5 + dx * (0.7 + along) - dz * lat, y = pos.getY() + 0.5 + up, z = pos.getZ() + 0.5 + dz * (0.7 + along) + dx * lat;
            level.addParticle(ModParticles.WIND_STREAK, x, y, z, dx * speed, 0, dz * speed);
        }
    }
}
