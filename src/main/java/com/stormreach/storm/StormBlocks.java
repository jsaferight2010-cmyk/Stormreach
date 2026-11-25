package com.stormreach.storm;

import com.stormreach.Stormreach;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.Direction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates a "Storm" counterpart for every vanilla block that can safely have one: same shape, same
 * behaviour class, storm-tinted look, and its own id (stormreach:storm_&lt;name&gt;). Blocks with block entities,
 * liquids, portals, fire and the like are skipped. The list of candidates is
 * stormreach_storm_blocks.txt, written by tools/gendata6.py together with all the models.
 *
 * Each storm block is built by calling the vanilla block's own constructor with a copy of its
 * Properties; any other constructor arguments (stair base, door set type, particle...) are read from the
 * vanilla block's fields of the matching type. Anything that can't be cloned cleanly is skipped and logged.
 */
public final class StormBlocks {
    /** vanilla block -> storm counterpart */
    public static final Map<Block, Block> STORM_OF = new IdentityHashMap<>();
    public static final List<Item> ITEMS = new ArrayList<>();
    private static final Set<Class<?>> SKIP = Set.of(FlowerPotBlock.class, InfestedBlock.class, CandleCakeBlock.class,
            NetherPortalBlock.class, BubbleColumnBlock.class, AirBlock.class);

    private StormBlocks() {}

    public static boolean isStorm(Block block) {
        return STORM_OF.containsValue(block);
    }

    public static void registerAll() {
        Set<String> wanted = readList("/stormreach_storm_blocks.txt");
        List<Block> vanilla = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            if (id.getNamespace().equals("minecraft") && wanted.contains(id.getPath())) vanilla.add(b);
        }
        int skipped = 0;
        java.util.Set<Object> holdersBefore = pendingIntrusiveHolders();
        for (Block v : vanilla) {
            try {
                Block storm = cloneBlock(v);
                if (storm != null) STORM_OF.put(v, storm); else skipped++;
            } catch (Throwable t) {
                skipped++;
                Stormreach.LOGGER.info("No storm counterpart for {}: {}", BuiltInRegistries.BLOCK.getKey(v), t.toString());
            }
        }
        dropLeakedHolders(holdersBefore);
        // Items second, so standing/wall pairs (torches) can find each other.
        for (Map.Entry<Block, Block> e : STORM_OF.entrySet()) {
            try {
                registerItem(e.getKey(), e.getValue());
            } catch (Throwable t) {
                Stormreach.LOGGER.debug("No storm item for {}: {}", BuiltInRegistries.BLOCK.getKey(e.getKey()), t.toString());
            }
        }
        Stormreach.LOGGER.info("Stormreach: generated {} storm blocks ({} skipped)", STORM_OF.size(), skipped);
    }

    private static Set<String> readList(String resource) {
        Set<String> names = new HashSet<>();
        try (InputStream in = StormBlocks.class.getResourceAsStream(resource)) {
            if (in == null) return names;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) names.add(line);
            }
        } catch (Exception e) {
            Stormreach.LOGGER.warn("Could not read {}", resource, e);
        }
        return names;
    }

    private static Block cloneBlock(Block v) throws Exception {
        if (v instanceof EntityBlock || v instanceof LiquidBlock || v instanceof BaseFireBlock || v instanceof AbstractCauldronBlock) return null;
        Class<?> cls = v.getClass();
        while (cls.isAnonymousClass()) cls = cls.getSuperclass();
        if (SKIP.contains(cls) || Modifier.isAbstract(cls.getModifiers())) return null;

        String path = "storm_" + BuiltInRegistries.BLOCK.getKey(v).getPath();
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(path));
        BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(v).setId(key);

        for (Constructor<?> ctor : cls.getDeclaredConstructors()) {
            Object[] args = argsFor(ctor, v, props);
            if (args == null) continue;
            ctor.setAccessible(true);
            Block storm = (Block) ctor.newInstance(args);
            return Registry.register(BuiltInRegistries.BLOCK, key, storm);
        }
        return null;
    }

    /** Fills constructor arguments: Properties gets our copy, everything else comes from a uniquely-typed field. */
    private static Object[] argsFor(Constructor<?> ctor, Block v, BlockBehaviour.Properties props) throws IllegalAccessException {
        Class<?>[] types = ctor.getParameterTypes();
        Object[] args = new Object[types.length];
        boolean sawProps = false;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == BlockBehaviour.Properties.class) {
                args[i] = props;
                sawProps = true;
                continue;
            }
            Field match = null;
            for (Class<?> c = v.getClass(); c != null && c != Block.class && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    if (wrap(types[i]).isAssignableFrom(wrap(f.getType()))) {
                        if (match != null && !match.equals(f)) return null; // ambiguous
                        match = f;
                    }
                }
            }
            if (match == null) return null;
            match.setAccessible(true);
            args[i] = match.get(v);
        }
        return sawProps ? args : null;
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == float.class) return Float.class;
        if (c == boolean.class) return Boolean.class;
        if (c == double.class) return Double.class;
        if (c == long.class) return Long.class;
        return c;
    }

    private static void registerItem(Block vanilla, Block storm) throws Exception {
        Item v = vanilla.asItem();
        if (v == Items.AIR) return;
        Identifier vid = BuiltInRegistries.ITEM.getKey(v);
        if (!vid.getPath().equals(BuiltInRegistries.BLOCK.getKey(vanilla).getPath())) return;
        Identifier sid = BuiltInRegistries.BLOCK.getKey(storm);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, sid);
        Item.Properties props = new Item.Properties().setId(key).useBlockDescriptionPrefix().stacksTo(v.getDefaultMaxStackSize());
        Item item;
        if (v instanceof DoubleHighBlockItem) item = new DoubleHighBlockItem(storm, props);
        else if (v instanceof PlaceOnWaterBlockItem) item = new PlaceOnWaterBlockItem(storm, props);
        else if (v instanceof ScaffoldingBlockItem) item = new ScaffoldingBlockItem(storm, props);
        else if (v instanceof StandingAndWallBlockItem) {
            Field wf = StandingAndWallBlockItem.class.getDeclaredField("wallBlock");
            Field df = StandingAndWallBlockItem.class.getDeclaredField("attachmentDirection");
            wf.setAccessible(true);
            df.setAccessible(true);
            Block stormWall = STORM_OF.get((Block) wf.get(v));
            item = stormWall == null ? new BlockItem(storm, props) : new StandingAndWallBlockItem(storm, stormWall, (Direction) df.get(v), props);
        } else item = new BlockItem(storm, props);
        ITEMS.add(Registry.register(BuiltInRegistries.ITEM, key, item));
    }

    /**
     * A Block reserves a registry slot (an "intrusive holder") the moment it is constructed. If a clone's constructor
     * throws part-way, that slot is never filled and the game refuses to start ("Some intrusive holders were not
     * registered"). These two methods remember which slots existed before cloning and release any a failed clone left.
     */
    @SuppressWarnings("unchecked")
    private static java.util.Map<Object, Object> intrusiveHolders() {
        try {
            Field f = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            f.setAccessible(true);
            return (java.util.Map<Object, Object>) f.get(BuiltInRegistries.BLOCK);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static java.util.Set<Object> pendingIntrusiveHolders() {
        java.util.Map<Object, Object> m = intrusiveHolders();
        return m == null ? java.util.Set.of() : new java.util.HashSet<>(m.keySet());
    }

    private static void dropLeakedHolders(java.util.Set<Object> before) {
        java.util.Map<Object, Object> m = intrusiveHolders();
        if (m == null) return;
        int dropped = 0;
        for (java.util.Iterator<Object> it = m.keySet().iterator(); it.hasNext(); ) {
            Object b = it.next();
            if (!before.contains(b)) { it.remove(); dropped++; }
        }
        if (dropped > 0) Stormreach.LOGGER.info("Stormreach: released {} half-built storm blocks", dropped);
    }
}
