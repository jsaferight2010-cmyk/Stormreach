package com.stormreach.client.tornado;

import com.stormreach.sim.FunnelShape;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoMorphology;

/**
 * The tornado's meshes, built as quads (4 vertices each) into a {@link Sink}. Kept apart from the renderer so the
 * same geometry can be rasterised offline into preview images (tools/preview) without starting the game.
 */
public final class TornadoMesh {
    private TornadoMesh() {}

    /** Receives vertices in quad order. Colours are ARGB. */
    public interface Sink {
        void vertex(float x, float y, float z, float u, float v, int argb);
    }

    // ------------------------------------------------------------------ layers (what the renderer submits)

    static TornadoMorphology morph(TornadoRenderState s) {
        return TornadoMorphology.values()[M.clamp(s.morph, 0, TornadoMorphology.values().length - 1)];
    }

    static TornadoFormation formation(TornadoRenderState s) {
        return TornadoFormation.values()[M.clamp(s.formation, 0, TornadoFormation.values().length - 1)];
    }

    static float time(TornadoRenderState s) {
        return s.ageInTicks + s.seed * 100;
    }

    /** The funnel, its sheath and any suction vortices (texture: tornado_funnel). */
    public static void funnelLayer(TornadoRenderState s, Sink out) {
        TornadoFormation formation = formation(s);
        boolean water = formation == TornadoFormation.WATERSPOUT;
        float t = time(s);
        // palette: supercell funnels are slate grey and darken with debris near the ground; landspouts are dusty;
        // waterspouts are pale and milky with a white spray vortex at the base
        int top = water ? 0xC9D4DC : formation == TornadoFormation.LANDSPOUT ? 0x9C9184 : 0x6F747B;
        int bottom = water ? 0xE6EEF3 : formation == TornadoFormation.LANDSPOUT ? 0x8A7559 : 0x4E4A45;
        float strength = 0.55f + 0.45f * M.clamp(s.intensity, 0, 1);
        int light = 0;
        funnel(out, s, morph(s), t, 1.0f, 0f, top, bottom, (int) (230 * strength), light, 1f);
        funnel(out, s, morph(s), t, 1.22f, 3.1f, top, bottom, (int) (90 * strength), light, -0.6f); // outer sheath (same centreline, its own lumps)
        if (s.subvortices > 0 && s.intensity > 0.5f && FunnelShape.touchedDown(s.reach)) {
            float orbit = s.width * 0.5f * 0.75f;
            for (int i = 0; i < s.subvortices; i++) {
                double a = s.rotation * t * 0.06 + i * Math.PI * 2 / s.subvortices;
                subvortex(out, s, t + i * 31, (float) Math.cos(a) * orbit, (float) Math.sin(a) * orbit, bottom, light);
            }
        }
    }

    public static boolean hasDebris(TornadoRenderState s) {
        return FunnelShape.dustWhirl(s.reach);
    }

    /** The debris (or spray) cloud at the base (texture: tornado_debris). */
    public static void debrisLayer(TornadoRenderState s, Sink out) {
        TornadoFormation formation = formation(s);
        boolean water = formation == TornadoFormation.WATERSPOUT;
        float down = (float) Math.pow(M.clamp((s.reach - 0.35f) / 0.65f, 0, 1), 4); // a low dust whirl until touchdown, then the full cloud
        int dust = water ? 0xF2F7FA : formation == TornadoFormation.LANDSPOUT ? 0x9E8763 : 0x6E5E4B;
        debrisCloud(out, s, time(s), down, dust, 0, water);
    }

    public static boolean hasWallCloud(TornadoRenderState s) {
        return true; // every tornado hangs from a cloud base; only supercells get the lowered, rotating wall cloud
    }

    /** The lowered wall cloud (texture: tornado_wallcloud). */
    public static void wallLayer(TornadoRenderState s, Sink out) {
        wallCloud(out, s, time(s), 0);
    }

    // ------------------------------------------------------------------ meshes

    private static final int SEG = 36;

    /** The condensation funnel (layer scale 1) or its looser outer sheath (scale > 1). */
    static void funnel(Sink out, TornadoRenderState s, TornadoMorphology morph, float t,
                               float scale, float noiseOffset, int topColor, int bottomColor, int alpha, int light, float spinMul) {
        int rings = 44;
        float base = s.cloudBase;
        float tip = (float) FunnelShape.tipHeight(base, s.reach);
        float half = s.width * 0.5f;
        float[][] xs = new float[rings + 1][SEG + 1], ys = new float[rings + 1][SEG + 1], zs = new float[rings + 1][SEG + 1];
        int[] cols = new int[rings + 1];
        for (int i = 0; i <= rings; i++) {
            float k = (float) i / rings;
            float y = tip + (base - 0.5f - tip) * (float) Math.pow(k, 1.25);   // rings bunch up near the bottom, where detail matters
            float h = y / base;
            float r = (float) FunnelShape.radius(morph, half, Math.min(y, base), base, s.reach) * scale;
            if (i == 0 && !FunnelShape.touchedDown(s.reach)) r = 0;
            // ropes and needles snake about; big funnels lean and sway a little
            float sway = (morph == TornadoMorphology.ROPE ? 4.5f : morph == TornadoMorphology.NEEDLE ? 1.8f : morph == TornadoMorphology.DRILL_BIT ? 2.4f : 0.9f)
                    * (1 - h) * (1 - h) * Math.min(1, base / 40f);
            float cx = M.sin(t * 0.045f + y * 0.11f) * sway + M.sin(t * 0.013f + 1.3f) * sway * 0.5f;
            float cz = M.cos(t * 0.039f + y * 0.09f) * sway;
            // spins faster near the ground; the bands also climb (upflow)
            // rigid per layer (per-height speeds would shear the mesh apart over time); the sheath lags the core
            float spin = s.rotation * spinMul * s.spin / scale;
            for (int j = 0; j <= SEG; j++) {
                float a = (float) (j * Math.PI * 2 / SEG);
                float lump = 1 + 0.14f * noise(a * 2.0f + noiseOffset, y * 0.07f - t * 0.035f, t * 0.01f)
                        + 0.07f * noise(a * 5.0f + noiseOffset, y * 0.21f - t * 0.06f, 3.3f);
                float rr = Math.max(0, r * lump);
                xs[i][j] = cx + M.cos(a + spin) * rr;
                zs[i][j] = cz + M.sin(a + spin) * rr;
                ys[i][j] = y;
            }
            // darker, browner near the ground (debris drawn into the funnel), and the tip fades out while descending
            float mix = M.clamp(1 - h * 1.6f, 0, 1);
            int a2 = alpha;
            if (!FunnelShape.touchedDown(s.reach)) a2 = (int) (alpha * M.clamp((y - tip) / Math.max(2, base * 0.12f), 0.15f, 1));
            if (h > 0.95f) a2 = (int) (a2 * M.clamp((1.03f - h) / 0.08f, 0, 1));   // melt into the cloud base
            cols[i] = (M.clamp(a2, 0, 255) << 24) | lerpColor(topColor, bottomColor, mix);
        }
        tube(out, xs, ys, zs, cols, rings, light);
    }

    /** One suction vortex: a thin, short, fast-spinning tube inside the main circulation. */
    static void subvortex(Sink out, TornadoRenderState s, float t, float ox, float oz, int color, int light) {
        int rings = 12;
        float height = Math.min(s.cloudBase * 0.4f, 10 + s.width * 0.5f);
        float r0 = Math.max(0.8f, s.width * 0.07f);
        float[][] xs = new float[rings + 1][SEG + 1], ys = new float[rings + 1][SEG + 1], zs = new float[rings + 1][SEG + 1];
        int[] cols = new int[rings + 1];
        for (int i = 0; i <= rings; i++) {
            float k = (float) i / rings;
            float y = k * height;
            float r = r0 * (0.7f + 0.6f * k);
            for (int j = 0; j <= SEG; j++) {
                float a = (float) (j * Math.PI * 2 / SEG) + s.rotation * t * 0.9f;
                float rr = r * (1 + 0.2f * noise(a * 3, y * 0.3f - t * 0.1f, 7.7f));
                xs[i][j] = ox + M.cos(a) * rr;
                zs[i][j] = oz + M.sin(a) * rr;
                ys[i][j] = y;
            }
            cols[i] = ((int) (200 * (1 - k * 0.9f)) << 24) | color;
        }
        tube(out, xs, ys, zs, cols, rings, light);
    }

    /** The dust and wreckage (or spray) cloud at the base: it spins up under the descending funnel and swells at touchdown. */
    static void debrisCloud(Sink out, TornadoRenderState s, float t, float down, int color, int light, boolean water) {
        int rings = 12;
        float half = s.width * 0.5f;
        float height = M.clamp(s.width * (water ? 0.5f : 0.9f), 6, 45) * (0.35f + 0.65f * down) * (0.5f + 0.5f * s.intensity);
        float alphaMax = (water ? 200 : 225) * (0.3f + 0.7f * down) * (0.5f + 0.5f * s.intensity);
        for (int layer = 0; layer < 2; layer++) {
            float[][] xs = new float[rings + 1][SEG + 1], ys = new float[rings + 1][SEG + 1], zs = new float[rings + 1][SEG + 1];
            int[] cols = new int[rings + 1];
            float spread = layer == 0 ? 1.15f : 1.6f;
            for (int i = 0; i <= rings; i++) {
                float k = (float) i / rings;
                float y = k * height - 0.3f;
                // bulges out low down and draws in toward the funnel at the top
                float r = half * spread * (1.2f + 0.35f * M.sin((float) Math.PI * Math.min(1, k * 1.6f))) * (1 - 0.35f * k) * (water && layer == 1 ? 1.4f : 1f) + 1.5f;
                for (int j = 0; j <= SEG; j++) {
                    float a0 = (float) (j * Math.PI * 2 / SEG);
                    float a = a0 + s.rotation * t * (0.12f + 0.1f * (1 - k)) * (layer == 0 ? 1 : 0.6f);
                    float rr = r * (1 + 0.3f * noise(a0 * 2 + layer * 5, y * 0.25f - t * 0.08f, t * 0.02f));
                    xs[i][j] = M.cos(a) * rr;
                    zs[i][j] = M.sin(a) * rr;
                    ys[i][j] = y + 1.2f * noise(a0 * 4, k * 3, t * 0.05f + layer);
                }
                float fade = M.clamp((1 - k) / 0.35f, 0, 1) * M.clamp(k * 6 + 0.4f, 0, 1); // solid low down, ragged top
                cols[i] = ((int) (alphaMax * fade * (layer == 0 ? 1 : 0.55f)) << 24) | color;
            }
            tube(out, xs, ys, zs, cols, rings, light);
        }
    }

    /**
     * The cloud base the funnel hangs from: a broad deck that fades out at its edges, and under a supercell a lowered,
     * slowly rotating wall cloud around the funnel. Tornado cloud bases sit far below Minecraft's own cloud layer,
     * so without this the funnel would end in clear air.
     */
    static void wallCloud(Sink out, TornadoRenderState s, float t, int light) {
        boolean supercell = formation(s) == TornadoFormation.SUPERCELL;
        boolean water = formation(s) == TornadoFormation.WATERSPOUT;
        int rings = 12, seg = 48;
        float half = s.width * 0.5f;
        float inner = half * 0.5f + 0.5f, outer = Math.max(half * 7f, supercell ? 190 : 120);
        float wallEdge = inner + Math.max(half * 2.2f, 18);           // where the lowered wall cloud rises to the deck
        float drop = supercell ? 6 + half * 0.25f : 1.5f;
        float spin = s.rotation * s.spin * 0.04f;
        int dark = supercell ? 0x3A3F46 : water ? 0x8A949C : 0x6C7178, light2 = supercell ? 0x575D66 : water ? 0xA9B2BA : 0x878C93;
        float[][] xs = new float[rings + 1][seg + 1], ys = new float[rings + 1][seg + 1], zs = new float[rings + 1][seg + 1];
        int[] cols = new int[rings + 1];
        for (int i = 0; i <= rings; i++) {
            float k = (float) i / rings;
            float r = inner + (outer - inner) * k * k;                   // dense rings near the funnel, sparse far out
            float wall = M.clamp((wallEdge - r) / (wallEdge - inner), 0, 1);
            for (int j = 0; j <= seg; j++) {
                float a0 = (float) (j * Math.PI * 2 / seg);
                float a = a0 + spin * wall;                               // only the wall cloud turns
                float bump = noise(a0 * 2, k * 2.5f, t * 0.004f);
                xs[i][j] = M.cos(a) * r;
                zs[i][j] = M.sin(a) * r;
                ys[i][j] = s.cloudBase + 1.5f - drop * wall * wall - bump * (1.0f + 1.5f * wall);
            }
            float edge = M.clamp((1 - k) / 0.45f, 0, 1);                  // fade out over the outer part of the deck
            cols[i] = ((int) (235 * edge) << 24) | lerpColor(light2, dark, Math.max(wall, 0.35f));
        }
        tube(out, xs, ys, zs, cols, rings, light, seg);
    }

    // ------------------------------------------------------------------ helpers

    private static void tube(Sink out, float[][] xs, float[][] ys, float[][] zs, int[] cols, int rings, int light) {
        tube(out, xs, ys, zs, cols, rings, light, SEG);
    }

    /** Quads between neighbouring rings; u runs round the ring (0..1), v up the tube (0..1). */
    private static void tube(Sink out, float[][] xs, float[][] ys, float[][] zs, int[] cols, int rings, int light, int seg) {
        for (int i = 0; i < rings; i++) {
            float v0 = (float) i / rings, v1 = (float) (i + 1) / rings;
            for (int j = 0; j < seg; j++) {
                float u0 = (float) j / seg, u1 = (float) (j + 1) / seg;
                vertex(out, xs[i][j], ys[i][j], zs[i][j], u0, v0, cols[i], light);
                vertex(out, xs[i][j + 1], ys[i][j + 1], zs[i][j + 1], u1, v0, cols[i], light);
                vertex(out, xs[i + 1][j + 1], ys[i + 1][j + 1], zs[i + 1][j + 1], u1, v1, cols[i + 1], light);
                vertex(out, xs[i + 1][j], ys[i + 1][j], zs[i + 1][j], u0, v1, cols[i + 1], light);
            }
        }
    }

    private static void vertex(Sink out, float x, float y, float z, float u, float v, int argb, int light) {
        out.vertex(x, y, z, u, 1 - v, argb);
    }

    private static int lerpColor(int a, int b, float k) {
        int r = (int) M.lerp(k, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) M.lerp(k, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) M.lerp(k, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    /** Cheap smooth noise in -1..1 (sums of incommensurate sines), periodic in {@code a} with period 2π for integer multipliers. */
    private static float noise(float a, float y, float z) {
        return (M.sin(a + y * 1.7f + z) * 0.5f + M.sin(a * 2 - y * 1.3f + z * 1.9f + 1.1f) * 0.3f
                + M.sin(a * 3 + y * 2.9f - z * 1.3f + 2.3f) * 0.2f);
    }

    /** Tiny math helpers (no Minecraft classes, so the offline preview can run this file). */
    static final class M {
        static float sin(float a) { return (float) Math.sin(a); }
        static float cos(float a) { return (float) Math.cos(a); }
        static float sqrt(float a) { return (float) Math.sqrt(a); }
        static float lerp(float k, float a, float b) { return a + (b - a) * k; }
        static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
        static int clamp(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }
    }
}
