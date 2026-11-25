package com.stormreach.tornado;

import com.stormreach.sim.StructuralIntegrity;
import com.stormreach.sim.StructuralIntegrity.MaterialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Maps Minecraft blocks onto wind-resistance classes and works out each block's failure wind. */
public final class BlockWind {
    private BlockWind() {}

    public static MaterialClass classify(ServerLevel level, BlockPos pos, BlockState s) {
        if (s.isAir() || !s.getFluidState().isEmpty() && s.getFluidState().isSource()) return MaterialClass.UNBREAKABLE;
        float hardness = s.getDestroySpeed(level, pos);
        if (hardness < 0 || s.getBlock().getExplosionResistance() >= 1200 || s.is(BlockTags.WITHER_IMMUNE) || s.is(BlockTags.DRAGON_IMMUNE)) {
            return MaterialClass.UNBREAKABLE;
        }
        if (s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS)
                || s.canBeReplaced() || s.is(Blocks.SNOW) || s.is(Blocks.VINE)) {
            return MaterialClass.FOLIAGE;
        }
        SoundType sound = s.getSoundType();
        if (sound == SoundType.GLASS) return MaterialClass.GLASS;
        if (s.is(BlockTags.WOOL) || s.is(BlockTags.WOOL_CARPETS) || s.is(BlockTags.BEDS) || s.is(Blocks.HAY_BLOCK) || sound == SoundType.WOOL) {
            return MaterialClass.FABRIC;
        }
        if (s.is(BlockTags.LOGS)) return MaterialClass.HEAVY_WOOD;
        if (s.is(BlockTags.PLANKS) || s.is(BlockTags.WOODEN_SLABS) || s.is(BlockTags.WOODEN_STAIRS) || s.is(BlockTags.WOODEN_FENCES)
                || s.is(BlockTags.FENCE_GATES) || s.is(BlockTags.WOODEN_DOORS) || s.is(BlockTags.WOODEN_TRAPDOORS) || s.is(BlockTags.ALL_SIGNS)
                || s.is(BlockTags.MINEABLE_WITH_AXE)) {
            return MaterialClass.LIGHT_WOOD;
        }
        if (sound == SoundType.METAL || sound == SoundType.ANVIL || sound == SoundType.NETHERITE_BLOCK || sound == SoundType.COPPER || hardness >= 5) {
            return MaterialClass.METAL;
        }
        if (s.is(BlockTags.MINEABLE_WITH_SHOVEL) || s.is(BlockTags.SAND) || s.is(BlockTags.DIRT)) return MaterialClass.LOOSE_EARTH;
        if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.STONE_BRICKS) || s.is(BlockTags.BASE_STONE_NETHER) || hardness >= 3) {
            return MaterialClass.STONE;
        }
        if (s.is(BlockTags.MINEABLE_WITH_PICKAXE)) return MaterialClass.MASONRY;
        if (s.is(BlockTags.MINEABLE_WITH_HOE)) return MaterialClass.FOLIAGE;
        return hardness <= 0.5f ? MaterialClass.FABRIC : MaterialClass.MASONRY;
    }

    /** Failure gust (mph) for the block at pos, considering connections, support and shelter. */
    public static double failureMph(ServerLevel level, BlockPos pos, BlockState s, MaterialClass m) {
        int solid = 0;
        boolean exposed = false;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            BlockState ns = level.getBlockState(n);
            if (ns.isSolidRender()) solid++;
            else if (ns.isAir() || ns.canBeReplaced()) exposed = true;
        }
        if (m == MaterialClass.LOOSE_EARTH && !level.getBlockState(pos.above()).isAir()) exposed = false; // only scour open ground
        boolean supported = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        boolean thin = s.getBlock() instanceof SlabBlock || s.getBlock() instanceof StairBlock || s.is(BlockTags.FENCES)
                || s.is(BlockTags.WALLS) || s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS) || !s.isSolidRender();
        return StructuralIntegrity.failureGustMph(m, solid, supported, exposed, thin, pos.asLong());
    }
}
