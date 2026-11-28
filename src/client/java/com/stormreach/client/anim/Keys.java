package com.stormreach.client.anim;

/**
 * Keyframe tracks: flat arrays {t0, v0, t1, v1, ...} with t in [0,1], sampled with Catmull-Rom splines,
 * so motion eases through every key. {@link #loop} wraps around (cycles); {@link #once} holds its ends (one-shot clips).
 */
public final class Keys {
    private Keys() {}

    public static float loop(float[] k, float t) {
        int n = k.length / 2;
        if (n == 1) return k[1];
        t -= (float) Math.floor(t);
        int i0 = n - 1;
        for (int j = 0; j < n; j++) {
            if (k[2 * j] > t) { i0 = j - 1; break; }
        }
        if (i0 < 0) i0 = n - 1;
        int i1 = (i0 + 1) % n, im = (i0 - 1 + n) % n, i2 = (i1 + 1) % n;
        float t0 = k[2 * i0], t1 = k[2 * i1];
        float span = t1 - t0;
        if (span <= 0) span += 1f;
        float u = t - t0;
        if (u < 0) u += 1f;
        return cr(k[2 * im + 1], k[2 * i0 + 1], k[2 * i1 + 1], k[2 * i2 + 1], Math.min(1f, u / span));
    }

    public static float once(float[] k, float t) {
        int n = k.length / 2;
        if (t <= k[0] || n == 1) return k[1];
        if (t >= k[2 * n - 2]) return k[2 * n - 1];
        int i0 = 0;
        while (i0 < n - 2 && k[2 * i0 + 2] <= t) i0++;
        int i1 = i0 + 1, im = Math.max(0, i0 - 1), i2 = Math.min(n - 1, i1 + 1);
        float u = (t - k[2 * i0]) / Math.max(1e-6f, k[2 * i1] - k[2 * i0]);
        return cr(k[2 * im + 1], k[2 * i0 + 1], k[2 * i1 + 1], k[2 * i2 + 1], u);
    }

    private static float cr(float p0, float p1, float p2, float p3, float u) {
        float u2 = u * u, u3 = u2 * u;
        return 0.5f * (2 * p1 + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u2 + (-p0 + 3 * p1 - 3 * p2 + p3) * u3);
    }
}
