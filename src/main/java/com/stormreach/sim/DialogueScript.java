package com.stormreach.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A cinematic script: one cue per line, {@code tick | cue id | subtitle key | sound id | actions}.
 * Blank lines and lines starting with '#' are ignored. Actions are comma-separated words such as
 * {@code rise}, {@code grow:3.0}, {@code thunder}, {@code rain}, {@code strike_ring}, {@code darken}, {@code fight}.
 * Keeping the text (lang file), the audio (sounds.json + .ogg) and the timing (this script) apart means a
 * line can be re-recorded or added without touching code.
 */
public final class DialogueScript {
    public record Cue(int tick, String id, String subtitleKey, String sound, List<String> actions) {}

    public final List<Cue> cues;

    private DialogueScript(List<Cue> cues) {
        this.cues = cues;
    }

    public static DialogueScript parse(String text) {
        List<Cue> out = new ArrayList<>();
        int last = -1;
        int lineNo = 0;
        for (String raw : text.split("\n")) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\|", -1);
            if (p.length != 5) throw new IllegalArgumentException("line " + lineNo + ": expected 5 fields");
            int tick = Integer.parseInt(p[0].trim());
            if (tick < last) throw new IllegalArgumentException("line " + lineNo + ": ticks must not go backwards");
            last = tick;
            List<String> actions = new ArrayList<>();
            for (String a : p[4].split(",")) if (!a.isBlank()) actions.add(a.trim());
            out.add(new Cue(tick, p[1].trim(), emptyToNull(p[2]), emptyToNull(p[3]), actions));
        }
        return new DialogueScript(List.copyOf(out));
    }

    private static String emptyToNull(String s) {
        s = s.trim();
        return s.isEmpty() || s.equals("-") ? null : s;
    }

    public int length() {
        return cues.isEmpty() ? 0 : cues.get(cues.size() - 1).tick();
    }
}
