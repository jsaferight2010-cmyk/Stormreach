package com.stormreach.progress;

import com.stormreach.Stormreach;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Awards code-driven advancements (those with a single "minecraft:impossible" criterion called "trigger").
 * Vanilla stores the progress per player and grants each advancement's rewards exactly once, so rewards can't
 * be duplicated by re-triggering.
 */
public final class StormAdvancements {
    private StormAdvancements() {}

    public static void award(ServerPlayer player, String name) {
        if (player == null || player.level().getServer() == null) return;
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Stormreach.id(name));
        if (holder == null) {
            Stormreach.LOGGER.debug("Missing advancement stormreach:{}", name);
            return;
        }
        player.getAdvancements().award(holder, "trigger");
    }

    public static boolean has(ServerPlayer player, String name) {
        if (player == null || player.level().getServer() == null) return false;
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Stormreach.id(name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
