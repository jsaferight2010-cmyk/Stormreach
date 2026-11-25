package com.stormreach.registry;

import com.stormreach.Stormreach;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Block sounds for every Stormreach block (5.1), from the mod's own sound effects. Each vanilla sound type a block
 * was registered with is mapped by family (stone, crystal, metal, soft, plant, loose ground, glass) to the matching
 * Stormreach one; a mixin on BlockStateBase.getSoundType applies it to blocks in the stormreach namespace only, so
 * vanilla and other mods keep their own sounds.
 */
public final class ModSoundTypes {
    private ModSoundTypes() {}

    public static final SoundType STONE = family(ModSfx.BLOCK_MAT_STONE_BREAK, ModSfx.BLOCK_MAT_STONE_STEP, ModSfx.BLOCK_MAT_STONE_PLACE);
    public static final SoundType CRYSTAL = family(ModSfx.BLOCK_MAT_CRYSTAL_BREAK, ModSfx.BLOCK_MAT_CRYSTAL_STEP, ModSfx.BLOCK_MAT_CRYSTAL_PLACE);
    public static final SoundType METAL = family(ModSfx.BLOCK_MAT_METAL_BREAK, ModSfx.BLOCK_MAT_METAL_STEP, ModSfx.BLOCK_MAT_METAL_PLACE);
    public static final SoundType SOFT = family(ModSfx.BLOCK_MAT_SOFT_BREAK, ModSfx.BLOCK_MAT_SOFT_STEP, ModSfx.BLOCK_MAT_SOFT_PLACE);
    public static final SoundType PLANT = family(ModSfx.BLOCK_MAT_PLANT_BREAK, ModSfx.BLOCK_MAT_PLANT_STEP, ModSfx.BLOCK_MAT_PLANT_PLACE);
    public static final SoundType LOOSE = family(ModSfx.BLOCK_MAT_LOOSE_BREAK, ModSfx.BLOCK_MAT_LOOSE_STEP, ModSfx.BLOCK_MAT_LOOSE_PLACE);
    public static final SoundType GLASS = family(ModSfx.BLOCK_MAT_GLASS_BREAK, ModSfx.BLOCK_MAT_GLASS_STEP, ModSfx.BLOCK_MAT_GLASS_PLACE);

    private static SoundType family(net.minecraft.sounds.SoundEvent brk, net.minecraft.sounds.SoundEvent step, net.minecraft.sounds.SoundEvent place) {
        return new SoundType(1.0f, 1.0f, brk, step, place, step, step);
    }

    private static final Map<SoundType, SoundType> BY_VANILLA = new IdentityHashMap<>();
    private static final Map<Block, SoundType> CACHE = new IdentityHashMap<>();

    static {
        for (Field f : SoundType.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != SoundType.class) continue;
            try {
                SoundType v = (SoundType) f.get(null);
                if (v != SoundType.EMPTY) BY_VANILLA.put(v, familyFor(f.getName()));
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    /** Which family a vanilla sound type (by its constant's name) belongs to. Public for tests. */
    public static SoundType familyFor(String name) {
        String n = name.toUpperCase(Locale.ROOT);
        if (n.contains("GLASS")) return GLASS;
        if (n.contains("AMETHYST") || n.contains("BUD") || n.contains("CALCITE") || n.contains("CRYSTAL")) return CRYSTAL;
        if (n.matches(".*(COPPER|METAL|NETHERITE|IRON|CHAIN|ANVIL|LANTERN|LODESTONE|HEAVY_CORE|VAULT|TRIAL|SPAWNER|GILDED).*")) return METAL;
        if (n.matches(".*(WOOL|SNOW|MOSS|SCULK|HONEY|SLIME|FROGSPAWN|CANDLE|SPONGE|POWDER).*")) return SOFT;
        if (n.matches(".*(GRASS|LEAVES|WOOD|BAMBOO|CROP|VINE|ROOT|FUNGUS|AZALEA|STEM|WART|BERRY|CHERRY|MANGROVE|LILY|SIGN|SAPLING|FLOWER|DRIPLEAF|SPORE|NYLIUM|HANGING|PETAL|CACTUS|LADDER|SCAFFOLD).*")) return PLANT;
        if (n.matches(".*(SAND|GRAVEL|DIRT|MUD|SOIL|SUSPICIOUS|SNOW_LAYER|PACKED).*")) return LOOSE;
        return STONE;
    }

    /** The sound type a Stormreach block uses, or null for blocks of other namespaces (and silent ones). */
    public static SoundType forBlock(Block block, SoundType registered) {
        if (registered == SoundType.EMPTY) return null;
        SoundType cached = CACHE.get(block);
        if (cached != null) return cached;
        if (!Stormreach.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) return null;
        SoundType ours = BY_VANILLA.getOrDefault(registered, registered);
        synchronized (CACHE) { CACHE.put(block, ours); }
        return ours;
    }

    public static void init() {}
}
