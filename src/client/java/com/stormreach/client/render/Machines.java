package com.stormreach.client.render;

import static com.stormreach.client.render.Mesh.*;

/**
 * Geometry of the big weather machines, shared by their block-entity renderers and their placement holograms (so a
 * preview is exactly what gets built). Origin: centre of the controller block's bottom face; units: blocks.
 */
public final class Machines {
    private Machines() {}

    static final int CONCRETE = 0xFFB9B5AD, CONCRETE_D = 0xFF8F8B84, GALV = 0xFFA9AFB5, GALV_D = 0xFF7C838A, WHITE = 0xFFF2F4F3,
            OFFWHITE = 0xFFD9DEE0, SEAM = 0xFFB9BFC4, YELLOW = 0xFFE3B31C, DARK = 0xFF2C3034, RED = 0xFFE0281E, BEIGE = 0xFFD9CFB4,
            WOOD = 0xFF6E4B2C, WOOD_D = 0xFF4E341E, STEEL = 0xFF8E969D, BLACK = 0xFF1C1E20, GLASS = 0xFF3A5D78;

    // ------------------------------------------------------------------ NEXRAD-style Doppler radar (controller 4 up)

    /** The whole radar: pad, equipment shelter, lattice tower, railed platform, pedestal and the white geodesic radome. */
    public static void dopplerRadar(Mesh.Out o) {
        float b = -4f;
        box(o, -1.5f, b, -1.5f, 1.5f, b + 0.18f, 1.5f, CONCRETE);                         // concrete pad
        box(o, -1.42f, b + 0.18f, -1.42f, 1.42f, b + 0.2f, 1.42f, CONCRETE_D, true, false);  // curb line
        // equipment shelter with a door, a vent and an air-conditioning unit
        box(o, 0.5f, b + 0.2f, -1.42f, 1.42f, b + 0.92f, -0.6f, BEIGE);
        box(o, 0.46f, b + 0.92f, -1.46f, 1.46f, b + 0.98f, -0.56f, CONCRETE_D);
        box(o, 0.62f, b + 0.2f, -0.6f, 0.92f, b + 0.75f, -0.58f, 0xFF6F6A5E);
        box(o, 1.0f, b + 0.6f, -0.6f, 1.3f, b + 0.75f, -0.58f, 0xFF4E5256);
        box(o, -1.35f, b + 0.2f, 0.85f, -0.85f, b + 0.55f, 1.35f, 0xFF9AA0A6);
        for (float x = -1.3f; x < -0.85f; x += 0.1f) box(o, x, b + 0.55f, 0.9f, x + 0.05f, b + 0.56f, 1.3f, DARK, true, false);
        // footings and the four-legged galvanised lattice tower (open truss, X-braced every bay)
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) box(o, sx * 0.85f - 0.12f, b + 0.18f, sz * 0.85f - 0.12f, sx * 0.85f + 0.12f, b + 0.32f, sz * 0.85f + 0.12f, CONCRETE_D);
        lattice(o, 0, 0, b + 0.3f, -0.36f, 0.85f, 0.55f, 4, 0.06f, GALV);
        // cable tray from the shelter into the tower and a caged ladder up one face
        beam(o, 0.55f, b + 0.8f, -0.75f, 0.2f, b + 1.2f, -0.6f, 0.07f, GALV_D);
        beam(o, 0.2f, b + 1.2f, -0.6f, 0.15f, -0.4f, -0.5f, 0.07f, GALV_D);
        for (int side = -1; side <= 1; side += 2) beam(o, side * 0.13f, b + 0.3f, 0.86f, side * 0.13f, -0.36f, 0.58f, 0.035f, GALV_D);
        for (float t = 0.05f; t < 1f; t += 0.1f) {
            float y = b + 0.3f + (3.64f - 0.3f) * t, z = 0.86f - 0.28f * t;
            beam(o, -0.13f, y, z, 0.13f, y, z, 0.025f, GALV_D);
        }
        // platform with grating, toe boards and yellow guard rails
        box(o, -1.1f, -0.36f, -1.1f, 1.1f, -0.26f, 1.1f, 0xFF7B828A);
        for (int k = 0; k < 4; k++) {
            float[][] e = {{-1.08f, -1.08f}, {1.08f, -1.08f}, {1.08f, 1.08f}, {-1.08f, 1.08f}};
            float[] p0 = e[k], p1 = e[(k + 1) % 4];
            beam(o, p0[0], 0.14f, p0[1], p1[0], 0.14f, p1[1], 0.04f, YELLOW);
            beam(o, p0[0], -0.06f, p0[1], p1[0], -0.06f, p1[1], 0.03f, YELLOW);
            for (int m = 0; m < 3; m++) {
                float t = m / 3f, x = p0[0] + (p1[0] - p0[0]) * t, z = p0[1] + (p1[1] - p0[1]) * t;
                beam(o, x, -0.26f, z, x, 0.16f, z, 0.04f, YELLOW);
            }
        }
        // pedestal, base ring and the radome (panelled sphere) with an aircraft obstruction light on top
        cylinder(o, 0, -0.26f, 0, 0.22f, 0.36f, 0.3f, 12, STEEL, true);
        cylinder(o, 0, 0.18f, 0, 0.34f, 0.98f, 1.02f, 20, OFFWHITE, true);
        sphere(o, 0, 1.56f, 0, 1.43f, 10, 18, WHITE, SEAM, -0.86f);
        cylinder(o, 0, 2.96f, 0, 3.0f, 0.05f, 0.05f, 6, DARK, true);
        cube(o, 0, 3.03f, 0, 0.045f, 0.03f, 0.045f, RED);
    }

    // ------------------------------------------------------------------ utility-scale wind turbine

    /** Tower, nacelle, hub and three blades. facingYaw turns the nacelle toward the wind side; spin turns the rotor. */
    public static void windTurbine(Mesh.Out o, float facingYaw, float spin, int tower) {
        float top = tower + 0.6f;
        cylinder(o, 0, 0, 0, 0.2f, 0.82f, 0.8f, 16, CONCRETE, true);                         // foundation
        cylinder(o, 0, 0.2f, 0, top, 0.5f, 0.3f, 16, WHITE, false);                          // tapered tubular tower
        for (float y = 3; y < top - 1; y += 4) cylinder(o, 0, y, 0, y + 0.05f, 0.5f - 0.2f * y / top + 0.01f, 0.5f - 0.2f * y / top + 0.01f, 16, OFFWHITE, false);
        box(o, -0.16f, 0.2f, -0.52f, 0.16f, 0.95f, -0.44f, 0xFF5A6067);                      // tower door
        box(o, -0.24f, 0.2f, -0.82f, 0.24f, 0.3f, -0.5f, CONCRETE_D);                         // steps
        Mesh.Out n = yaw(o, 0, 0, facingYaw);
        // nacelle: a long rounded box behind the rotor, with a cooling fin and a met mast on its roof
        box(n, -0.34f, top, -0.55f, 0.34f, top + 0.66f, 1.25f, WHITE);
        box(n, -0.3f, top + 0.66f, -0.45f, 0.3f, top + 0.72f, 1.15f, OFFWHITE, true, false);
        box(n, -0.28f, top + 0.06f, 1.25f, 0.28f, top + 0.58f, 1.32f, SEAM);
        beam(n, 0.15f, top + 0.72f, 1.0f, 0.15f, top + 1.15f, 1.0f, 0.03f, GALV_D);
        beam(n, 0.0f, top + 1.12f, 1.0f, 0.3f, top + 1.12f, 1.0f, 0.02f, GALV_D);
        cube(n, 0.15f, top + 1.17f, 1.0f, 0.03f, 0.02f, 0.03f, RED);
        // hub: a nose cone on the front of the nacelle, then the three blades turning about the main shaft (z axis)
        float hy = top + 0.33f, hz = -0.62f;
        Mesh.Out shaft = rotate(n, 0, hy, hz, 0, (float) (-Math.PI / 2), 0, 0, 0, 0);   // cone axis turned from +y to -z (forward)
        cylinder(shaft, 0, hy - 0.05f, hz, hy + 0.3f, 0.36f, 0.34f, 14, WHITE, true);
        cylinder(shaft, 0, hy + 0.3f, hz, hy + 0.62f, 0.34f, 0.05f, 14, WHITE, false);
        for (int k = 0; k < 3; k++) {
            Mesh.Out blade = rotate(n, 0, hy, hz - 0.1f, 0, 0, spin + (float) (k * Math.PI * 2 / 3), 0, 0, 0);
            blade(blade, 0, hy, hz - 0.1f);
        }
    }

    /** One blade along +y from the hub centre: a root cylinder, a broad chord near the root, tapering to a thin tip. */
    private static void blade(Mesh.Out o, float cx, float cy, float cz) {
        cylinder(o, cx, cy + 0.25f, cz, cy + 0.75f, 0.13f, 0.12f, 10, WHITE, false);
        float[][] st = {{0.75f, 0.12f}, {1.4f, 0.34f}, {2.2f, 0.3f}, {3.4f, 0.22f}, {4.6f, 0.15f}, {5.7f, 0.09f}, {6.4f, 0.04f}};
        for (int i = 0; i + 1 < st.length; i++) {
            float y0 = cy + st[i][0], y1 = cy + st[i + 1][0], w0 = st[i][1], w1 = st[i + 1][1];
            int col = i >= st.length - 2 ? 0xFFE8EAEA : WHITE;
            // leading edge slightly forward (+x), trailing edge back: a thin airfoil box per span segment
            float t0 = 0.05f * w0 / 0.34f + 0.012f, t1 = 0.05f * w1 / 0.34f + 0.012f;
            float[] a = {cx - w0 * 0.35f, y0, cz - t0}, bq = {cx + w0 * 0.65f, y0, cz - t0}, c = {cx + w1 * 0.65f, y1, cz - t1}, d = {cx - w1 * 0.35f, y1, cz - t1};
            float[] a2 = {a[0], y0, cz + t0}, b2 = {bq[0], y0, cz + t0}, c2 = {c[0], y1, cz + t1}, d2 = {d[0], y1, cz + t1};
            panel(o, a, bq, c, d, col);
            panel(o, a2, d2, c2, b2, col);
            panel(o, bq, b2, c2, c, Mesh.shade(col, 0.9f));
            panel(o, a, d, d2, a2, Mesh.shade(col, 0.9f));
        }
    }

    // ------------------------------------------------------------------ outdoor warning siren on a wooden utility pole

    /**
     * A rotating directional siren of the kind used in Indianapolis and across the Midwest: a creosote-brown wooden
     * pole with climbing steps, a grey control cabinet fed by conduit, a crossarm with the radio antenna, and on top a
     * rotator carrying the siren chopper housing and its big square projector horn.
     */
    public static void siren(Mesh.Out o, float headYaw) {
        float poleTop = 6.0f;
        cylinder(o, 0, 0, 0, poleTop, 0.2f, 0.16f, 10, WOOD, true);
        for (float y = 1.2f; y < poleTop - 0.6f; y += 0.55f) {                        // pole steps, alternating sides
            float side = ((int) (y / 0.55f) % 2 == 0) ? 1 : -1;
            beam(o, side * 0.12f, y, 0.0f, side * 0.38f, y, 0.0f, 0.04f, STEEL);
        }
        for (float y = 0.6f; y < poleTop; y += 1.7f) cylinder(o, 0, y, 0, y + 0.04f, 0.2f - 0.006f * y, 0.2f - 0.006f * y, 10, WOOD_D, false);
        // control cabinet with a disconnect switch, and conduit up the pole
        box(o, -0.27f, 1.45f, -0.52f, 0.27f, 2.35f, -0.18f, 0xFF8C9196);
        box(o, -0.3f, 2.35f, -0.55f, 0.3f, 2.42f, -0.15f, 0xFF6C7176);
        box(o, -0.22f, 1.6f, -0.53f, 0.22f, 2.25f, -0.52f, 0xFF7A7F84);
        box(o, 0.1f, 1.95f, -0.545f, 0.16f, 2.05f, -0.52f, BLACK);
        beam(o, 0.0f, 2.42f, -0.3f, 0.0f, poleTop - 0.5f, -0.2f, 0.06f, 0xFF5D6166);
        beam(o, 0.0f, 0.0f, -0.25f, 0.0f, 1.45f, -0.3f, 0.06f, 0xFF5D6166);
        // crossarm with the radio activation antenna and a lightning arrester
        beam(o, -0.7f, poleTop - 1.1f, 0, 0.7f, poleTop - 1.1f, 0, 0.11f, WOOD_D);
        beam(o, 0.6f, poleTop - 1.05f, 0, 0.6f, poleTop + 0.6f, 0, 0.025f, 0xFFB0B6BC);
        cube(o, -0.6f, poleTop - 0.95f, 0, 0.05f, 0.1f, 0.05f, 0xFF9A6A3A);
        // rotator, chopper housing and the projector horn (turns while sounding)
        cylinder(o, 0, poleTop, 0, poleTop + 0.18f, 0.24f, 0.22f, 12, STEEL, true);
        Mesh.Out h = yaw(o, 0, 0, headYaw);
        cylinder(h, 0, poleTop + 0.18f, 0, poleTop + 0.78f, 0.3f, 0.3f, 14, 0xFFC9CDD0, true);
        for (float y = poleTop + 0.25f; y < poleTop + 0.74f; y += 0.12f) cylinder(h, 0, y, 0, y + 0.03f, 0.31f, 0.31f, 14, 0xFF9DA3A8, false);
        cylinder(h, 0, poleTop + 0.78f, 0, poleTop + 0.92f, 0.34f, 0.1f, 14, 0xFFB7BCC0, true);
        float hy = poleTop + 0.48f, mouth = 0.62f, throat = 0.2f;
        float[][] m = {{-mouth, hy - mouth, -1.35f}, {mouth, hy - mouth, -1.35f}, {mouth, hy + mouth, -1.35f}, {-mouth, hy + mouth, -1.35f}};
        float[][] t = {{-throat, hy - throat, -0.28f}, {throat, hy - throat, -0.28f}, {throat, hy + throat, -0.28f}, {-throat, hy + throat, -0.28f}};
        for (int k = 0; k < 4; k++) panel(h, t[k], t[(k + 1) % 4], m[(k + 1) % 4], m[k], 0xFFD5D9DC);
        for (int k = 0; k < 4; k++) {                                                   // rolled lip around the mouth
            float[] a = m[k], b = m[(k + 1) % 4];
            beam(h, a[0], a[1], a[2], b[0], b[1], b[2], 0.06f, 0xFFB4B9BD);
        }
        panel(h, new float[]{-throat, hy - throat, -0.3f}, new float[]{throat, hy - throat, -0.3f}, new float[]{throat, hy + throat, -0.3f}, new float[]{-throat, hy + throat, -0.3f}, 0xFF2A2D30);
        beam(h, 0, poleTop + 0.2f, -0.3f, 0, poleTop + 0.05f, -0.95f, 0.05f, STEEL);    // horn brace
    }

    // ------------------------------------------------------------------ automated weather station mast

    /**
     * A three-block instrument mast: tripod, rain gauge, data logger, louvred radiation shield (Stevenson screen),
     * solar panel, crossarm with a three-cup anemometer and a wind vane, and a lightning rod on top.
     */
    public static void weatherStation(Mesh.Out o, float cupSpin, float vaneYaw) {
        for (int k = 0; k < 3; k++) {                                                  // tripod
            double a = k * Math.PI * 2 / 3 + 0.4;
            beam(o, (float) Math.cos(a) * 0.62f, 0, (float) Math.sin(a) * 0.62f, 0, 0.95f, 0, 0.05f, GALV);
            cube(o, (float) Math.cos(a) * 0.62f, 0.02f, (float) Math.sin(a) * 0.62f, 0.07f, 0.02f, 0.07f, GALV_D);
        }
        cylinder(o, 0, 0, 0, 2.86f, 0.055f, 0.05f, 8, GALV, true);                      // mast
        // rain gauge on its own little stand beside the tripod
        cylinder(o, 0.55f, 0, 0.45f, 0.4f, 0.03f, 0.03f, 6, GALV_D, false);
        cylinder(o, 0.55f, 0.4f, 0.45f, 0.72f, 0.13f, 0.13f, 12, 0xFFE4E7E8, true);
        cylinder(o, 0.55f, 0.72f, 0.45f, 0.75f, 0.14f, 0.14f, 12, DARK, false);
        // data logger enclosure and the battery box
        box(o, -0.2f, 0.55f, 0.06f, 0.2f, 0.95f, 0.28f, 0xFFE1E3DE);
        box(o, -0.17f, 0.62f, 0.28f, 0.17f, 0.88f, 0.29f, 0xFFC6CAC4);
        box(o, 0.12f, 0.85f, 0.285f, 0.16f, 0.88f, 0.3f, 0xFF30C050);
        // radiation shield: stacked white louvres around the temperature/humidity probe
        for (int i = 0; i < 6; i++) {
            float y = 1.15f + i * 0.07f, r = 0.2f - (i == 5 ? 0.02f : 0);
            cylinder(o, 0.32f, y, 0, y + 0.035f, r, r - 0.03f, 12, WHITE, true);
        }
        beam(o, 0.05f, 1.3f, 0, 0.14f, 1.3f, 0, 0.04f, GALV_D);
        // solar panel, tilted south
        float[] p0 = {-0.38f, 1.62f, 0.05f}, p1 = {-0.02f, 1.62f, 0.05f}, p2 = {-0.02f, 1.86f, -0.28f}, p3 = {-0.38f, 1.86f, -0.28f};
        panel(o, p0, p1, p2, p3, GLASS);
        beam(o, -0.2f, 1.6f, 0, -0.05f, 1.6f, 0, 0.04f, GALV_D);
        // crossarm, anemometer (three cups on arms, spinning with the wind) and wind vane (pointing into the wind)
        beam(o, -0.62f, 2.62f, 0, 0.62f, 2.62f, 0, 0.05f, GALV);
        cylinder(o, 0.58f, 2.62f, 0, 2.82f, 0.03f, 0.03f, 6, DARK, false);
        Mesh.Out cups = yaw(o, 0.58f, 0, cupSpin);
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI * 2 / 3;
            float ex = 0.58f + (float) Math.cos(a) * 0.22f, ez = (float) Math.sin(a) * 0.22f;
            beam(cups, 0.58f, 2.8f, 0, ex, 2.8f, ez, 0.018f, DARK);
            float tx = (float) -Math.sin(a) * 0.05f, tz = (float) Math.cos(a) * 0.05f;
            cube(cups, ex + tx, 2.8f, ez + tz, 0.055f, 0.055f, 0.055f, 0xFF2E3236);
        }
        cylinder(o, -0.58f, 2.62f, 0, 2.78f, 0.025f, 0.025f, 6, DARK, false);
        Mesh.Out vane = yaw(o, -0.58f, 0, vaneYaw);
        beam(vane, -0.58f, 2.78f, -0.3f, -0.58f, 2.78f, 0.28f, 0.025f, DARK);
        panel(vane, new float[]{-0.58f, 2.7f, 0.12f}, new float[]{-0.58f, 2.7f, 0.32f}, new float[]{-0.58f, 2.9f, 0.34f}, new float[]{-0.58f, 2.86f, 0.12f}, 0xFFC8CDD2);
        cube(vane, -0.58f, 2.78f, -0.32f, 0.03f, 0.03f, 0.05f, 0xFFE0A020);
        // lightning rod
        beam(o, 0, 2.86f, 0, 0, 3.0f, 0, 0.02f, 0xFFC08040);
    }
}
