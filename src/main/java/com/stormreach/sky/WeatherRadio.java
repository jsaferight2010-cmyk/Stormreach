package com.stormreach.sky;

import com.stormreach.progress.StormAdvancements;
import com.stormreach.registry.ModSfx;
import com.stormreach.sim.EFScale;
import com.stormreach.tornado.Instruments;
import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import com.stormreach.wild.Thunderbird;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 5.3 additions to the Weather Radio (tornado/WeatherItems). The radio already warns of tornadoes touching down within
 * 400 blocks; carried, it now also sounds the alert tone and broadcasts a Tornado Emergency when one comes within 150
 * blocks, a Severe Thunderstorm Warning when thunder starts, a statement when rain moves in, the all-clear, and a
 * wildlife advisory when a Thunderbird is about.
 */
public final class WeatherRadio {
    private static final Map<UUID, State> STATE = new HashMap<>();

    private WeatherRadio() {}

    static final class State {
        final Set<UUID> warned = new HashSet<>();
        boolean raining, thundering, thunderbird;
    }

    static boolean carries(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(com.stormreach.tornado.WeatherItems.WEATHER_RADIO)) return true;
        return false;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!carries(p)) { STATE.remove(p.getUUID()); continue; }
            ServerLevel level = p.level();
            boolean fresh = !STATE.containsKey(p.getUUID());
            State st = STATE.computeIfAbsent(p.getUUID(), k -> new State());
            if (fresh) { st.raining = level.isRaining(); st.thundering = level.isThundering(); }
            for (TornadoEntity t : TornadoManager.live(level)) {
                double d = Math.hypot(t.getX() - p.getX(), t.getZ() - p.getZ());
                double peak = t.currentPeakMph();
                if (d > 400 || peak < 65) continue;
                StormAdvancements.award(p, "radio_warning");
                if (d > 150 || !st.warned.add(t.getUUID())) continue;
                EFScale ef = EFScale.fromGustMph(peak);
                var f = t.windField();
                String bearing = Instruments.compass(Math.toDegrees(Math.atan2(t.getX() - p.getX(), -(t.getZ() - p.getZ()))));
                String heading = Instruments.compass(Math.toDegrees(Math.atan2(f.translateX, -f.translateZ)));
                String mph = com.stormreach.units.PlayerUnits.speed(p, EFScale.msToMph(Math.hypot(f.translateX, f.translateZ)));
                alert(p, "radio.stormreach.emergency", ChatFormatting.RED,
                        Component.translatable("radio.stormreach.tornado.detail", ef == null ? "EF0" : ef.name(), com.stormreach.units.PlayerUnits.distance(p, d), bearing, heading, mph));
            }
            boolean rain = level.isRaining(), thunder = level.isThundering();
            if (thunder && !st.thundering) alert(p, "radio.stormreach.severe", ChatFormatting.GOLD, Component.translatable("radio.stormreach.severe.detail"));
            else if (rain && !st.raining) alert(p, "radio.stormreach.statement", ChatFormatting.YELLOW, Component.translatable("radio.stormreach.rain.detail"));
            else if (!rain && st.raining) alert(p, "radio.stormreach.clear", ChatFormatting.GREEN, Component.translatable("radio.stormreach.clear.detail"));
            boolean bird = !level.getEntitiesOfClass(Thunderbird.class, p.getBoundingBox().inflate(160)).isEmpty();
            if (bird && !st.thunderbird) alert(p, "radio.stormreach.thunderbird", ChatFormatting.LIGHT_PURPLE, Component.translatable("radio.stormreach.thunderbird.detail"));
            st.raining = rain; st.thundering = thunder; st.thunderbird = bird;
            st.warned.removeIf(id -> TornadoManager.live(level).stream().noneMatch(t -> t.getUUID().equals(id)));
        }
    }

    static void alert(ServerPlayer p, String key, ChatFormatting colour, Component detail) {
        p.sendSystemMessage(Component.translatable(key).withStyle(colour, ChatFormatting.BOLD));
        p.sendSystemMessage(detail.copy().withStyle(ChatFormatting.GRAY));
        p.connection.send(new ClientboundSoundPacket(ModSfx.holder(ModSfx.ITEM_RADIO_ALERT), SoundSource.PLAYERS, p.getX(), p.getY(), p.getZ(), 1.0f, 1.0f, p.getRandom().nextLong()));
    }
}
