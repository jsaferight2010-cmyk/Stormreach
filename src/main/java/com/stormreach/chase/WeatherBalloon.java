package com.stormreach.chase;

import com.stormreach.progress.StormAdvancements;
import com.stormreach.registry.ModSfx;
import com.stormreach.tornado.Instruments;
import com.stormreach.weather.cloud.StormCellEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A weather balloon carrying a radiosonde. It climbs about 5 blocks a second, drifting with the wind; when it reaches
 * the storm tops (or after 40 seconds) it bursts and radios back a sounding to whoever launched it: the temperature and
 * moisture at the ground and aloft, instability (CAPE), wind shear, the freezing level, and the tornado risk.
 */
public class WeatherBalloon extends PathfinderMob {
    private UUID owner;
    private BlockPos launch;
    private double peakY;

    public WeatherBalloon(EntityType<? extends WeatherBalloon> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2);
    }

    public void launch(ServerPlayer by) {
        owner = by.getUUID();
        launch = by.blockPosition();
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 wind = com.stormreach.weather.StormWeather.windVector().scale(level().isThundering() ? 0.06 : 0.025);
        setDeltaMovement(wind.x, 0.25, wind.z);
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
        if (!(level() instanceof ServerLevel server)) return;
        peakY = Math.max(peakY, getY());
        if (verticalCollision || tickCount > 800 || getY() > Math.min(level().getMaxY() - 4, 300)) burst(server);
    }

    private void burst(ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, blockPosition(), ModSfx.ITEM_POP, SoundSource.NEUTRAL, 3.0f, 0.7f);
        if (owner != null && level.getPlayerByUUID(owner) instanceof ServerPlayer p) {
            for (Component line : sounding(level, launch == null ? blockPosition() : launch, peakY)) p.sendSystemMessage(line);
            StormAdvancements.award(p, "weather_balloon");
        }
        discard();
    }

    /** The sounding: what the radiosonde measured on the way up. */
    static java.util.List<Component> sounding(ServerLevel level, BlockPos at, double top) {
        double t = Instruments.temperatureC(level, at), rh = Instruments.humidity(level, at), dew = Instruments.dewPointC(t, rh);
        double[] wind = Instruments.windMph(level, at);
        boolean storm = level.isThundering(), rain = level.isRaining();
        int cells = 0;
        float growth = 0;
        for (StormCellEntity c : level.getEntitiesOfClass(StormCellEntity.class, new AABB(at).inflate(512, 400, 512))) {
            cells++;
            growth = Math.max(growth, c.growth());
        }
        // a simple parcel picture: moist warm air under a cold top is unstable; storms and growing cells add to it
        double spread = Math.max(0, t - dew);
        double cape = Math.max(0, (t - 5) * 90 - spread * 60 + rh * 8 + (storm ? 1200 : rain ? 400 : 0) + growth * 900);
        double shearKt = wind[1] * 0.87 + (storm ? 25 : 8) + cells * 2;
        double lcl = 125 * spread;                       // cloud base, metres above ground
        double freezing = Math.max(0, t) / 6.5 * 1000;   // standard lapse rate
        double topTemp = t - 6.5 * (top - at.getY()) / 100.0;
        String risk = Instruments.stormRisk(level, at);
        ChatFormatting col = cape > 2500 && shearKt > 40 ? ChatFormatting.RED : cape > 1000 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        return java.util.List.of(
                Component.translatable("chase.stormreach.sounding.title", at.getX(), at.getZ()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.translatable("chase.stormreach.sounding.surface", fmt(t), fmt(dew), Math.round(rh), Math.round(Instruments.pressureHpa(level, at))).withStyle(ChatFormatting.GRAY),
                Component.translatable("chase.stormreach.sounding.aloft", Math.round(top), fmt(topTemp), Math.round(lcl), Math.round(freezing)).withStyle(ChatFormatting.GRAY),
                Component.translatable("chase.stormreach.sounding.cape", Math.round(cape / 50) * 50, Math.round(shearKt)).withStyle(col),
                Component.translatable("chase.stormreach.sounding.risk", risk).withStyle(col));
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return ModSfx.ITEM_POP; }
}
