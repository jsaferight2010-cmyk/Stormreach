package com.stormreach.client.story;

import com.stormreach.Stormreach;
import com.stormreach.story.CinematicSync;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 6.0: the Cyrrus intro, shot like a film. Five shots follow the script's beats, each a slow, continuous move, with
 * 1.5 s cross-blends between them (no hard cuts), all framed on the live boss so they follow its rise and growth:
 * <ol>
 *   <li>0-110: a high establishing crane, drifting down toward the shrine from your side of it;</li>
 *   <li>110-270: a low angle beside the altar, sliding sideways as Cyrrus climbs out of the ground;</li>
 *   <li>270-520: a slow orbit at chest height while it speaks;</li>
 *   <li>520-770: a pull-back that keeps it framed as it grows;</li>
 *   <li>770-end: a push-in on its face, then a crane back to your own eyes as the fight begins.</li>
 * </ol>
 * Letterbox bars slide in and out. Press Jump to skip (the hint shows for the first five seconds); in multiplayer the
 * fight starts early only when everyone watching has skipped. The camera is client-only; the story runs on the server.
 */
public final class CinematicCamera {
    /** 6.0: forget the last session (called when the connection changes). */
    public static void resetSession() {
        active = false;
        endingTick = -1;
    }

    private CinematicCamera() {}

    private static boolean active;
    private static int entityId, length;
    private static BlockPos altar;
    private static double startTick;      // client game time of cinematic tick 0
    private static Vec3 side;             // unit vector from the altar toward where the player stood
    private static int endingTick = -1;   // client tick the blend back to the player began
    private static boolean jumpHeld = true;

    static final int BLEND = 30, OUT = 25;
    static final int[] CUTS = {0, 110, 270, 520, 770};

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(CinematicSync.Start.TYPE, (p, ctx) -> start(p));
        ClientPlayNetworking.registerGlobalReceiver(CinematicSync.End.TYPE, (p, ctx) -> { if (active && p.entityId() == entityId) end(); });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!active) return;
            if (mc.level == null || mc.player == null) { active = false; return; }
            if (endingTick >= 0 && now(mc) - endingTick > OUT) { active = false; return; }
            boolean jump = mc.options.keyJump.isDown();
            if (jump && !jumpHeld && endingTick < 0) {
                ClientPlayNetworking.send(new CinematicSync.Skip(entityId));
                end();
            }
            jumpHeld = jump;
            if (endingTick < 0 && mc.level.getEntity(entityId) == null) end();   // the boss is gone or out of range
        });
        HudElementRegistry.addLast(Stormreach.id("cinematic_bars"), (g, delta) -> bars(g));
    }

    private static long now(Minecraft mc) { return mc.level.getGameTime(); }

    static void start(CinematicSync.Start p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        active = true;
        entityId = p.entityId();
        altar = p.altar();
        length = Math.max(200, p.length());
        startTick = now(mc) - p.atTick();
        endingTick = -1;
        jumpHeld = true;   // a jump already held doesn't count as a skip
        Vec3 d = mc.player.position().subtract(Vec3.atCenterOf(altar)).multiply(1, 0, 1);
        side = d.lengthSqr() < 1 ? new Vec3(0, 0, 1) : d.normalize();
    }

    static void end() {
        Minecraft mc = Minecraft.getInstance();
        if (!active || endingTick >= 0 || mc.level == null) return;
        endingTick = (int) now(mc);
    }

    public static boolean active() { return active; }

    private static double smooth(double x) { x = Mth.clamp(x, 0, 1); return x * x * (3 - 2 * x); }

    /** Where the camera is and what it looks at, at cinematic time {@code t} (ticks), for shot {@code shot}. */
    private static Vec3[] shot(int shot, double t, Vec3 a, Vec3 boss, double size) {
        Vec3 right = new Vec3(-side.z, 0, side.x);
        Vec3 head = boss.add(0, 1.7 * size, 0), chest = boss.add(0, 1.1 * size, 0);
        switch (shot) {
            case 0 -> {   // establishing crane: high and wide, drifting down and in
                double k = smooth(t / 110.0);
                return new Vec3[]{a.add(side.scale(26 - 8 * k)).add(right.scale(6)).add(0, 15 - 6 * k, 0), a.add(0, 1, 0)};
            }
            case 1 -> {   // low angle beside the altar, sliding sideways as it rises
                double k = smooth((t - 110) / 160.0);
                return new Vec3[]{a.add(side.scale(5.5)).add(right.scale(-3 + 6 * k)).add(0, 0.4, 0), chest};
            }
            case 2 -> {   // slow orbit at chest height
                double ang = Math.atan2(side.z, side.x) + Math.toRadians(-35 + 70 * smooth((t - 270) / 250.0));
                double r = 5 + 2 * size;
                return new Vec3[]{boss.add(Math.cos(ang) * r, 1.3 * size, Math.sin(ang) * r), head};
            }
            case 3 -> {   // pull back to keep it framed as it grows
                double k = smooth((t - 520) / 250.0);
                double r = 7 + 4 * size + 6 * k;
                return new Vec3[]{boss.add(side.scale(r)).add(right.scale(r * 0.35)).add(0, 1.2 * size + 2 * k, 0), chest};
            }
            default -> {  // push in on the face
                double k = smooth((t - 770) / Math.max(40, length - 770 - 40));
                double r = 6 + 3 * size - 3 * k;
                return new Vec3[]{head.add(side.scale(r)).add(0, -0.4 * size, 0), head};
            }
        }
    }

    private static int shotAt(double t) {
        int s = 0;
        for (int i = 0; i < CUTS.length; i++) if (t >= CUTS[i]) s = i;
        return s;
    }

    /**
     * The camera this frame: {x, y, z, yaw, pitch}, or null when not playing. {@code playerEye}/{@code playerYaw}/
     * {@code playerPitch} are the normal view, used to blend out at the end (and in if joined at tick 0).
     */
    public static double[] pose(float partial, Vec3 playerEye, float playerYaw, float playerPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null) return null;
        Entity e = mc.level.getEntity(entityId);
        double t = now(mc) + partial - startTick;
        Vec3 a = Vec3.atBottomCenterOf(altar).add(0, 1, 0);
        Vec3 boss = e == null ? a : e.getPosition(partial);
        double size = e instanceof LivingEntity le ? Math.max(1, le.getScale()) : 1;
        int s = shotAt(t);
        Vec3[] cam = shot(s, t, a, boss, size);
        double into = t - CUTS[s];
        if (s > 0 && into < BLEND) {   // cross-blend from the previous shot (never a hard cut)
            Vec3[] prev = shot(s - 1, t, a, boss, size);
            double k = smooth(into / BLEND);
            cam = new Vec3[]{prev[0].lerp(cam[0], k), prev[1].lerp(cam[1], k)};
        }
        Vec3 dir = cam[1].subtract(cam[0]);
        double yaw = Math.toDegrees(Math.atan2(-dir.x, dir.z));
        double pitch = Math.toDegrees(-Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));
        // blend in over the first second, and back to the player's own eyes when it ends or is skipped
        double w = smooth(t / 20.0);
        if (endingTick >= 0) w *= 1 - smooth((now(mc) + partial - endingTick) / OUT);
        if (w >= 0.999) return new double[]{cam[0].x, cam[0].y, cam[0].z, yaw, pitch};
        Vec3 p = playerEye.lerp(cam[0], w);
        return new double[]{p.x, p.y, p.z, playerYaw + Mth.wrapDegrees(yaw - playerYaw) * w, playerPitch + (pitch - playerPitch) * w};
    }

    /** Letterbox bars (sliding in and out) and the skip hint. */
    private static void bars(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null) return;
        double t = now(mc) - startTick;
        double w = smooth(t / 20.0);
        if (endingTick >= 0) w *= 1 - smooth((now(mc) - endingTick) / (double) OUT);
        int h = (int) Math.round(g.guiHeight() * 0.11 * w);
        if (h <= 0) return;
        g.fill(0, 0, g.guiWidth(), h, 0xFF000000);
        g.fill(0, g.guiHeight() - h, g.guiWidth(), g.guiHeight(), 0xFF000000);
        if (t < 100 && endingTick < 0) {
            Component hint = Component.translatable("cinematic.stormreach.skip", mc.options.keyJump.getTranslatedKeyMessage());
            g.centeredText(mc.font, hint, g.guiWidth() / 2, g.guiHeight() - h / 2 - 4, 0xFFB0B0B0);
        }
    }
}
