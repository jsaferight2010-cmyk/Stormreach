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
    private static final Map<String, Identifier[]> CACHE = new HashMap<>();
    private static final Map<String, Integer> FAILED_AT = new HashMap<>();
    private static final java.util.Set<String> PENDING = new java.util.HashSet<>();
    /** Stages: 0 the Stranger (weathered); 1 and 2 Cyrrus's forms (storm-corrupted, with glowing veins). */
    public static final int STRANGER = 0, CORRUPTED = 1, FINAL = 2;

    private WeatheredSkins() {}

    /** The weathered texture for this skin, or the plain skin while the original isn't available yet. */
    public static Identifier get(PlayerSkin skin) {
        return get(skin, STRANGER)[0];
    }

    /**
     * {skin texture, glow texture or null} for this skin at a stage. While the player's skin isn't available yet,
     * returns the plain skin (and tries again a few seconds later).
     */
    public static Identifier[] get(PlayerSkin skin, int stage) {
        Identifier src = skin.body().texturePath();
        String key = src + "#" + stage;
        Identifier[] done = CACHE.get(key);
        if (done != null) return done;
        if (PENDING.contains(key)) return new Identifier[] {src, null};   // being uploaded this frame
        Minecraft mc = Minecraft.getInstance();
        int now = mc.player == null ? 0 : mc.player.tickCount;
        Integer failed = FAILED_AT.get(key);
        if (failed != null && now - failed < 100) return new Identifier[] {src, null};
        NativeImage img = read(mc, src);
        if (img == null) {
            FAILED_AT.put(key, now);
            return new Identifier[] {src, null};
        }
        try {
            int w = img.getWidth(), h = img.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = img.getPixel(x, y);
            int[] glow = stage > STRANGER ? new int[w * h] : null;
            weather(px, w, h, src.hashCode(), stage, glow);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setPixel(x, y, px[y * w + x]);
            String tag = Integer.toHexString(src.hashCode() & 0x7fffffff) + "_" + stage;
            // 6.0: the GPU upload (registering the textures) happens between frames, never in the middle of drawing
            // an entity: uploading mid-frame could crash the client the moment Cyrrus or the Stranger first appeared.
            PENDING.add(key);
            mc.schedule(() -> {
                try {
                    Identifier id = Stormreach.id("weathered/" + tag);
                    mc.getTextureManager().register(id, new DynamicTexture(() -> "stormreach weathered skin", img));
                    Identifier glowId = null;
                    if (glow != null) {
                        NativeImage g = new NativeImage(w, h, true);
                        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) g.setPixel(x, y, glow[y * w + x]);
                        glowId = Stormreach.id("weathered/" + tag + "_glow");
                        mc.getTextureManager().register(glowId, new DynamicTexture(() -> "stormreach storm veins", g));
                    }
                    CACHE.put(key, new Identifier[] {id, glowId});
                } catch (RuntimeException e) {
                    Stormreach.LOGGER.warn("Couldn't upload weathered skin {}", src, e);
                    FAILED_AT.put(key, mc.player == null ? 0 : mc.player.tickCount);
                } finally {
                    PENDING.remove(key);
                }
            });
            return new Identifier[] {src, null};
        } catch (RuntimeException e) {
            Stormreach.LOGGER.warn("Couldn't weather skin {}", src, e);
            img.close();
            FAILED_AT.put(key, now);
            return new Identifier[] {src, null};
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

    /** Stage 0 only (the Stranger). */
    public static void weather(int[] img, int w, int h, int seed) {
        weather(img, w, h, seed, STRANGER, null);
    }

    /**
     * Weathers ARGB pixels in place (pure: no game classes, so it can be tested offline). Stages above 0 also darken
     * the skin toward storm-indigo and grow branching lightning veins over it, written into {@code glow} too (the
     * emissive layer), along with the eyes, which burn instead of going pale.
     */
    public static void weather(int[] img, int w, int h, int seed, int stage, int[] glow) {
        float sc = w / 64f;
        int[] eye = new int[w * h];
        hollowEyes(img, eye, w, sc, 8, 8);
        hollowEyes(img, eye, w, sc, 40, 8);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img[y * w + x];
                int a = c >>> 24;
                if (a == 0 || eye[y * w + x] != 0) continue;
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
                if (stage > STRANGER) {
                    float k = stage == FINAL ? 0.42f : 0.24f;
                    r += (34 - r) * k; g += (38 - g) * k; b += (78 - b) * k;
                }
                int ri = clamp(r), gi = clamp(g), bi = clamp(b);
                img[y * w + x] = a << 24 | ri << 16 | gi << 8 | bi;
            }
        }
        for (int i = 0; i < img.length; i++) {
            if (eye[i] == 1) img[i] = stage > STRANGER ? 0xFF0E1018 : 0xFF16181E;
            else if (eye[i] == 2) {
                img[i] = stage > STRANGER ? 0xFFBFF4FF : 0xFFC4D2DC;
                if (glow != null) glow[i] = 0xFFBFF4FF;
            }
        }
        if (stage > STRANGER && glow != null) veins(img, glow, w, h, sc, seed, stage);
    }

    /** Branching lightning veins: short jagged walks down the body, arms, legs and face. */
    private static void veins(int[] img, int[] glow, int w, int h, float sc, int seed, int stage) {
        int[][] starts = {{10, 9}, {13, 10}, {21, 21}, {25, 22}, {23, 26}, {44, 21}, {37, 53}, {5, 21}, {21, 53}, {33, 21}, {48, 21}, {2, 21}};
        int walkers = stage == FINAL ? 14 : 7;
        int px = Math.max(1, Math.round(sc));
        for (int n = 0; n < walkers; n++) {
            int[] st = starts[n % starts.length];
            int u = st[0] + hash(n, 1, seed) % 3 - 1, v = st[1] + hash(n, 2, seed) % 2;
            int len = 3 + hash(n, 3, seed) % (stage == FINAL ? 7 : 5);
            for (int i = 0; i < len; i++) {
                for (int dy = 0; dy < px; dy++) for (int dx = 0; dx < px; dx++) {
                    int x = (int) (u * sc) + dx, y = (int) (v * sc) + dy;
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    int idx = y * w + x;
                    if ((img[idx] >>> 24) == 0) continue;
                    // bright where the bolt is young, fading toward its tip
                    boolean core = i < len / 2;
                    img[idx] = core ? 0xFF8FE4FF : 0xFF4A86B0;
                    glow[idx] = core ? 0xFF9FEAFF : 0x804A9AD0;
                }
                v++;
                int turn = hash(n, i + 10, seed) % 5;
                u += turn == 0 ? -1 : turn == 1 ? 1 : 0;
            }
        }
    }

    /** In the face (front of the head, or of the hat), eye whites go dark and the pupils beside them go pale. */
    private static void hollowEyes(int[] img, int[] eye, int w, float sc, int fx, int fy) {
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
                    eye[y * w + x] = 1;
                } else {
                    int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                    if (r + g + b < 360) eye[y * w + x] = 2;
                }
            }
        }
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }
}
