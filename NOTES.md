# Stormreach development notes

A running log of what was built, why, and what is left. Patch notes for players are in CHANGELOG.md. Technical handoff notes are in CLAUDE.md.

## Stage 4.0: The Tempest Realm (in progress, started 2026-11-24)

### What was asked
- A rare boss that spawns at a special structure in the Stormreach biomes. Both the structure and the biome work with `/locate`.
- The boss appears only during a thunderstorm. The storm can be natural, or started by a hard-to-make item that takes a while to use.
- The boss drops one new item. You need several of them (4 or more) to activate a portal.
- The portal leads to a whole new dimension built around the mod.
- The whole mod should lean toward an Aether-like sky adventure without copying it: a dimension of floating islands with its own blocks, ores, tool tiers, mobs, mounts and dungeons.
- Opening the portal should be very elaborate, not a simple frame-and-light.
- Lots of new things, plus notes on everything (this file) and patch notes.

### Design: the road into the Tempest Realm
1. **Find a Tempest Spire.** It's a rare ruined tower of Tempest Bricks in Stormreach Coast and Thunderspire Highlands (`/locate structure stormreach:tempest_spire`). A Sovereign Altar sits on its top floor.
2. **Wake the Tempest Sovereign.** Either:
   - **Naturally:** during a thunderstorm the altar may stir (a random tick, configurable chance) when a player is near. The awakening takes about 20 seconds.
   - **With a Tempest Sigil:** crafted from a Tempest Core (Stormwarden drop), a Thunderhead Horn, a Stormheart, 2 Blocks of Storm Crystal and 4 Storm Cells. Using it on the altar forces a thunderstorm and starts a 60-second ritual (configurable): lightning rings the tower, and then the boss descends.
3. **Defeat the Tempest Sovereign.** It's a three-phase flying boss: lightning and storm orbs, then Squalls and gust shockwaves, then enraged dives and a vortex pull. It drops exactly one **Tempest Eye**.
4. **Collect 4 Tempest Eyes** (configurable 4–16). That means beating the boss four times.
5. **Build a Stormgate.** It's a flat 5×5 ring of 16 Stormgate Frames (expensive to craft) around a 3×3 pool of stormwater sources. Each Eye goes into a frame.
6. **Call lightning onto the pool** (natural storm, Tempest Staff, Thunderhead Horn…). The stormwater turns into a Stormgate portal.
7. **Step in.** You arrive in the Tempest Realm, where a return gate is built for you if none is near. Coordinates are 1:1.

### Design: the Tempest Realm (Aether-like, storm themed)
- A dimension of floating islands (vanilla floating-island density) in a permanent dark-storm sky with sparks and lightning. Falling off the bottom drops you back into the Overworld sky with Slow Falling.
- Biomes: Galewood Highlands, Static Fields, Cloudsea Expanse, Aurora Spires.
- Blocks:
  - stone: Skyshale (+ cobbled, polished, bricks, stairs, slabs, walls)
  - soil: Skysoil and Static Grass
  - clouds: Nimbus Cloud (soft, no fall damage), Thundercloud (bounces you up), Gale Cloud (pushes you with the wind)
  - wood: the full Galewood set
  - ores: Voltite (iron-plus tier), Aurorite (diamond-plus tier), Ion (glowing healing shards)
  - other: Ion Lamp, Skybloom, Gale Grass
- Gear:
  - Voltite tools and armor
  - Aurorite tools and armor (a full set makes you lightning-proof and spares you fall damage)
  - Cloudstep Boots (sneak in mid-air to glide, no fall damage)
- Food: Cloudberries (from Galewood leaves), Cloudberry Tart, Skyray fillet, Storm Nectar, Ion Shards (instant healing).
- Mobs:
  - Skyray: a passive flying mount you can saddle and ride; you steer by looking
  - Puffling: a cloud sheep; shear it for Nimbus Clouds
  - Static Wisp: hostile, zaps you
  - Cyclone: hostile whirlwind that flings you
  - Stormhawk: hostile diving bird that drops Stormhawk Feathers
- Structure: **Sky Vaults**, floating ruins high in the realm sky with loot (Aurorite, Voltite, Ion, Tempest gear).

### Log
- Researched the 26.3 data formats from the vanilla jar:
  - dimension_type uses an `attributes` map, `default_clock` and `timelines`
  - noise_settings `default_block` is a plain id, and surface rules moved to `worldgen/material_rule` (`material_rule` references one)
  - level stems are `data/<ns>/dimension/<name>.json`
  - `TeleportTransition(ServerLevel, Vec3 pos, Vec3 velocity, yRot, xRot, PostTeleportTransition)`
  - `entityInside(state, level, pos, entity, InsideBlockEffectApplier, boolean)`
  - riding hooks: `tickRidden`, `getRiddenInput`, `getRiddenSpeed`, `getControllingPassenger`
- **Code written (compiles against the real jar, main and client):**
  - `realm/RealmBlocks`: 47 blocks. They cover the Tempest Spire set, the Sovereign Altar, the Stormgate Frame and portal, the Skyshale family, Skysoil, Static Grass, the 3 clouds, the Galewood family, the Voltite/Aurorite/Ion ores and blocks, the Ion Lamp, and the Skybloom, Stormcap and Gale Grass plants.
  - `realm/RealmItems`:
    - the Tempest Sigil and Tempest Eye
    - materials, and the Voltite and Aurorite tool and armor tiers
    - Cloudstep Boots
    - food: Cloudberries, Tart, Skyray, Storm Nectar, Gale Bread
    - Ion Shards (instant healing)
    - 6 spawn eggs
    - its own creative tab
  - `realm/SovereignAltarBlock`: a natural awakening needs a thunderstorm, a player within 32 blocks and a random-tick chance. It starts at stage 10, so it takes about 5 steps. The Sigil forces a storm in the Overworld and starts at stage 1, so it runs 15 steps across `sigilRitualSeconds`. Each step strikes visual lightning around the spire; at the end the boss spawns 5 blocks above the altar.
  - `realm/Stormgate`:
    - ring check: a 5×5 ring of 16 frames, a 3×3 stormwater-source pool, and at least `eyesRequired` Eyes
    - gate status messages
    - opening on a lightning strike (hooked from `StormWeather.onLightning`)
    - building the return gate
  - `realm/TempestRealm`:
    - Portal travel is queued and done at the end of the level tick, because the portal is detected inside entity movement.
    - Coordinates are 1:1. It finds an open gate within 20 blocks or builds one. Gates in the void go at y=120.
    - Falling below y−8 puts you back at the top of the Overworld with Slow Falling.
    - The realm's lightning and spark ambience also live here.
  - `realm/RealmGear`: Cloudstep glide and no fall damage. Full Aurorite gives no fall damage and lightning-proofing (added to `StormWeather.isLightningProof`). Full Voltite gives Speed and Haste in the realm.
  - Entities:
    - TempestSovereign: 3 phases, a home leash, and saves its phase and home
    - Skyray: saddle with a vanilla saddle (needs the `minecraft:can_equip_saddle` entity tag) and steer by looking
    - Puffling: shear it; it regrows in 3000–6000 ticks
    - StaticWisp, Cyclone, Stormhawk
  - Client: new SovereignModel (128×64), SkyrayModel and PufflingModel (with its own render state that hides the fluff once sheared). StaticWisp, Cyclone and Stormhawk reuse the Orb, Waterspout and Gull models with new textures.
  - Config: 11 new options on a sixth Mod Menu page called "Tempest Realm".
- **Assets and data written** by `tools/textures4.py` and `tools/gendata7.py`, both added to `run_all`:
  - all block, item, entity, armor and egg textures (our own pixel art; the tool icons are recoloured from our own Stormshard art)
  - an animated, translucent Stormgate texture
  - loot:
    - The Sovereign drops exactly 1 Tempest Eye, and Looting doesn't add more.
    - Galewood leaves drop Cloudberries the way oak drops apples.
    - Spire and Sky Vault chests have their own loot.
  - about 60 recipes:
    - The Sigil needs a Tempest Core, so the Stormwarden is a prerequisite.
    - Frames are crafted 2 at a time from Fulgurite, Charged Stormstone and Storm Crystal.
  - tags:
    - vanilla wood, stone, tool and armor tags; `can_equip_saddle`; `dirt` and `grass_blocks`, so plants and saplings grow on realm soil
    - `cannot_replace_below_tree_trunk`, so trees don't turn Static Grass into dirt
    - `c:` ores, ingots and gems
  - the dimension: floating-island density with the overworld temperature and vegetation noise added, so `multi_noise` can pick between the 4 biomes; and our own material rule
  - features:
    - Galewood trees (birch and fancy-oak shapes)
    - ores that replace Skyshale
    - **clouds, made as ore features that replace air**
    - flower and grass patches
    - Ion spires (a `block_column`)
  - structures:
    - Tempest Spire: 11×11×34; 3 floors with chests, a ladder shaft and a roof altar. It spawns 1 per 48×48 chunks at most.
    - Sky Vault: a floating ruin at y 150–200 that doesn't attach to terrain.
  - 12 advancements and all the lang.
- **Checks:**
  - `validate.py` was updated to read the realm registries. All checks pass: 103 hand-made blocks, 187 items, 15 mobs.
  - A separate worldgen reference check (biome → placed → feature, dimension → type, settings and rule, structure → pool and biome tag, NBT palette → blockstates) passes.
  - Clean compile of main and client.

### What to test first in game (4.0)
1. A world loads with the new dimension and there's no datapack error.
2. `/locate structure stormreach:tempest_spire` and `/locate structure stormreach:sky_vault` (from inside the realm).
3. Sigil on the altar: the storm starts, then the ritual, then the boss spawns. Then check the boss phases, and that it drops 1 Eye.
4. Build the gate, use `/summon lightning_bolt` on the pool, and check that it opens. Travel both ways, check the return gate is built, and check that falling out of the realm works.
5. Saddle and ride a Skyray. Shear a Puffling. Check the clouds' effects and the Cloudstep glide.
6. Check renderers: the Sovereign, Skyray and Puffling models are new and have never been seen in game.

### Ideas for later stages
- Realm dungeon tiers with their own bosses (for example a Thunder Citadel boss) and keys, like the Aether's dungeon chain.
- A double-jump accessory slot (needs a client-to-server packet).
- Hoe-tilling Skysoil into farmland (needs Fabric's tillable registry stubbed).
- Realm weather that actually rains (custom sky and weather rendering).


---

## 5.0 dev log (the Hollow Storm update)
Built from the user's "complete mod expansion" spec. The user also asked for rarity tables in the doc, allowed any testing method, and asked for Mod Menu to keep only performance settings.

### Design and code (new packages)
- `sim/`: pure Java, no Minecraft classes, unit tested.
  - EFScale, TornadoMorphology, TornadoFormation, VortexStructure, TornadoRelation, TornadoStage
  - WindField: modified Rankine vortex with inflow, updraft, subvortices and translation; calibrated so the ground-relative peak equals the target gust
  - TornadoProfile: climatology, morphology weights per EF, multiple-vortex chance, lifetimes
  - StructuralIntegrity, DebrisBudget, BackpackTier, MazeLayout, HidingRules, BossPlan, DialogueScript, ShadowRules
- `tornado/`:
  - TornadoEntity: lifecycle, movement, sampled block damage within budget, debris as falling blocks, entity drag/lift/damage, damage survey, roar
  - TornadoManager: natural genesis, satellites and companions, warnings
  - WeatherEvents: downburst, hail, supercell, ball lightning
  - Instruments: wind, pressure with tornado deficit, temperature, humidity, dew point, outlook, forecast
  - InstrumentBlock, WeatherItems: spawners and event items
- `backpack/`:
  - Backpacks, BackpackItem, BackpackStorage, BackpackMenu (ChestMenu with paging; locked slot; swap blocked), BackpackEnchants
  - open-backpack payload and B key
  - mixins: InventoryMixin (Soulbound), ItemEntityMixin (Resilience)
- `labyrinth/`:
  - LabyrinthStructure and Piece: deterministic rebuild per piece from the seed
  - Locker, Keyhole, HidingTracker
  - HollowKeeper: confined to its maze; hunts by sight and sound; searches lockers
  - LabyrinthTracker: applies the effect, spawns and despawns the Keeper
- `forest/`: Elderstorm wood set and TreeGrower (trees, mega trees, flower trees, fallback), Stormfruit pod, Glimmerstag, Thistlehop.
- `story/`:
  - Stranger and StrangerDirector: peripheral appearances, vanishing, signs, sighting stat
  - ShrineBlocks: pedestals, conduits hooked into StormWeather.onLightning, altar
  - Cinematics
  - CyrrusBoss: scripted intro with rise, grow, corrupt, music and fight actions; five themed phases; telegraphed attacks
  - RewardItems, StoryRegistry
- `TabOrganizer`: categorised creative tabs. The Mod Menu screen was reduced to two performance pages.
- `test/StormreachGameTests` (fabric-gametest entrypoint) plus JUnit in build.gradle, a `runGametest` Loom run, and CI steps.

### Assets and data
- `tools/textures5.py`: all 5.0 textures (procedural PIL), including the tornado funnel, humanoid and forest mob skins, equipment layers and advancement backgrounds.
- `tools/audio5.py`:
  - 15 voice lines: Piper TTS, then an ffmpeg corruption chain whose strength rises line by line
  - 3 synced Labyrinth music layers (32 s, 90 bpm, same key so they cross-fade cleanly) and a 64 s boss theme
  - siren, roar, heartbeat, a noise-vocoded whisper and a screech
  - **Intelligibility check:** the speech-envelope correlation between each processed line and its raw TTS take is 0.75–0.93.
- `tools/gendata8.py`:
  - models, blockstates, item definitions, equipment, sounds.json
  - cinematic scripts, **timed from the measured line lengths**
  - lang, loot, recipes (transmute for backpack upgrades), tags, 5 enchantments
  - worldgen: Elderstorm Grove biome added to the realm's multi-noise list; trees with an attached_to_leaves Stormfruit decorator; labyrinth and shrine structures and sets; the shrine NBT
  - 49 advancements (code-awarded ones use a single impossible "trigger")
- `tools/guide_tables.py` and `tools/make_guide.py`: GUIDE.md with rarity and drop-rate tables computed from the real files.

### Fixes along the way
- TreeGrower argument order (trees, mega trees, flower trees, fallback): checked with javap. The giant tree is the 2×2 mega tree.
- 26.3 data format: loot entries and pools use a singular `condition` object; `placed_block` uses `match_block`; damage tags are written `#minecraft:is_lightning`.
- The boss phases were rethemed to the spec (lightning / rain / wind / summons / all), and the gale and hail attacks got telegraphs.
- The Cyrrus `rise` action became a smooth emergence.
- The siren description was corrected (it's redstone-driven).
- Removed a redundant ternary in tornado genesis.

### Checks run here
- Clean compile of main and client against the real 26.3 jar.
- `tools/validate.py`: 0 problems. It now also checks the 5.0 registries, sounds, scripts, advancement parents, rewards and uniqueness, template-pool NBT, and placed-feature/biome references.
- 36 unit tests pass (local JUnit shim; the same sources run under real JUnit via Gradle).

### What to test first in game (5.0)
1. `./gradlew runGametest`: the datapack loads (no format waves) and all 7 GameTests pass.
2. Weather Events tab: spawn each tornado shape at a few EF levels. Watch the funnel renderer, debris, the survey message and performance.
3. Backpacks: open with B, paging, sort, upgrade a tier (contents kept), Soulbound on death, Resilience in lava.
4. `/locate structure stormreach:hollow_labyrinth`: the maze generates across chunk borders. Check the keys, keyhole, Keeper pathing, lockers and music.
5. `/locate biome stormreach:elderstorm_grove` (in the realm): trees, fruit, mobs.
6. Shrine ritual end to end, then the Cyrrus intro (sync, scaling, multiplayer), the phases and the drops.

### 5.0 first real runs (GitHub Actions GameTest server + the user's client)
The first runs were the user's client with a hand-built jar, then the CI GameTest server. Fixed in order:
1. The hand-built jar used Mixin annotation stubs with the wrong retention, so the invoker mixin failed. Only the sandbox stubs were affected; Gradle builds were fine.
2. "Some intrusive holders were not registered": a storm block clone that failed halfway left a half-built Block behind. StormBlocks now skips names Stormreach already uses (lantern) and releases any leftover holders.
3. **No storm mobs had ever been generated.** In 26.x `EntityType.getBaseClass()` always returns Entity.class. The classes now come from `stormreach_entity_classes.txt` (made by `tools/entity_classes.py` from the jar): 90 storm mobs.
4. Storm block items all failed with "Components not bound yet", because they read the vanilla stack size at init. They now use a fixed stack size: 944 items.
5. Datapack waves:
   - realm biomes needed `"modifier": "overlay"` on `natural_mob_spawns`
   - `tool/can_shear_blocks` is now `tool/can_shear`
   - `block_state_property` is now `match_block`
   - crafting categories `tools` and `food` are invalid
   - stale storm-block files had to be removed (amethyst clusters and buds, potent sulfur and lantern can't be cloned)
6. The GameTest server has no Tempest Realm level, so the test now checks the dimension type instead.

Result: CI is green. The build, 36 JUnit tests and all 8 required GameTests pass. validate.py now also checks loot types, predicates, recipe categories and biome attribute modifiers against vanilla 26.3.

### 5.1 overhaul, stage 1 (code-only; Minecraft not launched)
The user asked for the overhaul to be tested with builds, compiles, unit tests and validators only, without launching Minecraft, a client or a server from this session. CI still runs the headless GameTest server on GitHub.
- Altar: `SovereignAltarBlock.ritualStrike` steps the strike radius inward until it finds ground; `StormWeather.surfaceOr` never returns null, and `strike`/`strikeVisual` reject a null position with a clear message.
- Fireless lightning: `FirelessLightning` marks the staff's bolts. `LightningBoltMixin` cancels `spawnFire` for them and restores the fire ticks of the entities they hit (snapshot before the tick, restore after).
- Dispel: `WeatherEvents.dispel` dissipates tornadoes (`TornadoEntity.dissipate` speeds up the rope-out), drops pending events, discards nearby fronts, Squalls and ball lightning, and calls `setWeatherParameters`. Its sound is still a vanilla placeholder until the sound stage.
- Locker:
  - server: `HidingTracker` holds the player, refuses a second hider, and sends `LockerNet.HidePayload`; `ExitPayload` is the Use-to-exit request
  - client: `LockerClient` tracks the state, blocks attack and sends exit on Use; `KeyboardInputMixin` zeroes input; `CameraMixin` places the camera behind the grate with a ±70°/±35° view window
  - the model is an open box with a two-sided cutout grate; the collision box is empty for the hider
  - the purple-black effect icons: `mob_effect/hidden.png` and `labyrinth.png` were missing
- `RealmPassItem`: Creative-only travel via `TempestRealm.queueTravel`.
- `run_all.py` now refuses to start without the vanilla files, and puts the sounds back even if a script fails (a failed run had deleted them; restored from git).
- New GameTests: altar over the void, fireless staff bolt, dispel, locker hold and one-hider rule.
- Not runtime-tested: the client camera and input mixins and the grate rendering (the CI server never runs client code).

### 5.1 stage 2: tornado rebuild
- `sim/FunnelShape`: reach over life (descends by 14% of lifetime, lifts after 95.5%), touchdown gate, tip-tapered radius profile, flare into the cloud base. Unit-tested.
- `sim/VortexForces`: drag toward the wind (mass from the hitbox, ground friction, bracing), lift above a mass-dependent threshold, a carry ceiling with outward fling, wind damage per second, debris carry and eject vectors, block-check scaling. Unit-tested.
- `TornadoEntity`: touchdown gating; debris tracked as flights (carry, then thrown); debris impacts; waterspout steering, landfall and `findWater`; `skipTo`/`simulateWithoutPlayers` test hooks.
- Client: `TornadoMesh` builds every layer into a `Sink` (no Minecraft classes), and `TornadoRenderer` submits it with `submitCustomGeometry`. The spin is accumulated per tick in `TornadoEntity.clientSpin`. Textures come from `tools/textures6.py`.
- `tools/preview/TornadoPreview.java` (run with `/home/claude/tools/preview_tornado.sh`) rasterises the same meshes offline. Its lighting is approximate, so it is a shape check, not a screenshot.
- GameTests: no damage before touchdown; an EF5 tears up a plank floor and lifts a zombie; a waterspout stays over a pool, and none can be made on land.

### 5.1 stage 3: precipitation and clouds
- `sim/HailSize` (terminal speed, mass, impact energy to damage, size distribution by severity), `sim/Rainfall` (wetness from intensity and duration, puddle sizes, evaporation), `sim/Outflow` (gust front), `sim/CloudGrowth`. Unit-tested.
- `weather/precip/`: `HailstoneEntity` (cap of 600 alive), `HailLayerBlock` (scheduled-tick melt: 6–9 s per layer, 11–15 s with big stones; a random-tick safety net), `PuddleBlock`, `RainAccumulation`, `PrecipContent`.
- `weather/cloud/`: `StormCellEntity` and `StormCells` (up to 3 per player in thunder, 1 in rain).
- `WeatherEvents.Downburst` rewritten for wet and dry outflow; `HailBurst` drops real stones; natural spawning lives in `StormWeather.tick`.
- Client: `WeatherMesh` (hailstone, storm cloud; no Minecraft classes, so it can be previewed), `HailstoneRenderer`, `StormCellRenderer`.
- Config: `puddles`, `stormClouds` (properties file).
- GameTests: hail smashes a pane, hurts a zombie, piles up with a melt timer; puddles form, cap and vanish without ground; a downburst pushes a zombie out; a storm cloud grows and collapses when dispelled.

### 5.1 stages 4–6 (code-only; Minecraft not launched)
- Stage 4, backpack slot: the worn pack lives in `EquipmentSlot.BODY`, so vanilla saves it, syncs it to trackers and drops it on death.
  - `BackpackEquipment` holds the `WornSlot` and `WornContainer`; `BackpackEquipment.WORN = -2` marks a menu opened from the worn pack.
  - `InventoryMenuMixin` adds the slot at (77, 44) and handles shift-click.
  - Client: `InventoryScreenBackpackMixin` draws the slot frame, `CreativeInventoryBackpackMixin` (`@ModifyArgs`, `require = 0`) hides it in the creative tab, and `AvatarRendererMixin`, `AvatarRenderStateMixin`, `BackpackLayer` and `BackpackModel` draw the pack (textures from textures6.py, one per tier).
- Stage 5, weather screens: `tornado/WeatherUi` (Report, the payloads, `measure`, `stillAt`) and the client `weather/WeatherScreen` (tabs per device, a radar scope with reflectivity and velocity). Instruments and handhelds open the screen; sneak + use gives the old chat reading.
- Stage 6, Doppler radar: the `tornado/radar` package.
  - `RadarLayout` is the 3x3x7 footprint, with the controller at (0, 4, 0).
  - `RadarPartBlock` uses PART/OX/OY/OZ to find its controller; parts without one remove themselves through updateShape.
  - `DopplerRadarBlock` is the `tornado_radar` id and has a block entity; `DopplerRadarItem` places the structure.
  - Client: `DopplerRadarRenderer` with `RadarMesh`, and `RadarPlacementOutline`, which draws dust particles and follows the `radarPlacementOutline` config.
- Preview tool modes: `cloud,…`, `hail,N,…`, `radar,yawDeg,label`.
- Remaining from the overhaul spec:
  - the maze, vault door and keys, guardian, treasure room
  - the temple and boss arena
  - original sounds and maze music
  - the guidebooks
  - the full 64x art pass for everything else
  - particles

### 5.1 stages 7–10 (code-only; Minecraft not launched)
- Stage 7, Labyrinth:
  - `LabyrinthStructure` places the maze at minY + 9 with no stair piece.
  - `LabyrinthPiece` is one block taller and deeper, for the vault shell. It handles treasureShell, the door with built-in `KeyholeBlock` locks (KIND 0–2, FILLED), `vaultFor` (one key per corner vault), `treasureRoom`, and `HollowLampBlock` ceiling lamps.
  - `VaultDoor` checks the locks and sinks the door a row at a time (ticked from `LabyrinthTracker`). The new blocks are VAULT_DOOR, VAULT_WALL and HOLLOW_LAMP, plus KEYS[3]; the old LABYRINTH_KEY is kept but no lock takes it.
  - Not runtime-tested: actual world generation of the new maze (only the door and lamps have GameTests).
- Stage 8, guidebooks:
  - `guide/Guidebooks` handles the items and the give-once tag `stormreach.guidebooks_given`.
  - `tools/guidebooks.py` builds `assets/stormreach/guide/{guide,crafting}.json` from GUIDE.md and the recipes.
  - The client `guide/GuidebookScreen` renders it. Gson stubs were extended for compiling.
- Stage 9, sounds:
  - `tools/sfx_list.py` holds 41 ids and the vanilla sounds each replaces; `tools/sfx51.py` synthesises them and writes the Labyrinth score "Hollow Halls" (three 96 s layers). Run it by hand, like audio5.py.
  - `registry/ModSfx` is generated from sfx_list (the Holder wrapper is for equip sounds). Every `SoundEvents.*` use in the code was replaced by a codemod.
- Stage 10, particles: `particle/ModParticles` (via FabricParticleTypes.simple) and the client `particle/StormParticle` (via ParticleProviderRegistry, PendingParticleProvider and FabricSpriteSet; names confirmed from the CI Fabric jar). Textures and definitions come from textures6.py.
- GameTests in their own environment: `stormreach:isolated` (data/stormreach/test_environment) for tests that change world-wide state. Tornado tests must stay inside the test area's ticking chunks.
- Remaining from the overhaul spec:
  - Hollow Keeper (guardian) improvements
  - the temple redesign and optional boss arena
  - the full 64x art pass for the remaining 4.0/5.0 blocks, items, mobs and armour, possibly with Meshy for 3D references
  - the GameTests still run on a headless server in GitHub Actions (the user's "no server" constraint applies to this session, not CI; flag in the final report)

### 5.1 stages 11–12 and status
- Stage 11: the Tempest Spire was redesigned in gendata7.py (25x25x52 temple, spiral stair, crown altar). `realm/SovereignArena` is the optional sky arena (sovereignArena config) with safe teleport out and back, a fall-off catch and return on rejoin, ticked from ServerLevelMixin.
- Stage 12: `tools/hd64.py` runs last in run_all and upscales 16x block textures to 64x with a bevel and grain pass. It skips anything already larger, so real 64x art drawn later replaces it.
- Not done from the overhaul spec:
  - Hollow Keeper behaviour improvements
  - redrawn 64x items, mobs and armour (items are still 16x/32x)
  - Meshy-made 3D models (not used)
  - block step and break sounds (still vanilla SoundTypes)
- Verified by CI only (headless server GameTests, 28 of them); never run in a client:
  - all rendering: tornado and cloud meshes, radar dish, backpack layer, screens, particles, locker camera
  - generation of the new maze and temple in a real world

### 5.1.0 finished (stages 13–14)
- Stage 13:
  - `sim/KeeperSenses` (noise radii, hearing, guess error, listen rate, lunge), used by the HollowKeeper HuntGoal. The Keeper also enrages below 50% health and dims lamps within 4 blocks.
  - `registry/ModSoundTypes` holds seven material families of original block sounds (`block.mat.*` in sfx_list), applied by `BlockStateSoundMixin` to blocks in the stormreach namespace only.
- Stage 14: `tools/hd64.py` now covers items (to 64x) and entity/armour textures (4x small sheets, 2x medium). It skips the procedural textures.
- Final: GUIDE.md regenerated, 55 unit tests, 30 GameTests, validator clean.
- Still never run in a real client.
- Not done: Meshy models (the Meshy CLI needs the user to sign in in a browser).

### 5.1.0 stages 15–16 (after the user asked for real textures and models)
- Stage 15: `tools/remaster64.py` replaces hd64.py (deleted).
  - Blocks are repainted by material (see `block_painter`), with colours taken from each 16x original's palette.
  - Pictures (doors, trapdoors, non-square textures) and items get an EPX 4x remaster with volume shading, rims and outlines.
  - Mob textures from mobmodels.py are skipped. Other entity and armour sheets get the EPX pass.
- Stage 16: `tools/mobmodels.py` describes 17 models as parts and boxes. It packs UVs, paints the texture at 4x, and writes `client/model/gen/*Model.java` with the animation.
  - The old `client/model/*.java` and the forest Stag/Hop models were deleted.
  - Thunder Crab, Stormhawk, Stormwarden, Static Wisp and Cyclone now have their own models.
  - The Hollow Keeper's renderer is now a MobRenderer with its own model, not a zombie HumanoidModel.
  - Skyray and Puffling use a `State` with `saddled` / `sheared`.
  - Renderer scales were re-fitted to the hitboxes.
  - `tools/preview/mob_preview.py` draws an offline contact sheet of the textured models, as a source check only.
- run_all order: ... guidebooks, mobmodels, remaster64, fixorder.

### 5.1.0 stage 17 (the user asked for better models, real animation, and the Stranger as a weathered player)
- Animation runtime (client/anim):
  - `RigState` holds attack/hurt ages, smoothed air, speed and turn, a per-entity seed, and the saddled/sheared flags.
  - `RigRenderer` fills it, smoothing per entity in a WeakHashMap.
  - `Keys` samples Catmull-Rom tracks: loop for cycles, once for one-shots.
  - `Rig` provides the easing, fidget and pulse helpers.
- Attack sync: `anim/AnimPulse` broadcasts entity events 113 (attack) and 114 (cast).
  - `MobMixin` fires it on every doHurtTarget, and the Stormwarden, Sovereign, Cyclone and Wisp fire it at their custom attacks.
  - `LivingEntityMixin.handleEntityEvent` records it on the client, for stormreach mobs only.
- `tools/mobmodels.py` rewrite:
  - `Clip`s (time, dist, fidget, attack, cast, hurt) are made of tracks per part channel (degrees, units, scale delta), blended by moveW/idleW/air/ground/swim/dry, plus look and an `extra` Java block.
  - Every part gets a field.
  - Grounded models are auto-placed so their lowest point sits at y=24.
  - The painter quantizes a value field onto a 7-tone ramp with Bayer dithering and bevels.
- `tools/preview/mob_preview.py` modes:
  - `sheet`: the rest-pose sheet.
  - `anim`: per-clip frame strips, evaluated exactly like the Java. The Java Keys output was checked against the Python sampler, with a max difference of 5e-5.
  - `gif`: an animated GIF of one clip.
- Stranger:
  - `client/story/WeatheredSkins` reads the viewer's skin from the DynamicTexture or a resource, and `weather(int[] argb, ...)` is pure.
  - It was checked offline on the vanilla default skins.
  - `WeatheredModel` extends PlayerModel and adds the slump, breath, head drift and snap.
  - `StrangerRenderer` is now a LivingEntityRenderer that swaps between the wide and slim models.
  - Client-side drip particles were added in `Stranger.tick`.
- Renderers moved to RigRenderer. The Puffling and Skyray `State` classes are replaced by RigState.
