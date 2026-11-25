package com.stormreach.registry;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.entity.CliffCrab;
import com.stormreach.entity.Squall;
import com.stormreach.entity.StormGull;
import com.stormreach.entity.BallLightning;
import com.stormreach.entity.BrineWraith;
import com.stormreach.entity.HarborSeal;
import com.stormreach.entity.SquallBottleProjectile;
import com.stormreach.entity.Stormwarden;
import com.stormreach.entity.StormFront;
import com.stormreach.entity.ThunderCrab;
import com.stormreach.entity.Waterspout;
import net.minecraft.world.entity.monster.Monster;
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
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
    public static final EntityType<CliffCrab> CLIFF_CRAB = register("cliff_crab",
            EntityType.Builder.of(CliffCrab::new, MobCategory.CREATURE).sized(0.8f, 0.45f).eyeHeight(0.3f).clientTrackingRange(8));
    public static final EntityType<StormGull> STORM_GULL = register("storm_gull",
            EntityType.Builder.of(StormGull::new, MobCategory.CREATURE).sized(0.6f, 0.7f).eyeHeight(0.55f).clientTrackingRange(8));
    public static final EntityType<Squall> SQUALL = register("squall",
            EntityType.Builder.of(Squall::new, MobCategory.MONSTER).sized(0.8f, 1.4f).eyeHeight(1.0f).fireImmune().clientTrackingRange(8));

    public static final EntityType<ThunderCrab> THUNDER_CRAB = register("thunder_crab",
            EntityType.Builder.of(ThunderCrab::new, MobCategory.MONSTER).sized(1.2f, 0.7f).eyeHeight(0.45f).clientTrackingRange(8));
    public static final EntityType<HarborSeal> HARBOR_SEAL = register("harbor_seal",
            EntityType.Builder.of(HarborSeal::new, MobCategory.CREATURE).sized(0.9f, 0.6f).eyeHeight(0.45f).clientTrackingRange(10));
    public static final EntityType<BrineWraith> BRINE_WRAITH = register("brine_wraith",
            EntityType.Builder.of(BrineWraith::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).clientTrackingRange(8));
    public static final EntityType<BallLightning> BALL_LIGHTNING = register("ball_lightning",
            EntityType.Builder.of(BallLightning::new, MobCategory.MONSTER).sized(0.5f, 0.5f).eyeHeight(0.25f).fireImmune().clientTrackingRange(10));
    public static final EntityType<Waterspout> WATERSPOUT = register("waterspout",
            EntityType.Builder.of(Waterspout::new, MobCategory.MISC).sized(1.6f, 8.0f).eyeHeight(4.0f).fireImmune().clientTrackingRange(16));
    public static final EntityType<Stormwarden> STORMWARDEN = register("stormwarden",
            EntityType.Builder.of(Stormwarden::new, MobCategory.MONSTER).sized(2.2f, 4.0f).eyeHeight(3.0f).fireImmune().clientTrackingRange(16));
    public static final EntityType<SquallBottleProjectile> SQUALL_BOTTLE = register("squall_bottle",
            EntityType.Builder.<SquallBottleProjectile>of(SquallBottleProjectile::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(10));

    public static final EntityType<StormFront> STORM_FRONT = register("storm_front",
            EntityType.Builder.of(StormFront::new, MobCategory.MISC).sized(2.0f, 2.0f).fireImmune().clientTrackingRange(16));

    private ModEntities() {}

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(CLIFF_CRAB, CliffCrab.createAttributes());
        FabricDefaultAttributeRegistry.register(STORM_GULL, StormGull.createAttributes());
        FabricDefaultAttributeRegistry.register(SQUALL, Squall.createAttributes());
        FabricDefaultAttributeRegistry.register(THUNDER_CRAB, ThunderCrab.createAttributes());
        FabricDefaultAttributeRegistry.register(HARBOR_SEAL, HarborSeal.createAttributes());
        FabricDefaultAttributeRegistry.register(BRINE_WRAITH, BrineWraith.createAttributes());
        FabricDefaultAttributeRegistry.register(BALL_LIGHTNING, BallLightning.createAttributes());
        FabricDefaultAttributeRegistry.register(WATERSPOUT, Waterspout.createAttributes());
        FabricDefaultAttributeRegistry.register(STORMWARDEN, Stormwarden.createAttributes());
        FabricDefaultAttributeRegistry.register(STORM_FRONT, StormFront.createAttributes());

        SpawnPlacements.register(CLIFF_CRAB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> StormreachConfig.crabSpawning && Animal.checkAnimalSpawnRules(type, level, reason, pos, random));
        SpawnPlacements.register(STORM_GULL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> StormreachConfig.gullSpawning && Animal.checkAnimalSpawnRules(type, level, reason, pos, random));
        // Squalls are only created by Thunderheads (see StormWeather), never by normal spawning.
        SpawnPlacements.register(SQUALL, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> StormreachConfig.squallSpawning);
        SpawnPlacements.register(HARBOR_SEAL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> StormreachConfig.sealSpawning && Animal.checkAnimalSpawnRules(type, level, reason, pos, random));
        SpawnPlacements.register(BRINE_WRAITH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> StormreachConfig.wraithSpawning && Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
        SpawnPlacements.register(THUNDER_CRAB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
        SpawnPlacements.register(BALL_LIGHTNING, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> StormreachConfig.ballLightning);
        SpawnPlacements.register(STORMWARDEN, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> false);
    }
}
