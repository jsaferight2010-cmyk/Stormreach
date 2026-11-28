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
    enum Tab { OVERVIEW, RADAR, WIND, AIR, FORECAST, BAROMETER, VANE }

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
            case 0 -> t.add(Tab.WIND);                                     // anemometer
            case 8 -> t.addAll(List.of(Tab.VANE, Tab.WIND));               // wind vane: its own dial first
            case 1 -> t.add(Tab.AIR);                                      // thermometer
            case 7 -> t.addAll(List.of(Tab.BAROMETER, Tab.AIR));           // barometer: its own dial first
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
        notePressure(r.pressure());
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
        stepDials();
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
            case BAROMETER -> barometer(g, l + 8, y);
            case VANE -> vane(g, l + 8, y, partial);
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
        line(g, x, y + 14, "wind_line", 0xFFB8E0FF, com.stormreach.client.ClientUnits.speed(report.windAvg()), com.stormreach.client.ClientUnits.speed(report.windGust()), report.windFrom());
        line(g, x, y + 28, "air_line", 0xFFB8E0FF, com.stormreach.client.ClientUnits.temperature(report.tempC()), Math.round(report.humidity()));
        line(g, x, y + 42, "risk", riskColor(report.risk()), report.risk());
        if (report.contacts().isEmpty()) {
            line(g, x, y + 62, "no_tornadoes", 0xFF7CE07C);
        } else {
            line(g, x, y + 62, "tornado_warning", 0xFFFF5050);
            int yy = y + 76;
            for (WeatherUi.Contact c : report.contacts()) {
                line(g, x + 6, yy, "contact_short", 0xFFFFB0B0, Instruments.compass(Math.toDegrees(Math.atan2(c.dx(), -c.dz()))), com.stormreach.client.ClientUnits.distance(c.distance()),
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
        line(g, x, y, "wind_avg", 0xFFFFFFFF, com.stormreach.client.ClientUnits.speed(report.windAvg()));
        line(g, x, y + 14, "wind_gust", 0xFFFFFFFF, com.stormreach.client.ClientUnits.speed(report.windGust()));
        line(g, x, y + 28, "wind_from", 0xFFB8E0FF, report.windFrom());
        g.text(font, Instruments.beaufort(report.windAvg()), x, y + 42, 0xFFB8E0FF);
        bar(g, x, y + 60, 120, report.windGust() / 120f, 0xFF60B0FF);
    }

    private void air(GuiGraphicsExtractor g, int x, int y) {
        line(g, x, y, "temperature", 0xFFFFFFFF, com.stormreach.client.ClientUnits.temperature(report.tempC()));
        line(g, x, y + 14, "dew_point", 0xFFB8E0FF, com.stormreach.client.ClientUnits.temperature(report.dewC()));
        line(g, x, y + 28, "humidity", 0xFFB8E0FF, Math.round(report.humidity()));
        bar(g, x, y + 40, 160, report.humidity() / 100f, 0xFF50A0E0);
        line(g, x, y + 54, "pressure", 0xFFFFFFFF, com.stormreach.client.ClientUnits.pressure(report.pressure()));
        bar(g, x, y + 66, 160, (report.pressure() - 960) / 80f, 0xFFB0B0FF);
        line(g, x, y + 80, "ground", 0xFFB8E0FF, Math.round(report.wetness() * 100));
        bar(g, x, y + 92, 160, report.wetness(), 0xFF4060C0);
    }

    // ------------------------------------------------------------------ barometer (6.0)

    /** Pressure samples taken from each refresh (one a second), for the trend hand. */
    private final java.util.ArrayDeque<Float> pressureHistory = new java.util.ArrayDeque<>();
    private float shownPressure = Float.NaN, setHand = Float.NaN;
    private float vaneShown = Float.NaN, vaneVel;

    private void notePressure(float p) {
        pressureHistory.addLast(p);
        while (pressureHistory.size() > 120) pressureHistory.removeFirst();
        if (Float.isNaN(setHand)) setHand = p;  // the brass "set" hand marks the reading when the screen opened
    }

    /** hPa change over the stored history (about two minutes), positive when rising. */
    private float pressureTrend() {
        if (pressureHistory.size() < 4) return 0;
        return pressureHistory.peekLast() - pressureHistory.peekFirst();
    }

    /** Dial angle for a pressure: 950 hPa at the lower left, 1050 hPa at the lower right (a 270 degree sweep). */
    private static double dialAngle(float hpa) {
        float f = Mth.clamp((hpa - 950f) / 100f, 0, 1);
        return Math.toRadians(-135 + 270 * f);
    }

    private void hand(GuiGraphicsExtractor g, int cx, int cy, double a, int len, int width, int color) {
        for (int i = 0; i < len; i++) {
            int px = cx + (int) Math.round(Math.sin(a) * i), py = cy - (int) Math.round(Math.cos(a) * i);
            int hw = i > len - 6 ? Math.max(0, width - 1) : width;
            g.fill(px - hw, py - hw, px + hw + 1, py + hw + 1, color);
        }
    }

    /** Steps the needle and the vane once a tick (so they move the same at any frame rate). */
    private void stepDials() {
        float p = report.pressure();
        shownPressure = Float.isNaN(shownPressure) ? p : shownPressure + (p - shownPressure) * 0.15f;  // the needle settles rather than jumping
        double target = compassDeg(report.windFrom());
        // a damped spring with gust wobble, so the vane swings and settles like the real thing
        if (Float.isNaN(vaneShown)) vaneShown = (float) target;
        float diff = (float) Mth.wrapDegrees(target - vaneShown);
        float gust = report.windGust() - report.windAvg();
        float wobble = (float) Math.sin(ticks * 0.7) * Mth.clamp(gust, 0, 30) * 0.5f;
        vaneVel = vaneVel * 0.8f + (diff + wobble) * 0.06f;
        vaneShown += vaneVel;
    }

    private void barometer(GuiGraphicsExtractor g, int x, int y) {
        float p = report.pressure();
        if (Float.isNaN(shownPressure)) shownPressure = p;
        int cx = left() + 214, cy = top() + 112, r = 66;
        disc(g, cx, cy, r + 4, 0xFF6A4A22);               // brass bezel
        disc(g, cx, cy, r, 0xFFEDE4CC);                    // enamel face
        // weather words around the face, as on an aneroid barometer
        String[] words = {"stormy", "rain", "change", "fair", "very_dry"};
        int[] colours = {0xFF8A1A1A, 0xFF2A4A8A, 0xFF404040, 0xFF2A6A2A, 0xFF8A6A1A};
        for (int i = 0; i < words.length; i++) {
            double a = Math.toRadians(-135 + 270 * (i + 0.5) / words.length);
            int tx = cx + (int) Math.round(Math.sin(a) * (r - 22)), ty = cy - (int) Math.round(Math.cos(a) * (r - 22));
            g.centeredText(font, Component.translatable("gui.stormreach.weather.baro." + words[i]), tx, ty - 4, colours[i]);
        }
        // tick marks every 5 hPa, long ones every 10
        for (int hpa = 950; hpa <= 1050; hpa += 5) {
            double a = dialAngle(hpa);
            int len = hpa % 10 == 0 ? 7 : 3;
            for (int i = r - len; i < r - 1; i++) {
                int px = cx + (int) Math.round(Math.sin(a) * i), py = cy - (int) Math.round(Math.cos(a) * i);
                g.fill(px, py, px + 1, py + 1, 0xFF201810);
            }
        }
        hand(g, cx, cy, dialAngle(setHand), r - 8, 0, 0xFFC09030);        // set hand
        hand(g, cx, cy, dialAngle(shownPressure), r - 6, 1, 0xFF101010);  // pointer
        disc(g, cx, cy, 4, 0xFF6A4A22);
        // readings on the left
        line(g, x, y, "pressure", 0xFFFFFFFF, com.stormreach.client.ClientUnits.pressure(p));
        float trend = pressureTrend();
        String dir = trend > 0.15f ? "rising" : trend < -0.15f ? "falling" : "steady";
        int dirCol = trend < -0.6f ? 0xFFFF6050 : trend < -0.15f ? 0xFFFFB050 : trend > 0.15f ? 0xFF80E080 : 0xFFB8E0FF;
        line(g, x, y + 16, "baro.trend." + dir, dirCol);
        String outlook = p < 990 ? "stormy" : p < 1004 ? "rain" : p < 1016 ? "change" : p < 1030 ? "fair" : "very_dry";
        if (trend < -0.6f) outlook = "stormy";
        g.textWithWordWrap(font, Component.translatable("gui.stormreach.weather.baro.outlook." + outlook), x, y + 34, 120, 0xFFB8E0FF);
        line(g, x, y + 78, "baro.hint", 0xFF808890);
    }

    // ------------------------------------------------------------------ wind vane (6.0)

    private void vane(GuiGraphicsExtractor g, int x, int y, float partial) {
        if (Float.isNaN(vaneShown)) vaneShown = (float) compassDeg(report.windFrom());
        int cx = left() + 214, cy = top() + 112, r = 66;
        disc(g, cx, cy, r + 3, 0xFF3A5A78);
        disc(g, cx, cy, r, 0xFF0E1A24);
        String[] pts = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            int tx = cx + (int) Math.round(Math.sin(a) * (r - 10)), ty = cy - (int) Math.round(Math.cos(a) * (r - 10));
            g.centeredText(font, pts[i], tx, ty - 4, i == 0 ? 0xFFFF7050 : i % 2 == 0 ? 0xFFE0E0E0 : 0xFF8090A0);
        }
        // the vane: the arrow head points INTO the wind (where it blows from); the tail fin trails downwind
        double a = Math.toRadians(vaneShown);
        hand(g, cx, cy, a, r - 22, 1, 0xFFD8D8D8);
        hand(g, cx, cy, a + Math.PI, r - 26, 1, 0xFFA0A0A0);
        for (int i = 0; i < 9; i++) {                       // arrow head
            double side = Math.toRadians(vaneShown) + Math.PI / 2;
            int bx = cx + (int) Math.round(Math.sin(a) * (r - 22 - i)), by = cy - (int) Math.round(Math.cos(a) * (r - 22 - i));
            int sx = (int) Math.round(Math.sin(side) * i * 0.6), sy = (int) Math.round(-Math.cos(side) * i * 0.6);
            g.fill(Math.min(bx - sx, bx + sx), Math.min(by - sy, by + sy), Math.max(bx - sx, bx + sx) + 1, Math.max(by - sy, by + sy) + 1, 0xFFFF7050);
        }
        for (int i = 0; i < 12; i++) {                      // tail fin
            double side = Math.toRadians(vaneShown) + Math.PI / 2;
            int bx = cx - (int) Math.round(Math.sin(a) * (r - 30 - i)), by = cy + (int) Math.round(Math.cos(a) * (r - 30 - i));
            int sx = (int) Math.round(Math.sin(side) * (12 - i) * 0.7), sy = (int) Math.round(-Math.cos(side) * (12 - i) * 0.7);
            g.fill(Math.min(bx - sx, bx + sx), Math.min(by - sy, by + sy), Math.max(bx - sx, bx + sx) + 1, Math.max(by - sy, by + sy) + 1, 0xFF7090B0);
        }
        disc(g, cx, cy, 3, 0xFFE0E0E0);
        line(g, x, y, "wind_from", 0xFFFFFFFF, report.windFrom());
        line(g, x, y + 14, "wind_avg", 0xFFB8E0FF, com.stormreach.client.ClientUnits.speed(report.windAvg()));
        line(g, x, y + 28, "wind_gust", 0xFFB8E0FF, com.stormreach.client.ClientUnits.speed(report.windGust()));
        g.text(font, Instruments.beaufort(report.windAvg()), x, y + 42, 0xFFB8E0FF);
        line(g, x, y + 60, "vane.hint", 0xFF808890);
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
        // the scan itself: reflectivity (dBZ) or radial velocity, painted gate by gate from the server's scan
        if (RadarImage.prepare(report.scan(), r, velocity))
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, RadarImage.ID, cx - r, cy - r, 0f, 0f, 2 * r, 2 * r, 2 * r, 2 * r);
        for (int k = 1; k <= 4; k++) ring(g, cx, cy, r * k / 4, 0x701E4A2A);
        g.fill(cx - r, cy, cx + r, cy + 1, 0x60163A20);
        g.fill(cx, cy - r, cx + 1, cy + r, 0x60163A20);
        for (WeatherUi.Contact c : report.contacts()) {
            int px = cx + Math.round(c.dx() / range * r), py = cy + Math.round(c.dz() / range * r);
            if (Math.hypot(px - cx, py - cy) > r) continue;
            if (velocity) ring(g, px, py, 5, 0xFFFFFFFF);    // circle the couplet so it is easy to find
            else {
                g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFF2020);
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xFFFFFFFF);
            }
        }
        legend(g, l + 192, t + 120);
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
        g.text(font, Component.translatable("gui.stormreach.weather.range", com.stormreach.client.ClientUnits.distance(range)), l + 18, t + 30, 0xFF7CE07C);
        // contact list
        int x = l + 192, y = t + 30;
        if (report.contacts().isEmpty()) line(g, x, y, "no_tornadoes", 0xFF7CE07C);
        for (WeatherUi.Contact c : report.contacts()) {
            String brg = Instruments.compass(Math.toDegrees(Math.atan2(c.dx(), -c.dz())));
            g.text(font, brg + " " + com.stormreach.client.ClientUnits.distance(c.distance()) + "  " + (c.estRating() < 0 ? "EF-U" : "EF" + c.estRating()), x, y, 0xFFFF8080);
            g.text(font, com.stormreach.client.ClientUnits.speed(c.coupletMph()) + " shear", x + 4, y + 10, 0xFFB0B0B0);
            g.text(font, "moving " + Instruments.compass(c.headingDeg()) + " " + com.stormreach.client.ClientUnits.speed(c.motionMph()), x + 4, y + 20, 0xFFB0B0B0);
            y += 34;
            if (y > t + 160) break;
        }
    }

    /** Colour key under the contact list: dBZ steps, or inbound/outbound speeds in the player's units. */
    private void legend(GuiGraphicsExtractor g, int x, int y) {
        if (velocity) {
            int[] steps = {-50, -35, -20, -8, -2, 0, 2, 8, 20, 35, 50};
            for (int i = 0; i < steps.length; i++) g.fill(x + i * 9, y, x + i * 9 + 9, y + 8, com.stormreach.sim.RadarMath.velocityColour(steps[i]));
            g.text(font, Component.translatable("gui.stormreach.weather.inbound"), x, y + 10, 0xFF60FF90);
            g.text(font, Component.translatable("gui.stormreach.weather.outbound"), x + 56, y + 10, 0xFFFF7070);
            g.text(font, com.stormreach.client.ClientUnits.speed(50 / 0.44704), x, y + 22, 0xFFB0B0B0);
        } else {
            int[] steps = {5, 15, 25, 35, 45, 55, 65, 75};
            for (int i = 0; i < steps.length; i++) g.fill(x + i * 12, y, x + i * 12 + 12, y + 8, com.stormreach.sim.RadarMath.dbzColour(steps[i]));
            g.text(font, "5", x, y + 10, 0xFFB0B0B0); g.text(font, "75 dBZ", x + 64, y + 10, 0xFFB0B0B0);
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
