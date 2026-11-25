package com.stormreach.client.weather;

import com.stormreach.tornado.Instruments;
import com.stormreach.tornado.WeatherUi;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * One screen for every weather device: the tabs a device shows depend on what it measures (an anemometer only has
 * Wind, the Doppler radar only Radar, the Weather Monitor everything). The radar tab is a live scope: a rotating
 * sweep, reflectivity echoes for storm clouds, hail cores and downbursts, and tornado markers; switch to velocity to
 * see each tornado's rotation couplet (inbound green, outbound red).
 */
public class WeatherScreen extends Screen {
    enum Tab { OVERVIEW, RADAR, WIND, AIR, FORECAST }

    private WeatherUi.Report report;
    private Tab tab;
    private boolean velocity;
    private int ticks;
    private final List<Button> tabButtons = new ArrayList<>();
    private Button modeButton;

    public WeatherScreen(WeatherUi.Report report) {
        super(Component.translatable("gui.stormreach.weather.device." + deviceKey(report.device())));
        this.report = report;
        this.tab = tabs(report).get(0);
    }

    static String deviceKey(int device) {
        return switch (device) {
            case WeatherUi.STORM_RADAR_ITEM -> "storm_radar";
            case WeatherUi.WEATHER_RADIO_ITEM -> "weather_radio";
            default -> com.stormreach.tornado.InstrumentBlock.Kind.values()[Mth.clamp(device, 0, 8)].name().toLowerCase();
        };
    }

    static List<Tab> tabs(WeatherUi.Report r) {
        List<Tab> t = new ArrayList<>();
        switch (r.device()) {
            case 0, 8 -> t.add(Tab.WIND);                                  // anemometer, wind vane
            case 1, 7 -> t.add(Tab.AIR);                                   // thermometer, barometer
            case 2 -> t.addAll(List.of(Tab.OVERVIEW, Tab.WIND, Tab.AIR, Tab.FORECAST));
            case 4, WeatherUi.STORM_RADAR_ITEM -> t.add(Tab.RADAR);
            case 6 -> {                                                    // Weather Monitor: the central interface
                t.add(Tab.OVERVIEW);
                if (r.linked()) t.add(Tab.RADAR);
                t.addAll(List.of(Tab.WIND, Tab.AIR, Tab.FORECAST));
            }
            case WeatherUi.WEATHER_RADIO_ITEM -> t.addAll(List.of(Tab.OVERVIEW, Tab.FORECAST));
            default -> t.add(Tab.OVERVIEW);                                // storm detector, siren
        }
        return t;
    }

    public int device() { return report.device(); }

    public void update(WeatherUi.Report r) {
        this.report = r;
        if (!tabs(r).contains(tab)) tab = tabs(r).get(0);
    }

    private int left() { return (width - 300) / 2; }
    private int top() { return (height - 200) / 2; }

    @Override
    protected void init() {
        tabButtons.clear();
        List<Tab> list = tabs(report);
        int x = left() + 4;
        for (Tab t : list) {
            Button b = addRenderableWidget(Button.builder(Component.translatable("gui.stormreach.weather.tab." + t.name().toLowerCase()), btn -> tab = t)
                    .bounds(x, top() + 4, 56, 16).build());
            tabButtons.add(b);
            x += 58;
        }
        modeButton = addRenderableWidget(Button.builder(Component.translatable("gui.stormreach.weather.reflectivity"), b -> velocity = !velocity)
                .bounds(left() + 210, top() + 178, 86, 16).build());
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks % 20 == 0) ClientPlayNetworking.send(new WeatherUi.RefreshPayload(report.device(), report.pos()));
        modeButton.visible = tab == Tab.RADAR;
        modeButton.setMessage(Component.translatable(velocity ? "gui.stormreach.weather.velocity" : "gui.stormreach.weather.reflectivity"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        int l = left(), t = top();
        g.centeredText(font, title, width / 2, t - 12, 0xFFFFFFFF);
        int y = t + 26;
        switch (tab) {
            case OVERVIEW -> overview(g, l + 8, y);
            case RADAR -> radar(g, l, t, partial);
            case WIND -> wind(g, l + 8, y);
            case AIR -> air(g, l + 8, y);
            case FORECAST -> forecast(g, l + 8, y);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        int l = left(), t = top();
        g.fill(l, t, l + 300, t + 200, 0xE0101820);
        g.fill(l, t + 22, l + 300, t + 23, 0xFF3A5A78);
        g.outline(l, t, 300, 200, 0xFF5A8AB8);
    }

    // ------------------------------------------------------------------ tabs

    private void line(GuiGraphicsExtractor g, int x, int y, String key, int color, Object... args) {
        g.text(font, Component.translatable("gui.stormreach.weather." + key, args), x, y, color);
    }

    private static String sky(int s) { return s == 2 ? "thunderstorm" : s == 1 ? "rain" : "clear"; }

    private void overview(GuiGraphicsExtractor g, int x, int y) {
        line(g, x, y, "sky", 0xFFFFFFFF, Component.translatable("gui.stormreach.weather.sky." + sky(report.sky())));
        line(g, x, y + 14, "wind_line", 0xFFB8E0FF, Math.round(report.windAvg()), Math.round(report.windGust()), report.windFrom());
        line(g, x, y + 28, "air_line", 0xFFB8E0FF, String.format("%.1f", report.tempC()), Math.round(report.humidity()));
        line(g, x, y + 42, "risk", riskColor(report.risk()), report.risk());
        if (report.contacts().isEmpty()) {
            line(g, x, y + 62, "no_tornadoes", 0xFF7CE07C);
        } else {
            line(g, x, y + 62, "tornado_warning", 0xFFFF5050);
            int yy = y + 76;
            for (WeatherUi.Contact c : report.contacts()) {
                line(g, x + 6, yy, "contact_short", 0xFFFFB0B0, Instruments.compass(Math.toDegrees(Math.atan2(c.dx(), -c.dz()))), Math.round(c.distance()),
                        c.estRating() < 0 ? "EF-U" : "EF" + c.estRating());
                yy += 12;
                if (yy > top() + 190) break;
            }
        }
    }

    private void wind(GuiGraphicsExtractor g, int x, int y) {
        // compass rose with an arrow pointing where the wind blows from
        int cx = left() + 220, cy = top() + 105, r = 50;
        ring(g, cx, cy, r, 0xFF5A8AB8);
        ring(g, cx, cy, r - 1, 0xFF2A3A4A);
        g.centeredText(font, "N", cx, cy - r - 10, 0xFFFFFFFF);
        g.centeredText(font, "S", cx, cy + r + 3, 0xFFB0B0B0);
        g.text(font, "E", cx + r + 4, cy - 4, 0xFFB0B0B0);
        g.text(font, "W", cx - r - 10, cy - 4, 0xFFB0B0B0);
        double from = compassDeg(report.windFrom());
        double a = Math.toRadians(from);
        for (int i = 0; i < r - 6; i++) {
            int px = cx + (int) Math.round(Math.sin(a) * i), py = cy - (int) Math.round(Math.cos(a) * i);
            g.fill(px - 1, py - 1, px + 2, py + 2, i > r - 14 ? 0xFFFF7050 : 0xFFE0E0E0);
        }
        line(g, x, y, "wind_avg", 0xFFFFFFFF, Math.round(report.windAvg()), Math.round(report.windAvg() * 1.609));
        line(g, x, y + 14, "wind_gust", 0xFFFFFFFF, Math.round(report.windGust()), Math.round(report.windGust() * 1.609));
        line(g, x, y + 28, "wind_from", 0xFFB8E0FF, report.windFrom());
        g.text(font, Instruments.beaufort(report.windAvg()), x, y + 42, 0xFFB8E0FF);
        bar(g, x, y + 60, 120, report.windGust() / 120f, 0xFF60B0FF);
    }

    private void air(GuiGraphicsExtractor g, int x, int y) {
        line(g, x, y, "temperature", 0xFFFFFFFF, String.format("%.1f", report.tempC()), String.format("%.1f", report.tempC() * 9 / 5 + 32));
        line(g, x, y + 14, "dew_point", 0xFFB8E0FF, String.format("%.1f", report.dewC()));
        line(g, x, y + 28, "humidity", 0xFFB8E0FF, Math.round(report.humidity()));
        bar(g, x, y + 40, 160, report.humidity() / 100f, 0xFF50A0E0);
        line(g, x, y + 54, "pressure", 0xFFFFFFFF, String.format("%.1f", report.pressure()), String.format("%.2f", report.pressure() * 0.02953));
        bar(g, x, y + 66, 160, (report.pressure() - 960) / 80f, 0xFFB0B0FF);
        line(g, x, y + 80, "ground", 0xFFB8E0FF, Math.round(report.wetness() * 100));
        bar(g, x, y + 92, 160, report.wetness(), 0xFF4060C0);
    }

    private void forecast(GuiGraphicsExtractor g, int x, int y) {
        line(g, x, y, "sky", 0xFFFFFFFF, Component.translatable("gui.stormreach.weather.sky." + sky(report.sky())));
        g.textWithWordWrap(font, Component.translatable("gui.stormreach.weather.forecast", report.forecast()), x, y + 16, 280, 0xFFB8E0FF);
        line(g, x, y + 56, "risk", riskColor(report.risk()), report.risk());
        String[] cats = {"none", "general thunder", "marginal", "slight", "enhanced", "moderate", "high"};
        int[] cols = {0xFF404040, 0xFF60C060, 0xFF80E080, 0xFFF0F060, 0xFFF0A040, 0xFFF04040, 0xFFE040E0};
        for (int i = 0; i < cats.length; i++) {
            int bx = x + i * 40;
            g.fill(bx, y + 72, bx + 38, y + 82, cats[i].equals(report.risk()) ? cols[i] : (cols[i] & 0x00FFFFFF) | 0x50000000);
        }
    }

    private void radar(GuiGraphicsExtractor g, int l, int t, float partial) {
        int cx = l + 100, cy = t + 110, r = 82;
        float range = report.range();
        disc(g, cx, cy, r, 0xFF06140A);
        for (int k = 1; k <= 4; k++) ring(g, cx, cy, r * k / 4, 0xFF1E4A2A);
        g.fill(cx - r, cy, cx + r, cy + 1, 0xFF163A20);
        g.fill(cx, cy - r, cx + 1, cy + r, 0xFF163A20);
        // echoes
        for (WeatherUi.Cell c : report.cells()) {
            int px = cx + Math.round(c.dx() / range * r), py = cy + Math.round(c.dz() / range * r);
            float size = Math.max(2, c.width() * (0.4f + c.growth()) / range * r);
            if (velocity) { disc(g, px, py, Math.round(size), 0x3050A050); continue; }
            float strength = (float) Math.sin(Math.PI * Math.min(1, c.growth() * 1.2)) * (c.severe() ? 1.2f : 0.85f);
            disc(g, px, py, Math.round(size), 0xA020C040);
            if (strength > 0.35f) disc(g, px, py, Math.round(size * 0.65f), 0xC0E0E030);
            if (strength > 0.7f) disc(g, px, py, Math.round(size * 0.35f), 0xE0F03020);
            if (strength > 1.0f) disc(g, px, py, Math.max(1, Math.round(size * 0.15f)), 0xFFE020E0);
        }
        for (WeatherUi.Core c : report.cores()) {
            int px = cx + Math.round(c.dx() / range * r), py = cy + Math.round(c.dz() / range * r);
            int rad = Math.max(2, Math.round(c.radius() / range * r));
            if (c.kind() == 0) disc(g, px, py, rad, velocity ? 0x40FFFFFF : 0xD0F0F0FF);      // hail: white/pink core
            else ring(g, px, py, rad, c.kind() == 1 ? 0xFF4080FF : 0xFFC0A060);              // downburst outflow
        }
        for (WeatherUi.Contact c : report.contacts()) {
            int px = cx + Math.round(c.dx() / range * r), py = cy + Math.round(c.dz() / range * r);
            if (Math.hypot(px - cx, py - cy) > r) continue;
            if (velocity) {
                // rotation couplet: inbound (green) beside outbound (red), brighter for stronger rotation
                int a = Mth.clamp((int) (c.coupletMph() / 300f * 255), 90, 255);
                disc(g, px - 3, py, 3, (a << 24) | 0x20FF40);
                disc(g, px + 3, py, 3, (a << 24) | 0xFF3030);
            } else {
                g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFF2020);
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xFFFFFFFF);
            }
        }
        // the sweep, with a fading trail
        double sweep = (ticks + partial) * 0.06;
        for (int k = 0; k < 10; k++) {
            double a = sweep - k * 0.03;
            int alpha = 200 - k * 18;
            for (int i = 4; i < r; i += 2) {
                int px = cx + (int) Math.round(Math.sin(a) * i), py = cy - (int) Math.round(Math.cos(a) * i);
                g.fill(px, py, px + 1, py + 1, (alpha << 24) | 0x40FF60);
            }
        }
        g.text(font, Component.translatable("gui.stormreach.weather.range", Math.round(range)), l + 18, t + 30, 0xFF7CE07C);
        // contact list
        int x = l + 192, y = t + 30;
        if (report.contacts().isEmpty()) line(g, x, y, "no_tornadoes", 0xFF7CE07C);
        for (WeatherUi.Contact c : report.contacts()) {
            String brg = Instruments.compass(Math.toDegrees(Math.atan2(c.dx(), -c.dz())));
            g.text(font, brg + " " + Math.round(c.distance()) + "m  " + (c.estRating() < 0 ? "EF-U" : "EF" + c.estRating()), x, y, 0xFFFF8080);
            g.text(font, Math.round(c.coupletMph()) + " mph shear", x + 4, y + 10, 0xFFB0B0B0);
            g.text(font, "moving " + Instruments.compass(c.headingDeg()) + " " + Math.round(c.motionMph()) + " mph", x + 4, y + 20, 0xFFB0B0B0);
            y += 34;
            if (y > t + 160) break;
        }
    }

    // ------------------------------------------------------------------ drawing helpers

    private static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, argb);
        }
    }

    private static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int argb) {
        int steps = Math.max(24, r * 6);
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int px = cx + (int) Math.round(Math.cos(a) * r), py = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, argb);
        }
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float f, int color) {
        g.fill(x, y, x + w, y + 6, 0xFF202830);
        g.fill(x, y, x + Math.round(w * Mth.clamp(f, 0, 1)), y + 6, color);
    }

    private static int riskColor(String risk) {
        return switch (risk) {
            case "high" -> 0xFFE040E0;
            case "moderate" -> 0xFFF04040;
            case "enhanced" -> 0xFFF0A040;
            case "slight" -> 0xFFF0F060;
            case "marginal", "general thunder" -> 0xFF80E080;
            default -> 0xFFB0B0B0;
        };
    }

    private static double compassDeg(String pt) {
        String[] pts = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        for (int i = 0; i < pts.length; i++) if (pts[i].equals(pt)) return i * 22.5;
        return 0;
    }

    /** Registers the receiver that opens and refreshes these screens. */
    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(WeatherUi.ReportPayload.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (payload.open()) mc.gui.setScreen(new WeatherScreen(payload.report()));
            else if (mc.gui.screen() instanceof WeatherScreen ws && ws.device() == payload.report().device()) ws.update(payload.report());
        }));
    }
}
