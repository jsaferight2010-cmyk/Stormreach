package com.stormreach.client.story;

import com.stormreach.labyrinth.HollowKeeper;
import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModSounds;
import com.stormreach.sim.HidingRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

/**
 * Adaptive Labyrinth music: three looping layers (calm, tense, chase) play together while you're inside, and
 * their volumes cross-fade with how close the Hollow Keeper is and whether it's hunting you. Vanilla music is
 * held off while you're inside, and the layers fade out and stop when you leave.
 */
public final class LabyrinthMusic {
    private static Layer calm, tense, chase;
    private static float intensity;
    private static int fadeOut;

    private LabyrinthMusic() {}

    static final class Layer extends AbstractTickableSoundInstance {
        float target;

        Layer(SoundEvent e) {
            super(e, SoundSource.MUSIC, RandomSource.create());
            looping = true;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = 0.001f;
        }

        @Override
        public void tick() {
            volume += (target - volume) * 0.05f;
            if (target <= 0 && volume < 0.005f && fadeOut > 0) stop();
        }

        void kill() { stop(); }
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        boolean inside = p != null && p.hasEffect(ModEffects.LABYRINTH);
        if (!inside) {
            if (calm != null) {
                fadeOut++;
                calm.target = tense.target = chase.target = 0;
                if (fadeOut > 80) { calm.kill(); tense.kill(); chase.kill(); calm = tense = chase = null; }
            }
            return;
        }
        fadeOut = 0;
        if (calm == null || mc.getSoundManager().isActive(calm) == false) {
            if (calm != null) { calm.kill(); tense.kill(); chase.kill(); }
            calm = new Layer(ModSounds.LABYRINTH_CALM);
            tense = new Layer(ModSounds.LABYRINTH_TENSE);
            chase = new Layer(ModSounds.LABYRINTH_CHASE);
            mc.getSoundManager().play(calm);
            mc.getSoundManager().play(tense);
            mc.getSoundManager().play(chase);
        }
        mc.getMusicManager().stopPlaying();
        double dist = 999;
        boolean hunting = false;
        for (HollowKeeper k : p.level().getEntitiesOfClass(HollowKeeper.class, new AABB(p.blockPosition()).inflate(64))) {
            double d = k.distanceTo(p);
            if (d < dist) { dist = d; hunting = k.isAggressive(); }
        }
        float wanted = (float) HidingRules.musicIntensity(dist, hunting);
        intensity += (wanted - intensity) * 0.03f; // smooth transitions
        calm.target = 0.7f * Math.max(0, 1 - intensity);
        tense.target = 0.8f * Math.max(0, 1 - Math.abs(intensity - 1));
        chase.target = 0.9f * Math.max(0, intensity - 1);
    }
}
