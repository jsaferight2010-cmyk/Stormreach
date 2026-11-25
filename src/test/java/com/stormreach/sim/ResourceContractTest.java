package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the contract between the cinematic scripts, the voice recordings, sounds.json and the language file:
 * every voiced line exists, is translated, and the next cue never starts before the previous line has finished.
 */
class ResourceContractTest {
    private static final Path ROOT = Path.of(System.getProperty("stormreach.root", "."));
    private static final Path RES = ROOT.resolve("src/main/resources");

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private static DialogueScript script(String name) throws IOException {
        return DialogueScript.parse(read(RES.resolve("data/stormreach/cinematic/" + name + ".txt")));
    }

    private static double seconds(String voiceJson, String id) {
        Matcher m = Pattern.compile("\"" + id + "\": \\{[^}]*\"seconds\": ([0-9.]+)").matcher(voiceJson);
        assertTrue(m.find(), "voice_lines.json has no length for " + id);
        return Double.parseDouble(m.group(1));
    }

    @Test
    void everyVoicedCueHasAudioSoundsEntryAndSubtitle() throws IOException {
        String sounds = read(RES.resolve("assets/stormreach/sounds.json"));
        String lang = read(RES.resolve("assets/stormreach/lang/en_us.json"));
        int voiced = 0;
        for (String name : new String[]{"cyrrus_intro", "cyrrus_phases", "cyrrus_defeat"}) {
            for (DialogueScript.Cue cue : script(name).cues) {
                if (cue.sound() != null) {
                    voiced++;
                    String path = cue.sound().replace("stormreach:", "");
                    assertTrue(sounds.contains("\"" + path + "\""), "sounds.json lacks " + path);
                    Path ogg = RES.resolve("assets/stormreach/sounds/" + path.replace("voice.cyrrus.", "voice/cyrrus/") + ".ogg");
                    assertTrue(Files.size(ogg) > 2000, "recording missing or empty: " + ogg);
                }
                if (cue.subtitleKey() != null) assertTrue(lang.contains("\"" + cue.subtitleKey() + "\""), "untranslated " + cue.subtitleKey());
            }
        }
        assertEquals(15, voiced);
    }

    @Test
    void linesNeverOverlapSoAudioTextAndActionsStayInSync() throws IOException {
        String voice = read(ROOT.resolve("tools/voice_lines.json"));
        for (String name : new String[]{"cyrrus_intro", "cyrrus_defeat"}) {
            DialogueScript s = script(name);
            for (int i = 0; i < s.cues.size() - 1; i++) {
                DialogueScript.Cue cue = s.cues.get(i);
                if (cue.sound() == null) continue;
                double needed = seconds(voice, cue.id()) * 20;
                int gap = s.cues.get(i + 1).tick() - cue.tick();
                assertTrue(gap >= needed, name + ": " + cue.id() + " lasts " + needed + " ticks but the next cue starts after " + gap);
            }
        }
    }

    @Test
    void introBuildsUpAndEndsInTheFight() throws IOException {
        DialogueScript s = script("cyrrus_intro");
        DialogueScript.Cue last = s.cues.get(s.cues.size() - 1);
        assertTrue(last.actions().contains("fight"), "the intro must end by starting the fight");
        assertTrue(s.cues.stream().anyMatch(c -> c.actions().contains("corrupt")));
        assertTrue(s.cues.stream().anyMatch(c -> c.actions().contains("music")));
        double scale = 1;
        for (DialogueScript.Cue c : s.cues) {
            for (String a : c.actions()) {
                if (a.startsWith("grow:")) {
                    double next = Double.parseDouble(a.substring(5));
                    assertTrue(next > scale, "Cyrrus only grows during the intro");
                    scale = next;
                }
            }
        }
        assertEquals(4.0, scale, 1e-9);
        assertTrue(s.length() < 20 * 60, "the intro should stay under a minute");
    }

    @Test
    void phaseLinesCoverEveryPhaseChange() throws IOException {
        DialogueScript s = script("cyrrus_phases");
        for (int p = 2; p <= 5; p++) {
            final String id = "phase" + p;
            assertTrue(s.cues.stream().anyMatch(c -> c.id().equals(id) && c.sound() != null), "no voiced line for " + id);
        }
        for (double hp : new double[]{1.0, 0.81, 0.79, 0.61, 0.59, 0.41, 0.39, 0.21, 0.19, 0.0}) {
            int phase = BossPlan.phaseFor(hp);
            assertTrue(phase >= 1 && phase <= 5);
        }
    }
}
