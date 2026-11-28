package com.stormreach.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stormreach.Stormreach;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Procedural geometry for the big machines (radar, turbine, siren, weather station, TIV parts) and for placement
 * holograms. Everything is coloured quads on a plain white texture, with per-face shading baked into the colour the
 * way block models are shaded (top brightest, bottom darkest, north/south and east/west in between), so the shapes
 * read clearly without any lighting tricks. Units are blocks.
 */
public final class Mesh {
    private static Identifier white;

    /** The plain white texture the machines are coloured on (looked up lazily so the shapes work without a game). */
    public static Identifier white() { if (white == null) white = Stormreach.id("textures/misc/white.png"); return white; }

    private Mesh() {}

    /** Receives quads (four corners in counter-clockwise order seen from the front, plus the face normal). */
    public interface Out {
        void quad(float[] a, float[] b, float[] c, float[] d, int argb, float nx, float ny, float nz);

        /** Hologram look: every face takes the tint's colour and alpha, keeping only its brightness (shading) from the model. */
        default Out hologram(int tint) {
            Out self = this;
            return (a, b, c, d, argb, nx, ny, nz) -> {
                float lum = (0.3f * ((argb >> 16) & 255) + 0.59f * ((argb >> 8) & 255) + 0.11f * (argb & 255)) / 255f;
                float k = 0.55f + 0.6f * lum;
                int r = Math.min(255, (int) (((tint >> 16) & 255) * k)), g = Math.min(255, (int) (((tint >> 8) & 255) * k)), bl = Math.min(255, (int) ((tint & 255) * k));
                self.quad(a, b, c, d, (tint & 0xFF000000) | (r << 16) | (g << 8) | bl, nx, ny, nz);
            };
        }

        /** Same geometry, every colour multiplied by tint (used for holograms: one tint for the whole structure). */
        default Out tinted(int tint, boolean flat) {
            Out self = this;
            return (a, b, c, d, argb, nx, ny, nz) -> self.quad(a, b, c, d, flat ? tint : mul(argb, tint), nx, ny, nz);
        }
    }

    /** Writes quads to a vertex consumer: light from the block (or full bright), normals from the face. */
    public static Out to(PoseStack.Pose p, VertexConsumer vc, int light) {
        return (a, b, c, d, argb, nx, ny, nz) -> {
            float[][] v = {a, b, c, d};
            float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
            for (int i = 0; i < 4; i++)
                vc.addVertex(p, v[i][0], v[i][1], v[i][2]).setColor(argb).setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(p, nx, ny, nz);
        };
    }

    // ------------------------------------------------------------------ transforms

    /**
     * Rotates every quad about a pivot: first about Z (roll), then X (pitch), then Y (yaw), all in radians, then moves
     * it by (tx, ty, tz). Used for rotors, rotating siren heads, a weather vane's arrow and the turbine nacelle.
     */
    public static Out rotate(Out o, float px, float py, float pz, float yaw, float pitch, float roll, float tx, float ty, float tz) {
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), spt = (float) Math.sin(pitch),
                cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        return (a, b, c, d, argb, nx, ny, nz) -> {
            float[][] in = {a, b, c, d}, out = new float[4][];
            for (int i = 0; i < 4; i++) {
                float[] r = rot(in[i][0] - px, in[i][1] - py, in[i][2] - pz, cy, sy, cp, spt, cr, sr);
                out[i] = new float[]{r[0] + px + tx, r[1] + py + ty, r[2] + pz + tz};
            }
            float[] n = rot(nx, ny, nz, cy, sy, cp, spt, cr, sr);
            o.quad(out[0], out[1], out[2], out[3], argb, n[0], n[1], n[2]);
        };
    }

    public static Out yaw(Out o, float px, float pz, float yaw) { return rotate(o, px, 0, pz, yaw, 0, 0, 0, 0, 0); }

    private static float[] rot(float x, float y, float z, float cy, float sy, float cp, float sp, float cr, float sr) {
        float x1 = x * cr - y * sr, y1 = x * sr + y * cr, z1 = z;                 // roll about z
        float y2 = y1 * cp - z1 * sp, z2 = y1 * sp + z1 * cp, x2 = x1;            // pitch about x
        float x3 = x2 * cy + z2 * sy, z3 = -x2 * sy + z2 * cy;                    // yaw about y
        return new float[]{x3, y2, z3};
    }

    /** A flat quad given by four corners (drawn on both sides). */
    public static void panel(Out o, float[] a, float[] b, float[] c, float[] d, int argb) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz); if (l < 1e-6f) return;
        nx /= l; ny /= l; nz /= l;
        // one quad: the machine render types draw both sides, and a second coplanar quad would z-fight
        o.quad(a, b, c, d, shade(argb, Math.max(faceShade(nx, ny, nz), faceShade(-nx, -ny, -nz))), nx, ny, nz);
    }

    // ------------------------------------------------------------------ colour helpers

    public static int shade(int argb, float f) {
        int a = argb >>> 24, r = (int) (((argb >> 16) & 255) * f), g = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return (a << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    public static int mul(int x, int y) {
        int a = ((x >>> 24) * (y >>> 24)) / 255, r = (((x >> 16) & 255) * ((y >> 16) & 255)) / 255,
                g = (((x >> 8) & 255) * ((y >> 8) & 255)) / 255, b = ((x & 255) * (y & 255)) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int mix(int x, int y, float t) {
        int a = (int) ((x >>> 24) + ((y >>> 24) - (x >>> 24)) * t);
        int r = (int) (((x >> 16) & 255) + (((y >> 16) & 255) - ((x >> 16) & 255)) * t);
        int g = (int) (((x >> 8) & 255) + (((y >> 8) & 255) - ((x >> 8) & 255)) * t);
        int b = (int) ((x & 255) + ((y & 255) - (x & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float faceShade(float nx, float ny, float nz) {
        if (ny > 0.5f) return 1.0f;
        if (ny < -0.5f) return 0.5f;
        return 0.6f + 0.2f * Math.abs(nz) + 0.15f * Math.max(0, ny);
    }

    private static float[] p(float x, float y, float z) { return new float[]{x, y, z}; }

    // ------------------------------------------------------------------ shapes

    /** An axis-aligned box. */
    public static void box(Out o, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        box(o, x0, y0, z0, x1, y1, z1, argb, true, true);
    }

    public static void box(Out o, float x0, float y0, float z0, float x1, float y1, float z1, int argb, boolean top, boolean bottom) {
        if (top) o.quad(p(x0, y1, z0), p(x0, y1, z1), p(x1, y1, z1), p(x1, y1, z0), shade(argb, 1.0f), 0, 1, 0);
        if (bottom) o.quad(p(x0, y0, z1), p(x0, y0, z0), p(x1, y0, z0), p(x1, y0, z1), shade(argb, 0.5f), 0, -1, 0);
        o.quad(p(x1, y1, z0), p(x1, y0, z0), p(x0, y0, z0), p(x0, y1, z0), shade(argb, 0.8f), 0, 0, -1);  // north
        o.quad(p(x0, y1, z1), p(x0, y0, z1), p(x1, y0, z1), p(x1, y1, z1), shade(argb, 0.8f), 0, 0, 1);   // south
        o.quad(p(x0, y1, z0), p(x0, y0, z0), p(x0, y0, z1), p(x0, y1, z1), shade(argb, 0.6f), -1, 0, 0);  // west
        o.quad(p(x1, y1, z1), p(x1, y0, z1), p(x1, y0, z0), p(x1, y1, z0), shade(argb, 0.6f), 1, 0, 0);   // east
    }

    /** A box of half-size h around a centre. */
    public static void cube(Out o, float cx, float cy, float cz, float hx, float hy, float hz, int argb) {
        box(o, cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz, argb);
    }

    /** A square beam of width w from a to b (any direction): truss members, struts, rails, antennas. */
    public static void beam(Out o, float[] a, float[] b, float w, int argb) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-5f) return;
        dx /= len; dy /= len; dz /= len;
        // two vectors perpendicular to the beam
        float ux, uy, uz;
        if (Math.abs(dy) < 0.9f) { ux = dz; uy = 0; uz = -dx; } else { ux = 1; uy = 0; uz = 0; }
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz); ux /= ul; uy /= ul; uz /= ul;
        float vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
        float h = w / 2;
        float[][] ring = new float[4][3];
        float[][] offs = {{h, h}, {-h, h}, {-h, -h}, {h, -h}};
        for (int k = 0; k < 4; k++) for (int i = 0; i < 3; i++) ring[k][i] = (i == 0 ? ux : i == 1 ? uy : uz) * offs[k][0] + (i == 0 ? vx : i == 1 ? vy : vz) * offs[k][1];
        for (int k = 0; k < 4; k++) {
            float[] r0 = ring[k], r1 = ring[(k + 1) % 4];
            float nx = (r0[0] + r1[0]), ny = (r0[1] + r1[1]), nz = (r0[2] + r1[2]);
            float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz); nx /= nl; ny /= nl; nz /= nl;
            o.quad(p(a[0] + r0[0], a[1] + r0[1], a[2] + r0[2]), p(a[0] + r1[0], a[1] + r1[1], a[2] + r1[2]),
                    p(b[0] + r1[0], b[1] + r1[1], b[2] + r1[2]), p(b[0] + r0[0], b[1] + r0[1], b[2] + r0[2]), shade(argb, faceShade(nx, ny, nz)), nx, ny, nz);
        }
    }

    public static void beam(Out o, float ax, float ay, float az, float bx, float by, float bz, float w, int argb) {
        beam(o, p(ax, ay, az), p(bx, by, bz), w, argb);
    }

    /** A vertical frustum (cylinder when r0 == r1) with optional caps. */
    public static void cylinder(Out o, float cx, float y0, float cz, float y1, float r0, float r1, int seg, int argb, boolean caps) {
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg, am = (a0 + a1) / 2;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float nx = (float) Math.cos(am), nz = (float) Math.sin(am);
            o.quad(p(cx + c1 * r1, y1, cz + s1 * r1), p(cx + c1 * r0, y0, cz + s1 * r0), p(cx + c0 * r0, y0, cz + s0 * r0), p(cx + c0 * r1, y1, cz + s0 * r1),
                    shade(argb, faceShade(nx, 0, nz)), nx, 0, nz);
            if (caps) {
                o.quad(p(cx, y1, cz), p(cx, y1, cz), p(cx + c0 * r1, y1, cz + s0 * r1), p(cx + c1 * r1, y1, cz + s1 * r1), argb, 0, 1, 0);
                o.quad(p(cx, y0, cz), p(cx, y0, cz), p(cx + c1 * r0, y0, cz + s1 * r0), p(cx + c0 * r0, y0, cz + s0 * r0), shade(argb, 0.5f), 0, -1, 0);
            }
        }
    }

    /**
     * A sphere made of lat/long panels. When seams is non-zero every panel is drawn slightly inset and the seams
     * between them in that colour, which is how a radome's geodesic panelling reads from a distance.
     */
    public static void sphere(Out o, float cx, float cy, float cz, float r, int lat, int lon, int argb, int seams, float minLat) {
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat - Math.PI / 2, t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
            if (t1 < minLat) continue;
            for (int j = 0; j < lon; j++) {
                double a0 = Math.PI * 2 * j / lon + (i % 2) * Math.PI / lon, a1 = a0 + Math.PI * 2 / lon;
                float[] q00 = sp(cx, cy, cz, r, t0, a0), q01 = sp(cx, cy, cz, r, t0, a1), q11 = sp(cx, cy, cz, r, t1, a1), q10 = sp(cx, cy, cz, r, t1, a0);
                double tm = (t0 + t1) / 2, am = (a0 + a1) / 2;
                float nx = (float) (Math.cos(tm) * Math.cos(am)), ny = (float) Math.sin(tm), nz = (float) (Math.cos(tm) * Math.sin(am));
                int c = shade(argb, 0.62f + 0.38f * Math.max(0, ny) + 0.12f * Math.abs(nz) + ((i + j) % 2) * 0.03f);
                if (seams != 0) {
                    o.quad(q10, q00, q01, q11, shade(seams, 0.7f + 0.3f * Math.max(0, ny)), nx, ny, nz);
                    float k = 0.86f;
                    float[] mc = {(q00[0] + q01[0] + q11[0] + q10[0]) / 4, (q00[1] + q01[1] + q11[1] + q10[1]) / 4, (q00[2] + q01[2] + q11[2] + q10[2]) / 4};
                    float[] m00 = lerp(mc, q00, k), m01 = lerp(mc, q01, k), m11 = lerp(mc, q11, k), m10 = lerp(mc, q10, k);
                    for (float[] m : new float[][]{m00, m01, m11, m10}) { m[0] += nx * 0.004f; m[1] += ny * 0.004f; m[2] += nz * 0.004f; }
                    o.quad(m10, m00, m01, m11, c, nx, ny, nz);
                } else o.quad(q10, q00, q01, q11, c, nx, ny, nz);
            }
        }
    }

    private static float[] lerp(float[] a, float[] b, float t) { return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t}; }

    private static float[] sp(float cx, float cy, float cz, float r, double t, double a) {
        return p(cx + (float) (r * Math.cos(t) * Math.cos(a)), cy + (float) (r * Math.sin(t)), cz + (float) (r * Math.cos(t) * Math.sin(a)));
    }

    /** A flat horizontal disc (both sides). */
    public static void disc(Out o, float cx, float y, float cz, float r, int seg, int argb) {
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float[] c = p(cx, y, cz), e0 = p(cx + (float) Math.cos(a0) * r, y, cz + (float) Math.sin(a0) * r), e1 = p(cx + (float) Math.cos(a1) * r, y, cz + (float) Math.sin(a1) * r);
            o.quad(c, c, e0, e1, argb, 0, 1, 0);
        }
    }

    /** A four-legged lattice tower (open steel truss) from y0 to y1, square half-widths w0 at the bottom, w1 at the top. */
    public static void lattice(Out o, float cx, float cz, float y0, float y1, float w0, float w1, int bays, float member, int argb) {
        float[][] c0 = corners(cx, cz, w0), c1 = corners(cx, cz, w1);
        for (int k = 0; k < 4; k++) beam(o, new float[]{c0[k][0], y0, c0[k][1]}, new float[]{c1[k][0], y1, c1[k][1]}, member * 1.4f, argb);
        for (int b = 0; b <= bays; b++) {
            float t = (float) b / bays, y = y0 + (y1 - y0) * t, w = w0 + (w1 - w0) * t;
            float[][] c = corners(cx, cz, w);
            for (int k = 0; k < 4; k++) beam(o, new float[]{c[k][0], y, c[k][1]}, new float[]{c[(k + 1) % 4][0], y, c[(k + 1) % 4][1]}, member, argb);
            if (b == bays) break;
            float t2 = (float) (b + 1) / bays, y2 = y0 + (y1 - y0) * t2, w2 = w0 + (w1 - w0) * t2;
            float[][] d = corners(cx, cz, w2);
            for (int k = 0; k < 4; k++) {   // X bracing on each face
                beam(o, new float[]{c[k][0], y, c[k][1]}, new float[]{d[(k + 1) % 4][0], y2, d[(k + 1) % 4][1]}, member * 0.8f, argb);
                beam(o, new float[]{c[(k + 1) % 4][0], y, c[(k + 1) % 4][1]}, new float[]{d[k][0], y2, d[k][1]}, member * 0.8f, argb);
            }
        }
    }

    private static float[][] corners(float cx, float cz, float w) {
        return new float[][]{{cx - w, cz - w}, {cx + w, cz - w}, {cx + w, cz + w}, {cx - w, cz + w}};
    }
}
