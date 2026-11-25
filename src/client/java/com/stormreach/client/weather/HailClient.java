package com.stormreach.client.weather;

import com.stormreach.StormreachConfig;
import com.stormreach.particle.ModParticles;
import com.stormreach.sim.HailSize;
import com.stormreach.weather.precip.HailSim;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * 6.0: draws falling hail around the player as particles, from the hail field the server describes once a second.
 * Close to the camera only (LOD: small stones within ~16 blocks, big ones within ~28), scaled by the particle density
 * setting, and with nothing synced per stone. Stone sizes follow the same distribution as the server's impacts.
 */
public final class HailClient {
    private HailClient() {}

    private static HailSim.FieldPayload field;
    private static long fieldUntil;
    private static final Random RANDOM = new Random();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(HailSim.FieldPayload.TYPE, (p, ctx) -> { field = p; fieldUntil = System.currentTimeMillis() + p.ttl() * 50L; });
        ClientTickEvents.END_CLIENT_TICK.register(HailClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (field == null || mc.level == null || mc.player == null || mc.isPaused()) return;
        if (System.currentTimeMillis() > fieldUntil) { field = null; return; }
        HailSim.FieldPayload f = field;
        double px = mc.player.getX(), pz = mc.player.getZ();
        if (Math.hypot(px - f.x(), pz - f.z()) > f.radius() + 30) return;
        float density = StormreachConfig.particleDensity / 100f;
        int n = Math.round((8 + 34 * f.severity()) * density);
        for (int i = 0; i < n; i++) {
            HailSize size = HailSize.roll(RANDOM, f.severity());
            double reach = size.ordinal() >= HailSize.GOLF_BALL.ordinal() ? 28 : 16;
            double x = px + (RANDOM.nextDouble() - 0.5) * 2 * reach, z = pz + (RANDOM.nextDouble() - 0.5) * 2 * reach;
            if (Math.hypot(x - f.x(), z - f.z()) > f.radius()) continue;
            int top = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            double y = Math.max(top, mc.player.getY()) + 14 + RANDOM.nextDouble() * 10;
            var type = size.ordinal() <= HailSize.MARBLE.ordinal() ? ModParticles.HAIL_SMALL
                    : size.ordinal() <= HailSize.GOLF_BALL.ordinal() ? ModParticles.HAIL_MEDIUM : ModParticles.HAIL_LARGE;
            mc.level.addParticle(type, x, y, z, f.driftX(), -size.terminalMs() * 0.05, f.driftZ());
        }
    }
}
