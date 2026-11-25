package com.stormreach.storm;

import com.stormreach.Stormreach;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates a "Storm" counterpart (stormreach:storm_&lt;name&gt;) for every vanilla mob, bosses included.
 * Each one is the vanilla mob's own class with its own AI, registered under a new type with the same size,
 * category and attributes. StormMobPerks gives them their storm abilities, and the client draws them with
 * the vanilla renderer tinted storm-blue.
 */
public final class StormMobs {
    public static final Map<EntityType<?>, EntityType<?>> STORM_OF = new IdentityHashMap<>();
    public static final Set<EntityType<?>> STORM_TYPES = Collections.newSetFromMap(new IdentityHashMap<>());
    public static final Set<EntityType<?>> BOSSES = Collections.newSetFromMap(new IdentityHashMap<>());
    public static final List<Item> EGGS = new ArrayList<>();

    private StormMobs() {}

    public static boolean isStorm(EntityType<?> type) {
        return STORM_TYPES.contains(type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerAll() {
        List<EntityType<?>> vanilla = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals("minecraft") && DefaultAttributes.hasSupplier(t)) vanilla.add(t);
        }
        int skipped = 0;
        for (EntityType<?> t : vanilla) {
            try {
                Class<?> cls = classOf(t);
                if (cls == null) { skipped++; continue; }
                if (!Mob.class.isAssignableFrom(cls) || Modifier.isAbstract(cls.getModifiers())) { skipped++; continue; }
                Constructor<?> ctor = cls.getDeclaredConstructor(EntityType.class, Level.class);
                ctor.setAccessible(true);
                String path = "storm_" + BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath();
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(path));
                EntityDimensions dims = t.getDimensions();
                EntityType.Builder builder = EntityType.Builder.of((type, level) -> {
                    try {
                        return (Entity) ctor.newInstance(type, level);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Could not create " + path, e);
                    }
                }, t.getCategory()).sized(dims.width(), dims.height()).eyeHeight(dims.eyeHeight())
                        .clientTrackingRange(t.clientTrackingRange()).updateInterval(t.updateInterval());
                if (t.fireImmune()) builder = builder.fireImmune();
                EntityType<?> storm = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
                FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) storm,
                        DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) t));
                STORM_OF.put(t, storm);
                STORM_TYPES.add(storm);
                if (EnderDragon.class.isAssignableFrom(cls) || WitherBoss.class.isAssignableFrom(cls)
                        || ElderGuardian.class.isAssignableFrom(cls) || Warden.class.isAssignableFrom(cls)) BOSSES.add(storm);

                Identifier eggId = Identifier.fromNamespaceAndPath("minecraft", BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath() + "_spawn_egg");
                if (BuiltInRegistries.ITEM.containsKey(eggId)) {
                    ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, Stormreach.id(path + "_spawn_egg"));
                    EGGS.add(Registry.register(BuiltInRegistries.ITEM, eggKey,
                            new SpawnEggItem(new Item.Properties().setId(eggKey).spawnEgg(storm))));
                }
            } catch (Throwable e) {
                skipped++;
                Stormreach.LOGGER.info("No storm counterpart for {}: {}", BuiltInRegistries.ENTITY_TYPE.getKey(t), e.toString());
            }
        }
        Stormreach.LOGGER.info("Stormreach: generated {} storm mobs ({} skipped)", STORM_OF.size(), skipped);
    }

    /** Mobs the Stormblight never converts (converting the End's dragon would break the dragon fight). */
    public static boolean neverConvert(Entity e) {
        return e.getType() == EntityTypes.ENDER_DRAGON;
    }

    private static java.util.Map<EntityType<?>, Class<?>> classes;

    /**
     * The Java class behind a vanilla entity type. EntityType.getBaseClass() returns Entity.class for every type in
     * 26.x, so the classes come from stormreach_entity_classes.txt (EntityTypes field = class), generated from the
     * game jar by tools/entity_classes.py.
     */
    static Class<?> classOf(EntityType<?> t) {
        if (classes == null) {
            classes = new java.util.HashMap<>();
            try (java.io.InputStream in = StormMobs.class.getResourceAsStream("/stormreach_entity_classes.txt")) {
                if (in != null) {
                    for (String line : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                        String[] kv = line.split("=", 2);
                        try {
                            Object type = net.minecraft.world.entity.EntityTypes.class.getField(kv[0]).get(null);
                            classes.put((EntityType<?>) type, Class.forName(kv[1], false, StormMobs.class.getClassLoader()));
                        } catch (ReflectiveOperationException | LinkageError ignored) {
                            // renamed in a later Minecraft version: that mob just gets no storm counterpart
                        }
                    }
                }
            } catch (java.io.IOException e) {
                Stormreach.LOGGER.warn("Could not read stormreach_entity_classes.txt", e);
            }
        }
        return classes.get(t);
    }
}
