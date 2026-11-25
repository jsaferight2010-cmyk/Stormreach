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

    /** For APIs that take a sound holder (equip sounds, some mob sounds). */
    public static Holder<SoundEvent> holder(SoundEvent e) {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(e);
    }

    public static void init() {}
}
