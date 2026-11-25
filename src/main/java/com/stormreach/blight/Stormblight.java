package com.stormreach.blight;

import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModBiomes;
import com.stormreach.registry.ModBlocks;
import com.stormreach.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Stormblight: storm energy soaking into the land. It turns vanilla blocks into their Stormreach
 * counterparts (stone into stormstone, trees into stormpine, grass into stormscar...) and mobs into
 * Stormreach mobs (chickens into gulls, zombies into brine wraiths, creepers into ball lightning...).
 * Block properties such as facing, axis, slab type and waterlogging are kept.
 */
public final class Stormblight {
    private static Map<Block, Block> blocks;
    private static Map<EntityType<?>, EntityType<? extends Mob>> mobs;

    private Stormblight() {}

    private static Map<Block, Block> blocks() {
        if (blocks == null) {
            Map<Block, Block> m = new HashMap<>();
            for (Block b : List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE,
                    Blocks.GRANITE, Blocks.TUFF, Blocks.CALCITE)) m.put(b, ModBlocks.STORMSTONE);
            for (Block b : List.of(Blocks.POLISHED_ANDESITE, Blocks.POLISHED_DIORITE, Blocks.POLISHED_GRANITE, Blocks.SMOOTH_STONE))
                m.put(b, ModBlocks.POLISHED_STORMSTONE);
            m.put(Blocks.STONE_BRICKS, ModBlocks.STORMSTONE_BRICKS);
            m.put(Blocks.MOSSY_STONE_BRICKS, ModBlocks.STORMSTONE_BRICKS);
            m.put(Blocks.CRACKED_STONE_BRICKS, ModBlocks.CRACKED_STORMSTONE_BRICKS);
            m.put(Blocks.CHISELED_STONE_BRICKS, ModBlocks.CHISELED_STORMSTONE);
            m.put(Blocks.STONE_STAIRS, ModBlocks.STORMSTONE_STAIRS);
            m.put(Blocks.COBBLESTONE_STAIRS, ModBlocks.STORMSTONE_STAIRS);
            m.put(Blocks.STONE_BRICK_STAIRS, ModBlocks.STORMSTONE_BRICK_STAIRS);
            m.put(Blocks.STONE_SLAB, ModBlocks.STORMSTONE_SLAB);
            m.put(Blocks.COBBLESTONE_SLAB, ModBlocks.STORMSTONE_SLAB);
            m.put(Blocks.STONE_BRICK_SLAB, ModBlocks.STORMSTONE_BRICK_SLAB);
            m.put(Blocks.COBBLESTONE_WALL, ModBlocks.STORMSTONE_WALL);
            m.put(Blocks.STONE_BRICK_WALL, ModBlocks.STORMSTONE_BRICK_WALL);
            m.put(Blocks.GRASS_BLOCK, ModBlocks.STORMSCAR_TURF);
            m.put(Blocks.PODZOL, ModBlocks.STORMSCAR_TURF);
            m.put(Blocks.MYCELIUM, ModBlocks.STORMSCAR_TURF);
            m.put(Blocks.GLASS, ModBlocks.STORMGLASS);
            m.put(Blocks.GLASS_PANE, ModBlocks.STORMGLASS_PANE);
            m.put(Blocks.LANTERN, ModBlocks.FULGURITE_LANTERN);
            m.put(Blocks.GLOWSTONE, ModBlocks.STORM_LANTERN);
            m.put(Blocks.SEA_LANTERN, ModBlocks.STORM_LANTERN);
            m.put(Blocks.AMETHYST_BLOCK, ModBlocks.STORM_CRYSTAL_BLOCK);
            m.put(Blocks.COAL_ORE, ModBlocks.STORM_CRYSTAL_ORE);
            m.put(Blocks.COPPER_ORE, ModBlocks.STORM_CRYSTAL_ORE);
            m.put(Blocks.SHORT_GRASS, ModBlocks.SALTGRASS);
            m.put(Blocks.FERN, ModBlocks.SALTGRASS);
            m.put(Blocks.SWEET_BERRY_BUSH, ModBlocks.STORMBERRY_SHRUB);
            m.put(Blocks.WATER, com.stormreach.registry.ModFluids.STORMWATER_BLOCK);
            m.put(Blocks.LAVA, com.stormreach.registry.ModFluids.STORMLAVA_BLOCK);
            blocks = m;
        }
        return blocks;
    }

    private static Map<EntityType<?>, EntityType<? extends Mob>> mobs() {
        if (mobs == null) {
            Map<EntityType<?>, EntityType<? extends Mob>> m = new HashMap<>();
            m.put(EntityTypes.CHICKEN, ModEntities.STORM_GULL);
            m.put(EntityTypes.PARROT, ModEntities.STORM_GULL);
            m.put(EntityTypes.RABBIT, ModEntities.CLIFF_CRAB);
            m.put(EntityTypes.TURTLE, ModEntities.CLIFF_CRAB);
            m.put(EntityTypes.PIG, ModEntities.HARBOR_SEAL);
            m.put(EntityTypes.ZOMBIE, ModEntities.BRINE_WRAITH);
            m.put(EntityTypes.HUSK, ModEntities.BRINE_WRAITH);
            m.put(EntityTypes.DROWNED, ModEntities.BRINE_WRAITH);
            m.put(EntityTypes.CREEPER, ModEntities.BALL_LIGHTNING);
            m.put(EntityTypes.SPIDER, ModEntities.THUNDER_CRAB);
            m.put(EntityTypes.PHANTOM, ModEntities.SQUALL);
            m.put(EntityTypes.VEX, ModEntities.SQUALL);
            m.put(EntityTypes.BREEZE, ModEntities.SQUALL);
            m.put(ModEntities.CLIFF_CRAB, ModEntities.THUNDER_CRAB);
            mobs = m;
        }
        return mobs;
    }

    /** Turns the block at pos into its Stormreach counterpart, if it has one. Returns true if it changed. */
    public static boolean convertBlock(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState from = level.getBlockState(pos);
        if (from.isAir() || BuiltInRegistries.BLOCK.getKey(from.getBlock()).getNamespace().equals("stormreach")) return false;
        Block to = target(from, random);
        if (to == null) return false;
        place(level, pos, copyProperties(from, to));
        // Doors are two blocks: convert the other half in the same step so neither half pops off.
        if (from.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos other = from.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(from.getBlock())) place(level, other, copyProperties(otherState, to));
        }
        if (random.nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.05);
        }
        return true;
    }

    private static Block target(BlockState from, RandomSource random) {
        Block direct = blocks().get(from.getBlock());
        if (direct != null) return direct;
        if (from.is(BlockTags.LOGS)) {
            String name = BuiltInRegistries.BLOCK.getKey(from.getBlock()).getPath();
            if (name.contains("crimson") || name.contains("warped")) return null;
            return name.startsWith("stripped") ? ModBlocks.STRIPPED_STORMPINE_LOG : ModBlocks.STORMPINE_LOG;
        }
        if (from.is(BlockTags.LEAVES)) return ModBlocks.STORMPINE_LEAVES;
        if (from.is(BlockTags.SAPLINGS)) return ModBlocks.STORMPINE_SAPLING;
        if (from.is(BlockTags.PLANKS)) return ModBlocks.STORMPINE_PLANKS;
        if (from.is(BlockTags.WOODEN_STAIRS)) return ModBlocks.STORMPINE_STAIRS;
        if (from.is(BlockTags.WOODEN_SLABS)) return ModBlocks.STORMPINE_SLAB;
        if (from.is(BlockTags.WOODEN_FENCES)) return ModBlocks.STORMPINE_FENCE;
        if (from.is(BlockTags.FENCE_GATES)) return ModBlocks.STORMPINE_FENCE_GATE;
        if (from.is(BlockTags.WOODEN_DOORS)) return ModBlocks.STORMPINE_DOOR;
        if (from.is(BlockTags.WOODEN_TRAPDOORS)) return ModBlocks.STORMPINE_TRAPDOOR;
        if (from.is(BlockTags.WOODEN_BUTTONS)) return ModBlocks.STORMPINE_BUTTON;
        if (from.is(BlockTags.WOODEN_PRESSURE_PLATES)) return ModBlocks.STORMPINE_PRESSURE_PLATE;
        if (from.is(BlockTags.SMALL_FLOWERS)) return random.nextBoolean() ? ModBlocks.SEA_THRIFT : ModBlocks.SEA_ROCKET;
        if (from.is(BlockTags.SAND) && random.nextInt(10) == 0) return ModBlocks.FULGURITE;
        // Everything else: the generated storm counterpart, if there is one.
        return StormreachConfig.useGeneratedBlocks ? com.stormreach.storm.StormBlocks.STORM_OF.get(from.getBlock()) : null;
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        // No neighbour or shape updates: the shapes stay the same, and it stops doors, plants
        // and fences from breaking halfway through a conversion.
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /** Copies every property both blocks share (facing, axis, half, waterlogged, ...). */
    public static BlockState copyProperties(BlockState from, Block to) {
        BlockState state = to.defaultBlockState();
        for (Property<?> property : from.getProperties()) {
            if (state.hasProperty(property)) state = copy(from, state, property);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    /** Turns a mob into its Stormreach counterpart. Named mobs and tamed pets are left alone. */
    public static boolean convertMob(ServerLevel level, Mob mob) {
        if (!StormreachConfig.convertMobs || mob.hasCustomName()) return false;
        if (mob instanceof TamableAnimal pet && pet.isTame()) return false;
        if (com.stormreach.storm.StormMobs.neverConvert(mob) || com.stormreach.storm.StormMobs.isStorm(mob.getType())) return false;
        EntityType<? extends Mob> to = target(mob.getType());
        if (to == null) return false;
        if (mob.getType() == ModEntities.CLIFF_CRAB && (!level.isThundering() || !StormreachConfig.crabConversion)) return false;
        Mob converted = mob.convertTo(to, ConversionParams.single(mob, true, true), m -> {});
        if (converted != null) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, converted.getX(), converted.getY() + 0.5, converted.getZ(), 20, 0.4, 0.5, 0.4, 0.2);
            level.sendParticles(ParticleTypes.CLOUD, converted.getX(), converted.getY() + 0.5, converted.getZ(), 8, 0.4, 0.4, 0.4, 0.02);
            return true;
        }
        return false;
    }

    /** Converts one random mob in the box. */
    public static void convertOneMob(ServerLevel level, AABB box, RandomSource random) {
        List<Mob> found = level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && target(m.getType()) != null
                && !com.stormreach.storm.StormMobs.isStorm(m.getType()) && !com.stormreach.storm.StormMobs.neverConvert(m));
        if (!found.isEmpty()) convertMob(level, found.get(random.nextInt(found.size())));
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Mob> target(EntityType<?> type) {
        EntityType<? extends Mob> themed = mobs().get(type);
        if (themed != null) return themed;
        return StormreachConfig.useGeneratedMobs ? (EntityType<? extends Mob>) com.stormreach.storm.StormMobs.STORM_OF.get(type) : null;
    }

    /** Repaints the biome around a fully grown Stormheart (in 16-block pieces, under the volume limit). */
    public static void blightBiome(ServerLevel level, BlockPos center, int radius) {
        if (!StormreachConfig.blightBiome) return;
        net.minecraft.resources.ResourceKey<Biome> key = level.dimension() == net.minecraft.world.level.Level.NETHER ? ModBiomes.STORM_WASTES
                : level.dimension() == net.minecraft.world.level.Level.END ? ModBiomes.STORM_VOID : ModBiomes.STORMREACH_COAST;
        Holder<Biome> biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
        int minY = Math.max(level.getMinY(), center.getY() - 32);
        int maxY = Math.min(level.getMaxY(), center.getY() + 48);
        for (int x = -radius; x < radius; x += 16) {
            for (int z = -radius; z < radius; z += 16) {
                for (int y = minY; y < maxY; y += 16) {
                    BlockPos from = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    BlockPos to = from.offset(15, 15, 15);
                    FillBiomeCommand.fill(level, from, to, biome);
                }
            }
        }
    }
}
