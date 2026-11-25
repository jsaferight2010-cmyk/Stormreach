package com.stormreach.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * Stormreach's own sound effects (5.1). Every sound the mod plays is original: synthesised by tools/sfx51.py, listed
 * in tools/sfx_list.py (keep the two in step). Nothing here plays a vanilla sound.
 */
public final class ModSfx {
    private ModSfx() {}

    public static final SoundEvent WEATHER_THUNDER = ModSounds.register("weather.thunder");
    public static final SoundEvent WEATHER_GUST = ModSounds.register("weather.gust");
    public static final SoundEvent WEATHER_WIND = ModSounds.register("weather.wind");
    public static final SoundEvent MAGIC_CHIME = ModSounds.register("magic.chime");
    public static final SoundEvent MAGIC_SHATTER = ModSounds.register("magic.shatter");
    public static final SoundEvent MAGIC_HUM = ModSounds.register("magic.hum");
    public static final SoundEvent MAGIC_ACTIVATE = ModSounds.register("magic.activate");
    public static final SoundEvent MAGIC_DISPEL = ModSounds.register("magic.dispel");
    public static final SoundEvent MAGIC_WARP = ModSounds.register("magic.warp");
    public static final SoundEvent BLOCK_METAL_PLACE = ModSounds.register("block.metal_place");
    public static final SoundEvent ITEM_EQUIP_PACK = ModSounds.register("item.equip_pack");
    public static final SoundEvent ITEM_EQUIP_METAL = ModSounds.register("item.equip_metal");
    public static final SoundEvent ITEM_BOTTLE = ModSounds.register("item.bottle");
    public static final SoundEvent ITEM_PICK = ModSounds.register("item.pick");
    public static final SoundEvent ITEM_POP = ModSounds.register("item.pop");
    public static final SoundEvent ITEM_THROW = ModSounds.register("item.throw");
    public static final SoundEvent ITEM_SPEAR_HIT = ModSounds.register("item.spear_hit");
    public static final SoundEvent BLOCK_CHAIN = ModSounds.register("block.chain");
    public static final SoundEvent BLOCK_STONE_TAP = ModSounds.register("block.stone_tap");
    public static final SoundEvent BLOCK_GRIND = ModSounds.register("block.grind");
    public static final SoundEvent BLOCK_BOOM = ModSounds.register("block.boom");
    public static final SoundEvent BLOCK_METAL_DOOR_OPEN = ModSounds.register("block.metal_door_open");
    public static final SoundEvent BLOCK_METAL_DOOR_CLOSE = ModSounds.register("block.metal_door_close");
    public static final SoundEvent EVENT_HORN = ModSounds.register("event.horn");
    public static final SoundEvent CREATURE_SMALL_AMBIENT = ModSounds.register("creature.small_ambient");
    public static final SoundEvent CREATURE_SMALL_HURT = ModSounds.register("creature.small_hurt");
    public static final SoundEvent CREATURE_SMALL_DEATH = ModSounds.register("creature.small_death");
    public static final SoundEvent CREATURE_GLIDE_AMBIENT = ModSounds.register("creature.glide_ambient");
    public static final SoundEvent CREATURE_GLIDE_HURT = ModSounds.register("creature.glide_hurt");
    public static final SoundEvent CREATURE_GLIDE_DEATH = ModSounds.register("creature.glide_death");
    public static final SoundEvent CREATURE_WIND_HURT = ModSounds.register("creature.wind_hurt");
    public static final SoundEvent CREATURE_WIND_DEATH = ModSounds.register("creature.wind_death");
    public static final SoundEvent CREATURE_TITAN_AMBIENT = ModSounds.register("creature.titan_ambient");
    public static final SoundEvent CREATURE_TITAN_ROAR = ModSounds.register("creature.titan_roar");
    public static final SoundEvent CREATURE_TITAN_HURT = ModSounds.register("creature.titan_hurt");
    public static final SoundEvent CREATURE_TITAN_DEATH = ModSounds.register("creature.titan_death");
    public static final SoundEvent CREATURE_TITAN_CHARGE = ModSounds.register("creature.titan_charge");
    public static final SoundEvent CREATURE_TITAN_BLAST = ModSounds.register("creature.titan_blast");
    public static final SoundEvent WEATHER_HAIL = ModSounds.register("weather.hail");
    public static final SoundEvent WEATHER_PUDDLE_SPLASH = ModSounds.register("weather.puddle_splash");
    public static final SoundEvent BLOCK_RADAR_PING = ModSounds.register("block.radar_ping");
    public static final SoundEvent BLOCK_MAT_STONE_BREAK = ModSounds.register("block.mat.stone.break");
    public static final SoundEvent BLOCK_MAT_STONE_STEP = ModSounds.register("block.mat.stone.step");
    public static final SoundEvent BLOCK_MAT_STONE_PLACE = ModSounds.register("block.mat.stone.place");
    public static final SoundEvent BLOCK_MAT_CRYSTAL_BREAK = ModSounds.register("block.mat.crystal.break");
    public static final SoundEvent BLOCK_MAT_CRYSTAL_STEP = ModSounds.register("block.mat.crystal.step");
    public static final SoundEvent BLOCK_MAT_CRYSTAL_PLACE = ModSounds.register("block.mat.crystal.place");
    public static final SoundEvent BLOCK_MAT_METAL_BREAK = ModSounds.register("block.mat.metal.break");
    public static final SoundEvent BLOCK_MAT_METAL_STEP = ModSounds.register("block.mat.metal.step");
    public static final SoundEvent BLOCK_MAT_METAL_PLACE = ModSounds.register("block.mat.metal.place");
    public static final SoundEvent BLOCK_MAT_SOFT_BREAK = ModSounds.register("block.mat.soft.break");
    public static final SoundEvent BLOCK_MAT_SOFT_STEP = ModSounds.register("block.mat.soft.step");
    public static final SoundEvent BLOCK_MAT_SOFT_PLACE = ModSounds.register("block.mat.soft.place");
    public static final SoundEvent BLOCK_MAT_PLANT_BREAK = ModSounds.register("block.mat.plant.break");
    public static final SoundEvent BLOCK_MAT_PLANT_STEP = ModSounds.register("block.mat.plant.step");
    public static final SoundEvent BLOCK_MAT_PLANT_PLACE = ModSounds.register("block.mat.plant.place");
    public static final SoundEvent BLOCK_MAT_LOOSE_BREAK = ModSounds.register("block.mat.loose.break");
    public static final SoundEvent BLOCK_MAT_LOOSE_STEP = ModSounds.register("block.mat.loose.step");
    public static final SoundEvent BLOCK_MAT_LOOSE_PLACE = ModSounds.register("block.mat.loose.place");
    public static final SoundEvent BLOCK_MAT_GLASS_BREAK = ModSounds.register("block.mat.glass.break");
    public static final SoundEvent BLOCK_MAT_GLASS_STEP = ModSounds.register("block.mat.glass.step");
    public static final SoundEvent BLOCK_MAT_GLASS_PLACE = ModSounds.register("block.mat.glass.place");
    public static final SoundEvent CREATURE_WHALE_SONG = ModSounds.register("creature.whale_song");
    public static final SoundEvent CREATURE_WHALE_HURT = ModSounds.register("creature.whale_hurt");
    public static final SoundEvent CREATURE_WHALE_DEATH = ModSounds.register("creature.whale_death");
    public static final SoundEvent CREATURE_EEL_CRACKLE = ModSounds.register("creature.eel_crackle");
    public static final SoundEvent CREATURE_EEL_ZAP = ModSounds.register("creature.eel_zap");
    public static final SoundEvent CREATURE_EEL_HURT = ModSounds.register("creature.eel_hurt");
    public static final SoundEvent CREATURE_EEL_DEATH = ModSounds.register("creature.eel_death");
    public static final SoundEvent CREATURE_SKINK_CHIRP = ModSounds.register("creature.skink_chirp");
    public static final SoundEvent CREATURE_JELLY_CHIME = ModSounds.register("creature.jelly_chime");
    public static final SoundEvent CREATURE_JELLY_HURT = ModSounds.register("creature.jelly_hurt");
    public static final SoundEvent CREATURE_JELLY_DEATH = ModSounds.register("creature.jelly_death");
    public static final SoundEvent CREATURE_SPRITE_TRILL = ModSounds.register("creature.sprite_trill");
    public static final SoundEvent CREATURE_HERMIT_CLICK = ModSounds.register("creature.hermit_click");
    public static final SoundEvent CREATURE_HERMIT_HIDE = ModSounds.register("creature.hermit_hide");
    public static final SoundEvent CREATURE_THUNDERBIRD_CRY = ModSounds.register("creature.thunderbird_cry");
    public static final SoundEvent CREATURE_THUNDERBIRD_HURT = ModSounds.register("creature.thunderbird_hurt");
    public static final SoundEvent CREATURE_THUNDERBIRD_DEATH = ModSounds.register("creature.thunderbird_death");
    public static final SoundEvent ITEM_KITE_FLAP = ModSounds.register("item.kite_flap");
    public static final SoundEvent ITEM_CONCH = ModSounds.register("item.conch");
    public static final SoundEvent CREATURE_SERPENT_ROAR = ModSounds.register("creature.serpent_roar");
    public static final SoundEvent CREATURE_SERPENT_HURT = ModSounds.register("creature.serpent_hurt");
    public static final SoundEvent CREATURE_SERPENT_DEATH = ModSounds.register("creature.serpent_death");
    public static final SoundEvent CREATURE_GOLEM_CRACKLE = ModSounds.register("creature.golem_crackle");
    public static final SoundEvent CREATURE_GOLEM_HURT = ModSounds.register("creature.golem_hurt");
    public static final SoundEvent CREATURE_GOLEM_DEATH = ModSounds.register("creature.golem_death");
    public static final SoundEvent CREATURE_GOLEM_THROW = ModSounds.register("creature.golem_throw");
    public static final SoundEvent CREATURE_MOTH_FLUTTER = ModSounds.register("creature.moth_flutter");
    public static final SoundEvent CREATURE_STORMFLY_BUZZ = ModSounds.register("creature.stormfly_buzz");
    public static final SoundEvent ITEM_RADIO_ALERT = ModSounds.register("item.radio_alert");

    /** For APIs that take a sound holder (equip sounds, some mob sounds). */
    public static Holder<SoundEvent> holder(SoundEvent e) {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(e);
    }

    public static void init() {}
}
