package com.stormreach.weather;

import com.stormreach.StormreachConfig;
import com.stormreach.entity.BallLightning;
import com.stormreach.entity.Squall;
import com.stormreach.entity.Waterspout;
import com.stormreach.registry.ModBiomes;
import com.stormreach.registry.ModBlocks;
import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModEntities;
import com.stormreach.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stormreach weather. Whenever it rains in the Overworld, the Stormreach biomes get a Gale (gusts of wind).
 * When it thunders, they get a full Thunderhead: strong wind, frequent lightning, sea spray, Squalls,
 * Ball Lightning, Waterspouts over the sea and hail in the Thunderspire Highlands.
 * Lightning reshapes the land: Stormstone grows crystals, sand fuses into Fulgurite.
 */
public final class StormWeather {
    public static final ResourceKey<DamageType> HAIL = ResourceKey.create(Registries.DAMAGE_TYPE,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("stormreach", "hail"));

    private static float windAngle = 0.0f;
    private static int nextGust = 60;
    private static boolean wasThundering = false;
    private static final Set<UUID> warned = new HashSet<>();

    private StormWeather() {}

    public static Vec3 windVector() {
        return new Vec3(Mth.cos(windAngle), 0.0, Mth.sin(windAngle));
    }

    public static Direction windDirection() {
        Vec3 w = windVector();
        return Direction.getApproximateNearest(w.x, 0.0, w.z);
    }

    public static void tick(ServerLevel level) {
        if (!StormreachConfig.stormWeather) return;
        if (level.dimension() != Level.OVERWORLD) {
            if (StormreachConfig.stormInOtherDimensions) otherDimensionTick(level);
            return;
        }
        boolean thunder = level.isThundering();
        if (thunder != wasThundering) {
            warned.clear();
            wasThundering = thunder;
        }
        RandomSource random = level.getRandom();
        windAngle += (random.nextFloat() - 0.5f) * 0.02f;
        if (!level.isRaining()) return;

        Vec3 windDir = windVector();
        boolean gust = --nextGust <= 0;
        if (gust) nextGust = 40 + random.nextInt(thunder ? 40 : 100);

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            Holder<Biome> biome = level.getBiome(player.blockPosition());
            if (!isStormZone(biome)) continue;
            boolean outside = level.canSeeSky(player.blockPosition().above());
            boolean grounded = player.hasEffect(ModEffects.GROUNDED);

            if (thunder && StormreachConfig.stormMessages && warned.add(player.getUUID())) {
                player.sendOverlayMessage(Component.translatable("message.stormreach.thunderhead").withStyle(ChatFormatting.AQUA));
            }

            if (outside && StormreachConfig.wind && gust && !player.isCreative() && !grounded) {
                double strength = StormreachConfig.windStrength / 100.0 * (thunder ? 0.55 : 0.3);
                if (player.isShiftKeyDown()) strength *= 0.3;
                if (player.isFallFlying() && StormreachConfig.galeWingsBoost
                        && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GALE_WINGS)) {
                    // Gale Wings catch the gust instead of fighting it.
                    Vec3 look = player.getLookAngle();
                    player.push(look.x * 0.9 + windDir.x * 0.3, 0.35, look.z * 0.9 + windDir.z * 0.3);
                    level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 0.4, 0.2, 0.4, 0.05);
                } else {
                    player.push(windDir.x * strength, 0.04, windDir.z * strength);
                }
                player.needsSync = true;
            }

            if (outside && StormreachConfig.stormParticles && level.getGameTime() % 2 == 0) {
                spray(level, player, windDir, thunder, random, biome.is(ModBiomes.THUNDERSPIRE_HIGHLANDS));
            }

            if (StormreachConfig.armorSetBonus && thunder && hasFullStormshard(player) && level.getGameTime() % 80 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0, true, false, true));
            }

            if (thunder && StormreachConfig.lightningFrequency > 0
                    && random.nextInt(4000) < StormreachConfig.lightningFrequency) {
                stormBolt(level, player, random);
            }

            if (thunder && level.getGameTime() % 100 == 0) {
                if (StormreachConfig.squallSpawning) trySpawnSquall(level, player, random);
                if (StormreachConfig.ballLightning && random.nextInt(3) == 0) trySpawnBallLightning(level, player, random);
                if (StormreachConfig.waterspouts && random.nextInt(4) == 0) trySpawnWaterspout(level, player, random);
            }

            // hail: real hailstones from a storm core. The Thunderspire Highlands are hail country; anywhere else a
            // thunderstorm occasionally drops some. Big hail needs a strong storm (see HailSize).
            if (thunder && StormreachConfig.hail && level.getGameTime() % 200 == 0 && !biome.value().coldEnoughToSnow(player.blockPosition(), level.getSeaLevel())) {
                boolean highlands = biome.is(ModBiomes.THUNDERSPIRE_HIGHLANDS);
                if (random.nextInt(highlands ? 3 : 25) == 0 && !com.stormreach.tornado.WeatherEvents.hailNear(level, player.position(), 120)) {
                    double sev = highlands ? 0.35 + random.nextDouble() * 0.5 : 0.1 + Math.pow(random.nextDouble(), 2) * 0.8;
                    Vec3 at = player.position().add((random.nextDouble() - 0.5) * 40, 0, (random.nextDouble() - 0.5) * 40);
                    com.stormreach.tornado.WeatherEvents.hail(level, at, sev);
                }
            }
            // downbursts: wet ones under thunderstorms; dry ones where rain evaporates before reaching the ground
            if (StormreachConfig.downbursts && level.isRaining() && level.getGameTime() % 200 == 100) {
                float temp = biome.value().getBaseTemperature();
                boolean dryClimate = temp >= 1.5f || !biome.value().hasPrecipitation();
                int odds = thunder ? 30 : dryClimate ? 45 : 0;
                if (odds > 0 && random.nextInt(odds) == 0 && !com.stormreach.tornado.WeatherEvents.downburstNear(level, player.position(), 150)) {
                    double a = random.nextDouble() * Math.PI * 2, d = 30 + random.nextDouble() * 60;
                    Vec3 at = player.position().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                    at = new Vec3(at.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z)), at.z);
                    com.stormreach.tornado.WeatherEvents.downburst(level, at, !dryClimate);
                }
            }
        }
    }

    public static boolean isStormZone(Holder<Biome> biome) {
        if (biome.is(ModBiomes.STORMREACH_COAST) || biome.is(ModBiomes.SQUALLWIND_BLUFFS)
                || biome.is(ModBiomes.THUNDERSPIRE_HIGHLANDS) || biome.is(ModBiomes.BRINEWATER_MARSH)
                || biome.is(ModBiomes.STORM_WASTES) || biome.is(ModBiomes.STORM_VOID)) return true;
        return StormreachConfig.stormsOnAllCoasts && (biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN));
    }

    public static boolean hasFullStormshard(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STORMSHARD_HELMET)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.STORMSHARD_CHESTPLATE)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.STORMSHARD_LEGGINGS)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.STORMSHARD_BOOTS);
    }

    /** True when lightning shouldn't be able to hurt this entity (full Stormshard set or Grounded). */
    public static boolean isLightningProof(LivingEntity entity) {
        return entity.hasEffect(ModEffects.GROUNDED) || (StormreachConfig.armorSetBonus && (hasFullStormshard(entity) || com.stormreach.realm.RealmGear.fullAurorite(entity)
                || entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(com.stormreach.story.StoryRegistry.STORMCROWN)
                || com.stormreach.sky.ChaserGear.fullSet(entity)));
    }

    private static void spray(ServerLevel level, ServerPlayer player, Vec3 windDir, boolean thunder, RandomSource random, boolean highlands) {
        for (int i = 0; i < (thunder ? 3 : 1); i++) {
            double x = player.getX() - windDir.x * 6 + (random.nextDouble() - 0.5) * 16;
            double y = player.getY() + random.nextDouble() * 4;
            double z = player.getZ() - windDir.z * 6 + (random.nextDouble() - 0.5) * 16;
            level.sendParticles(player, highlands && thunder ? ParticleTypes.SNOWFLAKE : ParticleTypes.CLOUD, false, false, x, y, z, 0,
                    windDir.x, -0.05, windDir.z, 0.35);
        }
        if (thunder && random.nextInt(6) == 0) {
            level.sendParticles(player, com.stormreach.particle.ModParticles.STORM_SPARK, false, false,
                    player.getX() + (random.nextDouble() - 0.5) * 10, player.getY() + 2 + random.nextDouble() * 3,
                    player.getZ() + (random.nextDouble() - 0.5) * 10, 4, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private static void stormBolt(ServerLevel level, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 12 + random.nextDouble() * 28;
        BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * dist, player.getY(), player.getZ() + Math.sin(angle) * dist);
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        if (!level.isLoaded(top) || !level.canSeeSky(top)) return;
        if (StormreachConfig.lightningHarmful) strike(level, top, null);
        else strikeVisual(level, top);
    }

    /** A real lightning bolt (sets fire, damages mobs). */
    public static void strike(ServerLevel level, BlockPos pos, ServerPlayer cause) {
        strike(level, pos, cause, false);
    }

    /** A real lightning bolt; with {@code fireless} it damages but never burns anything (see FirelessLightning). */
    public static void strike(ServerLevel level, BlockPos pos, ServerPlayer cause, boolean fireless) {
        if (pos == null) throw new IllegalArgumentException("lightning needs a position (use surfaceOr, which never returns null)");
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(Vec3.atBottomCenterOf(pos));
        if (cause != null) bolt.setCause(cause);
        if (fireless) FirelessLightning.mark(bolt);
        level.addFreshEntity(bolt);
    }

    /** Looks and sounds like lightning but does no damage. */
    public static void strikeVisual(ServerLevel level, BlockPos pos) {
        if (pos == null) throw new IllegalArgumentException("lightning needs a position (use surfaceOr, which never returns null)");
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(Vec3.atBottomCenterOf(pos));
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    /**
     * Called for every lightning bolt in the world (vanilla, tridents, ours) on its first tick.
     * Stormstone can become Storm Crystal Ore or Charged Stormstone; sand fuses into Fulgurite.
     */
    public static void onLightning(ServerLevel level, BlockPos hitBlock) {
        com.stormreach.realm.Stormgate.onLightning(level, hitBlock);
        com.stormreach.story.ShrineBlocks.onLightning(level, hitBlock);
        com.stormreach.sky.BottledLightningItem.onLightning(level, hitBlock);
        com.stormreach.front.TeslaCoilBlockEntity.onLightning(level, hitBlock);
        com.stormreach.power.StormCapacitorBlock.Entity.onLightning(level, hitBlock);
        BlockState state = level.getBlockState(hitBlock);
        RandomSource random = level.getRandom();
        net.minecraft.world.level.material.FluidState fluid = level.getFluidState(hitBlock.above());
        if (fluid.isEmpty()) fluid = level.getFluidState(hitBlock);
        if (fluid.getType().isSame(com.stormreach.registry.ModFluids.STORMWATER)) {
            com.stormreach.storm.StormPerks.electrify(level, hitBlock);
        } else if (fluid.getType().isSame(com.stormreach.registry.ModFluids.STORMLAVA) && StormreachConfig.ballLightning && random.nextInt(3) == 0) {
            spawnAt(level, ModEntities.BALL_LIGHTNING.create(level, EntitySpawnReason.EVENT), hitBlock.above(2), random);
        }
        if (state.is(ModBlocks.STORMHEART)) {
            com.stormreach.block.StormheartBlock.activate(level, hitBlock, state);
            return;
        }
        if (StormreachConfig.oreGeneration && state.is(ModBlocks.STORMSTONE)) {
            level.setBlockAndUpdate(hitBlock, (random.nextInt(3) == 0 ? ModBlocks.STORM_CRYSTAL_ORE : ModBlocks.CHARGED_STORMSTONE).defaultBlockState());
            sparks(level, hitBlock);
        } else if (StormreachConfig.fulguriteFromLightning && state.is(BlockTags.SAND)) {
            // Fuse a small column of sand into glassy fulgurite.
            for (int dy = 0; dy < 3; dy++) {
                BlockPos p = hitBlock.below(dy);
                if (level.getBlockState(p).is(BlockTags.SAND) && random.nextInt(dy + 1) == 0) {
                    level.setBlockAndUpdate(p, ModBlocks.FULGURITE.defaultBlockState());
                }
            }
            sparks(level, hitBlock);
        }
    }

    private static void sparks(ServerLevel level, BlockPos pos) {
        level.sendParticles(com.stormreach.particle.ModParticles.STORM_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.4, 0.2, 0.4, 0.2);
    }

    private static BlockPos randomGroundNear(ServerLevel level, Player player, RandomSource random, double min, double extra) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = min + random.nextDouble() * extra;
        BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * dist, player.getY(), player.getZ() + Math.sin(angle) * dist);
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
    }

    private static void trySpawnSquall(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (level.getEntitiesOfClass(Squall.class, player.getBoundingBox().inflate(48.0)).size() >= StormreachConfig.maxSquallsPerPlayer) return;
        BlockPos ground = randomGroundNear(level, player, random, 16, 16);
        if (!level.isLoaded(ground) || !level.canSeeSky(ground) || !isStormZone(level.getBiome(ground))) return;
        spawnAt(level, ModEntities.SQUALL.create(level, EntitySpawnReason.EVENT), ground.above(3 + random.nextInt(4)), random);
    }

    private static void trySpawnBallLightning(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (level.getEntitiesOfClass(BallLightning.class, player.getBoundingBox().inflate(40.0)).size() >= 3) return;
        BlockPos ground = randomGroundNear(level, player, random, 14, 12);
        if (!level.isLoaded(ground) || !level.canSeeSky(ground)) return;
        spawnAt(level, ModEntities.BALL_LIGHTNING.create(level, EntitySpawnReason.EVENT), ground.above(2 + random.nextInt(3)), random);
    }

    private static void trySpawnWaterspout(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (!level.getEntitiesOfClass(Waterspout.class, player.getBoundingBox().inflate(64.0)).isEmpty()) return;
        BlockPos surface = randomGroundNear(level, player, random, 20, 20);
        if (!level.isLoaded(surface)) return;
        BlockPos water = surface.below();
        if (!level.getFluidState(water).is(Fluids.WATER) || !level.getBiome(water).is(BiomeTags.IS_OCEAN) && !isStormZone(level.getBiome(water))) return;
        spawnAt(level, ModEntities.WATERSPOUT.create(level, EntitySpawnReason.EVENT), surface, random);
    }

    private static void spawnAt(ServerLevel level, Mob mob, BlockPos pos, RandomSource random) {
        if (mob == null) return;
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.5, mob.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * The Nether and the End have no weather, so their storm biomes (made by the Stormblight) have a
     * permanent dry storm instead: crackling sparks, lightning now and then, Ball Lightning and Squalls.
     */
    private static void otherDimensionTick(ServerLevel level) {
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            Holder<Biome> biome = level.getBiome(player.blockPosition());
            if (!biome.is(ModBiomes.STORM_WASTES) && !biome.is(ModBiomes.STORM_VOID)) continue;
            if (StormreachConfig.stormParticles && level.getGameTime() % 3 == 0) {
                level.sendParticles(player, com.stormreach.particle.ModParticles.STORM_SPARK, false, false,
                        player.getX() + (random.nextDouble() - 0.5) * 14, player.getY() + random.nextDouble() * 5,
                        player.getZ() + (random.nextDouble() - 0.5) * 14, 3, 0.3, 0.3, 0.3, 0.05);
            }
            if (StormreachConfig.lightningFrequency > 0 && random.nextInt(8000) < StormreachConfig.lightningFrequency) {
                BlockPos at = BlockPos.containing(player.getX() + random.nextInt(41) - 20, player.getY(), player.getZ() + random.nextInt(41) - 20);
                BlockPos ground = surfaceBelow(level, at.above(6), 24);
                if (ground != null) {
                    if (StormreachConfig.lightningHarmful) strike(level, ground.above(), null);
                    else strikeVisual(level, ground.above());
                }
            }
            if (level.getGameTime() % 200 == 0) {
                BlockPos at = BlockPos.containing(player.getX() + random.nextInt(33) - 16, player.getY() + 3, player.getZ() + random.nextInt(33) - 16);
                if (level.isEmptyBlock(at)) {
                    if (StormreachConfig.ballLightning && random.nextBoolean()
                            && level.getEntitiesOfClass(BallLightning.class, player.getBoundingBox().inflate(40.0)).size() < 3) {
                        spawnAt(level, ModEntities.BALL_LIGHTNING.create(level, EntitySpawnReason.EVENT), at, random);
                    } else if (StormreachConfig.squallSpawning
                            && level.getEntitiesOfClass(Squall.class, player.getBoundingBox().inflate(48.0)).size() < StormreachConfig.maxSquallsPerPlayer) {
                        spawnAt(level, ModEntities.SQUALL.create(level, EntitySpawnReason.EVENT), at, random);
                    }
                }
            }
        }
    }

    /** First solid block with air above it, scanning down from {@code start}; works under the Nether roof. */
    public static BlockPos surfaceBelow(ServerLevel level, BlockPos start, int maxDepth) {
        BlockPos.MutableBlockPos p = start.mutable();
        for (int i = 0; i < maxDepth && p.getY() > level.getMinY(); i++) {
            p.move(Direction.DOWN);
            if (!level.isLoaded(p)) return null;
            if (!level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p.immutable();
        }
        return null;
    }

    /**
     * Where lightning aimed at {@code start} should land: the first solid surface below it within {@code maxDepth},
     * otherwise {@code start} itself (the bolt then strikes in mid-air over a drop or open water). Never null.
     */
    public static BlockPos surfaceOr(ServerLevel level, BlockPos start, int maxDepth) {
        BlockPos ground = surfaceBelow(level, start, maxDepth);
        return ground != null ? ground : start;
    }

    /** One-line forecast for /stormreach forecast. */
    public static Component forecast(ServerLevel level) {
        String now = level.isThundering() ? "thunderhead" : level.isRaining() ? "gale" : "clear";
        return Component.translatable("command.stormreach.forecast",
                Component.translatable("command.stormreach.weather." + now),
                Component.translatable("command.stormreach.wind." + windDirection().getSerializedName()));
    }
}
