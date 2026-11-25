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
7. Stormwild: new creatures
8. Storm chasing
9. Skyward: storm creatures, gear and gadgets
10. Thunderfront: signs of the storm
11. Live Wire: storm power and captured water
12. The Stranger *(spoilers)*
13. The Hollow Shrine *(spoilers)*
14. Cyrrus, the Hollow Storm *(spoilers)*
15. Achievements
16. Settings
17. Rarity and drop-rate tables
18. Credits and licences

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

A tornado's shape is a separate thing from its strength. The guide tables (section 17) list how often each shape appears at each rating.

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

## 7. Stormwild: new creatures

**On the coast** (Stormreach Coast, Squallwind Bluffs, Brinewater Marsh, Static Fields):

- **Sparkskink:** a small lizard that soaks up storm charge.
  - Out in the rain, or if lightning strikes it, it **charges**: its crest glows and it crackles with sparks.
  - While charged it sheds a **Charged Scale** every few minutes.
  - Breed with Glow Berries.
- **Tidewrack Hermit:** a hermit crab in a pearly spiral shell.
  - Walk up to one and it pulls into its shell, taking far less damage. Sneak, or hold Kelp, and it stays out.
  - Drops **Pearlescent Shells**. Breed with Kelp.
- **Shock Eel:** lives in coastal and marsh water and leaves you alone, unless it's thundering and you're in its water, or you hit it.
  - Its bite shocks you (Static Charge).
  - Drops **Voltaic Eel**: cook it for a filling meal that gives Haste.

**In the Tempest Realm:**

- **Cloud Whale** (Cloudsea Expanse): a gentle giant that drifts high above the ground, singing, with a little rain always falling from its belly.
  - Every five to ten minutes it leaves a lump of **Cloud Ambergris**.
- **Storm Jelly** (Cloudsea Expanse, Aurora Spires): a glowing jellyfish that swims through the air. Touching one stings (Static Charge). Drops **Glowgel**.
- **Gale Sprite** (Galewood Highlands, Aurora Spires, Elderstorm Grove): a tiny wind spirit.
  - Give it **Thistle Down** and it gives you a **Gale Essence**, then keeps you company for three minutes.
  - While it's with you, it catches you on the wind when you fall.

**In thunderstorms:**

- **Thunderbird:** a huge storm raptor that sometimes arrives over a player out under a thunderstorm.
  - It circles high, dives to strike with its talons, and calls lightning down on you.
  - It has 120 health and a boss bar, and leaves when the storm passes.
  - Drops **Thunderbird Plumes** and Charged Scales.

**What they give you:**

| Item | Made from | What it does |
|---|---|---|
| Stormkite | 3 Thunderbird Plumes, 2 string, a Charged Scale, a stick | Hold it while falling to glide the way you're looking. No fall damage while gliding. |
| Tide Conch | 3 Pearlescent Shells, a gold ingot, a prismarine shard | Blow it: Water Breathing (60 s) and Dolphin's Grace (30 s) for everyone within 8 blocks, and Shock Eels nearby calm down. 1-minute cooldown. |
| Bottled Gale | Gale Essence + glass bottle | Drink: Jump Boost III and Slow Falling (45 s). |
| Skylight Lantern | Cloud Ambergris, 2 Glowgel, 2 iron nuggets (makes 2) | The brightest lantern (light 15). |
| Glowgel Block | 9 Glowgel | Bouncy like slime, and it glows. |
| Nacre Block / Nacre Bricks | 4 Pearlescent Shells / 4 Nacre Blocks | Iridescent mother-of-pearl building blocks. |

---

## 8. Storm chasing

- **Tornado Intercept Vehicle (TIV):** an armoured chase truck. Craft it from iron, glass, an Anemometer and coal blocks, then right-click the ground to set it down.
  - **To drive:** right-click to get in, W/S for throttle and brake, A/D to steer, sneak to get out.
  - **To pick it up:** sneak + right-click from outside.
  - **Dashboard:** the action bar tracks the nearest tornado: its rating, distance and bearing, which way and how fast it's moving, its peak wind, and the wind where you are.
  - **Anchors:** press the anchor key (default **Z**, change it under Controls) and four hydraulic spikes slam down from the armour skirts at once. An anchored TIV can't be moved by any tornado, so you can wait for one to pass right over you. Press the key again to raise them before you drive off.
- **Weather Balloon:** use it to launch a radiosonde. It climbs with the wind, bursts at the storm tops, and radios back a **sounding**:
  - temperature and dew point
  - cloud base and freezing level
  - instability (CAPE) and wind shear
  - the severe-weather risk
- **Rain Gauge:** fills with rain (faster in thunderstorms) and slowly dries out.
  - Right-click to read it in millimetres. Sneak + right-click to empty it.
  - A comparator reads its level.
- **Tornado Probe:** a weighted instrument barrel that no tornado can move. Put it in a tornado's path.
  - When the tornado passes over, the probe records the peak wind and its light turns red.
  - Right-click to read it. A comparator reads its peak wind too.
- **Storm Chaser's Logbook:** carry one and every tornado you see (with a clear line of sight, within 600 blocks) is logged: how many, the strongest, and the closest you've been. Your TIV intercepts, probe readings and balloon launches are logged too. Right-click to read it.
- New achievements: Chaser, Too Close for Comfort, Violent, Veteran Chaser, Intercept, Data in the Core, Turtle in the Vortex and Upper Air.

---

## 9. Skyward: storm creatures, gear and gadgets

### Creatures
- **Squall Serpent** (Tempest Realm: Cloudsea Expanse, Aurora Spires, Galewood Highlands; rare, never more than one nearby): a horned sky-dragon three blocks long that swims through the air.
  - 160 health, 8 armour, with a purple boss bar.
  - It circles its prey, lunges to bite, and rears back to spit three balls of lightning.
  - When hurt it sometimes lashes the air into a gust that throws everything back.
  - Drops 3–6 **Serpent Scales** and its **Squall Heart**.
- **Hailstone Golem** (Overworld thunderstorms): a two-and-a-half-block giant of packed hail around a glowing core.
  - Forms out under the open sky during a thunderstorm (5% a minute for each player outside, at most two nearby).
  - 50 health, 10 armour. It smashes what it reaches and hurls fistfuls of hailstones at what it can't.
  - Out of the storm it melts: sunshine and warm biomes wear it down, and fire hurts it twice as much.
  - Drops a **Glacial Core** (half the time), Packed Ice and Snowballs.
- **Aurora Moth** (Elderstorm Grove, Galewood Highlands, Aurora Spires, the coasts; at night): a violet moth with sheer aurora wings that flutters round lanterns and torches. It sheds **Aurora Dust** every few minutes.
- **Stormfly:** a firefly whose tail glows with stored lightning. Swarms appear in the minutes before a storm and while it lasts, then fade away. Use a **glass bottle** on one to catch it.

### Gear and gadgets
| Item | Recipe | What it does |
|---|---|---|
| Gale Blade | Serpent Scale, Squall Heart, Breeze Rod | A sword: every hit throws the target into the air. Use it to ride an updraft yourself (5 s cooldown). Repaired with Serpent Scales. |
| Stormchaser Goggles | Leather, light-blue glass panes, copper | A heads-up readout of the nearest tornado (or wind and pressure when there isn't one). |
| Stormchaser Jacket | Leather, orange dye, glow ink sac | Braced against the wind: tornadoes and downbursts push you about a third as hard. |
| Stormchaser Trousers and Boots | Leather, orange dye / slime balls | Speed I while it rains on you. |
| Full Stormchaser suit | — | Lightning can't hurt you. |
| Bottled Lightning | Hold an empty bottle within 8 blocks of a lightning strike | Throw it and the bolt comes down where it lands. |
| Cloud Seeder | Firework rocket, Bottled Rain, snowball (makes 2) | Fire it under open sky to bring five minutes of rain. |
| Supercell Seeder | Cloud Seeder, Bottled Lightning, gunpowder | Fire it under open sky to start a thunderstorm (with everything that comes with one). |
| Wind Turbine | White wool, Anemometer, iron ingot, redstone | Turns in the wind and gives a redstone signal for how hard it blows (about 1 per 4 mph, 15 in a gale). Needs open sky. Right-click to read the wind. |
| Storm Shelter Hatch | Iron ingots, orange dye (makes 2) | A heavy steel trapdoor for a storm cellar, as blast-proof as obsidian. |
| Stormfly Jar | Copper ingot, glass panes, Bottled Stormfly | A soft lamp (light 12) that stands or hangs. |
| Glacial Lamp | Packed Ice, Glacial Core (makes 4) | Light 15. |
| Aurora Glass | Glass, Aurora Dust (makes 8) | Glass with the aurora drifting across it (light 7). |
| Serpentscale Block | 4 Serpent Scales | A gleaming building block. |
| Chaser's Coffee | 2 cocoa beans, sugar, glass bottle | Speed, Haste and Night Vision for two minutes. |
| Snow Cone | 2 snowballs, sugar, sweet berries, paper (makes 2) | Fire Resistance for 30 seconds. |

**Weather Radio, upgraded:** carried, it now sounds an alert tone and broadcasts a **Tornado Emergency** when a tornado comes within 150 blocks, a **Severe Thunderstorm Warning** when thunder starts, a statement when rain moves in, the **all-clear**, and a wildlife advisory when a Thunderbird is about.

New achievements: This Is Not a Test, Rainmaker, Playing God, Suited Up, Lightning in a Bottle, Jar of Sparks, Thaw, Serpent of the Squall and Riding the Gale.

### Tooltips
Every Stormreach item now has a short description under its name. Hold **Shift** to see how to use it and its exact effects.

---

## 10. Thunderfront: signs of the storm

### Creatures
- **Rainfrog** (Brinewater Marsh, the Stormreach Coast, Elderstorm Grove): a blue-green frog with golden eyes.
  - In the minutes before rain the whole marsh starts croaking, throats puffing, louder and faster the closer the rain. Once it's raining they come out and hop about.
  - A Rainfrog chorus is a forecast you can hear. Breed them with Slimeballs.
- **Storm Petrel** (over oceans and beaches, while rain is due or falling): a small sooty seabird with a white rump that flutters low over the waves.
  - Flocks of 3–5 gather ahead of a storm and leave once it has passed.
  - Drops feathers and, a quarter of the time, an **Omen Feather**.
- **Dust Devil** (deserts, badlands and savannas, on clear days): a whirling column of sand that skitters around for a minute or so.
  - It lifts sand and loose items and spins anyone it crosses, but does no harm. Hit it and it scatters; rain or water breaks it up.
  - Use a **glass bottle** on one to catch it.

### Gear and gadgets
| Item | Recipe | What it does |
|---|---|---|
| Umbrella | 3 wool, 2 sticks | Held in either hand: hailstones hit it, not you, and you float down from a height with no fall damage. In a thunderstorm it can draw lightning onto you. |
| Omen Feather | Storm Petrels (25%) | Use it to read the sky: when rain comes or stops, and whether thunder is due. |
| Bottled Dust Devil | Use a glass bottle on a Dust Devil | Throw it: a dust devil stays where it lands for 10 seconds and sweeps every dropped item within 8 blocks to the middle. |
| Tesla Coil | Lightning rod, 2 copper ingots, Storm Shard, 3 iron ingots | Stores lightning: half a charge from each strike within 24 blocks or each Storm Cell, and a trickle in thunderstorms. While charged it zaps the nearest hostile mob in sight within 10 blocks (6 damage) once a second. A redstone signal holds its fire. Comparators read the charge. |
| Wind Chimes | String, 3 wooden slabs, 2 copper ingots | Hang them under a block, fence, chain or leaves. They ring more the harder the wind blows, and barely at all indoors. |

New achievements: Red Sky at Night, Whirlwind in a Bottle, It's Alive and Under the Weather.

Every Stormreach enchantment now has a description too, for mods that show enchantment descriptions.

---

## 11. Live Wire: storm power and captured water

### Power
Electricity is measured in **Storm Units (SU)**. Generators make it and **Copper Cable** carries it (it joins on all six sides). Generators send power to machines first, then to Storm Capacitors, and capacitors feed machines. Use a **Multimeter** on anything for a reading.

| Block | Recipe | Makes, stores or uses |
|---|---|---|
| Wind Turbine | (see Skyward) | Makes 2 SU a tick per level of wind, up to 30 in a gale. Open sky only. |
| Water Wheel | Planks, sticks, iron ingot | Makes 4 SU a tick per side (or bottom) with moving water against it, up to 16; moving Stormwater doubles it. |
| Storm Capacitor | Iron, copper, glass, a Storm Cell | Stores 100,000 SU. Lightning within 24 blocks adds 50,000 at once. |
| Copper Cable | 6 dried kelp, 3 copper ingots (makes 8) | Carries power. |
| Arc Lamp | Iron nuggets, glowstone, cable (makes 2) | Uses 1 SU a tick for light level 15. |
| Shock Plate | 2 copper ingots, cable | Uses 100 SU per shock: 4 damage to a hostile mob on it, up to twice a second. Players and animals are safe. |
| Electric Pump | Iron, cable, pistons, bucket | Uses 80 SU to lift 250 mB a second from the water under it into a tank beside or above it. Ponds, rivers and the sea never run dry. |
| Sprinkler | Iron nuggets, iron ingot, cable | Every 2 seconds uses 80 SU and 50 mB from a tank it sits on or beside to water a 9x9 area. |
| Tesla Coil | (see Thunderfront) | Now also charges from a power network (20 SU per point of charge). |

### Water
| Block | Recipe | What it does |
|---|---|---|
| Rain Barrel | Planks, a wooden slab | Holds 4 buckets. Under open sky it catches 25 mB of rain a second, or 60 mB of **Stormwater** in a thunderstorm. It drains into a tank below it. |
| Cistern | Stone bricks, a bucket | Holds 32 buckets. |

On either tank: use a bucket for a bucket of water (or Stormwater), a glass bottle for a water bottle, or pour a bucket in. Comparators read how full they are.

### JEI
With **Just Enough Items** installed (or EMI or REI, which load JEI plugins), every Stormreach crafting, smelting and brewing recipe shows up. Stormreach also adds four pages:
- **Storm Power:** what makes, stores, carries and uses SU, and how much.
- **Water Capture:** where captured water comes from and goes.
- **Catching and Gathering:** bottling Stormflies, lightning and Dust Devils; Storm Cells from a collector; what creatures give you.
- **Creature Drops:** every Stormreach mob's drops with counts and chances.

Every Stormreach item also has an information page with its description and how to use it.

New achievements: Live Wire, Every Drop Counts and Green Thumb.

---

## 12. The Stranger *(spoilers)*

You are not alone.

- After a while you may notice **someone standing at the edge of your vision**, 28–48 blocks away. It looks like you: your own skin, drained and storm-worn, with hollow eyes. Look straight at it, or walk toward it, and it's gone.
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

## 13. The Hollow Shrine *(spoilers)*

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

## 14. Cyrrus, the Hollow Storm *(spoilers)*

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

## 15. Achievements

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

## 16. Settings

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
- `wildlifeSpawning` (the 5.2 and 5.3 creatures), `thunderbirdChance` (percent chance per minute, per player out in a thunderstorm; default 6), `hailGolemChance` (the same for Hailstone Golems; default 5)
- `bossDifficulty`, `guardianDifficulty`
- every setting from earlier versions

---

## 17. Rarity and drop-rate tables

All numbers below are calculated directly from the mod's loot tables, worldgen files and code (by `tools/guide_tables.py`), so they match the game.

{{TABLES}}

---

## 18. Credits and licences

- **Cyrrus's voice:**
  - spoken by the **Piper** text-to-speech engine with the "Ryan (high)" voice model
  - processed into his corrupted voice for this mod
  - the voice model's training data is licensed **CC BY-NC-SA 4.0**, so these recordings are for non-commercial use, with attribution, under the same licence
  - you can re-record or replace any line: see `data/stormreach/cinematic/*.txt` and `tools/audio5.py`
- **Music and sounds** (Labyrinth layers, Cyrrus's theme, siren, tornado roar, heartbeat, whisper, screech): synthesised from scratch for Stormreach. No outside samples.
- **Code and textures:** MIT, like the rest of Stormreach.
