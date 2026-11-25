package com.stormreach.client.weather;

import com.stormreach.client.tornado.TornadoMesh;

/**
 * The Doppler radar's moving parts, centred on the pedestal block (origin at its bottom centre): a turntable, a
 * yoke, and a 3-block parabolic dish with its feed horn on three struts, turning about the vertical axis and tilted
 * slightly up. No Minecraft classes, so the offline preview can draw it.
 */
public final class RadarMesh {
    private RadarMesh() {}

    public static void build(TornadoMesh.Sink out, float yaw) {
        float tilt = (float) Math.toRadians(14);
        int white = 0xFFEDF0F3, grey = 0xFF8A9098, dark = 0xFF4A5058;
        // turntable and yoke (turn with the dish)
        box(out, -0.4f, 0.70f, -0.4f, 0.4f, 0.85f, 0.4f, yaw, 0, 0, 0, grey);
        box(out, -0.55f, 0.85f, -0.08f, -0.45f, 1.55f, 0.08f, yaw, 0, 0, 0, dark);
        box(out, 0.45f, 0.85f, -0.08f, 0.55f, 1.55f, 0.08f, yaw, 0, 0, 0, dark);
        // the dish: a shallow paraboloid, 3 blocks across, facing +z before rotation
        float cy = 1.45f, radius = 1.3f, depth = 0.34f;
        int rings = 8, seg = 32;
        float[][] px = new float[rings + 1][seg + 1], py = new float[rings + 1][seg + 1], pz = new float[rings + 1][seg + 1];
        for (int i = 0; i <= rings; i++) {
            float r = radius * i / rings;
            float z = depth * (r / radius) * (r / radius) - depth;  // vertex at the back
            for (int j = 0; j <= seg; j++) {
                float a = (float) (j * Math.PI * 2 / seg);
                float[] p = place(r * (float) Math.cos(a), r * (float) Math.sin(a), z, tilt, yaw, cy);
                px[i][j] = p[0]; py[i][j] = p[1]; pz[i][j] = p[2];
            }
        }
        for (int i = 0; i < rings; i++) for (int j = 0; j < seg; j++) {
            int c = i == rings - 1 ? 0xFFC8CED4 : white;
            float u0 = (float) j / seg, u1 = (float) (j + 1) / seg, v0 = (float) i / rings, v1 = (float) (i + 1) / rings;
            out.vertex(px[i][j], py[i][j], pz[i][j], u0, v0, c);
            out.vertex(px[i][j + 1], py[i][j + 1], pz[i][j + 1], u1, v0, c);
            out.vertex(px[i + 1][j + 1], py[i + 1][j + 1], pz[i + 1][j + 1], u1, v1, c);
            out.vertex(px[i + 1][j], py[i + 1][j], pz[i + 1][j], u0, v1, c);
        }
        // feed horn on three struts at the focus
        float fz = 0.75f;
        for (int k = 0; k < 3; k++) {
            float a = (float) (k * Math.PI * 2 / 3 + Math.PI / 2);
            float[] rim = place(radius * 0.92f * (float) Math.cos(a), radius * 0.92f * (float) Math.sin(a), 0, tilt, yaw, cy);
            float[] focus = place(0, 0, fz, tilt, yaw, cy);
            strut(out, rim, focus, 0.035f, dark);
        }
        float[] f0 = place(0, 0, fz - 0.15f, tilt, yaw, cy), f1 = place(0, 0, fz + 0.12f, tilt, yaw, cy);
        strut(out, f0, f1, 0.12f, grey);
    }

    /** Dish-space (x right, y up, z out of the dish) to world: tilt up about x, then turn by yaw about y, then lift. */
    private static float[] place(float x, float y, float z, float tilt, float yaw, float lift) {
        float y1 = y * (float) Math.cos(tilt) + z * (float) Math.sin(tilt);
        float z1 = -y * (float) Math.sin(tilt) + z * (float) Math.cos(tilt);
        float x2 = x * (float) Math.cos(yaw) + z1 * (float) Math.sin(yaw);
        float z2 = -x * (float) Math.sin(yaw) + z1 * (float) Math.cos(yaw);
        return new float[]{x2, y1 + lift, z2};
    }

    private static void strut(TornadoMesh.Sink out, float[] a, float[] b, float w, int c) {
        // a square beam from a to b
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f) return;
        float[] d = {dx / len, dy / len, dz / len};
        float[] up = Math.abs(d[1]) > 0.9f ? new float[]{1, 0, 0} : new float[]{0, 1, 0};
        float[] s1 = norm(cross(d, up)), s2 = norm(cross(d, s1));
        float[][] corners = new float[4][];
        for (int k = 0; k < 4; k++) {
            float ca = (k == 0 || k == 3) ? w : -w, cb = (k < 2) ? w : -w;
            corners[k] = new float[]{s1[0] * ca + s2[0] * cb, s1[1] * ca + s2[1] * cb, s1[2] * ca + s2[2] * cb};
        }
        for (int k = 0; k < 4; k++) {
            float[] p = corners[k], q = corners[(k + 1) % 4];
            out.vertex(a[0] + p[0], a[1] + p[1], a[2] + p[2], 0, 0, c);
            out.vertex(a[0] + q[0], a[1] + q[1], a[2] + q[2], 1, 0, c);
            out.vertex(b[0] + q[0], b[1] + q[1], b[2] + q[2], 1, 1, c);
            out.vertex(b[0] + p[0], b[1] + p[1], b[2] + p[2], 0, 1, c);
        }
    }

    private static void box(TornadoMesh.Sink out, float x0, float y0, float z0, float x1, float y1, float z1, float yaw, float ox, float oy, float oz, int c) {
        float[][] v = new float[8][];
        for (int i = 0; i < 8; i++) {
            float x = (i & 1) == 0 ? x0 : x1, y = (i & 2) == 0 ? y0 : y1, z = (i & 4) == 0 ? z0 : z1;
            float rx = x * (float) Math.cos(yaw) + z * (float) Math.sin(yaw), rz = -x * (float) Math.sin(yaw) + z * (float) Math.cos(yaw);
            v[i] = new float[]{rx + ox, y + oy, rz + oz};
        }
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        for (int[] f : faces) {
            out.vertex(v[f[0]][0], v[f[0]][1], v[f[0]][2], 0, 0, c);
            out.vertex(v[f[1]][0], v[f[1]][1], v[f[1]][2], 1, 0, c);
            out.vertex(v[f[2]][0], v[f[2]][1], v[f[2]][2], 1, 1, c);
            out.vertex(v[f[3]][0], v[f[3]][1], v[f[3]][2], 0, 1, c);
        }
    }

    private static float[] cross(float[] a, float[] b) { return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]}; }

    private static float[] norm(float[] a) { float l = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]); return new float[]{a[0] / l, a[1] / l, a[2] / l}; }
}
