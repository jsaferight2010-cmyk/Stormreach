package com.stormreach.client.weather;

import com.mojang.blaze3d.platform.NativeImage;
import com.stormreach.Stormreach;
import com.stormreach.sim.RadarMath;
import com.stormreach.tornado.RadarScan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The radar scope picture: the polar scan from the server painted pixel by pixel into a texture (one per screen
 * size), rebuilt only when a new scan or a mode change arrives. Drawing 13 000 coloured pixels as one textured quad
 * is what makes the scope look like a real PPI instead of a few blobs.
 */
public final class RadarImage {
    public static final Identifier ID = Stormreach.id("dynamic/radar_scope");
    private static DynamicTexture texture;
    private static int size;
    private static Object lastScan;
    private static boolean lastVelocity;

    private RadarImage() {}

    /** Makes sure the texture shows this scan in this mode; returns false when there is nothing to draw. */
    public static boolean prepare(RadarScan.Grid scan, int radiusPx, boolean velocity) {
        if (scan == null) return false;
        int s = radiusPx * 2;
        Minecraft mc = Minecraft.getInstance();
        if (texture == null || size != s) {
            if (texture != null) texture.close();
            texture = new DynamicTexture(() -> "stormreach radar scope", s, s, true);
            mc.getTextureManager().register(ID, texture);
            size = s; lastScan = null;
        }
        if (scan == lastScan && velocity == lastVelocity) return true;
        NativeImage img = texture.getPixels();
        if (img == null) return false;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double dx = (x + 0.5 - radiusPx) / radiusPx, dz = (y + 0.5 - radiusPx) / radiusPx;
                int argb = 0;
                int[] gate = RadarMath.gateOf(dx, dz, 1.0);
                if (gate != null) {
                    int i = RadarMath.index(gate[0], gate[1]);
                    if (velocity) {
                        byte v = scan.velocity()[i];
                        argb = v == RadarMath.NO_DATA ? 0 : (RadarMath.velocityColour(v) & 0x00FFFFFF) | 0xE0000000;
                    } else {
                        argb = RadarMath.dbzColour(scan.dbz()[i]);
                        if (argb != 0) argb = (argb & 0x00FFFFFF) | 0xD8000000;
                    }
                }
                img.setPixel(x, y, argb);
            }
        }
        texture.upload();
        lastScan = scan; lastVelocity = velocity;
        return true;
    }
}
