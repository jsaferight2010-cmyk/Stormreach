# Stormreach 5.0 Player Guide

Everything added in Stormreach 5.0 (the *Hollow Storm* update): how to find it, how it works, and how rare it is. Minecraft Java 26.3, Fabric.

> **Spoilers.** The sections on the Stranger, the Hollow Shrine and Cyrrus give away the story. They are marked, so you can skip them.

**Contents**

1. Tornadoes and severe weather
2. Meteorology: weather instruments
3. The Weather Events creative tab
4. Backpacks
5. The Hollow Labyrinth
6. Elderstorm Grove (Tempest Realm)
7. The Stranger *(spoilers)*
8. The Hollow Shrine *(spoilers)*
9. Cyrrus, the Hollow Storm *(spoilers)*
10. Achievements
11. Settings
12. Rarity and drop-rate tables
13. Credits and licences

---

## 1. Tornadoes and severe weather

Tornadoes in Stormreach are modelled on real ones.

- **Where they come from.** Most tornadoes come out of **supercell thunderstorms**. When an Overworld thunderstorm is going, every 10 seconds there is a small chance (0.6% per player) that one forms near you, usually 150–260 blocks away.
  - Some storms first show a rotating **wall cloud** (a lowered, turning cloud base) before a tornado touches down.
- **Landspouts** are weak, short-lived and slow. They form from rising air under ordinary rain (no thunder) in warm biomes.
- **Waterspouts** form over oceans and rivers, mostly in rain. They are also weak.

### How strong is it? (the EF scale)

The rating is the **Enhanced Fujita (EF) scale**, which rates a tornado by the **damage it does**, not by how it looks. Each rating covers a range of 3-second gust speeds:

| Rating | Gust (mph) | Gust (km/h) | What it can do in Stormreach |
|---|---|---|---|
| EF0 | 65–85 | 105–137 | Strips leaves and crops, breaks glass and wool, scours loose sand and gravel |
| EF1 | 86–110 | 138–177 | Tears off planks, fences and doors; light wooden builds start to fail |
| EF2 | 111–135 | 178–217 | Logs and heavy timber give way; exposed bricks loosen |
| EF3 | 136–165 | 218–266 | Masonry and brick walls fail; exposed stone starts to break |
| EF4 | 166–200 | 267–322 | Well-built stone gives way; packed earth is scoured from the ground |
| EF5 | over 200 | over 322 | Even metal blocks are torn out |

No two tornadoes are the same. Each one gets a *peak* wind somewhere inside its rating's range, not a fixed value per rating. It also has a **life cycle**: it forms, strengthens, reaches its peak, shrinks and ropes out.

When a tornado lifts, everyone within 768 blocks gets a **damage survey** message. It gives:

- the final rating, worked out from the strongest damage the tornado actually caused
- the path length and maximum width
- the number of blocks damaged
- its appearance

Like a real survey, a powerful tornado that only crossed empty fields can come out rated EF0. If nothing was damaged, the rating is "EF-U" (unknown).

### What it looks like (morphology)

A tornado's shape is a separate thing from its strength. The guide tables (section 12) list how often each shape appears at each rating.

- **Rope:** a thin, twisting tube. This is often how a tornado looks at the end of its life.
- **Needle:** a thin, straight, tapering funnel.
- **Drill-bit:** a wide top narrowing to a thin tip.
- **Cone:** the classic shape.
- **Stovepipe:** a tall, straight-sided column.
- **Wedge:** wider than it is tall.
- **Large wedge:** a huge, dark wall of cloud and debris.

Some extra features have to do with how the vortex is built, not its outline:

- **Multiple-vortex:** smaller suction vortices orbit inside the main one. They cause the worst damage, in narrow streaks.
- **Satellite tornado:** a smaller tornado that orbits a large, violent one.
- **Companion tornado:** a second, independent tornado from the same storm.

### How a tornado moves and does damage

- **Wind field.** The wind is calm at the very centre. It peaks at the edge of the core and drops off with distance.
  - Air also flows inward and upward, and subvortices add sharp local peaks.
  - The side where the spin and the tornado's forward motion add up has the strongest winds.
  - Supercell tornadoes almost always spin counter-clockwise (seen from above).
- **Movement.** Supercell tornadoes travel at about 13 m/s (30 mph) on average, usually toward the east or northeast. They wobble along the way, and often turn left as they weaken.
- **Blocks.** Each block holds up to a different wind speed. Blocks that are exposed, unsupported or thin give way sooner.
  - Loose, light blocks get **picked up as debris** and thrown. Heavier ones just break.
  - Blocks deep inside a solid structure are protected by their neighbours. A basement really is the safest place.

| Material class | Fails at about | Examples |
|---|---|---|
| Foliage | 45 mph | leaves, crops, flowers |
| Glass | 75 mph | glass, panes, ice |
| Fabric | 85 mph | wool, carpet, beds |
| Light wood | 105 mph | planks, fences, doors, signs |
| Heavy wood | 125 mph | logs |
| Masonry | 150 mph | bricks, cobblestone, terracotta |
| Stone | 170 mph | stone, deepslate, concrete |
| Loose earth | 185 mph | dirt, sand, gravel (scoured from the surface) |
| Metal | 205 mph | iron, copper and other metal blocks |
| Unbreakable | never | bedrock, obsidian, portals, the mod's sealed blocks |

- **Entities.** Mobs and players get pulled in, lifted and thrown. Above about 110 mph they also take damage.

### Other severe weather

- **Downbursts:** a sudden blast of wind straight down that spreads out in all directions. It flattens crops and leaves and knocks things over.
- **Hail:** hurts anyone outside without a helmet.
- **Ball lightning:** drifting orbs of charge.

---

## 2. Meteorology: weather instruments

Right-click any instrument to read it. Several also give off a **redstone signal**, so you can build automatic warning systems.

| Instrument | What it reads | Redstone output | Recipe (key items) |
|---|---|---|---|
| Anemometer | Sustained wind and gusts (mph and km/h), Beaufort force | Gust speed ÷ 10 (0–15) | Copper and iron ingots |
| Wind Vane | Wind direction | — | (from earlier versions) |
| Storm Barometer | Pressure in hPa and inHg (drops sharply near a tornado) | — | (from earlier versions) |
| Thermometer | Temperature in °C and °F, dew point, humidity | — | Glass pane, redstone, iron nugget |
| Weather Station | Everything above, plus a forecast | — | Anemometer, Barometer, Thermometer, comparator, redstone block |
| Storm Detector | Rain, thunderstorms, and tornadoes within 512 blocks | 4 in rain, 15 in a thunderstorm or with a tornado nearby | Glass, Storm Shard, iron, redstone |
| Doppler Radar | Rotation within 1024 blocks: bearing, distance, shear, motion, estimated rating, width and shape | Stronger the closer the tornado (up to 256 blocks) | Glimmer Antler, daylight detector, Storm Cells, Storm Detector, iron blocks, observer |
| Tornado Siren | Wails while it gets a redstone signal (wire it to a Radar or Detector) | — | Gold, note block, redstone block, iron |
| Weather Monitor | A Weather Station readout plus the **severe-weather outlook** (none, general thunder, marginal, slight, enhanced, moderate, high) and full radar, once a Weather Station and a Doppler Radar are within 8 blocks | — | Glass panes, Eye of Ender, iron, redstone |
| Handheld Storm Radar | A short-range radar sweep (512 blocks), plus tornado warnings while you carry it | — | Iron, compass, Storm Detector, redstone |
| Weather Radio | The forecast and outlook, plus tornado warnings while you carry it | — | Iron, note blocks, Storm Shard, redstone |

**Radar shows rotation, not ratings.** A real Doppler radar can't see an EF rating. It sees the wind moving toward and away from it on each side of the vortex: the "gate-to-gate shear". The estimated rating on the radar is a guess. The real rating comes from the damage survey.

**Tornado warnings:** carrying a Weather Radio, a Handheld Storm Radar or the Chaser's Badge, or wearing the Storm Chaser's Cap, warns you whenever a tornado touches down within 400 blocks. The warning tells you which direction it is and how far away.

---

## 3. The Weather Events creative tab

The **Stormreach: Weather Events** creative tab holds every weather event, for building, testing or showing off.

### Tornado spawners

- There is one spawner for each shape: Natural, Rope, Needle, Drill-bit, Cone, Stovepipe, Wedge, Large Wedge, Multiple-Vortex, With Satellite, With Companion, Landspout and Waterspout.
- **Use** it while looking at the ground: the tornado forms where you're looking (up to 96 blocks away), or 40 blocks ahead if you're looking at the sky.
- **Sneak + use** to change the intensity:
  - *climatology* (random, with real-world odds) or EF0 through EF5
  - landspouts go up to EF2 and waterspouts up to EF1, as in reality
- The Max Tornadoes setting still applies.

### Events

- **Supercell:** a rotating storm with a wall cloud that may produce a tornado after 20–40 seconds.
- **Downburst**, **Hailstorm** and **Ball Lightning Swarm**.
- **Dispel Weather Events:** removes every tornado and event in the world.

The **Stormreach: Meteorology** tab has all the instruments. The mod's other items are sorted into Building, Nature, Tools & Combat, Food, Materials and Spawn Eggs tabs. The Storm Blocks and Storm Mobs tabs from 3.0 are unchanged.

---

## 4. Backpacks

### Getting a backpack

Backpacks come in seven tiers. You **craft a Diamond Backpack**, then upgrade it one tier at a time. To upgrade, craft an **Upgrade Kit** and combine it with your current backpack in a crafting grid.

**Upgrading keeps everything:** contents, enchantments and name all carry over.

| Tier | Slots | Upgrade kit needs (besides the previous backpack) |
|---|---|---|
| Diamond | 27 | *Crafted:* 4 diamonds, 2 leather, 2 Storm Shards, a chest |
| Obsidian | 36 | 4 obsidian, 2 Fulgurite, 2 Charged Stormstone, a Storm Cell |
| Netherite | 54 | 4 Blocks of Storm Crystal, 2 blaze rods, 2 Storm Cells, a netherite ingot |
| Storm Crystal | 72 | 4 Blocks of Storm Crystal, 2 Storm Cells, 2 Charged Stormstone, a Tempest Core |
| Voltite | 90 | 4 Blocks of Voltite, 2 Ion Shards, 2 Stormhawk Feathers, a Storm Cell |
| Aurorite | 108 | 4 Blocks of Aurorite, 2 Gilded Stormfruit, 2 Ion Lamps, a Tempest Eye |
| Maelstrom | 135 | 4 netherite ingots, 2 Blocks of Aurorite, 2 Tempest Eyes, a Maelstrom Core |

The **Storm Chaser's Pack** (45 slots, built-in Magnetism) can't be crafted. It's an achievement reward.

### Using a backpack

- **Use** it, or press **B** (rebindable), to open it.
- Packs bigger than 54 slots have **pages**: use the ◀ ▶ buttons.
- **Sort** stacks and orders the contents.
- You can't put a backpack inside a backpack.
- The backpack you have open is locked in your hotbar, so it can't be swapped, dropped or moved while open. That blocks the usual duplication tricks.
- The contents are stored on the item itself. They survive the backpack being dropped, stored, traded or carried across dimensions. On a server, every check happens on the server.
- If a backpack item is destroyed (lava, cactus, void), its contents spill out instead of vanishing.

### Backpack enchantments

These only go on backpacks. The table says where each one can be found.

| Enchantment | Max level | Effect | Where |
|---|---|---|---|
| Capacity | III | +9 slots per level | Enchanting table, librarians, loot |
| Magnetism | I | Pulls nearby dropped items into the pack (radius 4.5 blocks) | Enchanting table, librarians, loot |
| Organization | I | Sorts the pack automatically every time you close it | Enchanting table, librarians, loot |
| Resilience | I | A dropped pack can't be destroyed by fire, lava, explosions or cactus, and floats back up out of the void | Enchanting table, librarians, loot |
| Soulbound | I | You keep the pack when you die | Treasure only: librarians and loot, never the enchanting table |

---

## 5. The Hollow Labyrinth

**Where to find it:**

- A huge maze buried deep under the Overworld: 150 × 150 blocks, about 38 blocks below the surface.
- At most one in any 1024 × 1024 area, so they're **very rare**.
- The only way in is a ruined stone arch over a ladder shaft.
- `/locate structure stormreach:hollow_labyrinth` finds one.

**Inside:**

- **Sections.** The maze has four sections, each built from different stone, and dead ends everywhere. Loot is sparse:
  - side rooms hold scraps
  - one rare page of an old journal turns up in them
- **Three Labyrinth Keys.** The keys sit in vaults in three of the maze's corners, one guaranteed key per vault. Bring all three to the **Labyrinth Keyhole** next to the sealed **treasure chamber** in the centre.
  - The chamber's **Labyrinth Seals** can't be broken, so the keys are the only way in.
  - The **Heart of the Labyrinth** and a journal page wait inside.
- **The Hollow Keeper** is the maze's guardian. It is tall, fast and armoured, and it never leaves the maze. It also takes half damage from arrows and other projectiles.
  - It **hunts by sight and by sound**: if it sees you it runs you down, and if it doesn't it moves toward where it last heard you.
  - Leave the maze for a minute and it goes dormant.
- **Hide in a Rusted Locker.**
  - Use one to step inside. You get the **Hidden** effect and hear your own heartbeat when the Keeper is close.
  - Sneak to step out.
  - The Keeper searches lockers near where it last saw you:
    - If it saw you get in, it has about a 35% chance per second of finding you when it's right outside.
    - If it didn't, the chance is only 3%.
    - If you move while hiding, it finds you for sure.
  - Being found throws you out of the locker.
- **Adaptive music.** Three original music layers play together while you're in the maze: calm, tense and chase. They blend into each other as the Keeper gets closer, and the chase layer takes over the moment it spots you. Normal music pauses until you leave.

---

## 6. Elderstorm Grove (Tempest Realm)

A fairly common forest biome in the Tempest Realm, full of glowing life.

- **Elderstorm trees:** two kinds grow here.
  - Branching trees with crowns of glowing teal leaves.
  - Giant 2×2-trunk trees, up to about 30 blocks tall. Plant four saplings in a square to grow one.
  - Their purple wood comes in a full set: logs, stripped logs, planks, stairs, slabs, fences, gates, doors, trapdoors, buttons and pressure plates.
- **Stormfruit** hangs glowing under the leaves and ripens in three stages.
  - Right-click a ripe one to pick 1–2. It regrows.
  - Leaves sometimes drop it too.

| Food | Hunger | Effects |
|---|---|---|
| Stormfruit | 5 (2.5 shanks) | Speed (20 s), Static Charge (10 s) |
| Gilded Stormfruit (8 gold ingots + Stormfruit) | 8 | Regeneration II (10 s), Absorption II (2 min), Grounded (2 min); always edible |
| Stormfruit Preserve (2 Stormfruit, sugar, bottle) | 6 | Speed II (90 s) |

Gilded Stormfruit is also needed for the Aurorite Backpack upgrade and the Hollow Shrine.

**Plants:**

- **Thunderbloom:** a yellow flower that gives Haste when made into suspicious stew.
- **Sparkfern:** tall grass. Shear it to collect it; breaking it sometimes drops Thistle Down.

**Creatures:**

- **Glimmerstag:** a skittish deer with glowing antlers. It bolts from sprinting players.
  - Breed them with Stormfruit.
  - They drop leather, and sometimes a **Glimmer Antler**, which the Doppler Radar recipe needs.
- **Thistlehop:** a hopping puffball critter. It sheds **Thistle Down** every few minutes (3 make a string).
  - Breed them with Sparkfern.
  - They never take fall damage.

---

## 7. The Stranger *(spoilers)*

You are not alone.

- After a while you may notice **someone standing at the edge of your vision**, 28–48 blocks away. Look straight at it, or walk toward it, and it's gone.
- It turns up more often in forests, in rain, at night and in the dark. It never attacks.
- **Signs** that something is following you:
  - footsteps behind you
  - a whisper
  - smoke with no fire
  - a campfire suddenly put out
  - a small mossy cairn topped with a soul torch
- **After your third sighting** it starts leaving **Hollow Remnants** where it stood. You'll need one later.
- Torn **journal pages** are hidden around the world. The table shows where each one is found.

| Page | Where |
|---|---|
| I | Sky Vault chests (12%) |
| II | Tempest Spire chests (15%) |
| III | Hollow Labyrinth side-room chests (8%) |
| IV | The Labyrinth's treasure chamber (always) |
| V | The Hollow Shrine's chest (always) |

Craft all five pages with a book and a Hollow Remnant to rebind the **Weathered Journal**.

---

## 8. The Hollow Shrine *(spoilers)*

**Finding it:**

- A ring of broken pillars on a floating island in the Tempest Realm sky, between y 130 and y 175.
- At most one in any 1920 × 1920 area, so it's **legendary rare**.
- `/locate structure stormreach:hollow_shrine` finds it.

**The ritual has three stages.** Every step is clued in the world: right-click the pedestals, conduits and altar for hints.

### Stage 1: four Relic Pedestals

Each pedestal shows a riddle and takes one relic:

| Riddle | Relic | From |
|---|---|---|
| "What I left in the deepest room, bring it back." | Heart of the Labyrinth | The Labyrinth's treasure chamber |
| "The crowned thing's eye, which never closes." | Tempest Eye | The Tempest Sovereign |
| "Fruit of the old grove, dressed in gold." | Gilded Stormfruit | Crafted from Stormfruit |
| "What I leave behind when you look at me." | Hollow Remnant | Where the Stranger stood |

### Stage 2: four Storm Conduits

- When every pedestal glows, **call lightning onto the four Storm Conduits** around the shrine (a Tempest Staff works well, or wait for a storm).
- Strike them **in the order of their notches**: 1, 2, 3, 4. Count the marks on each conduit or right-click it.
- The order does **not** go around the ring.
- Strike out of order and every conduit goes dark again.

### Stage 3: the Hollow Altar

Lay the **Weathered Journal** on the **Hollow Altar**.

---

## 9. Cyrrus, the Hollow Storm *(spoilers)*

### The intro

The fight opens with a fully voiced cinematic, about 45 seconds long:

- The sky goes dark and thunder rolls.
- A figure rises out of the ground below the altar. It's the Stranger.
- He tells you who he was, and grows larger with each line as the storm fills him. Lightning rings the shrine.
- He corrupts into his true form and the fight begins.

During the intro you are slowed and protected, and Cyrrus can't be hurt. Each line appears in chat and above your hotbar, and plays aloud. In multiplayer everyone within 96 blocks sees and hears it in sync.

**Stats:** 600 health (adjustable) and heavy armour. He grows to four times a player's size.

### The five phases

Every dangerous attack is **telegraphed** first: sparks on the ground, a charging sound, or gathering hail.

| Phase | Health | Theme | What happens |
|---|---|---|---|
| 1 | 100–80% | Lightning | Strikes on every player (sparks mark the spot about a second early), and rings of lightning spreading outward |
| 2 | 80–60% | Rain and visibility | Downpours bring Darkness and Slowness, hail falls on anyone not under a roof (worse without a helmet), and he blinks through the gloom |
| 3 | 60–40% | Wind | Gale blasts throw you back, and **crosswinds blow arrows and tridents off course**. He can spawn a small tornado on you |
| 4 | 40–20% | Summons | Squalls, Static Wisps, Stormhawks and Brine Wraiths join the fight |
| 5 | below 20% | The whole storm | A maelstrom drags everything to the centre, lightning cages close around you, and tornadoes, blinks and crosswinds all return |

He speaks a voiced line as he enters each phase.

### When he falls

He has three last lines, and the storm clears.

**Drops:**

- 2 **Maelstrom Cores**
- the **Stormcrown**
- the **Weathervane Scepter**
- a nether star
- 2–4 Tempest Cores

**Endgame rewards:**

| Item | What it does |
|---|---|
| Stormcrown | Helmet (4 armour, 3.5 toughness); immune to lightning |
| Weathervane Scepter | Use: cycle the Overworld weather (clear → rain → thunder → clear). Sneak + use: a downburst 16 blocks ahead. 60-second cooldown |
| Weatherbreaker (Maelstrom Core + Tempest Eye + netherite sword) | Maelstrom-tier sword (2600 durability): every hit hurls the target with wind, and 1 in 4 calls lightning |
| Maelstrom Core | Needed for the Maelstrom Backpack and the Weatherbreaker |

---

## 10. Achievements

5.0 adds **49 achievements**, with three new tabs and new branches on the main tab.

- **Chains:** they come in tiers (Forecaster I–V, the backpack tiers, Funnel Cloud → Damage Survey → Violent → Incredible).
- **Hidden ones:** some stay invisible until you earn them.
- **Item rewards:** some give an item that can't be got any other way, and no item is given by more than one achievement.
- **Vanilla:** some cover vanilla play (surviving a lightning strike, Channeling, lightning rods).

**Tabs:**

- **Stormreach** (main tab): backpacks, lightning, Elderstorm Grove
- **Meteorology:** instruments and tornadoes
- **The Hollow Labyrinth**
- **Chronicle:** the story. This tab only appears once you've seen the Stranger.

The full reward list is in the tables below.

---

## 11. Settings

**Mod Menu** (Stormreach → Configure) now holds **performance settings only**:

| Page | Setting | Default |
|---|---|---|
| Tornadoes | Tornado Simulation Distance | 256 blocks |
| Tornadoes | Max Tornadoes at Once | 2 |
| Tornadoes | Block Checks per Tick | 96 |
| Tornadoes | Airborne Debris Limit | 48 |
| Tornadoes | Tornado Block Damage | On |
| Tornadoes | Tornado Size | 35% of real scale |
| Effects & Load | Particle Density | 100% |
| Effects & Load | Storm Particles | On |
| Effects & Load | Max Squalls per Player | 4 |
| Effects & Load | Stormblight Heart Radius | 32 blocks |
| Effects & Load | Stormblight Spread Speed | 5 |
| Effects & Load | Backpack Magnet Interval | 10 ticks |

Gameplay options are still available for server owners in `config/stormreach.properties`:

- `naturalTornadoes`, `tornadoFrequency`, `tornadoIntensityBias`, `tornadoDurationPercent`
- `landspouts`, `downbursts`
- `shadowAppearanceRate`, `shadowSigns`
- `labyrinthGeneration`
- `bossDifficulty`, `guardianDifficulty`
- every setting from earlier versions

---

## 12. Rarity and drop-rate tables

All numbers below are calculated directly from the mod's loot tables, worldgen files and code (by `tools/guide_tables.py`), so they match the game.

### Item rarity (name colour)

Minecraft colours item names by rarity: white = Common, yellow = Uncommon, aqua = Rare, magenta = Epic. Everything not listed is Common. (Tornado spawners and weather events are Epic and creative-only.)


| Rarity | Items |
|---|---|
| Epic (magenta) | Chaser's Badge, Heart of the Labyrinth, Maelstrom Core, Storm Chaser's Pack, Stormcrown, Weatherbreaker, Weathered Journal, Weathervane Scepter |
| Rare (aqua) | Gilded Stormfruit, Hollow Remnant, Labyrinth Key, Lightning Charm, Storm Chaser's Cap |
| Uncommon (yellow) | Glimmer Antler, Handheld Storm Radar, Torn Journal Page (I), Torn Journal Page (II), Torn Journal Page (III), Torn Journal Page (IV), Torn Journal Page (V) |


### Mob drops

Chance is per kill without Looting (Looting adds up to +1 per level to most counts). "Drop-rate rarity" uses the same bands everywhere in this guide: Guaranteed 100%, Common ≥50%, Uncommon 15–50%, Rare 5–15%, Very rare 1–5%, Legendary <1%.


| Mob | Drop | Count | Chance per kill | Drop-rate rarity |
|---|---|---|---|---|
| Cyrrus, the Hollow Storm | Maelstrom Core | 2 | 100.0% | Guaranteed |
| Cyrrus, the Hollow Storm | Stormcrown | 1 | 100.0% | Guaranteed |
| Cyrrus, the Hollow Storm | Weathervane Scepter | 1 | 100.0% | Guaranteed |
| Cyrrus, the Hollow Storm | Nether Star | 1 | 100.0% | Guaranteed |
| Cyrrus, the Hollow Storm | Tempest Core | 2–4 | 100.0% | Guaranteed |
| Tempest Sovereign | Tempest Eye | 1 | 100.0% | Guaranteed |
| The Stormwarden | Tempest Core | 1 | 100.0% | Guaranteed |
| The Stormwarden | Block of Storm Crystal | 2–4 | 100.0% | Guaranteed |
| The Stormwarden | Storm Cell | 1–3 | 100.0% | Guaranteed |
| Hollow Keeper | Echo Shard | 2 | 100.0% | Guaranteed |
| Hollow Keeper | Iron Ingot | 2–6 | 100.0% | Guaranteed |
| Hollow Keeper | Copper Ingot | 4–10 | 100.0% | Guaranteed |
| Glimmerstag | Leather | 0–2 | 66.7% | Common |
| Glimmerstag | Glimmer Antler | 1 | 25.0% | Uncommon |
| Thistlehop | Thistle Down | 0–2 | 66.7% | Common |
| Skyray | Raw Skyray | 1–3 | 100.0% | Guaranteed |
| Puffling | Nimbus Cloud | 0–1 | 50.0% | Common |
| Static Wisp | Ion Shard | 0–2 | 66.7% | Common |
| Static Wisp | Storm Shard | 0–1 | 50.0% | Common |
| Cyclone | Gale Cloud | 0–2 | 66.7% | Common |
| Stormhawk | Stormhawk Feather | 0–2 | 66.7% | Common |
| Thunder Crab | Crab Claw | 1–3 | 100.0% | Guaranteed |
| Thunder Crab | Storm Shard | 0–2 | 66.7% | Common |
| Cliff Crab | Crab Claw | 1–2 | 100.0% | Guaranteed |
| Brine Wraith | Salt | 0–2 | 66.7% | Common |
| Brine Wraith | Rotten Flesh | 0–2 | 66.7% | Common |
| Brine Wraith | Storm Shard | 1 | 8.0% | Rare |
| Squall | Storm Shard | 0–1 | 50.0% | Common |
| Ball Lightning | Storm Shard | 0–1 | 50.0% | Common |
| Storm Gull | Gull Feather | 0–2 | 66.7% | Common |


The Stranger and tornadoes drop nothing. Stranger: after you have seen it 3+ times, each time it vanishes there is a 25% chance it leaves a Hollow Remnant where it stood (Uncommon, but only after the third sighting).
Every Storm-variant vanilla mob drops what its vanilla counterpart drops.


### Chest loot (chance per chest)


**Hollow Labyrinth: treasure chamber**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Heart of the Labyrinth | 1 | 100.0% | 1.00 | Guaranteed |
| Torn Journal Page (IV) | 1 | 100.0% | 1.00 | Guaranteed |
| Diamond | 2–5 | 65.0% | 3.29 | Common |
| Bottle o' Enchanting | 3–8 | 57.6% | 4.31 | Common |
| Block of Storm Crystal | 1–2 | 49.0% | 0.94 | Uncommon |
| Tempest Core | 1 | 39.1% | 0.47 | Uncommon |
| Diamond Chestplate | 1 | 27.7% | 0.31 | Uncommon |
| Netherite Scrap | 1–2 | 27.7% | 0.47 | Uncommon |
| Totem of Undying | 1 | 21.4% | 0.24 | Uncommon |
| Enchanted Golden Apple | 1 | 14.7% | 0.16 | Rare |
| Music Disc (Otherside) | 1 | 14.7% | 0.16 | Rare |


**Hollow Labyrinth: corner key vault (×3)**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Labyrinth Key | 1 | 100.0% | 1.00 | Guaranteed |
| Gold Ingot | 2–6 | 47.9% | 2.40 | Uncommon |
| Charged Stormstone | 2–6 | 36.5% | 1.71 | Uncommon |
| Emerald | 2–5 | 36.5% | 1.50 | Uncommon |
| Book | 1 | 30.2% | 0.34 | Uncommon |
| Storm Cell | 1–2 | 30.2% | 0.51 | Uncommon |
| Diamond | 1–3 | 23.4% | 0.51 | Uncommon |
| Golden Apple | 1 | 19.8% | 0.21 | Uncommon |
| Iron Sword | 1 | 19.8% | 0.21 | Uncommon |
| Name Tag | 1 | 16.1% | 0.17 | Uncommon |


**Hollow Labyrinth: side rooms**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Bone | 1–4 | 44.8% | 1.38 | Uncommon |
| Rotten Flesh | 1–3 | 35.4% | 0.83 | Uncommon |
| String | 1–3 | 35.4% | 0.83 | Uncommon |
| Iron Nugget | 2–8 | 33.4% | 1.93 | Uncommon |
| Copper Ingot | 1–4 | 24.8% | 0.69 | Uncommon |
| Torch | 2–6 | 24.8% | 1.10 | Uncommon |
| Bread | 1–2 | 20.3% | 0.33 | Uncommon |
| Candle | 1–2 | 15.5% | 0.25 | Uncommon |
| Storm Shard | 1–3 | 15.5% | 0.33 | Uncommon |
| Iron Ingot | 1–2 | 10.6% | 0.17 | Rare |
| Torn Journal Page (III) | 1 | 8.0% | 0.08 | Rare |
| Echo Shard | 1 | 2.7% | 0.03 | Very rare |


**Hollow Shrine**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Torn Journal Page (V) | 1 | 100.0% | 1.00 | Guaranteed |
| Ion Shard | 2–6 | 58.1% | 3.11 | Common |
| Stormfruit | 2–4 | 58.1% | 2.33 | Common |
| Voltite Ingot | 1–4 | 45.1% | 1.39 | Uncommon |
| Bottle o' Enchanting | 2–5 | 37.7% | 1.56 | Uncommon |
| Aurorite | 1–2 | 25.0% | 0.42 | Uncommon |
| Gilded Stormfruit | 1 | 15.7% | 0.17 | Uncommon |


**Sky Vault**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Voltite Ingot | 2–6 | 67.7% | 4.25 | Common |
| Ion Shard | 2–6 | 63.6% | 3.82 | Common |
| Cloudberry Tart | 1–3 | 48.4% | 1.27 | Uncommon |
| Stormhawk Feather | 2–5 | 48.4% | 2.23 | Uncommon |
| Galewood Sapling | 1–3 | 42.2% | 1.06 | Uncommon |
| Storm Nectar | 1–2 | 42.2% | 0.80 | Uncommon |
| Aurorite | 1–3 | 35.3% | 0.85 | Uncommon |
| Saddle | 1 | 35.3% | 0.42 | Uncommon |
| Tempest Tonic | 1–2 | 27.7% | 0.48 | Uncommon |
| Voltite Sword | 1 | 19.3% | 0.21 | Uncommon |
| Cloudstep Boots | 1 | 14.8% | 0.16 | Rare |
| Torn Journal Page (I) | 1 | 12.0% | 0.12 | Rare |
| Aurorite Pickaxe | 1 | 10.1% | 0.11 | Rare |


**Tempest Spire**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Storm Shard | 3–8 | 68.2% | 5.82 | Common |
| Tempest Bricks | 4–12 | 48.6% | 5.08 | Uncommon |
| Charged Stormstone | 2–6 | 42.3% | 2.12 | Uncommon |
| Emerald | 2–5 | 42.3% | 1.85 | Uncommon |
| Storm Cell | 1–3 | 42.3% | 1.06 | Uncommon |
| Fulgurite | 2–6 | 35.4% | 1.69 | Uncommon |
| Tempest Tonic | 1–2 | 35.4% | 0.63 | Uncommon |
| Block of Storm Crystal | 1–2 | 27.7% | 0.48 | Uncommon |
| Bottled Thunder | 1–2 | 27.7% | 0.48 | Uncommon |
| Diamond | 1–3 | 27.7% | 0.63 | Uncommon |
| Golden Apple | 1 | 19.3% | 0.21 | Uncommon |
| Thunderhead Horn | 1 | 19.3% | 0.21 | Uncommon |
| Torn Journal Page (II) | 1 | 15.0% | 0.15 | Uncommon |


**Storm Shrine**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Storm Shard | 3–8 | 83.2% | 8.64 | Common |
| Fulgurite Shard | 2–5 | 63.7% | 3.30 | Common |
| Storm Cell | 1–2 | 56.5% | 1.18 | Common |
| Bottled Rain | 1–2 | 48.2% | 0.94 | Uncommon |
| Bottled Sunshine | 1–2 | 48.2% | 0.94 | Uncommon |
| Tempest Tonic | 1–2 | 38.6% | 0.71 | Uncommon |
| Bottled Thunder | 1 | 27.5% | 0.31 | Uncommon |
| Thunderhead Horn | 1 | 14.7% | 0.16 | Rare |


**Lighthouse**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Storm Shard | 2–6 | 71.3% | 4.66 | Common |
| Gull Feather | 2–5 | 60.1% | 3.06 | Common |
| Salted Cod | 2–5 | 60.1% | 3.06 | Common |
| Candle | 2–6 | 45.2% | 2.33 | Uncommon |
| Emerald | 1–4 | 45.2% | 1.46 | Uncommon |
| Squall in a Bottle | 1–3 | 45.2% | 1.17 | Uncommon |
| Empty Map | 1 | 38.0% | 0.47 | Uncommon |
| Storm Cell | 1–2 | 30.0% | 0.52 | Uncommon |
| Compass | 1 | 25.6% | 0.29 | Uncommon |
| Tempest Staff | 1 | 16.2% | 0.17 | Uncommon |
| Thunderhead Horn | 1 | 5.7% | 0.06 | Rare |


**Wrecked Skiff**

| Item | Stack | Chance per chest | Avg. per chest | Drop-rate rarity |
|---|---|---|---|---|
| Driftwood Planks | 3–8 | 66.4% | 5.50 | Common |
| Salt | 2–6 | 55.1% | 3.00 | Common |
| Salted Cod | 1–4 | 55.1% | 1.88 | Common |
| Iron Nugget | 2–8 | 40.6% | 2.50 | Uncommon |
| String | 1–5 | 40.6% | 1.50 | Uncommon |
| Rock Candy | 1–3 | 33.9% | 0.80 | Uncommon |
| Storm Shard | 1–3 | 33.9% | 0.80 | Uncommon |
| Gold Ingot | 1–2 | 18.4% | 0.30 | Uncommon |


### Structure rarity

Structures are placed on a grid: the world is cut into regions of *spacing × spacing* chunks and each region gets at most one attempt, at least *separation* chunks from the region edge. "One per" is the region size in blocks; the structure appears there only if the biome fits. Every structure here works with `/locate structure`.


| Structure | Where | Spacing (chunks) | Separation | At most one per | Rarity |
|---|---|---|---|---|---|
| `stormreach:hollow_shrine` | Tempest Realm, floating (any realm biome) | 120 | 40 | 1920×1920 blocks | Legendary |
| `stormreach:hollow_labyrinth` | Overworld, underground (any biome) | 64 | 24 | 1024×1024 blocks | Very rare |
| `stormreach:tempest_spire` | Overworld, Stormreach biomes | 48 | 16 | 768×768 blocks | Rare |
| `stormreach:lighthouse` | Stormreach Coast | 40 | 12 | 640×640 blocks | Uncommon |
| `stormreach:storm_shrine` | Stormreach biomes | 32 | 10 | 512×512 blocks | Uncommon |
| `stormreach:sky_vault` | Tempest Realm sky | 24 | 8 | 384×384 blocks | Uncommon |
| `stormreach:wrecked_skiff` | Stormreach beaches and shores | 20 | 6 | 320×320 blocks | Common |


### Ores and clouds


| Block | Dimension | Frequency | Vein size | Height | Skipped when touching air |
|---|---|---|---|---|---|
| Gale Cloud | Tempest Realm | 1 vein per 5 chunks | up to 44 blocks | y 40 to 220 | — |
| Nimbus Cloud | Tempest Realm | 1 vein per 2 chunks | up to 48 blocks | y 20 to 200 | — |
| Thundercloud | Tempest Realm | 1 vein per 4 chunks | up to 40 blocks | y 10 to 160 | — |
| Aurorite Ore | Tempest Realm | 3 veins per chunk | up to 4 blocks | y 0 to 120 | 50% |
| Aurorite Ore | Tempest Realm | 6 veins per chunk | up to 5 blocks | y 0 to 200 | 30% |
| Ion Ore | Tempest Realm | 8 veins per chunk | up to 7 blocks | y 0 to 220 | — |
| Storm Crystal Ore | Overworld (Stormreach biomes) | 8 veins per chunk | up to 6 blocks | y 0 to 112 | — |
| Storm Crystal Ore | Overworld (Stormreach biomes) | 16 veins per chunk | up to 6 blocks | y 40 to 200 | — |
| Stormstone | Overworld (Stormreach biomes) | 10 veins per chunk | up to 48 blocks | y 30 to 128 | — |
| Voltite Ore | Tempest Realm | 14 veins per chunk | up to 9 blocks | y 0 to 200 | — |


Aurorite is the rarest realm ore (3 small veins per chunk below y 120, half of them discarded if exposed to air; richer in Aurora Spires).


### Tornado ratings: how often each EF rating happens

Ratings follow the U.S. climatology (most tornadoes are weak; violent ones are rare). The rating is given *after* the tornado lifts, from the strongest damage it actually caused, just like a real damage survey: a violent tornado that only crosses open fields can be rated low. The Intensity Bias setting in the config file shifts these odds.


| Rating | 3-second gust | Supercell tornadoes | Rarity | Landspouts | Waterspouts | Multiple-vortex chance |
|---|---|---|---|---|---|---|
| EF0 | 65–85 mph | 55.0% | Common | 80% | 90% | 2% |
| EF1 | 86–110 mph | 32.0% | Uncommon | 18% | 10% | 5% |
| EF2 | 111–135 mph | 9.5% | Rare | 2% | — | 15% |
| EF3 | 136–165 mph | 2.6% | Very rare | — | — | 30% |
| EF4 | 166–200 mph | 0.8% | Legendary | — | — | 50% |
| EF5 | over 200 mph | 0.1% | Legendary | — | — | 60% |


**What they look like, by rating (supercell tornadoes)**


| Rating | Rope | Needle | Drill-bit | Cone | Stovepipe | Wedge | Large wedge |
|---|---|---|---|---|---|---|---|
| EF0 | 30% | 15% | 15% | 35% | 5% | — | — |
| EF1 | 20% | 8% | 15% | 45% | 12% | — | — |
| EF2 | 10% | — | 8% | 45% | 25% | 12% | — |
| EF3 | 5% | — | — | 35% | 30% | 28% | 2% |
| EF4 | — | — | — | 20% | 25% | 45% | 10% |
| EF5 | — | — | — | 10% | 25% | 50% | 15% |


Appearance is not intensity: a rope can still be strong, and wedges can be weak. Satellite tornadoes (8%) only form beside EF3+ tornadoes at least 200 m wide; a separate companion tornado forms 5% of the time.


### Natural events


| Event | Chance | When and where | Rarity |
|---|---|---|---|
| Supercell tornado | 0.6% per player every 10 s | Overworld thunderstorm, any non-frozen biome (about one per 2–4 thunderstorms near you) | Rare |
| Landspout | 0.15% per player every 10 s | Rain without thunder, warm biomes (temperature > 0.6), not over water | Very rare |
| Waterspout | 0.4% every 10 s (0.1% in clear weather) | Over oceans and rivers | Very rare |
| The Stranger appears | 2% every 30 s, ×1.8 in forests, ×1.5 in rain, ×1.4 at night, ×1.3 in darkness (max 25%) | Overworld, anywhere; grows a little bolder with every sighting | Rare |
| A sign of the Stranger | 3% every 30 s | Footsteps, whispers, smoke, a snuffed campfire, a cairn with a soul torch | Rare |


### Backpack tiers


| Backpack | Slots | Slots with Capacity III | Pages (max) | How to get |
|---|---|---|---|---|
| Diamond Backpack | 27 | 54 | 1 | crafted |
| Obsidian Backpack | 36 | 63 | 2 | Diamond Backpack + Obsidian Upgrade Kit |
| Netherite Backpack | 54 | 81 | 2 | Obsidian Backpack + Netherite Upgrade Kit |
| Storm Crystal Backpack | 72 | 99 | 2 | Netherite Backpack + Storm Crystal Upgrade Kit |
| Voltite Backpack | 90 | 117 | 3 | Storm Crystal Backpack + Voltite Upgrade Kit |
| Aurorite Backpack | 108 | 135 | 3 | Voltite Backpack + Aurorite Upgrade Kit |
| Maelstrom Backpack | 135 | 162 | 3 | Aurorite Backpack + Maelstrom Upgrade Kit |
| Storm Chaser's Pack | 45 | 72 | 2 | achievement reward only; built-in Magnetism |


### Achievements with rewards

Each reward item comes from exactly one achievement. The Sovereign Trophy is the reward for the existing *Regicide* achievement (defeat the Tempest Sovereign, +200 XP).


| Achievement | How | Item reward | XP | Hidden |
|---|---|---|---|---|
| Fireproof Freight | Upgrade to a Netherite Backpack | — | 100 | No |
| Bottled Lightning, Packed Neatly | Upgrade to a Storm Crystal Backpack | Storm Chaser's Pack | 150 | No |
| Northern Satchel | Upgrade to an Aurorite Backpack | — | 250 | No |
| The Eye of the Bag | Upgrade to a Maelstrom Backpack | — | 750 | No |
| Lightning Rod | Be struck by lightning and live | Lightning Charm | — | No |
| Forecaster IV: Hook Echo | Build a Doppler Radar | — | 100 | No |
| Forecaster V: The Whole Picture | Build a Weather Monitor | — | 150 | No |
| Violent | Witness a tornado rated EF4 or stronger | Tornado Trophy | 250 | No |
| Incredible | Witness a tornado rated EF5 | Chaser's Badge | 500 | No |
| Into the Bear's Cage | Get caught inside a tornado's wind field and survive | Storm Chaser's Cap | — | Yes |
| Holding My Breath | Stay hidden while the Hollow Keeper searches right outside your locker | — | 100 | Yes |
| A Heart Left Behind | Take the Heart of the Labyrinth from the treasure chamber | — | 200 | No |
| Keeper of Nothing | Defeat the Hollow Keeper | Keeper Trophy | 300 | No |
| Let the Sky Decide | Defeat Cyrrus, the Hollow Storm | Cyrrus Trophy | 1000 | No |
| Eye of the Hollow Storm | Defeat the Sovereign, the Keeper and Cyrrus, and carry a Maelstrom Backpack | — | 2000 | Yes |


---

## 13. Credits and licences

- **Cyrrus's voice:**
  - spoken by the **Piper** text-to-speech engine with the "Ryan (high)" voice model
  - processed into his corrupted voice for this mod
  - the voice model's training data is licensed **CC BY-NC-SA 4.0**, so these recordings are for non-commercial use, with attribution, under the same licence
  - you can re-record or replace any line: see `data/stormreach/cinematic/*.txt` and `tools/audio5.py`
- **Music and sounds** (Labyrinth layers, Cyrrus's theme, siren, tornado roar, heartbeat, whisper, screech): synthesised from scratch for Stormreach. No outside samples.
- **Code and textures:** MIT, like the rest of Stormreach.
