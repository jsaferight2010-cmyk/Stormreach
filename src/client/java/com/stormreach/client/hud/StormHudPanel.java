package com.stormreach.client.hud;

import com.stormreach.Stormreach;
import com.stormreach.client.ClientUnits;
import com.stormreach.client.ClientWeather;
import com.stormreach.tornado.Instruments;
import com.stormreach.weather.WeatherHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 6.0: the Storm Chaser Goggles / TIV dashboard as a compact panel in the top-left corner of the HUD (it used to be
 * an action-bar line at the bottom centre, over the hotbar). Shows the local wind with a direction arrow relative to
 * where you are facing, temperature and pressure, and, when a tornado is within range, its rating, distance and
 * bearing, motion and peak winds. Hidden with F1, in menus and when the debug screen is open. Uses the player's units.
 */
public final class StormHudPanel {
    private StormHudPanel() {}

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    public static void init() {
        HudElementRegistry.addLast(Stormreach.id("storm_hud"), (g, delta) -> draw(g));
    }

    static String arrow(float worldBearingDeg) {
        Minecraft mc = Minecraft.getInstance();
        float rel = worldBearingDeg - (mc.player == null ? 0 : mc.player.getYRot() + 180f);
        int i = Math.floorMod(Math.round(rel / 45f), 8);
        return ARROWS[i];
    }

    private static void draw(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null || mc.getDebugOverlay().showDebugScreen()) return;
        WeatherHud.Payload p = ClientWeather.last();
        if (p == null || p.mode() == WeatherHud.MODE_NONE) return;
        Font f = mc.font;
        int x = 4, y = 4, w = 132, line = 10;
        int rows = 3 + (p.tornado() ? 4 : 1) + (p.mode() == WeatherHud.MODE_TIV ? 1 : 0);
        int h = 16 + rows * line;
        int accent = p.tornado() ? (p.distance() < 120 ? 0xFFE04040 : 0xFFE0A030) : 0xFF4FB8E8;
        g.fill(x, y, x + w, y + h, 0xB0101418);
        g.fill(x, y, x + w, y + 12, 0xD0182028);
        g.fill(x, y, x + 2, y + h, accent);
        g.text(f, Component.translatable(p.mode() == WeatherHud.MODE_TIV ? "hud.stormreach.tiv" : "hud.stormreach.goggles"), x + 6, y + 2, 0xFFE8F4FF, false);
        int ty = y + 15;
        float windToDeg = (float) Math.toDegrees(Math.atan2(p.windDirX(), -p.windDirZ())), windFromDeg = windToDeg + 180f;
        String from = Instruments.compass(windFromDeg);
        g.text(f, Component.translatable("hud.stormreach.wind", ClientUnits.speed(p.windMph()), from, arrow(windToDeg)), x + 6, ty, 0xFFB8E0FF, false); ty += line;
        g.text(f, Component.translatable("hud.stormreach.gust", ClientUnits.speed(p.gustMph())), x + 6, ty, 0xFF9CC4E0, false); ty += line;
        g.text(f, Component.translatable("hud.stormreach.air", ClientUnits.temperature(p.tempC()), ClientUnits.pressure(p.pressureHpa())), x + 6, ty, 0xFF9CC4E0, false); ty += line;
        if (p.mode() == WeatherHud.MODE_TIV) {
            g.text(f, Component.translatable(p.anchored() ? "hud.stormreach.tiv.anchored" : "hud.stormreach.tiv.speed", ClientUnits.speed(p.speedMph())), x + 6, ty,
                    p.anchored() ? 0xFFFFD060 : 0xFFE8E8E8, false);
            ty += line;
        }
        if (!p.tornado()) {
            g.text(f, Component.translatable("hud.stormreach.no_tornado"), x + 6, ty, 0xFF7CE07C, false);
            return;
        }
        String ef = p.efRating() < 0 ? "EF-U" : "EF" + p.efRating();
        g.fill(x + 6, ty - 1, x + 6 + f.width(ef) + 4, ty + 9, accent);
        g.text(f, ef, x + 8, ty, 0xFF101010, false);
        g.text(f, Component.translatable("hud.stormreach.tornado.at", ClientUnits.distance(p.distance()), Instruments.compass(p.bearingDeg()), arrow(p.bearingDeg())),
                x + 12 + f.width(ef), ty, 0xFFFFFFFF, false); ty += line;
        g.text(f, Component.translatable("hud.stormreach.tornado.moving", Instruments.compass(p.headingDeg()), ClientUnits.speed(p.motionMph())), x + 6, ty, 0xFFD0D0D0, false); ty += line;
        g.text(f, Component.translatable("hud.stormreach.tornado.peak", ClientUnits.speed(p.peakMph())), x + 6, ty, 0xFFFFB0B0, false); ty += line;
        g.text(f, Component.translatable("hud.stormreach.tornado.here", ClientUnits.speed(p.hereMph())), x + 6, ty, 0xFFD0D0D0, false);
    }
}
