package com.stormreach.client.weather;

import com.stormreach.StormreachConfig;
import com.stormreach.tornado.radar.DopplerRadarItem;
import com.stormreach.tornado.radar.RadarLayout;
import com.stormreach.tornado.radar.RadarStructure;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * While you hold a Doppler radar and look at a block, outlines the 3x3x7 space it will take: green when it fits,
 * red when something is in the way. Turn it off with radarPlacementOutline=false in config/stormreach.properties.
 */
public final class RadarPlacementOutline {
    private RadarPlacementOutline() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!StormreachConfig.radarPlacementOutline || mc.player == null || mc.level == null || mc.gui.screen() != null) return;
            if (mc.player.tickCount % 3 != 0) return;
            boolean holding = false;
            for (InteractionHand h : InteractionHand.values()) holding |= mc.player.getItemInHand(h).getItem() instanceof DopplerRadarItem;
            if (!holding || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            BlockPos base = DopplerRadarItem.baseFor(mc.level, hit.getBlockPos(), hit.getDirection());
            boolean fits = RadarStructure.blocked(mc.level, base).isEmpty();
            DustParticleOptions dust = new DustParticleOptions(fits ? 0x40FF60 : 0xFF3030, 0.8f);
            double x0 = base.getX() - 1, y0 = base.getY(), z0 = base.getZ() - 1;
            double x1 = x0 + RadarLayout.WIDTH, y1 = y0 + RadarLayout.HEIGHT, z1 = z0 + RadarLayout.WIDTH;
            // the twelve edges of the box
            for (double t = 0; t <= 1.001; t += 1.0 / 12) {
                double x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t, z = z0 + (z1 - z0) * t;
                for (double a : new double[]{y0, y1}) for (double b : new double[]{z0, z1}) mc.level.addParticle(dust, x, a, b, 0, 0, 0);
                for (double a : new double[]{x0, x1}) for (double b : new double[]{z0, z1}) mc.level.addParticle(dust, a, y, b, 0, 0, 0);
                for (double a : new double[]{x0, x1}) for (double b : new double[]{y0, y1}) mc.level.addParticle(dust, a, b, z, 0, 0, 0);
            }
        });
    }
}
