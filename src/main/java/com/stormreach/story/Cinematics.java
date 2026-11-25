package com.stormreach.story;

import com.stormreach.registry.ModSounds;
import com.stormreach.sim.DialogueScript;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Plays dialogue cues: each cue's voice clip is played at the speaker's position (server-side, so every nearby
 * player hears the same line at the same time) and its subtitle is shown in chat and on the action bar.
 * Scripts, subtitles and audio are separate files; nothing here is specific to one recording.
 */
public final class Cinematics {
    public static final double HEARING_RANGE = 96;

    private record Playing(ServerLevel level, Vec3 at, DialogueScript script, int[] tick, int[] next) {}

    private static final List<Playing> DETACHED = new ArrayList<>();

    private Cinematics() {}

    public static void speak(ServerLevel level, Vec3 at, DialogueScript.Cue cue) {
        if (cue.sound() != null) {
            SoundEvent s = ModSounds.get(cue.sound());
            if (s != null) level.playSound(null, at.x, at.y, at.z, s, SoundSource.VOICE, 4.0f, 1.0f);
        }
        if (cue.subtitleKey() != null) {
            Component line = Component.translatable("dialogue.stormreach.speaker.cyrrus").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal(" ")).append(Component.translatable(cue.subtitleKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
            for (ServerPlayer p : level.players()) {
                if (p.position().distanceToSqr(at) < HEARING_RANGE * HEARING_RANGE) {
                    p.sendSystemMessage(line);
                    p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(cue.subtitleKey()).withStyle(ChatFormatting.LIGHT_PURPLE)));
                }
            }
        }
    }

    /** Plays a script that isn't tied to a living speaker (for example the defeat lines after death). */
    public static void playDetached(ServerLevel level, Vec3 at, String script) {
        DETACHED.add(new Playing(level, at, ModSounds.loadScript(script), new int[]{0}, new int[]{0}));
    }

    public static void tick(ServerLevel level) {
        for (Iterator<Playing> it = DETACHED.iterator(); it.hasNext(); ) {
            Playing p = it.next();
            if (p.level() != level) continue;
            int t = p.tick()[0]++;
            while (p.next()[0] < p.script().cues.size() && p.script().cues.get(p.next()[0]).tick() <= t) {
                speak(level, p.at(), p.script().cues.get(p.next()[0]++));
            }
            if (p.next()[0] >= p.script().cues.size()) it.remove();
        }
    }
}
