package com.stormreach.client.weather;

import com.stormreach.client.tornado.TornadoMesh;
import com.stormreach.sim.CloudGrowth;

import java.util.Random;

/**
 * Meshes for hailstones and storm clouds, built into a {@link TornadoMesh.Sink} with no Minecraft classes so the
 * offline preview can draw them too.
 */
public final class WeatherMesh {
    private WeatherMesh() {}

    /** One lumpy, slightly flattened ball of ice, {@code size} blocks across, centred on the origin. */
    public static void hailstone(TornadoMesh.Sink out, float size, int seed, float spin) {
        Random r = new Random(seed);
        float[] lump = new float[9];
        for (int i = 0; i < lump.length; i++) lump[i] = (float) (r.nextDouble() * 2 * Math.PI);
        // larger stones are lumpier (they grow in layers and fuse with others)
        float rough = 0.08f + Math.min(0.2f, size * 0.25f);
        ellipsoid(out, 0, size * 0.5f, 0, size * 0.5f, size * 0.42f, size * 0.5f, 12, 8, rough, lump, spin, 0xE8F4FFFF, 0xB8D6EEFF, 0, 1);
    }

    /**
     * A storm cloud at growth {@code f}: base billows, the tower up to its current height, and the anvil. Positions are
     * relative to the cloud base centre. {@code width} and {@code depth} are the full-grown size in blocks.
     */
    public static void stormCloud(TornadoMesh.Sink out, float f, float width, float depth, boolean severe, int seed, float time) {
        Random r = new Random(seed);
        float op = (float) CloudGrowth.opacity(f);
        if (op <= 0.01f) return;
        float top = depth * (float) CloudGrowth.tower(f);
        float baseW = width * 0.5f * (float) CloudGrowth.base(f);
        float anvil = (float) CloudGrowth.anvil(f);
        int dark = severe ? 0x464B53 : 0x5E646C, mid = severe ? 0xA2A8B0 : 0xBCC2C9, light = 0xF5F7FA;
        // base: a ring of wide, flat-bottomed billows (the dark cloud base you see from below)
        for (int i = 0; i < 12; i++) {
            float a = (float) (i * Math.PI * 2 / 12 + r.nextDouble() * 0.4);
            float d = baseW * (0.35f + 0.55f * (float) r.nextDouble());
            float rad = baseW * (0.35f + 0.2f * (float) r.nextDouble());
            puff(out, (float) Math.cos(a) * d, rad * 0.35f, (float) Math.sin(a) * d, rad, rad * 0.55f, rad, r, dark, mid, op, time);
        }
        puff(out, 0, baseW * 0.25f, 0, baseW * 0.6f, baseW * 0.3f, baseW * 0.6f, r, dark, mid, op, time);
        // tower: stacked cauliflower billows, narrowing upward; each appears as the top climbs past it
        int levels = 9;
        for (int k = 0; k < levels; k++) {
            float hk = (k + 0.5f) / levels;
            float y = depth * hk;
            float grown = clamp((top - y) / (depth * 0.08f) + 0.15f, 0, 1);
            if (grown <= 0) continue;
            // a real storm tower is nearly as wide as it is tall: keep the column broad, narrowing only a little
            float colR = baseW * (0.95f - 0.25f * hk);
            int n = 6 + (k % 2);
            for (int i = 0; i < n; i++) {
                float a = (float) (i * Math.PI * 2 / n + k * 0.7 + r.nextDouble() * 0.5);
                float d = colR * (0.45f + 0.15f * (float) r.nextDouble());
                float rad = colR * (0.5f + 0.2f * (float) r.nextDouble()) * grown;
                int c1 = lerp(mid, light, hk), c0 = lerp(dark, mid, hk);
                puff(out, (float) Math.cos(a) * d, y, (float) Math.sin(a) * d, rad, rad * 0.9f, rad, r, c0, c1, op, time);
            }
            if (grown > 0.3f) puff(out, 0, y, 0, colR * 0.6f * grown, colR * 0.5f * grown, colR * 0.6f * grown, r, lerp(dark, mid, hk), lerp(mid, light, hk), op, time);
        }
        // the growing top: a bright, bulging cauliflower head
        if (anvil < 0.6f) {
            float rad = baseW * 0.75f;
            puff(out, 0, top - rad * 0.3f, 0, rad, rad * 0.8f, rad, r, mid, light, op, time);
            for (int i = 0; i < 4; i++) {
                float a = (float) (i * Math.PI / 2 + 0.4);
                puff(out, (float) Math.cos(a) * rad * 0.6f, top - rad * 0.55f, (float) Math.sin(a) * rad * 0.6f, rad * 0.6f, rad * 0.5f, rad * 0.6f, r, mid, light, op, time);
            }
        }
        // anvil: wide flat puffs at the top, sheared downwind (+x) and thinning at the edges
        if (anvil > 0) {
            float spread = width * 2.2f * anvil;
            for (int i = 0; i < 24; i++) {
                float a = (float) (i * Math.PI * 2 / 12 + r.nextDouble() * 0.3);
                float d = spread * (i < 12 ? 0.25f + 0.2f * (float) r.nextDouble() : 0.55f + 0.3f * (float) r.nextDouble());
                float x = (float) Math.cos(a) * d + spread * 0.35f, z = (float) Math.sin(a) * d * 0.8f;
                float rad = width * (0.5f + 0.2f * (float) r.nextDouble()) * (0.4f + 0.6f * anvil);
                float edge = 1 - 0.6f * d / Math.max(1, spread);
                puff(out, x, top - rad * 0.12f, z, rad, rad * 0.2f, rad, r, mid, light, op * edge, time);
            }
            puff(out, spread * 0.25f, top, 0, width * 0.5f * anvil + 4, width * 0.12f, width * 0.5f * anvil + 4, r, mid, light, op, time);
        }
    }

    private static void puff(TornadoMesh.Sink out, float x, float y, float z, float rx, float ry, float rz, Random r, int bottom, int top, float op, float time) {
        float[] lump = new float[9];
        for (int i = 0; i < lump.length; i++) lump[i] = (float) (r.nextDouble() * 2 * Math.PI);
        int a = Math.round(235 * clamp(op, 0, 1));
        ellipsoid(out, x, y, z, rx, ry, rz, 10, 7, 0.16f, lump, time * 0.002f, (bottom << 8) | a, (top << 8) | a, 1, 0);
    }

    /**
     * A lumpy ellipsoid. Colours are RGBA (not ARGB) so the hailstone can pass constant colours; {@code shadeBottom}
     * picks the bottom colour for the lower half, {@code shadeTop} weights how much the top colour is used up top.
     */
    private static void ellipsoid(TornadoMesh.Sink out, float cx, float cy, float cz, float rx, float ry, float rz, int seg, int rings,
                                  float rough, float[] lump, float spin, int rgbaTop, int rgbaBottom, int topIsSecond, int unused) {
        float[][] px = new float[rings + 1][seg + 1], py = new float[rings + 1][seg + 1], pz = new float[rings + 1][seg + 1];
        int[] cols = new int[rings + 1];
        for (int i = 0; i <= rings; i++) {
            float v = (float) i / rings;
            float phi = (float) (Math.PI * v);              // 0 bottom .. PI top
            float sy = -(float) Math.cos(phi), sr = (float) Math.sin(phi);
            for (int j = 0; j <= seg; j++) {
                float th = (float) (j * Math.PI * 2 / seg);
                float bump = 1 + rough * ((float) Math.sin(th * 2 + lump[0] + phi * 2) * 0.5f + (float) Math.sin(th * 3 + lump[1] - phi * 3) * 0.3f
                        + (float) Math.sin(phi * 4 + lump[2]) * 0.2f);
                float t = th + spin;
                px[i][j] = cx + (float) Math.cos(t) * sr * rx * bump;
                pz[i][j] = cz + (float) Math.sin(t) * sr * rz * bump;
                py[i][j] = cy + sy * ry * bump;
            }
            // flattened, darker underside; brighter, sunlit top
            int lo = topIsSecond == 1 ? rgbaTop : rgbaBottom, hi = topIsSecond == 1 ? rgbaBottom : rgbaTop;
            int c = lerpRgba(lo, hi, (sy + 1) / 2);
            cols[i] = ((c & 0xFF) << 24) | (c >>> 8);       // RGBA -> ARGB
        }
        for (int i = 0; i < rings; i++) {
            float v0 = (float) i / rings, v1 = (float) (i + 1) / rings;
            for (int j = 0; j < seg; j++) {
                float u0 = (float) j / seg, u1 = (float) (j + 1) / seg;
                out.vertex(px[i][j], py[i][j], pz[i][j], u0, 1 - v0, cols[i]);
                out.vertex(px[i][j + 1], py[i][j + 1], pz[i][j + 1], u1, 1 - v0, cols[i]);
                out.vertex(px[i + 1][j + 1], py[i + 1][j + 1], pz[i + 1][j + 1], u1, 1 - v1, cols[i + 1]);
                out.vertex(px[i + 1][j], py[i + 1][j], pz[i + 1][j], u0, 1 - v1, cols[i + 1]);
            }
        }
    }

    private static int lerp(int a, int b, float k) {
        k = clamp(k, 0, 1);
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * k);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * k);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * k);
        return (r << 16) | (g << 8) | bl;
    }

    private static int lerpRgba(int a, int b, float k) {
        int rgb = lerp(a >>> 8, b >>> 8, k);
        int al = (int) ((a & 255) + ((b & 255) - (a & 255)) * clamp(k, 0, 1));
        return (rgb << 8) | al;
    }

    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
}
