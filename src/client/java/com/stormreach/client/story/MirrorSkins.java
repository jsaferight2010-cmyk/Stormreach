package com.stormreach.client.story;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

/** The in-game skin of a particular player, as this client has it (tab list first, then a loaded player entity). */
public final class MirrorSkins {
    private MirrorSkins() {}

    public static PlayerSkin of(UUID player) {
        Minecraft mc = Minecraft.getInstance();
        if (player != null) {
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(player);
                if (info != null) return info.getSkin();
            }
            if (mc.level != null) {
                Player p = mc.level.getPlayerByUUID(player);
                if (p instanceof AbstractClientPlayer cp) return cp.getSkin();
            }
            return DefaultPlayerSkin.get(player);
        }
        return mc.player != null ? mc.player.getSkin() : DefaultPlayerSkin.getDefaultSkin();
    }
}
