package com.stormreach.realm;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.entity.Cyclone;
import com.stormreach.entity.Puffling;
import com.stormreach.entity.Skyray;
import com.stormreach.entity.StaticWisp;
import com.stormreach.entity.Stormhawk;
import com.stormreach.entity.TempestSovereign;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

/** Mobs added in 4.0. */
public final class RealmEntities {
    public static final EntityType<TempestSovereign> TEMPEST_SOVEREIGN = register("tempest_sovereign",
            EntityType.Builder.of(TempestSovereign::new, MobCategory.MONSTER).sized(1.8f, 4.2f).eyeHeight(3.4f).fireImmune().clientTrackingRange(16));
    public static final EntityType<Skyray> SKYRAY = register("skyray",
            EntityType.Builder.of(Skyray::new, MobCategory.CREATURE).sized(2.2f, 0.7f).eyeHeight(0.4f).clientTrackingRange(10));
    public static final EntityType<Puffling> PUFFLING = register("puffling",
            EntityType.Builder.of(Puffling::new, MobCategory.CREATURE).sized(0.9f, 1.1f).eyeHeight(0.9f).clientTrackingRange(10));
    public static final EntityType<StaticWisp> STATIC_WISP = register("static_wisp",
            EntityType.Builder.of(StaticWisp::new, MobCategory.MONSTER).sized(0.5f, 0.5f).eyeHeight(0.25f).fireImmune().clientTrackingRange(8));
    public static final EntityType<Cyclone> CYCLONE = register("cyclone",
            EntityType.Builder.of(Cyclone::new, MobCategory.MONSTER).sized(0.9f, 2.2f).eyeHeight(1.6f).clientTrackingRange(8));
    public static final EntityType<Stormhawk> STORMHAWK = register("stormhawk",
            EntityType.Builder.of(Stormhawk::new, MobCategory.MONSTER).sized(1.0f, 0.8f).eyeHeight(0.5f).clientTrackingRange(10));

    /** 6.0: the Tempest Eye floating over the altar after the Sovereign falls (use it to take the Eye). */
    public static final EntityType<TempestEyeReward> TEMPEST_EYE_REWARD = register("tempest_eye_reward",
            EntityType.Builder.<TempestEyeReward>of(TempestEyeReward::new, MobCategory.MISC).sized(0.7f, 0.7f).clientTrackingRange(10).fireImmune());

    private RealmEntities() {}

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(TEMPEST_SOVEREIGN, TempestSovereign.createAttributes());
        FabricDefaultAttributeRegistry.register(SKYRAY, Skyray.createAttributes());
        FabricDefaultAttributeRegistry.register(PUFFLING, Puffling.createAttributes());
        FabricDefaultAttributeRegistry.register(STATIC_WISP, StaticWisp.createAttributes());
        FabricDefaultAttributeRegistry.register(CYCLONE, Cyclone.createAttributes());
        FabricDefaultAttributeRegistry.register(STORMHAWK, Stormhawk.createAttributes());

        SpawnPlacements.register(TEMPEST_SOVEREIGN, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> false);
        SpawnPlacements.register(SKYRAY, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> StormreachConfig.realmMobSpawning && pos.getY() > level.getMinY() + 40);
        SpawnPlacements.register(PUFFLING, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> StormreachConfig.realmMobSpawning && Animal.checkAnimalSpawnRules(type, level, reason, pos, random));
        SpawnPlacements.register(STATIC_WISP, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> StormreachConfig.realmMobSpawning && Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
        SpawnPlacements.register(CYCLONE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> StormreachConfig.realmMobSpawning && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random));
        SpawnPlacements.register(STORMHAWK, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> StormreachConfig.realmMobSpawning && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random));
    }
}
