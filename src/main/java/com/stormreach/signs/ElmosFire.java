package com.stormreach.signs;

import com.stormreach.Stormreach;
import com.stormreach.StormreachConfig;
import com.stormreach.registry.ModSfx;
import com.stormreach.sim.StormSigns;
import com.stormreach.weather.StormWeather;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 6.0 stage 10: St. Elmo's fire. In a Thunderhead, a player out in the open above y 90 with metal in hand (the
 * {@code stormreach:conducts_lightning} item tag) starts to glow: blue sparks crawl off the tip and it hisses. After 10
 * seconds they are warned; if they stay out for 30 seconds, the charge draws a bolt down on them. Going under cover,
 * putting the metal away, getting lower or standing within 16 blocks of a lightning rod bleeds the charge off.
 */
public final class ElmosFire {
    private ElmosFire() {}

    public static final TagKey<Item> CONDUCTS = TagKey.create(Registries.ITEM, Stormreach.id("conducts_lightning"));
    private static final int ROD_RANGE = 16;
    private static final Map<UUID, Integer> CHARGE = new HashMap<>();

    public static int charge(ServerPlayer p) { return CHARGE.getOrDefault(p.getUUID(), 0); }

    public static void resetForNewWorld() { CHARGE.clear(); }

    public static boolean holdingMetal(ServerPlayer p) {
        return p.getMainHandItem().is(CONDUCTS) || p.getOffhandItem().is(CONDUCTS);
    }

    public static boolean rodNearby(ServerLevel level, BlockPos pos) {
        return level.getPoiManager().findClosest(h -> h.is(PoiTypes.LIGHTNING_ROD), pos, ROD_RANGE, PoiManager.Occupancy.ANY).isPresent();
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 7) return;
        boolean thunder = StormreachConfig.elmosFire && level.isThundering() && level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
        for (ServerPlayer p : level.players()) {
            int before = charge(p);
            if (!thunder && before == 0) continue;
            BlockPos head = BlockPos.containing(p.getEyePosition());
            boolean metal = holdingMetal(p);
            boolean exposed = !p.isSpectator() && level.canSeeSky(head);
            boolean rod = thunder && metal && exposed && rodNearby(level, p.blockPosition());
            int now = StormSigns.elmoStep(before, thunder, exposed, metal, p.getBlockY(), rod);
            if (now == 0) CHARGE.remove(p.getUUID()); else CHARGE.put(p.getUUID(), now);
            if (now > before) glow(level, p, now);
            if (before < StormSigns.ELMO_WARN && now >= StormSigns.ELMO_WARN)
                p.sendOverlayMessage(Component.translatable("message.stormreach.elmos_fire.warn", metalName(p)).withStyle(ChatFormatting.AQUA));
            if (now >= StormSigns.ELMO_STRIKE) {
                CHARGE.remove(p.getUUID());
                if (StormreachConfig.elmosFireStrikes && !p.isCreative()) StormWeather.strike(level, p.blockPosition(), null, false);
                else StormWeather.strikeVisual(level, p.blockPosition());
            }
        }
    }

    private static Component metalName(ServerPlayer p) {
        ItemStack held = p.getMainHandItem().is(CONDUCTS) ? p.getMainHandItem() : p.getOffhandItem();
        return held.getHoverName();
    }

    /** Sparks crawl off the raised metal; more of them as the charge builds. */
    private static void glow(ServerLevel level, ServerPlayer p, int charge) {
        Vec3 look = p.getLookAngle();
        Vec3 tip = p.getEyePosition().add(look.x * 0.6, 0.35, look.z * 0.6);
        int n = 2 + charge * 8 / StormSigns.ELMO_STRIKE;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, n, 0.12, 0.25, 0.12, 0.02);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, tip.x, tip.y + 0.15, tip.z, 1, 0.05, 0.08, 0.05, 0.0);
        level.playSound(null, tip.x, tip.y, tip.z, ModSfx.AMBIENT_ELMOS_FIRE, SoundSource.WEATHER, 0.5f + charge / (float) StormSigns.ELMO_STRIKE * 0.4f,
                0.9f + level.getRandom().nextFloat() * 0.2f);
    }
}
