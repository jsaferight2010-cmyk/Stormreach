package com.stormreach.labyrinth;

import com.mojang.serialization.MapCodec;
import com.stormreach.StormreachConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The Hollow Labyrinth: a 25 x 25-cell underground maze (151 x 151 blocks) built procedurally in 25 pieces,
 * each covering 5 x 5 cells, plus an entrance shaft. Generation is deterministic from a stored seed so each
 * piece can rebuild its part of the layout when its chunks generate.
 */
public class LabyrinthStructure extends Structure {
    public static final MapCodec<LabyrinthStructure> CODEC = simpleCodec(LabyrinthStructure::new);
    public static final int SIZE = 25, CELL = 6, HEIGHT = 7, PIECE_CELLS = 5;

    public LabyrinthStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (!StormreachConfig.labyrinthGeneration) return Optional.empty();
        int ox = ctx.chunkPos().getMinBlockX();
        int oz = ctx.chunkPos().getMinBlockZ();
        int surface = ctx.chunkGenerator().getBaseHeight(ox + 3, oz + 3, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int minY = ctx.heightAccessor().getMinY();
        int oy = Math.max(minY + 12, Math.min(surface - 38, 24));
        if (surface - oy < 12) return Optional.empty();
        long seed = ctx.random().nextLong();
        BlockPos origin = new BlockPos(ox, oy, oz);
        return Optional.of(new GenerationStub(origin, builder -> {
            for (int px = 0; px < SIZE / PIECE_CELLS; px++)
                for (int pz = 0; pz < SIZE / PIECE_CELLS; pz++)
                    builder.addPiece(new LabyrinthPiece(origin, seed, px * PIECE_CELLS, pz * PIECE_CELLS, false, surface));
            builder.addPiece(new LabyrinthPiece(origin, seed, 0, 0, true, surface));
        }));
    }

    @Override
    public StructureType<?> type() {
        return LabyrinthRegistry.STRUCTURE_TYPE;
    }
}
