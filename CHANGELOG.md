# Stormreach patch notes

## 5.1.0 (in progress: the overhaul)
Built in stages. This section grows as each stage lands.

### Stage 4: backpacks get their own slot
- **A backpack slot** in the inventory, just above the offhand slot. It's separate from the chestplate, so you can wear armour and a backpack together.
  - Shift-click a backpack to put it on or take it off, or sneak + use with one in hand.
  - The Open Backpack key (B) opens the one you're wearing first.
- **Worn backpacks show on your back**, sitting over a chestplate instead of clipping into it. Every tier looks different:
  - its own colours and trim on every tier
  - side pockets from Storm Crystal up and a bedroll from Aurorite up
  - a glowing crystal on the Maelstrom pack
  - the Storm Chaser's Pack has its own look
- Contents stay in the backpack whether it's worn, carried or dropped. It saves with your character, Soulbound keeps a worn pack too, and without Soulbound it drops on death like armour.
- In the Creative inventory tab the slot is hidden; sneak + use or the B key still work there.

### Stage 3: rain, hail, downbursts and storm clouds
- **Real hailstones** in six sizes: pea, marble, quarter, golf ball, baseball and softball.
  - Each falls at its real terminal speed and is drawn as a lumpy ball of ice sized to its class.
  - Damage grows steeply with size. A pea stings, a golf ball takes a few hearts, a softball can kill. Helmets and armour help.
  - Quarter-size and up flattens crops and flowers, golf balls smash glass panes, and baseballs break glass blocks and shred leaves.
  - Only strong storms make big hail.
- **Hail on the ground** piles up to four layers and **melts on a timer**: small hail in about 30 seconds, piles with big stones in under a minute. It melts in every biome, so it can't build up into lag.
- **Hailstorms happen naturally** in thunderstorms: often in the Thunderspire Highlands, now and then anywhere else. The total number of stones in the air is capped. The old "hail" (particles that hurt you if you had no helmet) is gone.
- **Rain builds up into puddles.** After a couple of minutes of steady rain, puddles start forming on open, level ground; hollows fill first.
  - They form faster in a downpour and grow larger the longer it rains.
  - They dry up after the rain stops (faster in hot places).
  - Walking through them splashes, and they put out burning mobs and players.
  - Turn them off with `puddles=false`.
- **Downbursts, wet and dry:**
  - A wet downburst comes down in a curtain of heavy rain, and its gust front pushes a wall of spray outward.
  - A dry one falls from wisps of rain that evaporate on the way down and shows only as a ring of dust racing over the ground.
  - Winds peak just behind the expanding front.
  - They now happen naturally: wet in thunderstorms, dry in hot, dry climates.
- **Storm clouds you can watch develop.** During thunderstorms, a few storm clouds grow around you:
  - a low cumulus swells into a towering cloud, shoots up and spreads an anvil, then slumps and fades
  - supercells get a darker, bigger one overhead
  - they drift with the steering wind and collapse when the weather clears or is dispelled
  - turn them off with `stormClouds=false`
- Dispel Weather also clears hailstones and storm clouds.

### Stage 2: tornadoes and waterspouts rebuilt
- **They come down from the sky.** A new tornado starts as a bulge under the cloud base and lowers a pointed funnel until it reaches the ground. A dust whirl (or spray over water) spins up underneath first. It only damages blocks and pulls things in once it touches down, and it lifts back into the cloud when it ropes out or is dispelled.
- **New look**, drawn as layered, turbulent meshes instead of stacked boxes:
  - a lumpy condensation funnel with a looser outer sheath, spinning at a rate that fits its width (ropes whirl, wedges turn slowly)
  - a tall debris cloud at the base (dusty for landspouts, white spray for waterspouts)
  - a cloud base for the funnel to hang from, with a lowered, rotating wall cloud under supercells
  - suction vortices orbiting inside multiple-vortex tornadoes
  - ropes and needles snake about; each shape (rope, needle, drill bit, cone, stovepipe, wedge) keeps its own profile
- **Violent tornadoes are violent.** Stronger, wider tornadoes check up to four times as many blocks. They strip roofs first, then tear into the walls below. An EF5 levels most things in its core.
- **Debris is picked up, carried and thrown.** Blocks spiral up around the core, ride the vortex for a while (longer in stronger tornadoes), then fly out along the rotation. Anything they hit takes damage.
- **Wind forces on players and mobs** depend on the wind where you stand, so they fall off with distance from the funnel:
  - EF0 winds shove you around.
  - **EF1 winds pull you toward the funnel.**
  - **EF2 winds lift you** a few blocks.
  - Stronger tornadoes carry you higher (an EF5 up to about 45 blocks) and throw you out.
  - Heavier mobs and armour resist, and sneaking helps you brace. Fall damage counts from where you're dropped.
- **Tornado Director** (Creative only, in the Weather Events tab): point at the ground and use it to send the nearest tornado there. It curves toward the spot and stops on it. Sneak and use to let it wander again.
- **See inside tornadoes** (Creative and Spectator only): press **O** to toggle. Tornadoes turn nearly transparent, the debris and particles thin out, and a red band marks the core, where the winds are strongest. The setting is saved as `creativeSeeThroughTornadoes` in `config/stormreach.properties`.
- **Waterspouts stay over water.** They only form over open water, steer away from the shore, and fall apart if pushed onto land. They don't scour the lakebed. The waterspout spawner tells you if there's no water.
- Shape, formation and vortex structure stay independent. The EF rating still comes from the damage survey.

### Stage 1: crash and gameplay fixes
- **Sovereign Altar crash fixed.** The ritual could aim a lightning strike at a spot with no ground under it (an altar over the void or over open air), which crashed the server. Strikes now step inward to find ground and always have a target.
- **Lightning Staff no longer burns anything.** Its bolts still hit as hard, but they light no fires and set nothing (you, mobs, the ground) on fire. No Fire Resistance is applied or shown; only the staff's own bolts are affected, so natural lightning works as before.
- **Dispel Weather works.** It now:
  - ends the rain and thunder for 10–20 minutes (and with them the Gale and Thunderhead)
  - makes live tornadoes rope out and cancels the ones still forming
  - breaks up nearby storm fronts, Squalls and ball lightning
  - shows a ring of clearing cloud with a chime and a message, and says so if the sky is already clear
- **Rusted Locker rework.**
  - You stand inside, facing out, and look out through the grate's slats.
  - You're held in place: walking, jumping and sneaking don't take you out. **Press Use to step out.**
  - Only one player fits in a locker, and other players see the locker, not you.
  - The purple-and-black squares were missing icons for the Hidden and Labyrinth effects. Both now have icons.
- **Creative Realm Pass** (new, Creative only): use it to go to the Tempest Realm and back, for testing. It has no recipe and drops from nothing, and it does nothing outside Creative mode.

## 5.0.0 (the Hollow Storm update)
**Real tornadoes, weather instruments, backpacks, a buried labyrinth, a new realm forest, a story told in glimpses, and a voiced final boss.** The full player guide, with every rarity and drop-rate table, is in **GUIDE.md**.

### Tornadoes and severe weather
- **Tornadoes**, modelled on real ones:
  - They form from Overworld thunderstorms (supercells), as **landspouts** in warm rain and as **waterspouts** over water.
  - **12 forms**:
    - 7 shapes: rope, needle, drill-bit, cone, stovepipe, wedge and large wedge
    - multiple-vortex tornadoes
    - satellite and companion tornadoes
    - landspouts and waterspouts
  - **EF0–EF5 ratings** follow the official 3-second-gust ranges, and each tornado gets its own peak wind inside its range.
  - The rating is decided by a **damage survey** when the tornado lifts, so open-field tornadoes can rate low.
- **Wind field:** calm centre and a peak at the core's edge, with inflow and updraft. Subvortices and the tornado's own motion add to the winds on one side.
- **Movement and lifecycle:** tornadoes travel about 13 m/s with a wobble, and turn left as they decay. They grow, peak, shrink and rope out.
- **Blocks and debris:** every block has a failure wind (foliage 45 mph up to metal 205 mph). Exposed, unsupported or thin blocks fail first and sheltered blocks hold.
  - Light loose blocks become flying debris.
  - Players and mobs are dragged, lifted, thrown and hurt.
- **Downbursts, hailstorms, ball-lightning swarms** and **supercells with wall clouds**.
- Tornadoes **roar**, and fit within limits on simulation distance, block checks per tick, airborne debris and live tornado count. Everything runs on the server.

### Meteorology (new creative tab)
- **New instruments:**
  - Anemometer: wind and Beaufort force, plus a redstone signal
  - Thermometer: temperature, dew point and humidity
  - Weather Station: everything plus a forecast
  - Storm Detector
  - **Doppler Radar:** shows rotation (gate-to-gate shear), motion, estimated rating, width and shape, plus a redstone signal
  - Tornado Siren: redstone-driven
  - **Weather Monitor:** shows the severe-weather outlook once linked to a station and a radar
- Handheld Storm Radar and Weather Radio, which also give tornado warnings.
- The Barometer and Wind Vane now give readouts too.

### Weather Events (new creative tab)
- 13 tornado spawners: natural odds, each shape, multiple-vortex, satellite, companion, landspout, waterspout. Sneak + use picks EF0–EF5 or random.
- Supercell, Downburst, Hailstorm, Ball Lightning Swarm and **Dispel**.

### Backpacks
- **7 tiers:** Diamond 27 → Obsidian 36 → Netherite 54 → Storm Crystal 72 → Voltite 90 → Aurorite 108 → Maelstrom 135 slots.
  - Craft the Diamond Backpack, then combine each tier with its **Upgrade Kit**. Each kit uses vanilla and Stormreach materials, and the upgrade keeps the contents and enchantments.
  - Plus the **Storm Chaser's Pack**, an achievement reward.
- **Backpack window:**
  - its own screen with pages, a Sort button and the **B** hotkey
  - no backpacks inside backpacks
  - the open pack is locked in place
  - contents spill out if the pack is destroyed
- **5 backpack enchantments:** Capacity I–III, Magnetism, Organization, Resilience, Soulbound (treasure).

### The Hollow Labyrinth (very rare Overworld structure)
- A 150×150 maze buried about 38 blocks down. It has four sections and dead ends, sparse loot, and three corner vaults holding the **Labyrinth Keys**.
- The keys open a sealed treasure chamber holding the **Heart of the Labyrinth**.
- **The Hollow Keeper** guards the maze and never leaves it. It hunts by sight and sound, and searches lockers.
- **Rusted Lockers** let you hide (Hidden effect, heartbeat).
  - Moving gives you away.
  - The Keeper is likelier to find you if it saw you get in.
- **Adaptive music:** three original layers (calm, tense and chase) that blend as the Keeper gets closer.

### Elderstorm Grove (new Tempest Realm biome)
- Giant 2×2 Elderstorm trees and a full purple wood set.
- Glowing **Stormfruit** that ripens on the trees and can be picked again and again. Make it into Gilded Stormfruit or Stormfruit Preserve.
- Thunderbloom and Sparkfern.
- Two new passive mobs:
  - **Glimmerstags**: deer with glowing antlers
  - **Thistlehops**: hopping puffballs

### The story
- **The Stranger:** a figure at the edge of your vision that vanishes when looked at.
  - It sends signs: footsteps, whispers, smoke, snuffed campfires, cairns.
  - It leaves Hollow Remnants behind.
- **Five journal pages** spread across the Sky Vaults, Tempest Spires, the Labyrinth and the Shrine. Together they rebind the **Weathered Journal**.
- **The Hollow Shrine** (legendary-rare, floating in the Tempest Realm) has a three-stage ritual:
  1. four relics from four systems on riddle pedestals
  2. lightning on four conduits in their notched order
  3. the journal on the altar

### Cyrrus, the Hollow Storm (final boss)
- A **fully voiced cinematic intro**: he rises from the ground, grows larger with each line and corrupts into his true form.
  - Audio, subtitles and actions are synced and work in multiplayer.
  - Text (lang), audio (sounds.json + .ogg) and timing (`data/stormreach/cinematic/*.txt`) are kept apart, so lines can be added or re-recorded without code.
- **Five telegraphed phases:**
  1. lightning
  2. rain and visibility
  3. wind, with crosswinds that deflect projectiles
  4. summons
  5. the whole storm
- A voiced line opens each phase, and he has last words when he falls.
- **Drops:** Maelstrom Cores, the Stormcrown, the Weathervane Scepter. The **Weatherbreaker** is crafted from a core.

### Achievements
- **49 new achievements** across the main tab and three new tabs: Meteorology, the Hollow Labyrinth and the Chronicle.
  - Tiered chains, hidden achievements, and an endgame challenge.
  - Vanilla tie-ins: lightning, Channeling, lightning rods.
- **Exclusive rewards with real abilities**, each given by exactly one achievement:
  - Lightning Charm (Grounded in offhand)
  - Storm Chaser's Cap (tornado warnings)
  - Chaser's Badge (half tornado damage)
  - Storm Chaser's Pack
  - four trophies

### Changes
- **Creative tabs reorganised:** Building, Nature, Tools & Combat, Food, Materials, Spawn Eggs, Weather Events and Meteorology. Storm Blocks and Storm Mobs are unchanged.
- **Mod Menu now shows performance settings only**, on two pages (Tornadoes, Effects & Load). Gameplay options moved to `config/stormreach.properties`.
- The Tempest Sovereign now leaves a Sovereign Trophy (via its achievement).

### Testing
- 36 JUnit tests (`./gradlew test`) cover:
  - tornado physics, EF scale, climatology and materials
  - maze, hiding and boss logic
  - the dialogue/audio/subtitle contract
- 7 in-game GameTests (`./gradlew runGametest`, also run in GitHub Actions) load the full datapack and exercise:
  - backpacks
  - tornadoes
  - lockers
  - the shrine ritual
  - Stormfruit
- `tools/validate.py` checks every model, texture, lang key, loot table, recipe, tag, sound, script, advancement and worldgen reference.

## 4.0.0 (the Tempest Realm update)
**A whole new dimension above the storm, and an elaborate road to reach it.**

### The road to the Tempest Realm
1. **Find a Tempest Spire.** It's a rare ruined tower of purple Tempest Bricks in Stormreach Coast, Thunderspire Highlands and Squallwind Bluffs. `/locate structure stormreach:tempest_spire` finds one, and `/locate biome stormreach:stormreach_coast` finds the coast. Its floors hold loot chests, and the Sovereign Altar sits on its roof.
2. **Wake the Tempest Sovereign.** There are two ways:
   - **Naturally:** during a thunderstorm, the altar may stir by itself when a player is within 32 blocks. It's rare and can be changed in Mod Menu.
   - **With a Tempest Sigil:** this needs a Tempest Core (from the Stormwarden), a Thunderhead Horn, a Stormheart, 2 Blocks of Storm Crystal and 4 Storm Cells. Using it on the altar forces a thunderstorm and starts a 60-second ritual: lightning walks around the spire, faster and closer, until the Sovereign descends.
3. **Defeat the Tempest Sovereign**, a brand-new three-phase flying boss with a purple boss bar.
   - **Phase 1:** it calls lightning on you and launches Ball Lightning orbs.
   - **Phase 2 (below 60%):** Squalls join it, lightning strikes in rings, and gust shockwaves throw you back.
   - **Phase 3 (below 25%):** it's enraged, faster and stronger. It dives at you and opens a vortex that drags you in before blasting you away.
   - The storm heals it, and it won't stray far from its spire. It drops exactly **one Tempest Eye**.
4. **Collect 4 Tempest Eyes** (you can set it anywhere from 4 to 16). That means four Sovereign fights.
5. **Build a Stormgate.**
   - Lay a flat 5×5 ring of 16 **Stormgate Frames** (crafted from Fulgurite, Charged Stormstone and Storm Crystal) around a 3×3 pool of **stormwater sources**.
   - Set the Eyes into the frames. Right-clicking a frame tells you what the gate still needs.
6. **Call lightning onto the pool** (a natural storm, a Tempest Staff…). The stormwater tears open into a crackling **Stormgate**.
7. **Step in.**
   - You arrive in the **Tempest Realm** at the same coordinates.
   - If there's no gate nearby, a return gate is built for you (on a sky platform if you arrive over the void).
   - Breaking any frame collapses the gate, and an Eye in a broken frame is lost.

### The Tempest Realm
- A storm-sky dimension of **floating islands** above a sea of clouds. Distant thunder and real lightning strike the islands.
- **Falling off the bottom** drops you back into the Overworld sky with Slow Falling. Make sure your gear is safe.
- **4 biomes:**
  - Galewood Highlands: forests
  - Static Fields: open meadows of Skyblooms and Stormcaps
  - Cloudsea Expanse: sparse islands lost in clouds
  - Aurora Spires: purple-lit peaks full of Aurorite and glowing Ion spires
- **Sky Vaults:** floating stone ruins high in the sky with loot (Voltite, Aurorite, Ion Shards, Cloudstep Boots, saddles, Galewood saplings…). `/locate structure stormreach:sky_vault` finds one.

### New blocks (47)
- **Tempest Spire:** Tempest Bricks (cracked, chiseled, stairs, slab, wall). You can also make them from Stormstone Bricks and an amethyst shard. The Sovereign Altar.
- **Stormgate:** Stormgate Frame and the Stormgate portal.
- **Skyshale:** the realm's stone, plus Cobbled, Polished, Bricks, Chiseled, stairs, slabs and walls.
- **Skysoil** and **Static Grass:** cyan turf that spreads onto Skysoil.
- **Clouds** (you fall into them, not onto them):
  - Nimbus Cloud: you sink slowly through it and take no fall damage
  - Thundercloud: flings you skyward with a crack of static
  - Gale Cloud: carries you along with the wind
- **Galewood:** a pale, wind-streaked wood. Log, stripped log, planks, leaves (they drop Cloudberries), sapling, stairs, slab, fence, gate, door, trapdoor, button and pressure plate.
- **Ores:** Voltite Ore, Aurorite Ore and Ion Ore, plus Blocks of Voltite and Aurorite (both work as beacon bases), and the Ion Lamp.
- **Plants:** Skybloom, Stormcap and Gale Grass.

### New items
- Tempest Sigil and Tempest Eye
- Raw Voltite and Voltite Ingot, Aurorite, Ion Shards (eat one to heal instantly), Stormhawk Feathers
- **Voltite gear** (better than iron):
  - sword, pickaxe, axe, shovel, hoe and armor
  - Voltite swords arc lightning to a second nearby enemy
  - a full set gives Speed and Haste in the realm
- **Aurorite gear** (better than diamond, fireproof):
  - Aurorite swords arc harder
  - a full set makes you immune to lightning and fall damage
- **Cloudstep Boots:** no fall damage; sneak in mid-air to glide.
- **Food:** Cloudberries (Slow Falling), Cloudberry Tart (Jump Boost), Raw and Cooked Skyray, Storm Nectar (Regeneration and Slow Falling), Gale Bread
- A new creative tab, **Stormreach: Tempest Realm**

### New mobs
- **Tempest Sovereign:** the boss described above
- **Skyray:** a gentle flying ray. Put a saddle on it and ride it; it flies wherever you look (W to fly, A/D to drift). Breed with Cloudberries.
- **Puffling:** a little cloud sheep. Shear it for Nimbus Clouds. While fluffy it floats down gently, and the fluff grows back.
- **Static Wisp:** a hostile spark that zaps you up close and arcs at you from range
- **Cyclone:** a hostile whirlwind that flings you into the air
- **Stormhawk:** a hostile raptor that circles high, then dives. It drops Stormhawk Feathers.

### Advancements
- A new branch after the Stormwarden: Where the Storm Is Crowned, A Royal Summons, Regicide, Eyes of the Storm, Above the Storm, Ray Rider, Bottled Current, Northern Lights, Dressed for the Storm, Castles in the Air, Head in the Clouds, and a hidden one.

### Settings (new "Realm" page in Mod Menu)
- Sovereign Answers Storms, Natural Summon Chance, Sigil Ritual Length, Sovereign Health, Eyes to Open a Stormgate, Build Return Gates, Fall Back to the Overworld, Realm Lightning, Realm Mob Spawning, Cloud Effects, Skyray Flight Speed.

## 3.0.0 (the Storm Everything update)
**Every block and every mob now has a Storm version**
- **978 Storm blocks**: every vanilla block from every dimension that can have a copy gets one: stone, ores, every wood, wool, concrete, terracotta, glass, plants, crops, Nether blocks, End blocks, deepslate, sculk, copper, redstone parts, rails, doors, slabs, stairs and the rest.
  - They act exactly like the original (same hardness, tool, sounds, shape and behaviour) and are drawn in a cold storm-blue tint.
  - They drop their own Storm versions, join the same tags as the original, and so work in recipes that accept a tag (any planks, any logs, any wool…).
  - They have their own creative tab, **Stormreach: Storm Blocks**.
  - Not copied: blocks that hold an inventory or block entity (chests, furnaces, signs, beds, banners, heads, shulker boxes, spawners…), portals, fire, cauldrons, flower pots, air and technical blocks.
- **Storm mobs for every mob, bosses included**: Storm Zombie, Storm Cow, Storm Enderman, Storm Piglin, Storm Warden, Storm Wither, Storm Elder Guardian and every other living mob.
  - They look like the original with a storm-blue tint, have 25% more health, can't be hurt by lightning, give off sparks, get Speed in the rain and have a 1-in-4 chance to shock what they hit (extra damage and Slowness).
  - **Storm bosses** call lightning down on their target every few seconds and summon Squalls once they drop below half health.
  - Each drops its normal loot plus Storm Shards when a player kills it. Each has a tinted spawn egg in the new **Stormreach: Storm Mobs** tab.
  - The Ender Dragon gets a Storm version, but the Stormblight never converts the real one, so the End fight still works.
- **The Stormblight can now take over everything**: anything without a hand-made Stormreach counterpart becomes its Storm version, in any dimension. A Stormheart in the Nether paints the land **Storm Wastes**, and one in the End paints it **Storm Void**.

**Stormwater**
- A new storm-charged blue water. It flows and can be bucketed like water, glows faintly, and counts as water for other mods.
- Swimming in it gives Static Charge and Dolphin's Grace, but it shocks you every two seconds unless you are Grounded or wearing full Stormshard armor.
- Lightning hitting stormwater electrifies it and hurts every unprotected creature in it within 10 blocks.
- In *Unlimited* spread mode it slowly turns the water next to it into more stormwater.
- The Stormblight turns water into stormwater.

**Stormlava**
- Lava taken over by the storm: bright blue liquid lightning with its own animated texture. It burns like lava and also deals lightning damage and makes you glow.
- Lightning striking it can spark Ball Lightning out of it.
- When water meets it, it hardens into storm stone instead of cobblestone or obsidian: flowing stormlava becomes Stormstone, a stormlava source becomes Charged Stormstone, and a source hit by stormwater becomes Storm Crystal Ore. That gives you a renewable crystal generator.
- The Stormblight turns lava into stormlava.

**Nether and End storms**
- New biomes **Storm Wastes** (Nether) and **Storm Void** (End), created only by the Stormblight. They have a permanent dry storm: sparks in the air, lightning, Ball Lightning and Squalls.

**New settings (Stormblight page in Mod Menu)**
- Storm Every Block, Storm Every Mob, Storm Mob Powers, Stormwater Shocks, Nether & End Storms. All are on by default.

**Also**
- New advancements: Charged Waters, Liquid Lightning and The Withering Storm (beat a Storm Wither).
- Storm block and mob names, models and loot are made by `tools/gendata6.py`. Rerun it after a Minecraft update.

## 2.1.0 (the Stormblight update)
**The Stormblight: storm energy that spreads and takes over**
- **Stormheart** block (crafted, or found in Storm Shrines).
  - Wake it with a Storm Cell, a lightning strike or a redstone signal.
  - An awake heart pulses outward in a growing sphere and turns everything into its Stormreach version. Facing, axis, slab type and waterlogging are kept, and doors convert both halves at once.
  - When it reaches full size, the area becomes Stormreach Coast, so storms come there.
  - Use Salt on it to calm it again. Right-click it with an empty hand to see how far it has spread.
- **Blocks it converts:**
  - stone, cobblestone, andesite, diorite, granite, tuff → Stormstone
  - polished stones → Polished Stormstone, stone bricks → Stormstone Bricks (cracked and chiseled too), stone stairs, slabs and walls → stormstone ones
  - any overworld tree's logs, planks, leaves, saplings, stairs, slabs, fences, gates, doors, trapdoors, buttons and pressure plates → Stormpine
  - grass, podzol and mycelium → the new **Stormscar Turf**
  - flowers → Sea Thrift or Sea Rocket, grass and ferns → Saltgrass, sweet berry bushes → Stormberry Shrubs
  - glass and glass panes → Stormglass, lanterns → Fulgurite Lanterns, glowstone and sea lanterns → Storm Lanterns
  - amethyst → Storm Crystal, coal and copper ore → Storm Crystal Ore, and some sand → Fulgurite
- **Mobs it converts:**
  - chickens and parrots → Storm Gulls
  - rabbits and turtles → Cliff Crabs
  - pigs → Harbor Seals
  - zombies, husks and drowned → Brine Wraiths
  - creepers → Ball Lightning
  - spiders → Thunder Crabs
  - phantoms, vexes and breezes → Squalls
  - Cliff Crabs → Thunder Crabs during thunderstorms
  - Named mobs and tamed pets are never touched.
- **Storm Front**: use a Thunderhead Horn on a Stormheart to release a travelling black storm cloud. It drifts with the wind, rains, throws lightning and converts the land and mobs beneath it for a few minutes.
- **Stormscar Turf**: grass that plants grow on. In *Unlimited* spread mode it keeps spreading by itself.
- New **Stormblight** page in Mod Menu:
  - spread mode: Off, Contained (the default) or Unlimited
  - heart radius (8–64) and spread speed
  - mob conversion, biome change, waking on placement, and Storm Front on/off and lifetime

**More content**
- **Bottled Sunshine** (clears the weather), **Bottled Rain** (starts a Gale) and **Bottled Thunder** (a short Thunderhead).
- **Storm Shrines** in the Thunderspire Highlands and Squallwind Bluffs, with a dormant Stormheart on an altar, a Storm Collector and a loot chest.
- 3 new paintings: Storm Lighthouse, Squall at Sea and The Stormwarden.
- 3 new advancements: Heart of the Storm, Scarred Earth and Weather Maker.

## 2.0.0 (the big storm update, Minecraft 26.3)
**New biomes**
- **Squallwind Bluffs**: stormy sea cliffs (takes half of the Stony Shores). Gulls, seals, sea rocket, rich stormstone.
- **Thunderspire Highlands**: windswept crystal peaks (takes half of the Windswept Gravelly Hills). Stormpine forests, charged stormstone spires, extra storm crystal ore, ash in the air, and hail during Thunderheads.
- **Brinewater Marsh**: salt flats and driftwood (takes half of the Swamps). Salt crystals, saltgrass, Brine Wraiths at night.
- Each biome can be switched off in settings. Vanilla biomes still generate, just less often.

**Weather**
- Hail in the Thunderspire Highlands (wear a helmet!), Ball Lightning that drifts toward you and bursts, and Waterspouts over the sea that lift and fling anything nearby.
- Any lightning (vanilla included) now turns Stormstone into Storm Crystal Ore or Charged Stormstone, and fuses sand into Fulgurite.
- New blocks: the **Wind Vane** turns with the storm wind, and the **Storm Collector** charges in rain and thunder and gives you Storm Cells (comparators read its charge).
- `/stormreach forecast` shows the weather and wind direction.

**Mobs**
- **Harbor Seal** (peaceful, fast swimmer, breeds with fish)
- **Brine Wraith** (drowned sailor, causes Hunger, drops salt)
- **Thunder Crab** (lightning turns a Cliff Crab into one)
- **Ball Lightning** and **Waterspout** storm hazards
- **The Stormwarden** boss: use a Storm Cell on a Block of Storm Crystal in a thunderstorm. It has a boss bar, lightning strikes, gust shockwaves and summons Squalls. It drops the Tempest Core.
- Storm Gulls now lay Gull Eggs.

**Blocks**
- Stormstone stairs, slab and wall, and the same for Polished Stormstone
- Chiseled Stormstone, Cracked Stormstone Bricks, Charged Stormstone and a Stormstone Brick Wall
- A full Driftwood set: fence, gate, door, trapdoor, button and pressure plate
- A new **Stormpine** tree with log, stripped log, planks, leaves, sapling, stairs, slab, fence, gate, door, trapdoor, button and pressure plate
- Stormglass Pane, Fulgurite, Fulgurite Lantern, Block of Salt and Salt Crystal
- Sea Rocket and Saltgrass plants

**Gear**
- **Stormshard armor**: a full set makes you immune to lightning and gives Speed in thunderstorms
- **Gale Wings**: a glider like the elytra, and storm gusts boost it
- **Squall in a Bottle**: a throwable gust that can launch you
- **Stormbreaker**: a hammer made from the Tempest Core that sends out a thunderclap shockwave

**Enchantments**
- **Conductive** (swords): shocks what you hit; always works in rain
- **Galeforce** (boots): you're faster outdoors in the rain
- **Grounded** (armor): protects against lightning

**Effects and potions**
- Static Charge (shocks anyone who hits you), brewed with a Storm Shard
- Grounded (immune to lightning and wind), brewed with a Fulgurite Shard
- Both have long versions, plus splash and lingering potions

**Food**
- Gull Egg / Fried Gull Egg, Crab Cakes, Stormberry Jam, Stormberry Juice, Seaweed Salad, Salted Cod, Sailor's Stew, Rock Candy and Tempest Tonic
- **Stormfin**: a fish you can only catch while it rains

**Structures**
- **Storm Lighthouses** on the coasts and bluffs
- **Wrecked Skiffs** on beaches and marshes
- Both have their own loot chests

**Also**
- 10 new advancements, recipe book unlocks for every recipe, and the Mod Menu screen now has 4 pages with every new option.

**Fixes**
- Biome features are now sorted in an order that always agrees with vanilla. This prevents a possible "feature order cycle" crash when creating a world.

## 1.0.0 (first release, Minecraft 26.3)
- New biome: **Stormreach Coast**, with three frequency settings.
- New weather on the coast: **Gale** (rain) and **Thunderhead** (thunder), with wind gusts, sea spray, nearby lightning and Squall spawns.
- Lightning can turn Stormstone into Storm Crystal Ore.
- New mobs: Cliff Crab, Storm Gull and Squall.
- 16 new blocks, including the Storm Barometer redstone weather sensor.
- New gear: Stormshard tools, Tempest Staff and Thunderhead Horn.
- 6 new foods.
- Stormreach advancement tab and recipe book unlocks.
- Mod Menu config screen (3 pages) plus `config/stormreach.properties`.
- `/stormreach` command.
