package com.stormreach.wild;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Stormwild (5.2): new storm-touched wildlife and what it gives you.
 * Coast: Sparkskinks (lizards that charge up in storms and shed scales), Tidewrack Hermits (shy hermit crabs) and
 * Shock Eels (which hunt swimmers when it thunders). Tempest Realm: Cloud Whales (drifting giants that rain and leave
 * ambergris), Storm Jellies (floating, stinging, glowing) and Gale Sprites (befriend them with Thistle Down).
 * In thunderstorms: the Thunderbird, a huge raptor that circles, dives and calls lightning.
 */
public final class WildRegistry {
    public static final List<Item> ITEMS = new ArrayList<>();

    public static final Block GLOWGEL_BLOCK = block("glowgel_block", SlimeBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK)
            .mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 11).noOcclusion());
    public static final Block SKYLIGHT_LANTERN = block("skylight_lantern", LanternBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
            .mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 15));
    public static final Block NACRE_BLOCK = block("nacre_block", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.SNOW).strength(1.2f));
    public static final Block NACRE_BRICKS = block("nacre_bricks", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.SNOW).strength(1.5f));

    public static final Item CLOUD_AMBERGRIS = item("cloud_ambergris", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item GLOWGEL = item("glowgel", Item::new, new Item.Properties());
    public static final Item VOLTAIC_EEL = item("voltaic_eel", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(ModEffects.STATIC_CHARGE, 200, 0))).build()));
    public static final Item COOKED_VOLTAIC_EEL = item("cooked_voltaic_eel", Item::new, new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8f).build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HASTE, 600, 0))).build()));
    public static final Item CHARGED_SCALE = item("charged_scale", Item::new, new Item.Properties());
    public static final Item GALE_ESSENCE = item("gale_essence", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final Item BOTTLED_GALE = item("bottled_gale", Item::new, new Item.Properties().stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(0).saturationModifier(0f).alwaysEdible().build(),
                    Consumables.defaultDrink().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                            new MobEffectInstance(MobEffects.JUMP_BOOST, 900, 2), new MobEffectInstance(MobEffects.SLOW_FALLING, 900, 0)))).build()));
    public static final Item PEARLESCENT_SHELL = item("pearlescent_shell", Item::new, new Item.Properties());
    public static final Item TIDE_CONCH = item("tide_conch", TideConchItem::new, new Item.Properties().durability(64).rarity(Rarity.UNCOMMON));
    public static final Item THUNDERBIRD_PLUME = item("thunderbird_plume", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final Item STORMKITE = item("stormkite", Item::new, new Item.Properties().durability(480).rarity(Rarity.RARE));

    public static final EntityType<CloudWhale> CLOUD_WHALE = entity("cloud_whale",
            EntityType.Builder.of(CloudWhale::new, MobCategory.CREATURE).sized(4.5f, 2.6f).eyeHeight(1.6f).clientTrackingRange(16));
    public static final EntityType<ShockEel> SHOCK_EEL = entity("shock_eel",
            EntityType.Builder.of(ShockEel::new, MobCategory.WATER_CREATURE).sized(0.7f, 0.5f).eyeHeight(0.3f).clientTrackingRange(8));
    public static final EntityType<Sparkskink> SPARKSKINK = entity("sparkskink",
            EntityType.Builder.of(Sparkskink::new, MobCategory.CREATURE).sized(0.6f, 0.35f).eyeHeight(0.25f).clientTrackingRange(8));
    public static final EntityType<StormJelly> STORM_JELLY = entity("storm_jelly",
            EntityType.Builder.of(StormJelly::new, MobCategory.CREATURE).sized(0.8f, 1.1f).eyeHeight(0.6f).fireImmune().clientTrackingRange(10));
    public static final EntityType<GaleSprite> GALE_SPRITE = entity("gale_sprite",
            EntityType.Builder.of(GaleSprite::new, MobCategory.CREATURE).sized(0.4f, 0.6f).eyeHeight(0.4f).clientTrackingRange(10));
    public static final EntityType<TidewrackHermit> TIDEWRACK_HERMIT = entity("tidewrack_hermit",
            EntityType.Builder.of(TidewrackHermit::new, MobCategory.CREATURE).sized(0.6f, 0.55f).eyeHeight(0.35f).clientTrackingRange(8));
    public static final EntityType<Thunderbird> THUNDERBIRD = entity("thunderbird",
            EntityType.Builder.of(Thunderbird::new, MobCategory.MONSTER).sized(2.2f, 1.4f).eyeHeight(1.0f).fireImmune().clientTrackingRange(16));

    public static final Item CLOUD_WHALE_SPAWN_EGG = egg("cloud_whale", CLOUD_WHALE);
    public static final Item SHOCK_EEL_SPAWN_EGG = egg("shock_eel", SHOCK_EEL);
    public static final Item SPARKSKINK_SPAWN_EGG = egg("sparkskink", SPARKSKINK);
    public static final Item STORM_JELLY_SPAWN_EGG = egg("storm_jelly", STORM_JELLY);
    public static final Item GALE_SPRITE_SPAWN_EGG = egg("gale_sprite", GALE_SPRITE);
    public static final Item TIDEWRACK_HERMIT_SPAWN_EGG = egg("tidewrack_hermit", TIDEWRACK_HERMIT);
    public static final Item THUNDERBIRD_SPAWN_EGG = egg("thunderbird", THUNDERBIRD);

    private WildRegistry() {}

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
        ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        ITEMS.add(Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(b, new Item.Properties().setId(ik).useBlockDescriptionPrefix())));
        return b;
    }

    private static Item item(String name, Function<Item.Properties, Item> f, Item.Properties p) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, f.apply(p.setId(key)));
        ITEMS.add(it);
        return it;
    }

    private static Item egg(String mob, EntityType<?> type) {
        return item(mob + "_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(type));
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> b) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Stormreach.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key));
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(CLOUD_WHALE, CloudWhale.createAttributes());
        FabricDefaultAttributeRegistry.register(SHOCK_EEL, ShockEel.createAttributes());
        FabricDefaultAttributeRegistry.register(SPARKSKINK, Sparkskink.createAttributes());
        FabricDefaultAttributeRegistry.register(STORM_JELLY, StormJelly.createAttributes());
        FabricDefaultAttributeRegistry.register(GALE_SPRITE, GaleSprite.createAttributes());
        FabricDefaultAttributeRegistry.register(TIDEWRACK_HERMIT, TidewrackHermit.createAttributes());
        FabricDefaultAttributeRegistry.register(THUNDERBIRD, Thunderbird.createAttributes());
        SpawnPlacements.register(SPARKSKINK, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && Sparkskink.checkSpawn(l, p));
        SpawnPlacements.register(TIDEWRACK_HERMIT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && TidewrackHermit.checkSpawn(l, p));
        SpawnPlacements.register(SHOCK_EEL, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && WaterAnimal.checkSurfaceWaterAnimalSpawnRules(t, l, r, p, rnd));
        SpawnPlacements.register(CLOUD_WHALE, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && StormreachConfig.realmMobSpawning && p.getY() > l.getMinY() + 40 && rnd.nextInt(3) == 0);
        SpawnPlacements.register(STORM_JELLY, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && StormreachConfig.realmMobSpawning && p.getY() > l.getMinY() + 40);
        SpawnPlacements.register(GALE_SPRITE, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (t, l, r, p, rnd) -> StormreachConfig.wildlifeSpawning && StormreachConfig.realmMobSpawning && p.getY() > l.getMinY() + 40);
        SpawnPlacements.register(THUNDERBIRD, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (t, l, r, p, rnd) -> false); // only ever arrives with a thunderstorm (below)
        ServerTickEvents.END_SERVER_TICK.register(server -> thunderbirdArrivals(server.overworld()));
    }

    /**
     * In a thunderstorm, now and then a Thunderbird arrives over a player who is out under the open sky
     * (thunderbirdChance in stormreach.properties, as a percent per minute per player; one bird per 160 blocks).
     */
    private static void thunderbirdArrivals(ServerLevel level) {
        if (level == null || !level.isThundering() || level.getGameTime() % 1200 != 600) return;
        if (!StormreachConfig.wildlifeSpawning || StormreachConfig.thunderbirdChance <= 0) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative() || !level.canSeeSky(p.blockPosition())) continue;
            if (level.getRandom().nextInt(100) >= StormreachConfig.thunderbirdChance) continue;
            if (!level.getEntitiesOfClass(Thunderbird.class, p.getBoundingBox().inflate(160)).isEmpty()) continue;
            Thunderbird bird = THUNDERBIRD.create(level, EntitySpawnReason.EVENT);
            if (bird == null) continue;
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(p.getX() + Math.cos(a) * 40, 0, p.getZ() + Math.sin(a) * 40));
            bird.snapTo(ground.getX() + 0.5, Math.max(ground.getY(), p.getY()) + 28, ground.getZ() + 0.5, (float) Math.toDegrees(a) + 90f, 0f);
            level.addFreshEntity(bird);
        }
    }
}
