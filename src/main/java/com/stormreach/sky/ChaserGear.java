package com.stormreach.sky;

import com.stormreach.chase.InterceptVehicle;
import com.stormreach.progress.StormAdvancements;
import com.stormreach.sim.EFScale;
import com.stormreach.tornado.Instruments;
import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Stormchaser suit:
 * - Goggles: a heads-up readout of the nearest tornado (or the wind and pressure when there isn't one).
 * - Jacket: braced against the wind; tornadoes and downbursts push you about a third as hard.
 * - Trousers and boots: in the rain you move quicker (Speed I).
 * - The full suit: lightning can't hurt you.
 */
public final class ChaserGear {
    private ChaserGear() {}

    public static boolean wears(LivingEntity e, EquipmentSlot slot, net.minecraft.world.item.Item item) {
        return e.getItemBySlot(slot).is(item);
    }

    public static boolean fullSet(LivingEntity e) {
        return wears(e, EquipmentSlot.HEAD, SkyRegistry.CHASER_GOGGLES) && wears(e, EquipmentSlot.CHEST, SkyRegistry.CHASER_JACKET)
                && wears(e, EquipmentSlot.LEGS, SkyRegistry.CHASER_TROUSERS) && wears(e, EquipmentSlot.FEET, SkyRegistry.CHASER_BOOTS);
    }

    /** Multiplies an entity's effective mass against tornado winds. */
    public static double massFactor(Entity e) {
        return e instanceof LivingEntity le && wears(le, EquipmentSlot.CHEST, SkyRegistry.CHASER_JACKET) ? 3.0 : 1.0;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerLevel level = p.level();
            if ((wears(p, EquipmentSlot.FEET, SkyRegistry.CHASER_BOOTS) || wears(p, EquipmentSlot.LEGS, SkyRegistry.CHASER_TROUSERS))
                    && level.isRainingAt(p.blockPosition())) {
                p.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false, true));
            }
            if (fullSet(p)) StormAdvancements.award(p, "stormchaser_suit");
        }
    }

    /** 6.0: the goggles' readout is now the top-corner HUD panel (WeatherHud); this stays for reference by tests. */
    public static boolean wearsGoggles(LivingEntity e) { return wears(e, EquipmentSlot.HEAD, SkyRegistry.CHASER_GOGGLES); }

    @SuppressWarnings("unused")
    private static void goggles(ServerLevel level, ServerPlayer p) {
        TornadoEntity best = null;
        double bestD = 1024;
        for (TornadoEntity t : TornadoManager.live(level)) {
            double d = Math.hypot(t.getX() - p.getX(), t.getZ() - p.getZ());
            if (d < bestD) { bestD = d; best = t; }
        }
        Component msg;
        if (best == null) {
            double[] wind = Instruments.windMph(level, p.blockPosition());
            msg = Component.translatable("item.stormreach.stormchaser_goggles.clear", com.stormreach.units.PlayerUnits.speed(p, wind[0]), Instruments.windFrom(),
                    com.stormreach.units.PlayerUnits.pressure(p, Instruments.pressureHpa(level, p.blockPosition()))).withStyle(ChatFormatting.AQUA);
        } else {
            EFScale ef = EFScale.fromGustMph(best.currentPeakMph());
            String bearing = Instruments.compass(Math.toDegrees(Math.atan2(best.getX() - p.getX(), -(best.getZ() - p.getZ()))));
            msg = Component.translatable("item.stormreach.stormchaser_goggles.tornado", ef == null ? "EF?" : ef.name(), com.stormreach.units.PlayerUnits.distance(p, bestD), bearing,
                    com.stormreach.units.PlayerUnits.speed(p, best.currentPeakMph())).withStyle(bestD < 120 ? ChatFormatting.RED : ChatFormatting.GOLD);
        }
        p.sendOverlayMessage(msg);
    }
}
