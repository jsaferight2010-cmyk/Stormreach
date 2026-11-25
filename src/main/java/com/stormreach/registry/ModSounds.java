package com.stormreach.registry;

import com.stormreach.Stormreach;
import com.stormreach.sim.DialogueScript;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * All of the mod's own sound events. Voice-over lines are registered from the cinematic scripts in
 * {@code data/stormreach/cinematic/*.txt}, so adding or replacing a recording only needs the script line,
 * a sounds.json entry and the .ogg file.
 */
public final class ModSounds {
    public static final String[] SCRIPTS = {"cyrrus_intro", "cyrrus_phases", "cyrrus_defeat"};
    private static final Map<String, SoundEvent> BY_ID = new HashMap<>();

    public static final SoundEvent TORNADO_SIREN = register("block.tornado_siren");
    public static final SoundEvent TORNADO_ROAR = register("entity.tornado.roar");
    public static final SoundEvent LABYRINTH_CALM = register("music.labyrinth.calm");
    public static final SoundEvent LABYRINTH_TENSE = register("music.labyrinth.tense");
    public static final SoundEvent LABYRINTH_CHASE = register("music.labyrinth.chase");
    public static final SoundEvent HEARTBEAT = register("ambient.heartbeat");
    public static final SoundEvent STRANGER_WHISPER = register("entity.stranger.whisper");
    public static final SoundEvent GUARDIAN_SCREECH = register("entity.hollow_keeper.screech");
    public static final SoundEvent CYRRUS_THEME = register("music.cyrrus");

    private ModSounds() {}

    public static SoundEvent register(String path) {
        return BY_ID.computeIfAbsent(path, p -> {
            Identifier id = Stormreach.id(p);
            return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        });
    }

    public static SoundEvent get(String fullId) {
        String path = fullId.startsWith("stormreach:") ? fullId.substring("stormreach:".length()) : fullId;
        return BY_ID.get(path);
    }

    public static DialogueScript loadScript(String name) {
        try (InputStream in = ModSounds.class.getResourceAsStream("/data/stormreach/cinematic/" + name + ".txt")) {
            if (in == null) return DialogueScript.parse("");
            return DialogueScript.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Stormreach.LOGGER.error("Could not read cinematic script {}", name, e);
            return DialogueScript.parse("");
        }
    }

    public static void init() {
        for (String s : SCRIPTS) {
            for (DialogueScript.Cue cue : loadScript(s).cues) {
                if (cue.sound() != null) register(cue.sound().replace("stormreach:", ""));
            }
        }
    }
}
