package com.stormreach.tornado;

import com.stormreach.Stormreach;
import com.stormreach.sim.EFScale;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoMorphology;
import com.stormreach.sim.TornadoProfile;
import com.stormreach.sim.VortexStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

/** Meteorological instruments, handheld storm gear and the creative Weather Events items. */
public final class WeatherItems {
    public static final List<Item> METEOROLOGY = new ArrayList<>();
    public static final List<Item> WEATHER_EVENTS = new ArrayList<>();

    public static final Block ANEMOMETER = block("anemometer", InstrumentBlock.Kind.ANEMOMETER);
    public static final Block THERMOMETER = block("thermometer", InstrumentBlock.Kind.THERMOMETER);
    public static final Block WEATHER_STATION = block("weather_station", InstrumentBlock.Kind.WEATHER_STATION);
    public static final Block STORM_DETECTOR = block("storm_detector", InstrumentBlock.Kind.STORM_DETECTOR);
    public static final Block TORNADO_RADAR = block("tornado_radar", InstrumentBlock.Kind.TORNADO_RADAR);
    public static final Block TORNADO_SIREN = block("tornado_siren", InstrumentBlock.Kind.TORNADO_SIREN);
    public static final Block WEATHER_MONITOR = block("weather_monitor", InstrumentBlock.Kind.WEATHER_MONITOR);

    public static final Item STORM_RADAR = item("storm_radar", p -> new Handheld(p, true), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).useCooldown(1.0f), METEOROLOGY);
    public static final Item WEATHER_RADIO = item("weather_radio", p -> new Handheld(p, false), new Item.Properties().stacksTo(1).useCooldown(1.0f), METEOROLOGY);

    static {
        for (SpawnerKind k : SpawnerKind.values()) {
            item("tornado_spawner_" + k.name().toLowerCase(), p -> new TornadoSpawner(p, k), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), WEATHER_EVENTS);
        }
        for (EventKind k : EventKind.values()) {
            item("weather_event_" + k.name().toLowerCase(), p -> new EventItem(p, k), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), WEATHER_EVENTS);
        }
    }

    private WeatherItems() {}

    public static void init() {}

    private static Block block(String name, InstrumentBlock.Kind kind) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Stormreach.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, new InstrumentBlock(kind, BlockBehaviour.Properties.of().setId(key)
                .mapColor(MapColor.METAL).strength(2.5f, 6.0f).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion()));
        ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        METEOROLOGY.add(Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(b, new Item.Properties().setId(ik).useBlockDescriptionPrefix())));
        return b;
    }

    private static Item item(String name, Function<Item.Properties, Item> f, Item.Properties props, List<Item> list) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Stormreach.id(name));
        Item it = Registry.register(BuiltInRegistries.ITEM, key, f.apply(props.setId(key)));
        list.add(it);
        return it;
    }

    /** Portable radar (shows nearby tornadoes) or weather radio (forecasts and automatic tornado warnings). */
    public static class Handheld extends Item {
        private final boolean radar;

        Handheld(Properties p, boolean radar) {
            super(p);
            this.radar = radar;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel server) {
                if (radar) InstrumentBlock.radar(server, player.position(), player, 512, false);
                else {
                    player.sendSystemMessage(Component.translatable("instrument.stormreach.radio", Instruments.forecast(server),
                            Instruments.stormRisk(server, player.blockPosition())).withStyle(ChatFormatting.AQUA));
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Creative-only tornado spawners. Sneak-use cycles the intensity; use spawns where you're looking. */
    public enum SpawnerKind { NATURAL, ROPE, NEEDLE, DRILL_BIT, CONE, STOVEPIPE, WEDGE, LARGE_WEDGE, MULTI_VORTEX, SATELLITE, COMPANION, LANDSPOUT, WATERSPOUT }

    public static class TornadoSpawner extends Item {
        private final SpawnerKind kind;

        TornadoSpawner(Properties p, SpawnerKind kind) {
            super(p);
            this.kind = kind;
        }

        static int rating(ItemStack stack) {
            CustomData d = stack.get(DataComponents.CUSTOM_DATA);
            return d == null ? -1 : d.copyTag().getIntOr("ef", -1);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            int max = kind == SpawnerKind.LANDSPOUT ? 2 : kind == SpawnerKind.WATERSPOUT ? 1 : 5;
            if (player.isSecondaryUseActive()) {
                int next = rating(stack) + 1;
                if (next > max) next = -1;
                CompoundTag tag = new CompoundTag();
                tag.putInt("ef", next);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendOverlayMessage(Component.translatable("item.stormreach.tornado_spawner.rating", next < 0 ? "climatology" : "EF" + next));
                return InteractionResult.SUCCESS_SERVER;
            }
            Vec3 at = target(level, player);
            Random random = new Random(server.getRandom().nextLong());
            int ef = rating(stack);
            TornadoFormation formation = kind == SpawnerKind.LANDSPOUT ? TornadoFormation.LANDSPOUT
                    : kind == SpawnerKind.WATERSPOUT ? TornadoFormation.WATERSPOUT : TornadoFormation.SUPERCELL;
            TornadoMorphology morph = switch (kind) {
                case ROPE -> TornadoMorphology.ROPE;
                case NEEDLE -> TornadoMorphology.NEEDLE;
                case DRILL_BIT -> TornadoMorphology.DRILL_BIT;
                case CONE -> TornadoMorphology.CONE;
                case STOVEPIPE -> TornadoMorphology.STOVEPIPE;
                case WEDGE, MULTI_VORTEX, SATELLITE -> TornadoMorphology.WEDGE;
                case LARGE_WEDGE -> TornadoMorphology.LARGE_WEDGE;
                default -> null;
            };
            EFScale rating = ef < 0 ? null : EFScale.values()[ef];
            if (rating == null && (kind == SpawnerKind.MULTI_VORTEX || kind == SpawnerKind.SATELLITE)) rating = EFScale.EF4;
            TornadoProfile p = rating == null && morph == null
                    ? TornadoProfile.roll(random, formation, 1.0, TornadoManager.sizeScale(), TornadoManager.durationScale(), TornadoManager.steeringHeadingDeg())
                    : TornadoProfile.rollWith(random, formation, rating == null ? EFScale.EF2 : rating, morph, TornadoManager.sizeScale(),
                    TornadoManager.durationScale(), TornadoManager.steeringHeadingDeg());
            if (kind == SpawnerKind.MULTI_VORTEX && p.structure() == VortexStructure.SINGLE) {
                p = new TornadoProfile(p.formation(), p.potentialRating(), p.peakGustMph(), p.morphology(), VortexStructure.MULTIPLE_VORTEX,
                        3 + random.nextInt(3), p.width(), p.translationMs(), p.headingDeg(), p.rotationSign(), p.lifetimeTicks(), p.relation());
            }
            if (formation == TornadoFormation.WATERSPOUT && TornadoEntity.findWater(server, at, 48) == null) {
                player.sendOverlayMessage(Component.translatable("item.stormreach.tornado_spawner.needs_water"));
                return InteractionResult.FAIL;
            }
            TornadoEntity t = TornadoEntity.spawn(server, at, p);
            if (t == null) {
                player.sendOverlayMessage(Component.translatable("item.stormreach.tornado_spawner.cap"));
                return InteractionResult.FAIL;
            }
            if (kind == SpawnerKind.SATELLITE) TornadoManager.spawnSatellite(server, t, random);
            if (kind == SpawnerKind.COMPANION) TornadoManager.spawnCompanion(server, t, random);
            TornadoManager.announce(server, t);
            return InteractionResult.SUCCESS_SERVER;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            int ef = rating(stack);
            tooltip.accept(Component.translatable("item.stormreach.tornado_spawner.current", ef < 0 ? "climatology" : "EF" + ef).withStyle(ChatFormatting.YELLOW));
            tooltip.accept(Component.translatable("item.stormreach.tornado_spawner.help").withStyle(ChatFormatting.GRAY));
        }
    }

    public enum EventKind { SUPERCELL, DOWNBURST, HAILSTORM, BALL_LIGHTNING, DISPEL }

    public static class EventItem extends Item {
        private final EventKind kind;

        EventItem(Properties p, EventKind kind) {
            super(p);
            this.kind = kind;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            Vec3 at = target(level, player);
            switch (kind) {
                case SUPERCELL -> WeatherEvents.supercell(server, at, null, null);
                case DOWNBURST -> WeatherEvents.downburst(server, at);
                case HAILSTORM -> WeatherEvents.hail(server, at);
                case BALL_LIGHTNING -> WeatherEvents.ballLightningSwarm(server, at);
                case DISPEL -> WeatherEvents.dispel(server, player);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        }
    }

    static Vec3 target(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(96));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (hit.getType() == HitResult.Type.BLOCK) return hit.getLocation();
        Vec3 flat = player.getLookAngle().multiply(1, 0, 1).normalize().scale(40);
        return player.position().add(flat);
    }
}
