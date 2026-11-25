package com.stormreach.story;

import com.stormreach.StormreachConfig;
import com.stormreach.progress.StormAdvancements;
import com.stormreach.registry.ModSounds;
import com.stormreach.sim.ShadowRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Decides when and where the Stranger appears, and leaves the subtle signs that it has been near: footsteps
 * behind you, a whisper, smoke at the edge of sight, a bare patch in a tree's leaves, a snuffed campfire,
 * or a small cairn of mossy stones that wasn't there before.
 */
public final class StrangerDirector {
    public static final Identifier SIGHTINGS = Identifier.fromNamespaceAndPath("stormreach", "stranger_sightings");

    private StrangerDirector() {}

    public static void init() {
        net.minecraft.core.Registry.register(BuiltInRegistries.CUSTOM_STAT, SIGHTINGS, SIGHTINGS);
    }

    public static int sightings(ServerPlayer p) {
        return p.getStats().getValue(Stats.CUSTOM.get(SIGHTINGS));
    }

    static void recordSighting(ServerPlayer p) {
        p.awardStat(SIGHTINGS);
        int n = sightings(p);
        StormAdvancements.award(p, "stranger_seen");
        if (n >= 5) StormAdvancements.award(p, "stranger_five");
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 600 != 0) return;
        if (level.dimension() != Level.OVERWORLD && !com.stormreach.realm.TempestRealm.isRealm(level)) return;
        double rate = StormreachConfig.shadowAppearanceRate / 100.0;
        RandomSource r = level.getRandom();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            boolean forest = level.getBiome(p.blockPosition()).is(BiomeTags.IS_FOREST) || level.getBiome(p.blockPosition()).is(BiomeTags.IS_TAIGA);
            long t = level.getOverworldClockTime() % 24000;
            boolean night = t > 13000 && t < 23000;
            boolean dark = level.getMaxLocalRawBrightness(p.blockPosition()) < 7;
            double chance = ShadowRules.appearanceChance(0.02 * rate, forest, level.isRaining(), night, dark, sightings(p));
            if (rate > 0 && r.nextDouble() < chance) appear(level, p, r);
            else if (StormreachConfig.shadowSigns && r.nextDouble() < 0.03 * rate) sign(level, p, r);
        }
    }

    private static void appear(ServerLevel level, ServerPlayer p, RandomSource r) {
        Vec3 look = p.getViewVector(1.0f);
        double base = Math.atan2(look.z, look.x);
        for (int attempt = 0; attempt < 8; attempt++) {
            // in the corner of your eye: 50-110 degrees off your view, 28-48 blocks away
            double side = (r.nextBoolean() ? 1 : -1) * Math.toRadians(50 + r.nextDouble() * 60);
            double d = 28 + r.nextDouble() * 20;
            int x = (int) (p.getX() + Math.cos(base + side) * d), z = (int) (p.getZ() + Math.sin(base + side) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isSolidRender() || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (Math.abs(y - p.getY()) > 12) continue;
            Stranger s = StoryRegistry.STRANGER.create(level, EntitySpawnReason.EVENT);
            if (s == null) return;
            s.snapTo(x + 0.5, y, z + 0.5, 0, 0);
            s.watch(p, 160 + r.nextInt(300));
            level.addFreshEntity(s);
            return;
        }
    }

    private static void sign(ServerLevel level, ServerPlayer p, RandomSource r) {
        int kind = r.nextInt(6);
        Vec3 look = p.getViewVector(1.0f);
        switch (kind) {
            case 0 -> { // footsteps behind you
                Vec3 behind = p.position().subtract(look.multiply(1, 0, 1).normalize().scale(10));
                for (int i = 0; i < 4; i++) level.playSound(null, BlockPos.containing(behind), com.stormreach.registry.ModSfx.BLOCK_STONE_TAP, SoundSource.AMBIENT, 0.5f, 0.8f);
            }
            case 1 -> level.playSound(null, p.blockPosition().offset(r.nextInt(21) - 10, 0, r.nextInt(21) - 10), ModSounds.STRANGER_WHISPER, SoundSource.AMBIENT, 0.25f, 0.9f);
            case 2 -> { // smoke at the edge of sight
                Vec3 at = p.position().add(look.yRot((float) Math.toRadians(70)).scale(30));
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.5, at.z, 12, 0.2, 0.8, 0.2, 0.0);
            }
            case 3 -> { // a bare patch in the leaves of a nearby tree
                BlockPos c = p.blockPosition().offset(r.nextInt(33) - 16, 4, r.nextInt(33) - 16);
                for (BlockPos q : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) {
                    if (level.getBlockState(q).is(BlockTags.LEAVES)) level.removeBlock(q, false);
                }
            }
            case 4 -> { // a campfire goes out
                for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-16, -4, -16), p.blockPosition().offset(16, 4, 16))) {
                    BlockState s = level.getBlockState(q);
                    if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) {
                        level.setBlockAndUpdate(q, s.setValue(CampfireBlock.LIT, false));
                        level.sendParticles(ParticleTypes.SMOKE, q.getX() + 0.5, q.getY() + 0.7, q.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.01);
                        break;
                    }
                }
            }
            default -> { // a small cairn appears out of sight
                double a = Math.atan2(look.z, look.x) + Math.PI + (r.nextDouble() - 0.5);
                int x = (int) (p.getX() + Math.cos(a) * 40), z = (int) (p.getZ() + Math.sin(a) * 40);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos.below()).is(BlockTags.DIRT) || level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                    level.setBlockAndUpdate(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                    level.setBlockAndUpdate(pos.above(), Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
                    level.setBlockAndUpdate(pos.above(2), Blocks.SOUL_TORCH.defaultBlockState());
                }
            }
        }
    }
}
