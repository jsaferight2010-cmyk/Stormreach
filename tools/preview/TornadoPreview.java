import com.stormreach.client.tornado.TornadoMesh;
import com.stormreach.client.tornado.TornadoRenderState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline tornado preview: builds the exact meshes the game renderer submits (TornadoMesh) and rasterises them in
 * software, so the look can be checked without launching Minecraft. Lighting is approximated (flat ambient plus a
 * simple directional term), so treat it as a shape and composition check, not a pixel-exact in-game screenshot.
 *
 * usage: java TornadoPreview <textures dir> <out.png> <spec>...   spec = morph,formation,reach,width,intensity,subs,label[,see]
 */
public class TornadoPreview {
    static final int W = 360, H = 480;
    record V(float x, float y, float z, float u, float v, int argb) {}
    record Quad(V[] v, BufferedImage tex, double depth) {}

    public static void main(String[] a) throws Exception {
        String texDir = a[0];
        BufferedImage funnel = ImageIO.read(new File(texDir, "tornado_funnel.png"));
        BufferedImage debris = ImageIO.read(new File(texDir, "tornado_debris.png"));
        BufferedImage wall = ImageIO.read(new File(texDir, "tornado_wallcloud.png"));
        int n = a.length - 2, cols = Math.min(4, n), rows = (n + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(W * cols, H * rows, BufferedImage.TYPE_INT_RGB);
        for (int k = 0; k < n; k++) {
            String[] p = a[k + 2].split(",");
            if (p[0].equals("cloud") || p[0].equals("hail") || p[0].equals("radar")) {
                BufferedImage img = renderOther(p, ImageIO.read(new File(texDir, p[0].equals("cloud") ? "storm_cloud.png" : p[0].equals("radar") ? "../block/radar_dish.png" : "hailstone.png")));
                java.awt.Graphics2D g = img.createGraphics();
                g.setColor(java.awt.Color.WHITE); g.drawString(p[p.length - 1].replace('_', ' '), 8, 16); g.dispose();
                sheet.getGraphics().drawImage(img, (k % cols) * W, (k / cols) * H, null);
                continue;
            }
            TornadoRenderState s = new TornadoRenderState();
            s.morph = Integer.parseInt(p[0]); s.formation = Integer.parseInt(p[1]); s.reach = Float.parseFloat(p[2]);
            s.width = Float.parseFloat(p[3]); s.intensity = Float.parseFloat(p[4]); s.subvortices = Integer.parseInt(p[5]);
            s.seeThrough = p.length > 7 && p[7].equals("see");
            s.cloudBase = s.formation == 2 ? 55 : 85; s.rotation = -1; s.ageInTicks = 400; s.seed = 3; s.spin = 37;
            BufferedImage img = render(s, funnel, debris, wall, s.formation == 2);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.WHITE); g.drawString(p.length > 6 ? p[6].replace('_', ' ') : "", 8, 16); g.dispose();
            sheet.getGraphics().drawImage(img, (k % cols) * W, (k / cols) * H, null);
        }
        ImageIO.write(sheet, "png", new File(a[1]));
    }

    static BufferedImage render(TornadoRenderState s, BufferedImage funnel, BufferedImage debris, BufferedImage wall, boolean water) {
        int[] px = new int[W * H];
        // storm sky and the ground (or sea) below the horizon
        double dist = Math.max(70, s.width * 2.4 + 60), eyeY = 2.0;
        double lookY = s.cloudBase * 0.42;
        double pitch = Math.atan2(lookY - eyeY, dist), f = (H / 2.0) / Math.tan(Math.toRadians(70) / 2);
        int horizon = (int) (H / 2.0 + Math.tan(pitch) * f);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int c;
            if (y < horizon) { double t = (double) y / Math.max(1, horizon); c = mix(0x3E444C, 0x7D858E, t); }
            else c = water ? mix(0x3B5566, 0x22323C, (y - horizon) / (double) (H - horizon)) : mix(0x4D5A3A, 0x2F3824, (y - horizon) / (double) (H - horizon));
            px[y * W + x] = c;
        }
        List<Quad> quads = new ArrayList<>();
        collect(quads, funnel, sink -> TornadoMesh.funnelLayer(s, sink), dist, eyeY, pitch);
        if (TornadoMesh.hasDebris(s)) collect(quads, debris, sink -> TornadoMesh.debrisLayer(s, sink), dist, eyeY, pitch);
        if (TornadoMesh.hasWallCloud(s)) collect(quads, wall, sink -> TornadoMesh.wallLayer(s, sink), dist, eyeY, pitch);
        quads.sort((q1, q2) -> Double.compare(q2.depth, q1.depth));
        for (Quad q : quads) {
            tri(px, q.v[0], q.v[1], q.v[2], q.tex, dist, eyeY, pitch, f);
            tri(px, q.v[0], q.v[2], q.v[3], q.tex, dist, eyeY, pitch, f);
        }
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, W, H, px, 0, W);
        return img;
    }

    interface Build { void run(TornadoMesh.Sink s); }

    /** cloud,growth,width,depth,severe,label  or  hail,sizeOrdinal,label */
    static BufferedImage renderOther(String[] p, BufferedImage tex) {
        int[] px = new int[W * H];
        boolean cloud = p[0].equals("cloud"), radar = p[0].equals("radar");
        double dist = cloud ? 520 : radar ? 6 : 1.2, eyeY = cloud ? 2 : radar ? 2.2 : 0.15, pitch = cloud ? Math.toRadians(14) : radar ? Math.toRadians(-6) : 0;
        double f = (H / 2.0) / Math.tan(Math.toRadians(70) / 2);
        int horizon = (int) (H / 2.0 + Math.tan(pitch) * f);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++)
            px[y * W + x] = y < horizon ? mix(0x6C8DB0, 0xA9BED3, (double) y / Math.max(1, horizon)) : mix(0x4D5A3A, 0x2F3824, (y - horizon) / (double) (H - horizon));
        List<Quad> quads = new ArrayList<>();
        if (cloud) {
            float g = Float.parseFloat(p[1]), w = Float.parseFloat(p[2]), d = Float.parseFloat(p[3]);
            boolean sev = p[4].equals("1");
            collect(quads, tex, sink -> com.stormreach.client.weather.WeatherMesh.stormCloud((x, y, z, u, v, c) -> sink.vertex(x, y + 90, z, u, v, c), g, w, d, sev, 7, 0), dist, eyeY, pitch);
        } else if (radar) {
            float yaw = (float) Math.toRadians(Float.parseFloat(p[1]));
            collect(quads, tex, sink -> com.stormreach.client.weather.RadarMesh.build((x, y, z, u, v, c) -> sink.vertex(x, y + 0.5f, z, u, v, c), yaw), dist, eyeY, pitch);
        } else {
            com.stormreach.sim.HailSize hs = com.stormreach.sim.HailSize.values()[Integer.parseInt(p[1])];
            collect(quads, tex, sink -> com.stormreach.client.weather.WeatherMesh.hailstone(sink, hs.visualSize(), 3, 0), dist, eyeY, pitch);
        }
        quads.sort((q1, q2) -> Double.compare(q2.depth, q1.depth));
        for (Quad q : quads) { tri(px, q.v[0], q.v[1], q.v[2], q.tex, dist, eyeY, pitch, f); tri(px, q.v[0], q.v[2], q.v[3], q.tex, dist, eyeY, pitch, f); }
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, W, H, px, 0, W);
        return img;
    }

    static void collect(List<Quad> out, BufferedImage tex, Build b, double dist, double eyeY, double pitch) {
        List<V> vs = new ArrayList<>();
        b.run((x, y, z, u, v, argb) -> vs.add(new V(x, y, z, u, v, argb)));
        for (int i = 0; i + 3 < vs.size(); i += 4) {
            V[] q = {vs.get(i), vs.get(i + 1), vs.get(i + 2), vs.get(i + 3)};
            double d = 0;
            for (V v : q) d += view(v, dist, eyeY, pitch)[2];
            out.add(new Quad(q, tex, d / 4));
        }
    }

    /** Camera at (0, eyeY, -dist) looking toward +z, tilted up by pitch. Returns camera-space x, y, z. */
    static double[] view(V v, double dist, double eyeY, double pitch) {
        double x = v.x, y = v.y - eyeY, z = v.z + dist;
        double cy = Math.cos(pitch), sy = Math.sin(pitch);
        return new double[]{x, y * cy - z * sy, y * sy + z * cy};
    }

    static void tri(int[] px, V a, V b, V c, BufferedImage tex, double dist, double eyeY, double pitch, double f) {
        double[][] p = new double[3][];
        V[] vs = {a, b, c};
        for (int i = 0; i < 3; i++) {
            double[] cv = view(vs[i], dist, eyeY, pitch);
            if (cv[2] < 1) return;
            p[i] = new double[]{W / 2.0 + cv[0] / cv[2] * f, H / 2.0 - cv[1] / cv[2] * f, 1 / cv[2]};
        }
        // flat shade from the face normal: ambient plus a key light from upper left
        double[] e1 = {b.x - a.x, b.y - a.y, b.z - a.z}, e2 = {c.x - a.x, c.y - a.y, c.z - a.z};
        double nx = e1[1] * e2[2] - e1[2] * e2[1], ny = e1[2] * e2[0] - e1[0] * e2[2], nz = e1[0] * e2[1] - e1[1] * e2[0];
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-9;
        double lam = Math.abs((nx * -0.5 + ny * 0.6 + nz * -0.6) / nl);
        double shade = 0.62 + 0.38 * lam;
        int minX = (int) Math.max(0, Math.floor(Math.min(p[0][0], Math.min(p[1][0], p[2][0]))));
        int maxX = (int) Math.min(W - 1, Math.ceil(Math.max(p[0][0], Math.max(p[1][0], p[2][0]))));
        int minY = (int) Math.max(0, Math.floor(Math.min(p[0][1], Math.min(p[1][1], p[2][1]))));
        int maxY = (int) Math.min(H - 1, Math.ceil(Math.max(p[0][1], Math.max(p[1][1], p[2][1]))));
        double area = (p[1][0] - p[0][0]) * (p[2][1] - p[0][1]) - (p[2][0] - p[0][0]) * (p[1][1] - p[0][1]);
        if (Math.abs(area) < 1e-6) return;
        int tw = tex.getWidth(), th = tex.getHeight();
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            double sx = x + 0.5, sy = y + 0.5;
            double w0 = ((p[1][0] - sx) * (p[2][1] - sy) - (p[2][0] - sx) * (p[1][1] - sy)) / area;
            double w1 = ((p[2][0] - sx) * (p[0][1] - sy) - (p[0][0] - sx) * (p[2][1] - sy)) / area;
            double w2 = 1 - w0 - w1;
            if (w0 < 0 || w1 < 0 || w2 < 0) continue;
            double iz = w0 * p[0][2] + w1 * p[1][2] + w2 * p[2][2];
            double u = (w0 * a.u * p[0][2] + w1 * b.u * p[1][2] + w2 * c.u * p[2][2]) / iz;
            double v = (w0 * a.v * p[0][2] + w1 * b.v * p[1][2] + w2 * c.v * p[2][2]) / iz;
            int t = tex.getRGB(Math.floorMod((int) (u * tw), tw), Math.floorMod((int) (v * th), th));
            int ca = a.argb, cb = b.argb, cc = c.argb;
            double al = (w0 * (ca >>> 24) + w1 * (cb >>> 24) + w2 * (cc >>> 24)) / 255.0 * ((t >>> 24) / 255.0);
            if (al < 0.1) continue; // entityTranslucent discards near-transparent fragments
            double r = (w0 * ((ca >> 16) & 255) + w1 * ((cb >> 16) & 255) + w2 * ((cc >> 16) & 255)) * ((t >> 16) & 255) / 255.0 * shade;
            double g = (w0 * ((ca >> 8) & 255) + w1 * ((cb >> 8) & 255) + w2 * ((cc >> 8) & 255)) * ((t >> 8) & 255) / 255.0 * shade;
            double bl = (w0 * (ca & 255) + w1 * (cb & 255) + w2 * (cc & 255)) * (t & 255) / 255.0 * shade;
            int dst = px[y * W + x];
            int rr = (int) (r * al + ((dst >> 16) & 255) * (1 - al)), gg = (int) (g * al + ((dst >> 8) & 255) * (1 - al)), bb = (int) (bl * al + (dst & 255) * (1 - al));
            px[y * W + x] = (Math.min(255, rr) << 16) | (Math.min(255, gg) << 8) | Math.min(255, bb);
        }
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
