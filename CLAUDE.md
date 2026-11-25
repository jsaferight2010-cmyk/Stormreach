# Stormreach: Claude Code handoff

Fabric content mod for **Minecraft Java 26.3** (Java 25, Mojang names, no remapping). Theme: a storm-swept coast where everything connects. Storms grow crystals, crystals make gear, gear calls storms, and the Stormblight spreads storm versions of things. Version **2.1.0**. Patch notes are in `CHANGELOG.md` and the player guide is in `README.md`.

This project was built in a claude.ai cloud session that couldn't reach Fabric's or Mojang's servers. Everything was compile-checked against the real 26.3 jar using stubs, but it has **never been built with Gradle or run in game**. On this PC you can do both, so start there.

## First steps on this PC
1. Install JDK 25 (Temurin), then run `gradlew.bat build`. The jar goes to `build/libs/`. Fix whatever the first real build reports; the toolchain versions are in `gradle.properties`.
2. `gradlew.bat runClient` launches a dev client to test in game. Check the log for mixin `APPLY` failures and datapack errors at world creation.
3. To regenerate resources (textures and JSON are produced by scripts, so don't hand-edit generated files; change the script instead):
   - `pip install pillow nbtlib`
   - Extract the vanilla 26.3 jar's `assets/` and `data/` into `.vanilla/`. Also copy the jar itself to `.vanilla/minecraft-26.3.jar`, or set the `STORMREACH_VANILLA` and `STORMREACH_MCJAR` env vars.
   - `python tools/run_all.py`, then `python tools/validate.py` and `python tools/featorder.py`. Both must pass.

## Layout
- `src/main/java/com/stormreach/`
  - `Stormreach.java`: init, creative tab, commands, lightning damage immunity
  - `StormreachConfig.java`: `config/stormreach.properties`
  - `registry/`: ModBlocks, ModItems, ModEntities, ModEffects (effects and potions), ModBiomes
  - `block/`, `item/`, `entity/`: the content classes
  - `weather/StormWeather.java`: Gale and Thunderhead, wind, hail, spawns, `onLightning`
  - `blight/Stormblight.java`: the vanilla→storm block and mob conversion maps
  - `mixin/`: ServerLevel tick, LightningBolt tick, OverworldBiomeBuilder (biome placement and splitting) plus its invoker, LivingEntity tick and Mob doHurtTarget (storm mob perks)
  - `storm/`: 3.0 generated counterparts. `StormBlocks` clones every vanilla block listed in `stormreach_storm_blocks.txt` by reflection: it copies the Properties and fills the other constructor arguments from uniquely-typed fields, and skips the block on any failure. `StormMobs` makes a `storm_<id>` EntityType for each vanilla Mob class. `StormPerks` holds the storm mob powers and fluid effects.
  - `realm/` (4.0): RealmBlocks, RealmItems (own creative tab), RealmEntities, SovereignAltarBlock (natural and Sigil rituals), Stormgate (gate check, lightning ignition, return-gate builder), StormgateFrameBlock and StormgatePortalBlock, TempestRealm (dimension key, queued portal travel, falling back to the Overworld, realm lightning), RealmGear (Cloudstep, Aurorite and Voltite set bonuses), CloudBlock, SkyGrassBlock, VoltiteSwordItem and TooltipItem. Boss: `entity/TempestSovereign`. Realm mobs: `entity/Skyray` (saddle and fly), `Puffling`, `StaticWisp`, `Cyclone`, `Stormhawk`.
  - `fluid/` and `registry/ModFluids`: Stormwater (extends WaterFluid) and Stormlava (extends LavaFluid). `StormlavaBlock` has the water-contact hardening.
- `src/client/java/com/stormreach/client/`: renderers, models, the Mod Menu config screen (5 pages), the ModMenu entrypoint. `StormClientContent` sets up the storm block tint (BlockColorRegistry, 2 constant layers), copies vanilla renderers for storm mobs through `EntityRenderersAccessor`, and registers the fluid models. `LivingEntityRendererMixin` tints storm mobs. Client mixins are in `stormreach.client.mixins.json`.
- `tools/`: Python generators for all assets and data, plus the validators. `textures4.py` and `gendata7.py` hold all the 4.0 content: textures, models, loot, recipes, tags, the dimension, noise settings, material rule, 4 realm biomes, features, the Tempest Spire and Sky Vault NBT structures, advancements and lang. `gendata6.py` writes every generated storm block and mob file: the block list, flattened models with `tintindex 0` that reuse vanilla textures, item defs with constant tints, loot, `required:false` tag entries, lang, eggs, the fluids, and the Storm Wastes and Storm Void biomes. Re-run it after any Minecraft update.
- `.github/workflows/build.yml`: GitHub Actions build that uploads the jar as an artifact

## 26.3 facts that were verified against the jar (don't "fix" these back)
- `ResourceLocation` is now `net.minecraft.resources.Identifier`.
- Entity type constants live in `EntityTypes` (for example `EntityTypes.LIGHTNING_BOLT`), not `EntityType`.
- `Entity.hurtMarked` is now `needsSync`.
- To change screens use `minecraft.gui.setScreen(...)`.
- Fabric API packages: `fabric-creative-tab-api-v1` (`FabricCreativeModeTab`), `loot.v3.LootTableEvents`, `entity.event.v1.ServerLivingEntityEvents`. `SpawnPlacements.register` and `CreativeModeTab$Output` are widened by Fabric API.
- Data formats:
  - Loot uses `modifier` lists and single `condition`.
  - Block states are written `{"id", "properties"}` (also in NBT palettes, DataVersion 5023).
  - A `simple_block` `to_place` is the state itself.
  - Biomes use an `attributes` map (`minecraft:visual/sky_color`, `minecraft:gameplay/natural_mob_spawns`, ...).
  - Brewing recipes are data files (`recipe/brewing/*.json`).
- Dimensions: `dimension_type` uses an `attributes` map plus `default_clock` and `timelines`. Level stems go in `data/<ns>/dimension/`. noise_settings `default_block` and `default_fluid` are plain ids, and surface rules live in `worldgen/material_rule` (noise_settings names one with `material_rule`). Ore targets and block providers use plain states (`"state": "id"`, `"provider": {"id": ...}`).
- Advancement entity predicates use namespaced keys: `minecraft:location`, `minecraft:vehicle`, `minecraft:entity_type`.
- `TeleportTransition(ServerLevel, Vec3, Vec3, yRot, xRot, PostTeleportTransition)` and `Entity.teleport(...)`. `entityInside(..., InsideBlockEffectApplier, boolean)`. `mobInteract` is public.
- The chunk render layer is picked from texture alpha. Don't register cutout or translucent layers in code. Glass uses `force_translucent` in the model.
- Biome feature lists must agree with vanilla's global order, or world creation crashes. `tools/fixorder.py` sorts them, so always run it after editing biomes.

## User preferences
- Wants patch notes (CHANGELOG.md) with every version, plus a running dev log of everything done (NOTES.md).
- Wants the mod to lean toward an Aether-like sky adventure (without copying it), with elaborate progression such as the multi-step Stormgate.
- Tests builds in game themselves and reports bugs back.
- Since 5.0: Mod Menu holds **performance settings only**; gameplay options live in the properties file.
- Wants rarity / drop-rate tables in the player docs (GUIDE.md is generated by tools/make_guide.py; rerun it after changing loot or worldgen).
- Everything in the mod should be connected to the storm theme.
- Prefers working features over many unfinished ones, delivered in stages.

## Status
- 5.1.0 (the Overhaul) is code-complete: 14 stages, all compiled against the real 26.3 jar, `tools/validate.py` passes, 55 unit tests and 30 GameTests pass in CI. Nothing client-side has been run in a real client yet (renderers, screens, particles, locker camera, backpack layer). NOTES.md has the per-stage dev log. Audio is regenerated with `tools/sfx51.py` (effects and the Labyrinth score, listed in `tools/sfx_list.py`, Java constants in `ModSfx`). `tools/hd64.py` runs last in run_all and gives every block/item texture its 64x detail pass (mob and armour textures 4x); draw real 64x art and it is left alone.
- 5.0.0 (Hollow Storm) is code-complete: main and client compile against the real 26.3 jar, `tools/validate.py` passes, 36 unit tests pass. **Not run in game.** `./gradlew runGametest` (also in CI) is the first real check of the datapack and the new systems. Highest risk:
  - datapack format waves at load (new: enchantments, block_state_provider, attached_to_leaves decorator, advancement criteria, structure type `stormreach:labyrinth`)
  - the client-only code (tornado funnel renderer, backpack screen, adaptive music, humanoid/forest renderers), which a server never exercises
  - Cyrrus's fight balance and the Hollow Keeper's pathing in the maze
- Audio: `tools/audio5.py` (Piper TTS + numpy) makes all .ogg files; `run_all.py` keeps the sounds folder. Changing a voice line means rerunning audio5.py, then run_all.py (cinematic timing is derived from the measured line lengths in tools/voice_lines.json).
- 4.0.0 (Tempest Realm) is code-complete. It compiles and passes the validators, but it has not been run in game yet. See NOTES.md for the full design, a log of the work, and a test checklist.
- 3.0.0 is code-complete. Main and client compile against the real 26.3 jar, and `tools/validate.py` passes. It has **not been run in game yet**, so the first in-game test is the real check. Things most likely to need fixes:
  - storm blocks that the reflection clone skips, or that behave oddly (the log line "generated N storm blocks (M skipped)" shows how many; turn on debug logging to see why)
  - storm mobs whose vanilla renderer misbehaves
  - the client mixins, which a dedicated-server boot doesn't exercise

## Ideas not started
- Storm versions of modded blocks and mobs (this would need runtime-generated assets).
- Hand-made boss extras beyond the shared lightning-and-Squalls perk.
