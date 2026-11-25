package com.stormreach.labyrinth;

import com.stormreach.Stormreach;
import com.stormreach.sim.MazeLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/** One 5 x 5-cell section of the Labyrinth, or (when {@code stair}) the entrance shaft to the surface. */
public class LabyrinthPiece extends StructurePiece {
    private static final int S = LabyrinthStructure.SIZE, C = LabyrinthStructure.CELL, H = LabyrinthStructure.HEIGHT;
    private final int ox, oy, oz, gx, gz, surface;
    private final long seed;
    private final boolean stair;
    private MazeLayout layout;

    static final ResourceKey<LootTable> VAULT = loot("labyrinth_vault"), TREASURE = loot("labyrinth_treasure"), ROOM = loot("labyrinth_room");

    private static ResourceKey<LootTable> loot(String n) {
        return ResourceKey.create(Registries.LOOT_TABLE, Stormreach.id("chests/" + n));
    }

    public LabyrinthPiece(BlockPos origin, long seed, int gx, int gz, boolean stair, int surface) {
        super(LabyrinthRegistry.PIECE, 0, box(origin, gx, gz, stair, surface));
        this.ox = origin.getX(); this.oy = origin.getY(); this.oz = origin.getZ();
        this.gx = gx; this.gz = gz; this.seed = seed; this.stair = stair; this.surface = surface;
    }

    public LabyrinthPiece(CompoundTag tag) {
        super(LabyrinthRegistry.PIECE, tag);
        ox = tag.getIntOr("OX", 0); oy = tag.getIntOr("OY", 0); oz = tag.getIntOr("OZ", 0);
        gx = tag.getIntOr("GX", 0); gz = tag.getIntOr("GZ", 0); surface = tag.getIntOr("Surface", oy + 20);
        seed = tag.getLongOr("Seed", 0L); stair = tag.getBooleanOr("Stair", false);
    }

    private static BoundingBox box(BlockPos o, int gx, int gz, boolean stair, int surface) {
        if (stair) return new BoundingBox(o.getX() + 1, o.getY(), o.getZ() + 1, o.getX() + 5, surface + 4, o.getZ() + 5);
        int x0 = o.getX() + gx * C, z0 = o.getZ() + gz * C;
        int x1 = o.getX() + Math.min(S * C, (gx + LabyrinthStructure.PIECE_CELLS) * C);
        int z1 = o.getZ() + Math.min(S * C, (gz + LabyrinthStructure.PIECE_CELLS) * C);
        return new BoundingBox(x0, o.getY(), z0, x1, o.getY() + H - 1, z1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("OX", ox); tag.putInt("OY", oy); tag.putInt("OZ", oz);
        tag.putInt("GX", gx); tag.putInt("GZ", gz); tag.putInt("Surface", surface);
        tag.putLong("Seed", seed); tag.putBoolean("Stair", stair);
    }

    private MazeLayout layout() {
        if (layout == null) layout = new MazeLayout(S, seed);
        return layout;
    }

    private static BlockState vanilla(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id)).defaultBlockState();
    }

    private static long hash(int x, int y, int z) {
        long h = x * 3129871L ^ z * 116129781L ^ y;
        return h * h * 42317861L + h * 11L;
    }

    /** Section palettes: Weathered Halls, Ashen Crypts, Rusted Works and Drowned Galleries. */
    private BlockState wall(int section, long h) {
        int r = (int) Math.floorMod(h >> 16, 10);
        return switch (section) {
            case 0 -> r < 5 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : r < 8 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
            case 1 -> r < 6 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : r < 9 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.GILDED_BLACKSTONE.defaultBlockState();
            case 2 -> r < 6 ? Blocks.TUFF_BRICKS.defaultBlockState() : r < 8 ? vanilla("weathered_copper") : Blocks.CHISELED_TUFF.defaultBlockState();
            default -> r < 6 ? Blocks.MUD_BRICKS.defaultBlockState() : r < 9 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        };
    }

    private BlockState floor(int section, long h) {
        int r = (int) Math.floorMod(h >> 20, 10);
        return switch (section) {
            case 0 -> r < 7 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            case 1 -> r < 8 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.SOUL_SOIL.defaultBlockState();
            case 2 -> r < 8 ? Blocks.POLISHED_TUFF.defaultBlockState() : vanilla("oxidized_cut_copper");
            default -> r < 7 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD.defaultBlockState();
        };
    }

    private void set(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, BlockState s) {
        BlockPos p = new BlockPos(x, y, z);
        if (chunkBox.isInside(p)) level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        if (stair) {
            buildStair(level, chunkBox);
            return;
        }
        MazeLayout m = layout();
        BoundingBox bb = getBoundingBox();
        int c = S / 2;
        for (int x = Math.max(bb.minX(), chunkBox.minX()); x <= Math.min(bb.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(bb.minZ(), chunkBox.minZ()); z <= Math.min(bb.maxZ(), chunkBox.maxZ()); z++) {
                int lx = x - ox, lz = z - oz;
                int cx = Math.min(S - 1, lx / C), cz = Math.min(S - 1, lz / C);
                int ix = lx - cx * C, iz = lz - cz * C; // 0..6 for the last cell
                int section = m.section[m.i(cx, cz)];
                boolean treasure = m.isTreasure(cx, cz);
                for (int y = 0; y < H; y++) {
                    long h = hash(x, y, z);
                    BlockState s;
                    if (y == 0) s = treasure ? (Math.floorMod(h, 5) == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState()) : floor(section, h);
                    else if (y == H - 1) s = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    else if (solidAt(m, cx, cz, ix, iz)) {
                        boolean door = isTreasureDoor(m, cx, cz, ix, iz, c);
                        s = door ? LabyrinthRegistry.SEAL.defaultBlockState() : treasure ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : wall(section, h);
                    } else s = Blocks.CAVE_AIR.defaultBlockState();
                    set(level, chunkBox, x, oy + y, z, s);
                }
            }
        }
        decorate(level, chunkBox, random, m, c);
    }

    /** Wall cells: grid lines are walls unless the maze opens there; the outer east/south edge is always wall. */
    private static boolean solidAt(MazeLayout m, int cx, int cz, int ix, int iz) {
        if (ix == C || iz == C) return true;            // far edge of the whole maze
        if (ix == 0 && iz == 0) return true;            // corner posts
        if (ix == 0) return cx == 0 || (m.openAt(cx, cz) & MazeLayout.W) == 0;
        if (iz == 0) return cz == 0 || (m.openAt(cx, cz) & MazeLayout.N) == 0;
        return false;
    }

    private static boolean isTreasureDoor(MazeLayout m, int cx, int cz, int ix, int iz, int c) {
        return cx == c && cz == m.treasureMin && iz == 0 && ix > 0;
    }

    private void decorate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, MazeLayout m, int c) {
        for (int cx = gx; cx < Math.min(S, gx + LabyrinthStructure.PIECE_CELLS); cx++) {
            for (int cz = gz; cz < Math.min(S, gz + LabyrinthStructure.PIECE_CELLS); cz++) {
                int bx = ox + cx * C, bz = oz + cz * C;
                long h = hash(bx, 7, bz);
                MazeLayout.CellType type = m.typeAt(cx, cz);
                // sparse, dim lighting
                if (Math.floorMod(h, 4) == 0 || type != MazeLayout.CellType.CORRIDOR) {
                    set(level, chunkBox, bx + 3, oy + H - 2, bz + 3, Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
                }
                if (Math.floorMod(h >> 8, 6) == 0) set(level, chunkBox, bx + 1, oy + H - 2, bz + 1, Blocks.COBWEB.defaultBlockState());
                if (Math.floorMod(h >> 12, 9) == 0) set(level, chunkBox, bx + 5, oy + 1, bz + 5, Blocks.BONE_BLOCK.defaultBlockState());
                switch (type) {
                    case KEY_VAULT -> chest(level, chunkBox, random, bx + 3, bz + 3, VAULT);
                    case TREASURE -> { if (cx == c && cz == c) chest(level, chunkBox, random, bx + 3, bz + 3, TREASURE); }
                    case ROOM -> { if (Math.floorMod(h >> 4, 10) < 3) chest(level, chunkBox, random, bx + 3, bz + 3, ROOM); }
                    default -> {}
                }
                if (cx == c && cz == m.treasureMin - 1) {
                    BlockPos key = new BlockPos(bx + 3, oy + 2, bz + C - 1);
                    if (chunkBox.isInside(key)) level.setBlock(key, LabyrinthRegistry.KEYHOLE.defaultBlockState()
                            .setValue(KeyholeBlock.FACING, Direction.NORTH), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int[] spot : m.lockerSpots) {
            if (spot[0] < gx || spot[0] >= gx + LabyrinthStructure.PIECE_CELLS || spot[1] < gz || spot[1] >= gz + LabyrinthStructure.PIECE_CELLS) continue;
            int open = m.openAt(spot[0], spot[1]);
            int bx = ox + spot[0] * C, bz = oz + spot[1] * C;
            Direction facing;
            int lx, lz;
            if ((open & MazeLayout.N) == 0) { facing = Direction.SOUTH; lx = 3; lz = 1; }
            else if ((open & MazeLayout.W) == 0) { facing = Direction.EAST; lx = 1; lz = 3; }
            else if ((open & MazeLayout.S) == 0) { facing = Direction.NORTH; lx = 3; lz = 5; }
            else continue;
            BlockState lower = LabyrinthRegistry.LOCKER.defaultBlockState().setValue(LockerBlock.FACING, facing).setValue(LockerBlock.HALF, DoubleBlockHalf.LOWER);
            set(level, chunkBox, bx + lx, oy + 1, bz + lz, lower);
            set(level, chunkBox, bx + lx, oy + 2, bz + lz, lower.setValue(LockerBlock.HALF, DoubleBlockHalf.UPPER));
        }
    }

    private void chest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int z, ResourceKey<LootTable> table) {
        BlockPos p = new BlockPos(x, oy + 1, z);
        if (chunkBox.isInside(p)) createChest(level, chunkBox, random, p, table, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
    }

    private void buildStair(WorldGenLevel level, BoundingBox chunkBox) {
        // a 3 x 3 shaft with a ladder, walled in stone bricks, rising to a small ruined arch at the surface
        for (int y = oy + 1; y <= surface + 3; y++) {
            for (int x = ox + 1; x <= ox + 5; x++) {
                for (int z = oz + 1; z <= oz + 5; z++) {
                    boolean edge = x == ox + 1 || x == ox + 5 || z == oz + 1 || z == oz + 5;
                    boolean aboveGround = y > surface;
                    if (edge && y < oy + H) continue; // inside the maze itself the entrance cell stays open
                    BlockState s;
                    if (edge) {
                        if (aboveGround) {
                            boolean pillar = (x == ox + 1 || x == ox + 5) && (z == oz + 1 || z == oz + 5);
                            s = pillar && y <= surface + 3 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        } else s = Math.floorMod(hash(x, y, z), 3) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                    } else s = y <= oy + H - 2 || y <= surface + 3 ? Blocks.CAVE_AIR.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    if (aboveGround && !edge) s = Blocks.AIR.defaultBlockState();
                    set(level, chunkBox, x, y, z, s);
                }
            }
            if (y <= surface) set(level, chunkBox, ox + 3, y, oz + 4, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        set(level, chunkBox, ox + 3, surface + 3, oz + 1, Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState());
        set(level, chunkBox, ox + 3, surface + 3, oz + 5, Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState());
    }
}
