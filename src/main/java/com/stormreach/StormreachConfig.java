package com.stormreach;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * All Stormreach settings. Saved as config/stormreach.properties (plain text, easy to edit by hand)
 * and editable in game through Mod Menu.
 */
public final class StormreachConfig {
    public enum BiomeFrequency { RARE, NORMAL, COMMON }
    /** OFF: nothing spreads. CONTAINED: only an active Stormheart converts, up to its radius. UNLIMITED: Stormscar keeps spreading on its own forever. */
    public enum SpreadMode { OFF, CONTAINED, UNLIMITED }

    // World
    public static boolean biomeEnabled = true;
    public static BiomeFrequency biomeFrequency = BiomeFrequency.NORMAL;
    public static boolean oreGeneration = true;

    // Weather
    public static boolean stormWeather = true;
    public static boolean stormsOnAllCoasts = false;
    public static int lightningFrequency = 40;      // 0-100
    public static boolean lightningHarmful = true;
    public static boolean wind = true;
    public static int windStrength = 50;            // 0-100
    public static boolean stormParticles = true;
    public static boolean stormMessages = true;

    // Mobs
    public static boolean crabSpawning = true;
    // 5.2 Stormwild
    public static boolean wildlifeSpawning = true;
    public static int thunderbirdChance = 6;
    public static int hailGolemChance = 5;          // 5.3: percent per minute per player out in a thunderstorm, 0-100
    public static boolean gullSpawning = true;
    public static boolean squallSpawning = true;
    public static int maxSquallsPerPlayer = 4;      // 0-12

    // Items
    public static int tempestStaffCooldown = 3;     // seconds
    public static int tempestStaffRange = 32;       // blocks
    public static int stormbladeLightningChance = 15; // percent
    public static int hornStormMinutes = 5;


    // 2.0
    public static boolean bluffsEnabled = true;
    public static boolean highlandsEnabled = true;
    public static boolean marshEnabled = true;
    public static boolean fulguriteFromLightning = true;
    public static boolean hail = true;
    public static boolean ballLightning = true;
    public static boolean waterspouts = true;
    public static boolean sealSpawning = true;
    public static boolean wraithSpawning = true;
    public static boolean crabConversion = true;
    public static boolean bossSummoning = true;
    public static boolean armorSetBonus = true;
    public static boolean galeWingsBoost = true;
    public static boolean stormfinFishing = true;

    // 2.1 Stormblight
    public static SpreadMode spreadMode = SpreadMode.CONTAINED;
    public static int heartMaxRadius = 32;     // 8-64
    public static int spreadSpeed = 5;         // 1-10
    public static boolean convertMobs = true;
    public static boolean blightBiome = true;
    public static boolean heartActivatesOnPlace = false;
    /** 6.0: a fully grown, awake Stormheart on a power network keeps spreading past heartMaxRadius while it is fed. */
    public static boolean heartPoweredSpread = true;
    public static int heartPowerDraw = 400;          // SU a second while overcharged (50-10000)
    public static int heartPoweredMaxRadius = 0;     // 0 = no limit (then only the world border stops it)
    public static boolean stormFront = true;
    public static int stormFrontMinutes = 4;   // 1-15

    // 3.0
    public static boolean useGeneratedBlocks = true;
    public static boolean useGeneratedMobs = true;
    public static boolean stormMobPerks = true;
    public static boolean stormwaterShocks = true;
    public static boolean stormInOtherDimensions = true;
    // 4.0: Tempest Sovereign, Stormgate and the Tempest Realm
    public static boolean sovereignNaturalSpawns = true;
    public static int sovereignSpawnChance = 20;
    public static int sigilRitualSeconds = 60;
    public static int eyesRequired = 4;
    public static int sovereignHealth = 400;
    public static boolean realmFallToOverworld = true;
    public static boolean realmLightning = true;
    public static int skyraySpeed = 15;
    public static boolean realmMobSpawning = true;
    public static boolean cloudBlocks = true;
    public static boolean returnGateBuilt = true;
    // 5.0: tornadoes, Stranger, Labyrinth, Cyrrus (performance options are the ones shown in Mod Menu)
    public static boolean tornadoDestruction = true;
    public static int tornadoSimulationDistance = 256;
    public static int maxActiveTornadoes = 2;
    public static int tornadoBlockChecksPerTick = 96;
    public static int tornadoDebrisLimit = 80;
    public static int tornadoSizePercent = 35;
    /** 6.0: forward speed of tornadoes as a share of real-world speeds (real ones felt far too fast at block scale). */
    public static int tornadoSpeedPercent = 40;
    /** 6.0: tornadoes flatten and destroy crops, and break trees down by intensity. */
    public static boolean tornadoCropDamage = true, tornadoTreeDamage = true;
    /** 6.0: rare fire whirls in the Nether. */
    public static boolean fireWhirls = true;
    public static int particleDensity = 100;
    public static int backpackMagnetInterval = 10;
    public static boolean naturalTornadoes = true;
    public static int tornadoFrequency = 100;
    public static int tornadoIntensityBias = 100;
    public static int tornadoDurationPercent = 50;
    public static boolean landspouts = true;
    /** Creative/Spectator only: draw tornadoes nearly transparent, with the core marked, so you can see inside. */
    public static boolean creativeSeeThroughTornadoes = false;
    /** Rain puddles collect during long or heavy rain and dry up afterwards. */
    public static boolean puddles = true;
    /** Storm clouds you can watch build, tower and spread during thunderstorms. */
    public static boolean stormClouds = true;
    /** Outline the Doppler radar's footprint while you hold one. */
    public static boolean radarPlacementOutline = true;
    /** 6.0: display units for every reading: imperial (mph, F, in, inHg, ft/mi; the default) or metric. */
    public static com.stormreach.sim.Units.System units = com.stormreach.sim.Units.System.IMPERIAL;
    /** The Tempest Sovereign is fought on a floating arena above the spire (players are lifted up and brought back). */
    public static boolean sovereignArena = true;
    public static boolean downbursts = true;
    public static int shadowAppearanceRate = 100;
    public static boolean shadowSigns = true;
    public static boolean labyrinthGeneration = true;
    public static int bossDifficulty = 100;
    public static int guardianDifficulty = 100;

    private StormreachConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("stormreach.properties");
    }

    public static void load() {
        Path path = file();
        if (!Files.exists(path)) { save(); return; }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            Stormreach.LOGGER.warn("Could not read {}, using defaults", path, e);
            return;
        }
        biomeEnabled = bool(p, "biomeEnabled", biomeEnabled);
        biomeFrequency = freq(p.getProperty("biomeFrequency"), biomeFrequency);
        oreGeneration = bool(p, "oreGeneration", oreGeneration);
        stormWeather = bool(p, "stormWeather", stormWeather);
        stormsOnAllCoasts = bool(p, "stormsOnAllCoasts", stormsOnAllCoasts);
        lightningFrequency = num(p, "lightningFrequency", lightningFrequency, 0, 100);
        lightningHarmful = bool(p, "lightningHarmful", lightningHarmful);
        wind = bool(p, "wind", wind);
        windStrength = num(p, "windStrength", windStrength, 0, 100);
        stormParticles = bool(p, "stormParticles", stormParticles);
        stormMessages = bool(p, "stormMessages", stormMessages);
        crabSpawning = bool(p, "crabSpawning", crabSpawning);
        gullSpawning = bool(p, "gullSpawning", gullSpawning);
        squallSpawning = bool(p, "squallSpawning", squallSpawning);
        maxSquallsPerPlayer = num(p, "maxSquallsPerPlayer", maxSquallsPerPlayer, 0, 12);
        wildlifeSpawning = bool(p, "wildlifeSpawning", wildlifeSpawning);
        thunderbirdChance = num(p, "thunderbirdChance", thunderbirdChance, 0, 100);
        hailGolemChance = num(p, "hailGolemChance", hailGolemChance, 0, 100);
        tempestStaffCooldown = num(p, "tempestStaffCooldown", tempestStaffCooldown, 0, 60);
        tempestStaffRange = num(p, "tempestStaffRange", tempestStaffRange, 8, 64);
        stormbladeLightningChance = num(p, "stormbladeLightningChance", stormbladeLightningChance, 0, 100);
        hornStormMinutes = num(p, "hornStormMinutes", hornStormMinutes, 1, 30);
        String sm = p.getProperty("spreadMode");
        if (sm != null) try { spreadMode = SpreadMode.valueOf(sm.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException ignored) {}
        heartMaxRadius = num(p, "heartMaxRadius", heartMaxRadius, 8, 64);
        spreadSpeed = num(p, "spreadSpeed", spreadSpeed, 1, 10);
        convertMobs = bool(p, "convertMobs", convertMobs);
        blightBiome = bool(p, "blightBiome", blightBiome);
        heartPoweredSpread = bool(p, "heartPoweredSpread", heartPoweredSpread);
        heartPowerDraw = num(p, "heartPowerDraw", heartPowerDraw, 50, 10000);
        heartPoweredMaxRadius = num(p, "heartPoweredMaxRadius", heartPoweredMaxRadius, 0, 30000000);
        heartActivatesOnPlace = bool(p, "heartActivatesOnPlace", heartActivatesOnPlace);
        stormFront = bool(p, "stormFront", stormFront);
        stormFrontMinutes = num(p, "stormFrontMinutes", stormFrontMinutes, 1, 15);
        useGeneratedBlocks = bool(p, "useGeneratedBlocks", useGeneratedBlocks);
        useGeneratedMobs = bool(p, "useGeneratedMobs", useGeneratedMobs);
        stormMobPerks = bool(p, "stormMobPerks", stormMobPerks);
        stormwaterShocks = bool(p, "stormwaterShocks", stormwaterShocks);
        stormInOtherDimensions = bool(p, "stormInOtherDimensions", stormInOtherDimensions);
        sovereignNaturalSpawns = bool(p, "sovereignNaturalSpawns", sovereignNaturalSpawns);
        sovereignSpawnChance = num(p, "sovereignSpawnChance", sovereignSpawnChance, 1, 100);
        sigilRitualSeconds = num(p, "sigilRitualSeconds", sigilRitualSeconds, 10, 180);
        eyesRequired = num(p, "eyesRequired", eyesRequired, 4, 16);
        sovereignHealth = num(p, "sovereignHealth", sovereignHealth, 100, 2000);
        realmFallToOverworld = bool(p, "realmFallToOverworld", realmFallToOverworld);
        realmLightning = bool(p, "realmLightning", realmLightning);
        skyraySpeed = num(p, "skyraySpeed", skyraySpeed, 5, 40);
        realmMobSpawning = bool(p, "realmMobSpawning", realmMobSpawning);
        cloudBlocks = bool(p, "cloudBlocks", cloudBlocks);
        returnGateBuilt = bool(p, "returnGateBuilt", returnGateBuilt);
        tornadoDestruction = bool(p, "tornadoDestruction", tornadoDestruction);
        tornadoSimulationDistance = num(p, "tornadoSimulationDistance", tornadoSimulationDistance, 64, 512);
        maxActiveTornadoes = num(p, "maxActiveTornadoes", maxActiveTornadoes, 0, 8);
        tornadoBlockChecksPerTick = num(p, "tornadoBlockChecksPerTick", tornadoBlockChecksPerTick, 0, 512);
        tornadoDebrisLimit = num(p, "tornadoDebrisLimit", tornadoDebrisLimit, 0, 200);
        tornadoSizePercent = num(p, "tornadoSizePercent", tornadoSizePercent, 10, 100);
        tornadoSpeedPercent = num(p, "tornadoSpeedPercent", tornadoSpeedPercent, 10, 100);
        tornadoCropDamage = bool(p, "tornadoCropDamage", tornadoCropDamage);
        tornadoTreeDamage = bool(p, "tornadoTreeDamage", tornadoTreeDamage);
        fireWhirls = bool(p, "fireWhirls", fireWhirls);
        particleDensity = num(p, "particleDensity", particleDensity, 0, 100);
        backpackMagnetInterval = num(p, "backpackMagnetInterval", backpackMagnetInterval, 5, 40);
        naturalTornadoes = bool(p, "naturalTornadoes", naturalTornadoes);
        tornadoFrequency = num(p, "tornadoFrequency", tornadoFrequency, 0, 300);
        tornadoIntensityBias = num(p, "tornadoIntensityBias", tornadoIntensityBias, 50, 300);
        tornadoDurationPercent = num(p, "tornadoDurationPercent", tornadoDurationPercent, 10, 100);
        landspouts = bool(p, "landspouts", landspouts);
        creativeSeeThroughTornadoes = bool(p, "creativeSeeThroughTornadoes", creativeSeeThroughTornadoes);
        puddles = bool(p, "puddles", puddles);
        stormClouds = bool(p, "stormClouds", stormClouds);
        radarPlacementOutline = bool(p, "radarPlacementOutline", radarPlacementOutline);
        if (p.getProperty("units") != null) units = com.stormreach.sim.Units.System.parse(p.getProperty("units"));
        sovereignArena = bool(p, "sovereignArena", sovereignArena);
        downbursts = bool(p, "downbursts", downbursts);
        shadowAppearanceRate = num(p, "shadowAppearanceRate", shadowAppearanceRate, 0, 300);
        shadowSigns = bool(p, "shadowSigns", shadowSigns);
        labyrinthGeneration = bool(p, "labyrinthGeneration", labyrinthGeneration);
        bossDifficulty = num(p, "bossDifficulty", bossDifficulty, 50, 300);
        guardianDifficulty = num(p, "guardianDifficulty", guardianDifficulty, 50, 300);
        bluffsEnabled = bool(p, "bluffsEnabled", bluffsEnabled);
        highlandsEnabled = bool(p, "highlandsEnabled", highlandsEnabled);
        marshEnabled = bool(p, "marshEnabled", marshEnabled);
        fulguriteFromLightning = bool(p, "fulguriteFromLightning", fulguriteFromLightning);
        hail = bool(p, "hail", hail);
        ballLightning = bool(p, "ballLightning", ballLightning);
        waterspouts = bool(p, "waterspouts", waterspouts);
        sealSpawning = bool(p, "sealSpawning", sealSpawning);
        wraithSpawning = bool(p, "wraithSpawning", wraithSpawning);
        crabConversion = bool(p, "crabConversion", crabConversion);
        bossSummoning = bool(p, "bossSummoning", bossSummoning);
        armorSetBonus = bool(p, "armorSetBonus", armorSetBonus);
        galeWingsBoost = bool(p, "galeWingsBoost", galeWingsBoost);
        stormfinFishing = bool(p, "stormfinFishing", stormfinFishing);
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("biomeEnabled", String.valueOf(biomeEnabled));
        p.setProperty("biomeFrequency", biomeFrequency.name());
        p.setProperty("oreGeneration", String.valueOf(oreGeneration));
        p.setProperty("stormWeather", String.valueOf(stormWeather));
        p.setProperty("stormsOnAllCoasts", String.valueOf(stormsOnAllCoasts));
        p.setProperty("lightningFrequency", String.valueOf(lightningFrequency));
        p.setProperty("lightningHarmful", String.valueOf(lightningHarmful));
        p.setProperty("wind", String.valueOf(wind));
        p.setProperty("windStrength", String.valueOf(windStrength));
        p.setProperty("stormParticles", String.valueOf(stormParticles));
        p.setProperty("stormMessages", String.valueOf(stormMessages));
        p.setProperty("crabSpawning", String.valueOf(crabSpawning));
        p.setProperty("gullSpawning", String.valueOf(gullSpawning));
        p.setProperty("squallSpawning", String.valueOf(squallSpawning));
        p.setProperty("maxSquallsPerPlayer", String.valueOf(maxSquallsPerPlayer));
        p.setProperty("wildlifeSpawning", String.valueOf(wildlifeSpawning));
        p.setProperty("thunderbirdChance", String.valueOf(thunderbirdChance));
        p.setProperty("hailGolemChance", String.valueOf(hailGolemChance));
        p.setProperty("tempestStaffCooldown", String.valueOf(tempestStaffCooldown));
        p.setProperty("tempestStaffRange", String.valueOf(tempestStaffRange));
        p.setProperty("stormbladeLightningChance", String.valueOf(stormbladeLightningChance));
        p.setProperty("hornStormMinutes", String.valueOf(hornStormMinutes));
        p.setProperty("spreadMode", spreadMode.name());
        p.setProperty("heartMaxRadius", String.valueOf(heartMaxRadius));
        p.setProperty("spreadSpeed", String.valueOf(spreadSpeed));
        p.setProperty("convertMobs", String.valueOf(convertMobs));
        p.setProperty("blightBiome", String.valueOf(blightBiome));
        p.setProperty("heartPoweredSpread", String.valueOf(heartPoweredSpread));
        p.setProperty("heartPowerDraw", String.valueOf(heartPowerDraw));
        p.setProperty("heartPoweredMaxRadius", String.valueOf(heartPoweredMaxRadius));
        p.setProperty("heartActivatesOnPlace", String.valueOf(heartActivatesOnPlace));
        p.setProperty("stormFront", String.valueOf(stormFront));
        p.setProperty("stormFrontMinutes", String.valueOf(stormFrontMinutes));
        p.setProperty("useGeneratedBlocks", String.valueOf(useGeneratedBlocks));
        p.setProperty("useGeneratedMobs", String.valueOf(useGeneratedMobs));
        p.setProperty("stormMobPerks", String.valueOf(stormMobPerks));
        p.setProperty("stormwaterShocks", String.valueOf(stormwaterShocks));
        p.setProperty("stormInOtherDimensions", String.valueOf(stormInOtherDimensions));
        p.setProperty("sovereignNaturalSpawns", String.valueOf(sovereignNaturalSpawns));
        p.setProperty("sovereignSpawnChance", String.valueOf(sovereignSpawnChance));
        p.setProperty("sigilRitualSeconds", String.valueOf(sigilRitualSeconds));
        p.setProperty("eyesRequired", String.valueOf(eyesRequired));
        p.setProperty("sovereignHealth", String.valueOf(sovereignHealth));
        p.setProperty("realmFallToOverworld", String.valueOf(realmFallToOverworld));
        p.setProperty("realmLightning", String.valueOf(realmLightning));
        p.setProperty("skyraySpeed", String.valueOf(skyraySpeed));
        p.setProperty("realmMobSpawning", String.valueOf(realmMobSpawning));
        p.setProperty("cloudBlocks", String.valueOf(cloudBlocks));
        p.setProperty("returnGateBuilt", String.valueOf(returnGateBuilt));
        p.setProperty("tornadoDestruction", String.valueOf(tornadoDestruction));
        p.setProperty("tornadoSimulationDistance", String.valueOf(tornadoSimulationDistance));
        p.setProperty("maxActiveTornadoes", String.valueOf(maxActiveTornadoes));
        p.setProperty("tornadoBlockChecksPerTick", String.valueOf(tornadoBlockChecksPerTick));
        p.setProperty("tornadoDebrisLimit", String.valueOf(tornadoDebrisLimit));
        p.setProperty("tornadoSizePercent", String.valueOf(tornadoSizePercent));
        p.setProperty("tornadoSpeedPercent", String.valueOf(tornadoSpeedPercent));
        p.setProperty("tornadoCropDamage", String.valueOf(tornadoCropDamage));
        p.setProperty("tornadoTreeDamage", String.valueOf(tornadoTreeDamage));
        p.setProperty("fireWhirls", String.valueOf(fireWhirls));
        p.setProperty("particleDensity", String.valueOf(particleDensity));
        p.setProperty("backpackMagnetInterval", String.valueOf(backpackMagnetInterval));
        p.setProperty("naturalTornadoes", String.valueOf(naturalTornadoes));
        p.setProperty("tornadoFrequency", String.valueOf(tornadoFrequency));
        p.setProperty("tornadoIntensityBias", String.valueOf(tornadoIntensityBias));
        p.setProperty("tornadoDurationPercent", String.valueOf(tornadoDurationPercent));
        p.setProperty("landspouts", String.valueOf(landspouts));
        p.setProperty("creativeSeeThroughTornadoes", String.valueOf(creativeSeeThroughTornadoes));
        p.setProperty("puddles", String.valueOf(puddles));
        p.setProperty("stormClouds", String.valueOf(stormClouds));
        p.setProperty("radarPlacementOutline", String.valueOf(radarPlacementOutline));
        p.setProperty("units", units.name().toLowerCase());
        p.setProperty("sovereignArena", String.valueOf(sovereignArena));
        p.setProperty("downbursts", String.valueOf(downbursts));
        p.setProperty("shadowAppearanceRate", String.valueOf(shadowAppearanceRate));
        p.setProperty("shadowSigns", String.valueOf(shadowSigns));
        p.setProperty("labyrinthGeneration", String.valueOf(labyrinthGeneration));
        p.setProperty("bossDifficulty", String.valueOf(bossDifficulty));
        p.setProperty("guardianDifficulty", String.valueOf(guardianDifficulty));
        p.setProperty("bluffsEnabled", String.valueOf(bluffsEnabled));
        p.setProperty("highlandsEnabled", String.valueOf(highlandsEnabled));
        p.setProperty("marshEnabled", String.valueOf(marshEnabled));
        p.setProperty("fulguriteFromLightning", String.valueOf(fulguriteFromLightning));
        p.setProperty("hail", String.valueOf(hail));
        p.setProperty("ballLightning", String.valueOf(ballLightning));
        p.setProperty("waterspouts", String.valueOf(waterspouts));
        p.setProperty("sealSpawning", String.valueOf(sealSpawning));
        p.setProperty("wraithSpawning", String.valueOf(wraithSpawning));
        p.setProperty("crabConversion", String.valueOf(crabConversion));
        p.setProperty("bossSummoning", String.valueOf(bossSummoning));
        p.setProperty("armorSetBonus", String.valueOf(armorSetBonus));
        p.setProperty("galeWingsBoost", String.valueOf(galeWingsBoost));
        p.setProperty("stormfinFishing", String.valueOf(stormfinFishing));
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                p.store(w, "Stormreach settings. Also editable in game through Mod Menu.");
            }
        } catch (IOException e) {
            Stormreach.LOGGER.warn("Could not save {}", path, e);
        }
    }

    public static void resetToDefaults() {
        biomeEnabled = true; biomeFrequency = BiomeFrequency.NORMAL; oreGeneration = true;
        stormWeather = true; stormsOnAllCoasts = false; lightningFrequency = 40; lightningHarmful = true;
        wind = true; windStrength = 50; stormParticles = true; stormMessages = true;
        crabSpawning = true; gullSpawning = true; squallSpawning = true; maxSquallsPerPlayer = 4;
        wildlifeSpawning = true; thunderbirdChance = 6; hailGolemChance = 5;
        tempestStaffCooldown = 3; tempestStaffRange = 32; stormbladeLightningChance = 15; hornStormMinutes = 5;
        spreadMode = SpreadMode.CONTAINED; heartMaxRadius = 32; spreadSpeed = 5; convertMobs = true; blightBiome = true;
        heartActivatesOnPlace = false; heartPoweredSpread = true; heartPowerDraw = 400; heartPoweredMaxRadius = 0; stormFront = true; stormFrontMinutes = 4;
        useGeneratedBlocks = true; useGeneratedMobs = true; stormMobPerks = true; stormwaterShocks = true; stormInOtherDimensions = true;
        sovereignNaturalSpawns = true; sovereignSpawnChance = 20; sigilRitualSeconds = 60; eyesRequired = 4; sovereignHealth = 400; realmFallToOverworld = true; realmLightning = true; skyraySpeed = 15; realmMobSpawning = true; cloudBlocks = true; returnGateBuilt = true;
        tornadoDestruction = true; tornadoSimulationDistance = 256; maxActiveTornadoes = 2; tornadoBlockChecksPerTick = 96; tornadoDebrisLimit = 80; tornadoSizePercent = 35; particleDensity = 100; backpackMagnetInterval = 10; naturalTornadoes = true; tornadoFrequency = 100; tornadoIntensityBias = 100; tornadoDurationPercent = 50; landspouts = true; downbursts = true; shadowAppearanceRate = 100; shadowSigns = true; labyrinthGeneration = true; bossDifficulty = 100; guardianDifficulty = 100;
        bluffsEnabled = true; highlandsEnabled = true; marshEnabled = true; fulguriteFromLightning = true; hail = true; ballLightning = true; waterspouts = true; sealSpawning = true; wraithSpawning = true; crabConversion = true; bossSummoning = true; armorSetBonus = true; galeWingsBoost = true; stormfinFishing = true;
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int num(Properties p, String key, int def, int min, int max) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(v.trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static BiomeFrequency freq(String v, BiomeFrequency def) {
        if (v == null) return def;
        try { return BiomeFrequency.valueOf(v.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { return def; }
    }
}
