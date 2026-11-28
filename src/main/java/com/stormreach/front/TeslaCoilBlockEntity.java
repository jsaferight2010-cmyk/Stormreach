package com.stormreach.front;

import com.stormreach.registry.ModSfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The coil's stored charge (0-500): a lightning strike nearby fills half, a Storm Cell half, a thunderstorm 1 a second. */
public class TeslaCoilBlockEntity extends BlockEntity implements com.stormreach.power.PowerNode {
    /** SU per point of charge when the coil is fed from a power network. */
    public static final int SU_PER_CHARGE = 20;
    public static final int MAX = 500, ZAP_COST = 10;
    public static final float ZAP_DAMAGE = 6.0f;
    private int charge;

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(FrontRegistry.TESLA_ENTITY, pos, state);
    }

    public int charge() { return charge; }

    @Override public Role role() { return Role.CONSUMER; }
    @Override public int stored() { return charge * SU_PER_CHARGE; }
    @Override public int capacity() { return MAX * SU_PER_CHARGE; }
    @Override public int insert(int amount) { int n = Math.min(amount / SU_PER_CHARGE, MAX - charge); if (n > 0) { charge += n; setChanged(); } return n * SU_PER_CHARGE; }
    @Override public int extract(int amount) { return 0; }

    public void addCharge(ServerLevel level, int amount) {
        charge = Math.min(MAX, charge + amount);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 30, 0.5, 0.2, 0.5, 0.3);
        level.playSound(null, worldPosition, ModSfx.CREATURE_EEL_ZAP, SoundSource.BLOCKS, 1.0f, 0.6f);
        sync(level);
    }

    private void sync(ServerLevel level) {
        setChanged();
        int vis = charge == 0 ? 0 : 1 + charge * 4 / MAX;
        BlockState s = getBlockState();
        if (s.getValue(TeslaCoilBlock.CHARGE) != vis) level.setBlock(worldPosition, s.setValue(TeslaCoilBlock.CHARGE, vis), Block.UPDATE_ALL);
        level.updateNeighbourForOutputSignal(worldPosition, s.getBlock());
    }

    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (level.isThundering() && level.canSeeSky(pos.above()) && charge < MAX) { charge++; setChanged(); }
        if (charge < ZAP_COST || level.hasNeighborSignal(pos)) { sync(level); return; }
        Vec3 top = Vec3.atCenterOf(pos).add(0, 0.9, 0);
        LivingEntity target = null;
        double best = 100;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(10), e -> e instanceof Enemy && e.isAlive())) {
            double d = e.distanceToSqr(top);
            if (d >= best) continue;
            Vec3 aim = e.getBoundingBox().getCenter();
            if (level.clip(new ClipContext(top, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.BLOCK) continue;
            best = d; target = e;
        }
        if (target == null) { sync(level); return; }
        Vec3 aim = target.getBoundingBox().getCenter();
        // a jagged arc from the toroid to the target
        Vec3 step = aim.subtract(top).scale(1.0 / 12);
        for (int i = 0; i <= 12; i++) {
            Vec3 p = top.add(step.scale(i));
            double j = i == 0 || i == 12 ? 0 : 0.25;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + (level.getRandom().nextDouble() - 0.5) * j, p.y + (level.getRandom().nextDouble() - 0.5) * j,
                    p.z + (level.getRandom().nextDouble() - 0.5) * j, 2, 0.02, 0.02, 0.02, 0.0);
        }
        target.hurtServer(level, level.damageSources().lightningBolt(), ZAP_DAMAGE);
        level.playSound(null, pos, ModSfx.CREATURE_EEL_ZAP, SoundSource.BLOCKS, 1.2f, 1.0f + level.getRandom().nextFloat() * 0.3f);
        charge -= ZAP_COST;
        sync(level);
    }

    /** Lightning struck at {@code hit}: every coil within 24 blocks soaks up half a charge. */
    public static void onLightning(ServerLevel level, BlockPos hit) {
        int cx = hit.getX() >> 4, cz = hit.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (!level.hasChunk(cx + dx, cz + dz)) continue;
            LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof TeslaCoilBlockEntity coil && be.getBlockPos().distSqr(hit) <= 24 * 24) coil.addCharge(level, MAX / 2);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Charge", charge);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        charge = in.getIntOr("Charge", 0);
    }
}
