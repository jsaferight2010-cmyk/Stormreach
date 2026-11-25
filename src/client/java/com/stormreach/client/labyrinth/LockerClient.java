package com.stormreach.client.labyrinth;

import com.stormreach.labyrinth.LockerBlock;
import com.stormreach.labyrinth.LockerNet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The hiding player's side of the Rusted Locker. While hidden: movement keys do nothing (see KeyboardInputMixin),
 * the camera sits just behind the grate looking out (see CameraMixin), attacking is blocked, and Use steps out.
 */
public final class LockerClient {
    private static BlockPos locker;
    private static Direction facing = Direction.NORTH;

    private LockerClient() {}

    public static boolean hidden() { return locker != null; }

    public static Direction facing() { return facing; }

    /** Eye point: centred in the locker, 3 pixels behind the inner grate, at standing eye height. */
    public static Vec3 eye() {
        Vec3 c = Vec3.atBottomCenterOf(locker);
        double inset = 0.5 - 2.0 / 16 - 3.0 / 16;
        return c.add(facing.getStepX() * inset, 1.6, facing.getStepZ() * inset);
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(LockerNet.HidePayload.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
            locker = payload.hidden() ? payload.locker() : null;
            facing = Direction.from2DDataValue(payload.facing());
            LockerBlock.clientHiddenAt = locker;
        }));
        ClientTickEvents.END_CLIENT_TICK.register(LockerClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (locker == null) return;
        if (mc.player == null || mc.level == null) { locker = null; LockerBlock.clientHiddenAt = null; return; }
        while (mc.options.keyAttack.consumeClick()) { /* no attacking out of a locker */ }
        boolean exit = false;
        while (mc.options.keyUse.consumeClick()) exit = true;
        if (exit && mc.gui.screen() == null) ClientPlayNetworking.send(new LockerNet.ExitPayload());
    }
}
