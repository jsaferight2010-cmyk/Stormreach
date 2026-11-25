package com.stormreach.client.story;

import com.mojang.blaze3d.platform.NativeImage;
import com.stormreach.Stormreach;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Makes a storm-weathered copy of a player's skin for the Stranger: drained of colour and cold, soaked darker toward
 * the feet, rain-streaked and salt-flecked, with muddy boots, frayed hems on the outer layer, and the eyes turned
 * hollow (whites dark, pupils pale). Works on any skin, at any resolution, at runtime; results are cached per skin.
 */
public final class WeatheredSkins {
    private static final Map<Identifier, Identifier> CACHE = new HashMap<>();
    private static final Map<Identifier, Integer> FAILED_AT = new HashMap<>();

    private WeatheredSkins() {}

    /** The weathered texture for this skin, or the plain skin while the original isn't available yet. */
    public static Identifier get(PlayerSkin skin) {
        Identifier src = skin.body().texturePath();
        Identifier done = CACHE.get(src);
        if (done != null) return done;
        Minecraft mc = Minecraft.getInstance();
        int now = mc.player == null ? 0 : mc.player.tickCount;
        Integer failed = FAILED_AT.get(src);
        if (failed != null && now - failed < 100) return src;
        NativeImage img = read(mc, src);
        if (img == null) {
            FAILED_AT.put(src, now);
            return src;
        }
        try {
            int w = img.getWidth(), h = img.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = img.getPixel(x, y);
            weather(px, w, h, src.hashCode());
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setPixel(x, y, px[y * w + x]);
            Identifier id = Stormreach.id("weathered/" + Integer.toHexString(src.hashCode() & 0x7fffffff));
            mc.getTextureManager().register(id, new DynamicTexture(() -> "stormreach weathered skin", img));
            CACHE.put(src, id);
            return id;
        } catch (RuntimeException e) {
            Stormreach.LOGGER.warn("Couldn't weather skin {}", src, e);
            img.close();
            FAILED_AT.put(src, now);
            return src;
        }
    }

    private static NativeImage read(Minecraft mc, Identifier src) {
        try {
            AbstractTexture t = mc.getTextureManager().getTexture(src);
            if (t instanceof DynamicTexture d && d.getPixels() != null) {
                NativeImage p = d.getPixels();
                NativeImage copy = new NativeImage(p.getWidth(), p.getHeight(), false);
                copy.copyFrom(p);
                return copy;
            }
        } catch (RuntimeException ignored) {
        }
        try (InputStream in = mc.getResourceManager().open(src)) {
            return NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- the weathering --------------------------------------------------------------------------------------
    private static int hash(int a, int b, int seed) {
        int h = a * 73856093 ^ b * 19349663 ^ seed * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return (h ^ h >>> 15) & 0x7fffffff;
    }

    /** Where a skin pixel sits on the body: [vertical 0 (crown) .. 1 (soles), side face?, outer layer?, row within its face 0..1]. */
    private static float[] locate(int u, int v) {
        // {x0, y0, x1, y1, top-of-figure, bottom-of-figure, outer}
        float[][] bands = {
                {0, 8, 32, 16, 0f, 0.25f, 0}, {32, 8, 64, 16, 0f, 0.25f, 1},          // head sides, hat sides
                {16, 20, 40, 32, 0.25f, 0.62f, 0}, {16, 36, 40, 48, 0.25f, 0.62f, 1}, // body, jacket
                {40, 20, 56, 32, 0.25f, 0.6f, 0}, {40, 36, 56, 48, 0.25f, 0.6f, 1},   // right arm, sleeve
                {32, 52, 48, 64, 0.25f, 0.6f, 0}, {48, 52, 64, 64, 0.25f, 0.6f, 1},   // left arm, sleeve
                {0, 20, 16, 32, 0.62f, 1f, 0}, {0, 36, 16, 48, 0.62f, 1f, 1},         // right leg, pants
                {16, 52, 32, 64, 0.62f, 1f, 0}, {0, 52, 16, 64, 0.62f, 1f, 1},        // left leg, pants
        };
        for (float[] b : bands) {
            if (u >= b[0] && u < b[2] && v >= b[1] && v < b[3]) {
                float row = (v - b[1] + 0.5f) / (b[3] - b[1]);
                return new float[] {b[4] + (b[5] - b[4]) * row, 1, b[6], row};
            }
        }
        boolean outer = (u >= 32 && v < 16) || (v >= 32 && v < 48) || (v >= 48 && (u < 16 || u >= 48));
        return new float[] {v < 16 ? 0.05f : 0.5f, 0, outer ? 1 : 0, 0};
    }

    /** Weathers ARGB pixels in place (pure: no game classes, so it can be tested offline). */
    public static void weather(int[] img, int w, int h, int seed) {
        float sc = w / 64f;
        hollowEyes(img, w, sc, 8, 8);
        hollowEyes(img, w, sc, 40, 8);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img[y * w + x];
                int a = c >>> 24;
                if (a == 0) continue;
                int u = (int) (x / sc), v = (int) (y / sc);
                float[] loc = locate(u, v);
                float down = loc[0];
                boolean sideFace = loc[1] > 0, outer = loc[2] > 0;
                float r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                // drained and cold
                float lum = 0.3f * r + 0.59f * g + 0.11f * b;
                r = lum + (r - lum) * 0.45f; g = lum + (g - lum) * 0.45f; b = lum + (b - lum) * 0.45f;
                r *= 0.8f; g *= 0.86f; b *= 0.97f;
                b += 6;
                // soaked darker toward the feet
                float soak = 1f - 0.38f * down * down;
                r *= soak; g *= soak; b *= soak;
                // mud on the boots
                if (down > 0.9f && sideFace) {
                    float m = Math.min(1f, (down - 0.9f) * 9f) * 0.55f;
                    r += (58 - r) * m; g += (47 - g) * m; b += (38 - b) * m;
                }
                // rain streaks run down the side faces
                if (sideFace) {
                    int col = hash(x, 0, seed);
                    if (col % 6 == 0) {
                        int len = 3 + (col >> 4) % 6;
                        int start = (col >> 9) % 12;
                        int rowPx = (int) (loc[3] * 12);
                        if (rowPx >= start && rowPx < start + len) { r *= 0.82f; g *= 0.84f; b *= 0.86f; }
                    } else if (col % 11 == 1) {
                        r += (170 - r) * 0.1f; g += (182 - g) * 0.1f; b += (196 - b) * 0.1f;
                    }
                }
                // salt and frost flecks
                int sp = hash(x, y, seed + 7);
                if (sp % 31 == 0) { r += (205 - r) * 0.35f; g += (214 - g) * 0.35f; b += (224 - b) * 0.35f; }
                else if (sp % 37 == 0) { r *= 0.8f; g *= 0.8f; b *= 0.82f; }
                // the outer layer is frayed at its hems and torn here and there
                if (outer && sideFace) {
                    float fray = 1f - (2 + hash(x, 3, seed) % 3) / (12f * sc);
                    if (loc[3] > fray) a = 0;
                    else if (hash(x / Math.max(1, (int) sc), y / Math.max(1, (int) sc), seed + 3) % 53 == 0) a = 0;
                }
                int ri = clamp(r), gi = clamp(g), bi = clamp(b);
                img[y * w + x] = a << 24 | ri << 16 | gi << 8 | bi;
            }
        }
    }

    /** In the face (front of the head, or of the hat), eye whites go dark and the pupils beside them go pale. */
    private static void hollowEyes(int[] img, int w, float sc, int fx, int fy) {
        int x0 = (int) (fx * sc), y0 = (int) ((fy + 1) * sc), x1 = (int) ((fx + 8) * sc), y1 = (int) ((fy + 7) * sc);
        boolean[][] white = new boolean[x1 - x0][y1 - y0];
        int found = 0, minX = Integer.MAX_VALUE, maxX = -1, minY = Integer.MAX_VALUE, maxY = -1;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int c = img[y * w + x];
                if (c >>> 24 < 128) continue;
                int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
                if (mx > 190 && mx - mn < 40) {
                    white[x - x0][y - y0] = true;
                    found++;
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                }
            }
        }
        // eyes are small: skip faces where "white" covers a lot (a pale face, a mask)
        if (found == 0 || found > 6 * sc * sc) return;
        for (int y = Math.max(y0, minY); y <= Math.min(y1 - 1, maxY); y++) {
            for (int x = Math.max(x0, minX - (int) (2 * sc)); x <= Math.min(x1 - 1, maxX + (int) (2 * sc)); x++) {
                int c = img[y * w + x];
                if (c >>> 24 < 128) continue;
                if (white[x - x0][y - y0]) {
                    img[y * w + x] = 0xFF16181E;
                } else {
                    int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                    if (r + g + b < 360) img[y * w + x] = 0xFFC4D2DC;
                }
            }
        }
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }
}
