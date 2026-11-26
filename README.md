# Stormreach 6.0 (Fabric, Minecraft 26.3)

A storm-swept coast where everything is connected: storms grow crystals, crystals become gear, gear calls storms.

## What's in it

The full list of everything added in 2.0–6.0 is in **CHANGELOG.md**, and **GUIDE.md** is the player guide for 6.0 (tornadoes, instruments, backpacks, the Hollow Labyrinth, Elderstorm Grove, the story and Cyrrus), with rarity and drop-rate tables. That includes the spreading Stormblight, Storm versions of every block and mob (bosses included), Stormwater, Stormlava, the Nether and End storm biomes, and in 4.0 a whole sky dimension, the **Tempest Realm**. You reach it by beating the Tempest Sovereign for its Eyes and opening a Stormgate with lightning. The original 1.0 content is described below.


**Biome: Stormreach Coast.** A grey-skied, windswept shore that replaces part of the cool, wet beaches in new chunks. It has stormstone cliffs and boulders, fallen driftwood, sea thrift flowers, stormberry shrubs and storm crystal ore. It counts as a beach for other mods and for buried treasure.

**Weather**
- **Gale** (whenever it rains): wind gusts push you around outside and sea spray blows past. Sneak to brace yourself.
- **Thunderhead** (whenever it thunders): stronger wind, frequent lightning nearby and hostile Squalls forming in the clouds. Lightning that hits Stormstone can turn it into Storm Crystal Ore, which is how crystals renew.

**Mobs**
- **Cliff Crab**: scuttles along the shore. Breed it with Stormberries. Drops Crab Claws.
- **Storm Gull**: flies over the coast. Breed it with fish. Drops Gull Feathers.
- **Squall**: a living gust that only exists inside a Thunderhead. It rams you and knocks you back, and lightning heals it. Drops Storm Shards and blows apart when the storm ends.

**Blocks**: Stormstone, Polished Stormstone, Stormstone Bricks (+ stairs and slab), Driftwood Log, Planks (+ stairs and slab), Stormglass, Storm Crystal Ore, Block of Storm Crystal, Storm Lantern, Storm Barometer (redstone signal: 0 clear, 7 rain, 15 thunder), Sea Thrift and Stormberry Shrub.

**Items**: Storm Shard, Gull Feather, the Stormshard sword, pickaxe, axe and shovel (in a thunderstorm the sword can call lightning on what you hit), the Tempest Staff (strikes lightning where you aim, with no durability cost during storms), the Thunderhead Horn (summons a thunderstorm) and spawn eggs.

**Food**: Crab Claw, Cooked Crab Claw, Stormberries (Speed), Stormberry Pie, Storm Chowder (Resistance) and Thrift Tea (Regeneration).

There are also advancements in their own Stormreach tab, and every recipe unlocks in the recipe book.

## Settings (Mod Menu)
With [Mod Menu](https://modrinth.com/mod/modmenu) installed, go to **Mods → Stormreach → Configure**. Since 5.0 it only holds **performance** settings (Tornadoes; Effects & Load), each with a tooltip. Gameplay options are in `config/stormreach.properties`, which you can edit by hand (see GUIDE.md, section 17).

## Commands
Anyone:
- `/stormreach forecast`: show the weather and which way the wind is blowing
- `/stormreach status`: show the current weather and whether you're in a storm zone

Operators only:
- `/stormreach thunderhead [minutes]`: start a Thunderhead
- `/stormreach calm`: clear the weather
- `/stormreach reload`: re-read the config file

## Multiplayer
Install Stormreach on the server and on every player's client. The server's own `config/stormreach.properties` decides gameplay; each player's Mod Menu settings only change their own visuals, and each player picks their own units.

## The Tempest Realm (4.0)
1. Find a **Tempest Spire** (`/locate structure stormreach:tempest_spire`) in the Stormreach biomes.
2. During a thunderstorm the Sovereign Altar on its roof may wake the **Tempest Sovereign**. Or you can craft a **Tempest Sigil** and use it on the altar to force a storm and a 60-second ritual.
3. Beat the Sovereign for one **Tempest Eye**. You need 4.
4. Build a flat 5×5 ring of 16 **Stormgate Frames** around a 3×3 pool of stormwater, then set the Eyes into the frames.
5. Strike the pool with lightning and step into the **Stormgate**.

In the realm you'll find floating islands, clouds you fall into, Galewood forests, Voltite and Aurorite gear, rideable Skyrays and Sky Vault ruins. Fall off the bottom and you land back in the Overworld sky.

## The Stormblight (spreading)
Wake a **Stormheart** with a Storm Cell, lightning or redstone and it converts the land and mobs around it into Stormreach ones, up to the radius you set. It works in every dimension. Anything without a hand-made counterpart becomes its Storm version (Storm Stone, Storm Zombie, Storm Wither…). Water becomes Stormwater and lava becomes Stormlava. Calm it with Salt. A Thunderhead Horn on a Stormheart releases a travelling **Storm Front**. By default the spreading is *Contained* (only Stormhearts convert). Set it to *Unlimited* in Mod Menu if you want Stormscar Turf to keep spreading across the world by itself, or *Off* to disable it.

## Compatibility
- It only needs Fabric Loader and Fabric API. Mod Menu is optional.
- It doesn't overwrite any vanilla files. Storm blocks are added to vanilla tags as optional entries, so tags never break.
- Storm counterparts are only made for vanilla blocks and mobs. Other mods' blocks and mobs are left alone. The biome takes only some ordinary beaches, so other worldgen mods keep their biomes.
- Items and blocks are added to shared tags (`c:ores`, `c:gems`, `c:foods`, `minecraft:logs`, `minecraft:planks`, and so on), so other mods' recipes and machines recognise them.
- It uses only a few small mixins: a weather tick, a lightning hook, biome placement, storm-mob perks, and (on the client) a tint for storm mobs. The weather and lightning hooks are wrapped so an error in them is logged instead of crashing the game.

## Building the jar on GitHub (no setup on your PC)
1. Create a new repository on github.com and upload everything in this folder (drag and drop works).
2. Open the **Actions** tab. The **Build Stormreach** workflow runs automatically on every upload. You can also click **Run workflow**.
3. When it finishes, open the run and download **Stormreach-mod** under *Artifacts*. It's a zip with `stormreach-4.0.0.jar` inside.
4. Put the jar in your `.minecraft/mods` folder along with Fabric API (and Mod Menu if you want the settings screen).

## Building locally
Install JDK 25, then run `./gradlew build` (or `gradlew.bat build` on Windows). The jar ends up in `build/libs/`.

Versions: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.19.4, Gradle 9.9.2, Java 25.

## Testing (5.0)
- `./gradlew test`: unit tests for the simulation code and the cinematic/audio contract.
- `./gradlew runGametest`: boots a headless server with the mod, loads the whole datapack and runs the in-game tests. GitHub Actions runs both and uploads the reports.

## Credits
Cyrrus's voice lines were made with the Piper text-to-speech engine ("Ryan (high)" voice, whose training data is CC BY-NC-SA 4.0), then processed for the mod. Those recordings are non-commercial, share-alike. All other sounds and music were synthesised for Stormreach.
