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
- Mirror skins (the user then asked for the follower to adapt to the player's own in-game skin):
  - `Stranger` and `CyrrusBoss` sync a MIRROR UUID string. The Stranger gets it from `watch(p)`, and Cyrrus from `mirror(player)` at the Hollow Altar. Cyrrus saves it as "Mirror".
  - Client `MirrorSkins.of(uuid)` looks in the tab list, then the loaded player, then the default skin for that UUID.
  - `WeatheredSkins.get(skin, stage)` returns {texture, glow}:
    - Stage 0 is the Stranger.
    - Stages 1 and 2 are Cyrrus's forms: an indigo shift, branching vein walkers, and burning eyes.
    - The glow texture is drawn by a `RenderTypes.eyes` layer, pulsing.
    - It was checked offline on the default skins (Steve, Alex, Efe) at all three stages.
  - `CyrrusModel` extends PlayerModel and layers poses by form over the walk and attack swings.

## 5.2.0 Stormwild (the user asked for a lot more content, then more meteorology and storm chasing)
- `wild/` (WildRegistry):
  - Mobs: CloudWhale, ShockEel (WaterAnimal), Sparkskink (CHARGED), StormJelly, GaleSprite (friend/gift), TidewrackHermit (HIDING), Thunderbird.
  - Items: TideConchItem and Stormkite, which glides from `LivingEntityMixin` tick on both sides.
  - Thunderbirds arrive from `ServerTickEvents.END_SERVER_TICK` in thunderstorms, controlled by `thunderbirdChance`.
- `chase/` (ChaseRegistry):
  - InterceptVehicle: a PathfinderMob vehicle with its own travel(). Its dashboard uses sendOverlayMessage. It anchors after 40 still ticks, and TornadoEntity.pushEntities skips anchored vehicles.
  - WeatherBalloon: a NoAi mob that rises and sends a sounding.
  - RainGaugeBlock: random ticks fill it from `Rainfall.intensity`.
  - TornadoProbeBlock with a block entity that records the peak wind from `windMphAt`. It has explosion resistance 1200, so BlockWind ignores it.
  - ChaserLog: CustomData on the logbook. The server tick checks every second, with a ClipContext line-of-sight check.
- Client:
  - `client/wild/WildRenderers`.
  - RigState gains `charged`, `anchored` and `hide`, plus a `RigRenderer.smooth()` helper.
  - mobmodels supports `translucent` models (the jelly and sprite), with new materials: spots, tread, grille, decal, jelly.
- Tools:
  - `textures7.py`: 32x item icons shaded on a ramp with outlines (remaster64 takes them to 64x), eggs, and 64x blocks. The lantern texture is recoloured from vanilla's.
  - `gendata9.py`: models, blockstates, lang, loot, recipes, spawns and advancements.
  - sfx51 `NEW52`: 19 synthesized sounds.
- Tests: 8 new GameTests: content loads, creatures live, skink charging, hermit hiding, balloon rising, kite gliding, gauge comparator, anchored TIV plus probe in an EF5.
- Not run in a client. The vehicle's driving feel, the dashboard, the models and the textures all need in-game checks.

## 5.3.0 Skyward (the user asked to "continue to add more, any and everything", then for a description on every item and a cleaner tooltip)
- `sky/SkyRegistry` registers everything; `SkyRegistry.init()` is called from `Stormreach.onInitialize` and runs `StormArrivals`, `WeatherRadio` and `ChaserGear` on END_SERVER_TICK.
- Creatures: `SquallSerpent` (boss bar, HuntGoal circle/lunge/ball-lightning, WeaveGoal, lashGust), `HailstoneGolem` (HurlHailGoal via `HailstoneEntity.drop`, melts out of storms, fire x2), `AuroraMoth` (seeks block light > 11, drops Aurora Dust), `Stormfly` (lifetime unless thundering, bottle it). Golems and Stormflies are spawned by `StormArrivals`, not natural spawning; serpents and moths use biome spawns plus placement predicates.
- Models in `tools/mobmodels.py` (SquallSerpentModel with 6 chained segments and turn-curl, HailstoneGolemModel, AuroraMothModel, StormflyModel). New painter materials: `dscale` (overlapping dragon scales), `hail` (packed round hailstones), `mothwing` (gradient sheer wing with veins, eyespot and fringe). Offline previews were checked; nothing has been seen in a real client.
- Weather Radio: the mod already had one (`tornado/WeatherItems`). My first pass registered a second `weather_radio` (it would have crashed at registration) and its generator overwrote the original's texture/recipe; fixed by deleting my item and turning `sky/WeatherRadio` into an add-on for the existing radio (emergency within 150, severe/rain/clear/Thunderbird alerts, alert tone). Regenerating restored the original assets byte-for-byte.
- Wind Turbine: POWER 0-15 from `Instruments.windMph` (about mph/4, open sky only), updated once a second with a per-position phase; the rotor is drawn by `client/sky/TurbineRenderer` (custom geometry through `HailstoneRenderer.sink`, now public), spin accumulated per block entity so speed changes don't jump. Rotor yaw = 180 - facing.toYRot.
- Stormchaser suit: `ChaserGear.massFactor` multiplies tornado mass (hooked in TornadoEntity), full set added to `StormWeather.isLightningProof`, goggles HUD via overlay message every 2 s.
- Tooltips: `client/tooltip/StormTooltips` (Fabric `ItemTooltipCallback`; a stub was added to the local libstubs.jar for compile checks). It inserts the wrapped `desc.stormreach.<id>` under the name, removes the item's static ".tooltip" lines and shows them as bullets while Shift is held; lines with arguments stay put. `tools/descriptions.py` holds all descriptions and adds Shift details (food effects, redstone, controls) for items that had none.
- Also fixed: BackpackLayer and the Cyrrus glow layer passed a colour where `submitModel`'s 7-arg overload expects the outline colour.
- Tests: GameTests `skywardContentLoaded`, `skywardCreaturesSpawnAndLive`, `windTurbinePowerFollowsTheWind`, `theStormchaserSuitBracesAndGrounds`.

## 5.4.0 Thunderfront (the user asked for "even more", connected to their PC read-only: never change files there)
- Read the user's Modrinth profile (read-only). It still runs Stormreach 5.0.0 alongside Sodium, Iris, JEI, Jade, EMF/ETF and Enchantment Descriptions. Last night's crash reports: two client "intrusive holders were not registered" crashes (the storm-block cloner leaking a Block; already fixed by `StormBlocks.dropLeakedHolders`) and one server NPE in `SovereignAltarBlock.tick` → `strikeVisual(null)` (already fixed: `ritualStrike` never returns null). The log showed Enchantment Descriptions warning that all 8 Stormreach enchantments lacked descriptions; added.
- `front/` package: Rainfrog (chorus via `FrontArrivals.rainSoon`, AnimPulse CAST plays the throat-sac croak clip), StormPetrel (PathfinderMob flyer with SkimGoal, leaves after ~1 min of calm), DustDevil (anchor() for the bottled version; sweeps ItemEntities, spins living things, scatters on a player hit/rain/water), TeslaCoil (charge 0-500 in the BE, CHARGE 0-5 blockstate for light/model, zap 6 lightning damage per 10 charge, redstone holds fire), WindChimes (scheduled tick; ringChance(gust, outdoors)), Umbrella, OmenFeather (reads WeatherData clear/rain/thunder timers), BottledDustDevil.
- Models: RainfrogModel (hop clip on walk distance, croak on cast), StormPetrelModel (bird() at k 0.75), DustDevilModel (7 hollow sand rings spun and swayed in `extra`).
- Tests: thunderfrontContentLoaded, thunderfrontCreaturesSpawnAndLive, aChargedTeslaCoilZapsAMonster, umbrellasFloatAndChimesFollowTheWind.

## 5.5.0 Live Wire (the user asked for electricity, captured water and different uses for water; then for all of it to work with JEI or its counterparts; then for the Stormheart in creative)
- Power: SU buffers on block entities (`power/NodeEntity`), networks found by BFS through Copper Cable each time a generator or capacitor shares (every 10-20 ticks; capped at 1024 cables). No external energy API, so no extra dependency.
- Water: `power/TankBlock` (Rain Barrel 4000 mB, catches rain; Cistern 32000 mB) with a single BE type; Stormwater flag survives only unmixed. Pump (infinite from 2+ neighbouring sources), Sprinkler (moisture, random ticks, fire).
- JEI: read the user's installed jei-26.3-fabric-31.17.0.108.jar (read-only) to compile against its API locally (class files patched to Java 21 in /tmp for javac). Gradle uses `mezz.jei:jei-26.3-fabric-api:${jei_version}` from maven.blamejared.com (blocked from the workspace, so CI is the check). Avoided two APIs JEI marks for removal (addFluidStack, addRecipeArrow). Fluid amounts are Fabric droplets (81 per mB).
- Stormheart: it already had an item but was buried in the Building tab; now first in Storm Blocks and listed under Weather Events.
- Tests: aCapacitorPowersALampDownACable, rainBarrelsDrainIntoCisternsAndKeepStormwaterApart, aPumpLiftsWaterIntoATank, aSprinklerWetsFarmland, aShockPlateZapsAMonster.

## 6.0.0 Overhaul (in progress)

Source: the 25-section overhaul request (crash, visuals, tornadoes, gameplay, structures, sound, perf) plus the
crash report from 2026-11-25 15:54. Order of work follows its priority list. No game launched by me; CI GameTests
boot the headless test server in GitHub Actions as before.

### Stage 1: worldgen crash (done)
- Cause: `stormreach:trees_thunderspire` (configured random_selector, used by placed `trees_thunderspire` in
  Thunderspire Highlands and by placed `trees_marsh` in Brinewater Marsh) picked `stormreach:fallen_driftwood` 5% of
  the time. That placed feature is the coastal decoration and carries `minecraft:biome`. Nested placement has no
  top-level feature, so `BiomeFilter.shouldPlace` threw. Only on the 5% branch, which is why it was intermittent.
- Fix: new `placed_feature/fallen_driftwood_checked` (survival filter only) for the selector; the coastal step keeps
  its biome filter. Generated in gendata3.py.
- `tools/worldgen_check.py` (called from validate.py): resolves every biome step, placed->configured link and nested
  placed reference (ids, inline, weighted entries, recursively), rejects biome filters on nested placed features and
  biome-filtered placed features that no biome uses. Self-tests against vanilla data. It flagged exactly the two
  crash paths above and nothing else.
- GameTest `thunderspireTreesPlaceWithoutABiomeCheckCrash` places the three tree selectors 200x each.

### Stage 2: visuals (models, armour, machines, radar data, HUD)
- Armour: `tools/armor60.py` paints each set piece by piece on the humanoid UV layout (helmet with eye slit and nose
  guard, cuirass that stops above the belt, pauldrons and bracers, boots on the lower leg only, belt/tassets/knee
  guards on the leggings layer) with transparency between pieces. The 5.x layers flooded every box with one noisy
  colour, which is why a full set read as a speckled blob. Preview: `python3 ~/scratch/armor_preview.py set...`.
- Machines are procedural meshes in `client/render/Machines.java` drawn by their BERs and by the placement hologram:
  NEXRAD-style Doppler radar (pad, shelter, lattice tower, railed platform, panelled radome), 16-block utility wind
  turbine (yaws into the synced wind), Indianapolis-style rotating siren on a wooden pole, 3-block weather-station
  mast (spinning cups, live vane, radiation shield, solar panel, rain gauge). `client/render/Mesh.java` is the
  shape kit. Tall machines use `structure/Multiblock` + invisible `machine_part` cells (placement refused when
  blocked, any part breaks the whole, parts self-remove without a controller). Icons in tools/art were rendered
  from the same meshes by an offline rasteriser (~/scratch/prev/MeshPreview.java).
- `client/render/PlacementHologram` (Fabric LevelRenderEvents.COLLECT_SUBMITS) replaces the particle outline:
  translucent cyan/red copy of the real model + red cells where blocked.
- Radar velocity was never real (faint discs + two dots). Now `tornado/RadarScan` builds a 90x40 polar scan of dBZ
  and radial velocity from cells, hail cores, downbursts (radial outflow), tornado wind fields and the ambient wind;
  `sim/RadarMath` holds the geometry and NWS colour scales (unit-tested); the client paints it into a texture
  (`client/weather/RadarImage`). Same scope for the Doppler radar, Portable Storm Radar and Weather Monitor.
- Units: `sim/Units` (imperial default, metric option, unit-tested), `units=` in stormreach.properties.
- HUD: `weather/WeatherHud` sends one packet a second (wind, temp, pressure, nearest tornado for goggles/TIV); the
  client draws `client/hud/StormHudPanel` top-left. Wind "from" was inverted in 5.x (`Instruments.windFrom`): fixed.
- Salt crystal: halite cube cluster (hopper faces), hitbox 12 px tall. Cistern: open tank, grate, real water surface
  and a sight glass on every side (levels 0-4, water or Stormwater). Umbrella and Stormkite: 3D held models (flat icon
  in GUI via a display_context select). Held transforms are computed, not seen: need an in-game look.
- TIV: new armoured model (skirts, glacis, slit-window cab, camera turret, light bar, mast), first-person cockpit
  (cab hidden for the driver), passenger seat lowered into the cab, hitbox 2.4x2.6. Anchor key (default Z) toggles the
  spikes immediately (TivControls payload); the 2 s auto-anchor is gone.

### Stage 3: weather visuals and hail
- Storm clouds: drawn cutout (opaque) so overlapping billows merge into one mass (5.x drew translucent ellipsoids,
  which sorted badly into the "shards" in the screenshot); flat dark base (billows clipped at the base), a core billow
  at every tower level, a neck joining the anvil; a translucent rain shaft scrolls from base to ground. Bases now
  start at y>=165 and 90+ above the terrain (`StormCells.cloudBase`).
- Hail: no entities. `weather/precip/HailSim` schedules impacts (landing point and tick from terminal speed + drift)
  and resolves them in a per-tick budget; the burst sends `HailSim.FieldPayload` once a second and
  `client/weather/HailClient` draws falling stones as particles (three size classes, bounce, melt). Server work per
  stone: one heightmap lookup and one tiny box query, nothing synced. Damage and breakage still scale with HailSize.
  HailstoneEntity stays for the Hailstone Golem's thrown stones.
- Hail on the ground: crust + loose stones (bigger lumps after golf-ball hail), 3 random layouts per state.
- Puddles: only creative players can target them (pick block gives the new Puddle item, they can clear them);
  survival clicks pass to the ground. They dry on a scheduled timer (slowly under rain, 15-35 s once dry) plus the
  old random-tick evaporation and a timer safety net.
- Downbursts: wet/dry now also follows the ground (soaked after long rain, water/mud/puddles under it, swamps); wet
  ones flood hollows and put out fires, dry ones hit 15% harder, raise a dust wall and dry out farmland.

### Stage 4: tornadoes
- Morphologies: each type has its own radius profile (`TornadoMorphology.radiusProfile`) and look in TornadoMesh:
  ropes snake and lean with the base trailing, drill bits get spiral grooves, cones bow outward, stovepipes are
  columns, wedges/large wedges are dense dark walls with a rain-wrap curtain and a grey (not brown) debris base; the
  drawn width is capped at ~1.9x the funnel height so the biggest wedge reads as a wall. Any touched-down funnel meets
  the ground with at least ~0.5 block width and a whirl >=2 blocks (ropes used to fade out above the ground).
  Preview: tools/preview/TornadoPreview.java (see ~/scratch/tp).
- Terrain: absolute cloud base (`cloudBaseAbs`, from the nearest storm cell or StormCells.cloudBase); the funnel
  stretches to it as the ground rises and falls. Ground following snaps up, eases down.
- Speed: `tornadoSpeedPercent` (default 40) scales forward speed.
- Entities: `VortexForces.captured/ride/rideTicks` (unit-tested). Caught bodies get their own orbit radius, climb rate
  and rhythm, circle, bob up and down, and are flung out after their ride; still server-side with needsSync.
- Debris: sustained-wind fatigue below the failure speed (stalled tornadoes keep eroding), exposed soil/turf lifted
  from 125 mph, foliage partly lifted, stone/masonry only above 200 mph. Lifting keeps the exact state; containers
  are emptied in place first, other block entities ride along in FallingBlockEntity.blockData. Wider orbits and
  surging climb. Default debris cap 80.
- Trees: `breakTree` by severity (EF0 canopy .. EF5 uproot), stripped stumps from EF3, removed trunk pieces laid
  down downwind (or lifted, or dropped as items, one for one); each tree processed once per severity.
- Crops: flattened (lose growth) from 65 mph, torn out from 90 mph, once per tornado (config tornadoCropDamage).
- Waterspout satellites no longer follow a parent ashore (landfall still dissipates them deliberately).
- Fire whirl (`FireWhirlEntity`, Nether only, rare natural spawn near lava/soul ground, config fireWhirls).

### Stage 5: gameplay
- Rusted Locker: doors repainted (solid lower door, louvres only in the upper half). Hiding now seats the player on an
  invisible `LockerSeat` entity instead of teleporting every tick; `LockerClient` + `CameraMixin` glide the camera in and
  out over 0.35 s and limit the look to ±85° yaw / ±50° pitch. Seats discard themselves when empty and on disconnect.
- Backpack: the worn slot draws the game's own `container/slot` sprite; in the Creative inventory tab it now has its own
  spot (127, 20) with a frame instead of being moved off-screen. B also opens the pack while the inventory is open
  (key mappings don't fire under a screen, so the key is polled there).
- Barometer and Wind Vane open the weather screen with their own first tab: an aneroid dial (pointer that settles, brass
  set hand, rising/falling/steady trend from the last two minutes, outlook words) and a vane on a damped spring with
  gust wobble. Physics step once a tick, not per frame.
- Units everywhere: the weather screen formats every reading through `ClientUnits`. On the server `units/PlayerUnits`
  keeps each player's setting (the client sends it on join and when it changes) and every chat/overlay reading
  (instruments, station, radar, tornado warning, damage survey, rain gauge, probe, logbook, TIV, sounding, radio,
  goggles, turbine) is formatted per recipient. The lang strings no longer carry units.
- Creative Wind Generator (`wind/`): creative-only fan block (no recipe, no drop, unbreakable in survival). Settings
  screen: 0-320 mph, reach 8-64, gustiness, on/off, block damage (also needs mobGriefing). `WindSources` is the field
  (`sim/FanField` cone shape, unit-tested); `Instruments.windMph` and the weather report's wind direction include it,
  so anemometers, vanes, the HUD and turbines all react. Pushes entities with a v² force capped at the air speed;
  anchored TIVs hold. Redstone switches it off. Client streak particles from the synced block entity.
- Stormheart overcharge (`blight/StormheartBlockEntity`): a grown, awake heart on a power network draws
  `heartPowerDraw` SU a second (storage first, then generators) and pushes a frontier ring (`sim/HeartFrontier`) out a
  block at a time, converting surface columns; every 8 blocks the ring's loaded chunks get the storm biome. Chunks
  that were unloaded when the ring passed are queued (up to 4096) and converted when they load. No limit except the
  world border or `heartPoweredMaxRadius`. Starved of power it stops where it is. Config: `heartPoweredSpread`,
  `heartPowerDraw`, `heartPoweredMaxRadius` (properties file, not Mod Menu).
- JEI rows for the overcharged heart and the generator; descriptions for generator, barometer, vane and the 6.0
  turbine numbers.
- Tests: GameplayStage5Test (fan field, frontier, units); GameTests windGeneratorBlowsAndInstrumentsFeelIt,
  poweredStormheartKeepsSpreading, readingsFollowEachPlayersUnits.

### Stage 6: mobs, gear, Dispel Weather, saplings
- Gale Cloud: one gust per entry, aimed by `sim/GaleLaunch` from the entity's velocity, its offset inside the cloud
  and its facing (side entries carry on through with lift, falls bounce up, jumps from below go higher; standing
  still pushes out the far side). It used to add the global wind vector, so everyone went the same way.
- Ball lightning: boss summons (`summonedBy`) live 8 s, fizzle when the boss dies, leaves or is 64+ blocks away,
  and are never saved. Wild orbs keep 30 s; `life` is saved now.
- Stormwarden: fights on the ground (walk + melee, rare lightning). Takes off only after a long ground phase (never in
  the first 15 s) or when the target is out of reach for 3 s; 7 s in the air (circle, lightning, small gusts), then
  lands in a knock-up shockwave and is staggered 3 s taking +40% damage. Swaps ground/air navigation and move control.
- Squall Serpent and Thunderbird: natural lightning heals 6 at most every 10 s; a player's lightning (Stormblade,
  staff, Channeling) now hurts them. It used to heal 10/20 per bolt. Thunderbird: no boss bar; hunt is circle 3-5 s,
  a lock-on stoop that leads the target and accelerates to 1.5 b/t, talon hit + knock, recover climb.
- Rainfrog: `HopMoveControl` (never walks on land: crouch 2 ticks, leap, pause); the hop animation is an attack-pulse
  clip fired per leap.
- Dust Devil: 20 health (each hit tears dust out; dies in a scatter, explosions still scatter it). Its egg is kept on
  purpose; egg-spawned devils aren't broken up by rain.
- Stranger egg: egg/command-spawned Strangers mirror the nearest player for a minute and only leave if you walk up to
  one (the natural one vanished on its first tick with no player to watch). Strangers are never saved.
- Shock Eel (the user's "Shock Ray"): its bite gave the victim Static Charge, a protective buff. Now `shock()` deals
  `stormreach:shock` damage, Slowness III and Mining Fatigue briefly, with a spark arc; being hit makes it discharge
  into everything in the same water within 4 blocks (or a close attacker on the bank), every 2 s at most. New sound
  `creature.eel_discharge`.
- Brine Wraith: own rig model (`BrineWraithModel` in mobmodels.py: hunched drowned sailor, navy coat, red knit cap,
  barnacles, seaweed, sea-green eyes, loose jaw; idle sway, dragging shamble, two-handed grab, brine-retch cast) and
  original sounds (`creature.brine_*`, synthesised voice with formants + bubbles). Behaviour: brine spit cone (3-8
  blocks, Hunger II, Slowness, 2 damage), undertow on hits in water, no gear, no zombie villagers, no reinforcements.
- Gear: Stormshard 1100/7.5/3.0, armour 3/6/7/3 t1.5; Voltite diamond tier 1700/8.5/3.5, armour 3/6/8/3 t2;
  Aurorite netherite tier 2400/10/4.5, armour 3/7/8/3 t3.5 kb0.1; Maelstrom 3200/11/5.5; Gale material 1800/9/4;
  Stormcrown t4. Storm Surge (`RealmGear.stormSurge`): Stormreach weapons/tools in rain +8% damage/+15% mining, in a
  thunderstorm +15%/+30% (transient attribute modifiers). Swords use the vanilla sword tool (they mine in Survival;
  vanilla blocks every sword from breaking blocks in Creative); asked the user whether to change that.
- Lightning Staff: its bolts already never ignite (FirelessLightning, no Fire Resistance); now holding the staff also
  stops any lightning setting the holder alight.
- Dispel Weather: also clears the entity-less hail (`HailSim.clear`, plus an empty hail field to clients).
- Saplings: 26.3 `TreeGrower(name, trees, megaTrees, flowerTrees, shortest)`. Galewood and Stormpine had their trees
  in the 2x2 slot, so a lone sapling never grew. Fixed (Galewood 4:1 normal:tall).
- Creative dimension access: the Creative Realm Pass already exists (first in the Materials tab); a GameTest now
  takes a Creative player into the realm and back.
- Tests: MobFixesStage6Test (Gale launch); GameTests for the three saplings (bone meal and natural growth separately),
  boss orbs, realm travel, dispel + hail.

### Stage 7: structures and the Cyrrus cinematic (part 1)
- Shrine "sigil" crash: the Sovereign Altar's lightning crash was fixed in 5.0.1. The Hollow Shrine path (relic
  pedestals, Weathered Journal on the altar, Cyrrus) is safe server-side; the likely client crash was Cyrrus/the
  Stranger building a weathered copy of the player's skin and uploading it to the GPU mid-frame. `WeatheredSkins` now
  does the pixel work at once but registers the textures between frames (`mc.schedule`), showing the plain skin
  until then. Cinematic action parsing tolerates a missing number.
- Cinematic (`story/CinematicSync` + `client/story/CinematicCamera`): five shots on the script's beats (establishing
  crane, low angle as it rises, orbit, pull-back as it grows, push-in then back to the player), 1.5 s cross-blends,
  framed on the live boss, letterbox bars, third person. Jump skips (hint for 5 s); the fight starts early when every
  watcher has skipped (`fastForward` applies the remaining actions without voices). Late arrivals join in sync.
- Tempest Spire: ground floor added (the bottom chest hung over air), every spiral step has a solid stringer under it,
  the spiral now continues on a tighter ring through the narrow top section and the crown floor (before, it stopped
  nine blocks short of the altar), with a flat landing at the ring change. Chests stand in alcoves against a newel
  pillar on each floor. Checked offline: no floating chests or steps, no wall holes, walkable from the door to the altar.
- Tempest Eye: no longer Sovereign loot (it was left in the sky arena when everyone was sent back down). On death a
  `TempestEyeReward` floats over the Sovereign Altar (saved with the world, can't be hurt), gives exactly one Eye to
  the first player who uses it. JEI still lists the Eye as the Sovereign's reward.
- Arenas (`SovereignArena`, now with styles): built only when the fight starts, taken down after (loose items brought
  down with the players, leftover orbs removed); 4 s pause after a win before the return; a boss knocked off is put
  back. New SHRINE style (radius 26 skyshale ring, broken parapet, storm-broken pillars with soul lanterns): the Cyrrus
  fight moves to it when the intro ends (`shrineArena` in the properties file).
- Labyrinth: already at bedrock depth with no surface shaft (5.1), three keys, unbreakable vault, Hollow Keeper,
  original score; unchanged here.
- Tests: GameTests for the Eye (exactly once), the arena lifecycle and skipping the intro.

### Stage 7 part 2: the Hollow Shrine rebuilt
- gendata8.py `hollow_shrine` (35 x 26 x 35, was 17 x 9 x 17): a floating skyshale island (inverted cone, ion veins,
  hanging spurs), a patterned plaza (chiseled rim, worn brick, a tempest-brick ring), four tempest-brick causeways with
  stairs up a three-tier dais to the Hollow Altar, relic pedestals on the dais diagonals, storm conduits on obelisks at
  the causeway ends (open to the sky, no lightning rods so natural strikes can't spoil the order), soul braziers, a
  colonnade of sixteen storm-broken pillars (tall ones hang soul lanterns, short ones leave rubble), a reliquary chest
  beside the altar, rubble drifting off the corners. Checked offline: every ritual block in ShrineBlocks' search box,
  conduits open to the sky, altar, pedestals and chest all reachable on foot. The fight itself moves to the sky arena.

### Resume point (overhaul in progress)
Done: stages 1-7 (stage 7: Spire, Tempest Eye, arenas, Cyrrus cinematic + skip, skin-upload crash, Hollow Shrine).
CI green at 69 GameTests / 74 unit tests (commit after "stage 7"). Waiting on the user: should Stormreach swords
break blocks in Creative (vanilla says no)?
Next, in order:
2. Stage 8: original sound redesign pass, guidebooks (beginner guide without rarity; crafting book only accuracy
   fixes, same look), placement previews (section 21), performance pass.
3. Multiplayer pass, then "even more" storm content.
4. Tests, CHANGELOG/NOTES/CLAUDE.md, final double-check.
5. Last of all: branding (name, icon and description for Mod Menu, GitHub, the .zip and the .jar). No jar is
   delivered until everything is done and checked.
