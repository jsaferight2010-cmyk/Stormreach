"""Stormreach item descriptions (5.3). Every item gets a short description, shown under its name in a clean tooltip
(client/tooltip/StormTooltips): what it is and what it's for, in a sentence or two. Usage details and exact effects
are the item's ".tooltip" lines, shown as bullets while Shift is held; DETAILS below adds those for items that had none.

Writes "desc.stormreach.<id>" (and any new ".tooltip" lines) into the lang file. Runs after the gendata scripts.
validate.py checks that every hand-made item has a description; the generated Storm blocks and Storm mob eggs share
the two templates at the bottom."""
import json, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
LANG = ROOT / 'src/main/resources/assets/stormreach/lang/en_us.json'

D = {}
def d(ids, text):
    for i in ids.split(): D[i] = text

# ---------------------------------------------------------------------------------------------- materials
d('storm_shard', 'A splinter of solidified storm, dropped by Squalls. The base of Stormshard gear and most storm instruments.')
d('storm_cell', 'A bottled charge of a whole thunderstorm. Wakes a Stormheart, and powers the better backpacks and radar.')
d('tempest_core', 'The core of a spent storm, crackling and fire-proof. Repairs the Stormbreaker and builds the top-tier backpack.')
d('gull_feather', 'A grey-white feather from a Storm Gull. Repairs Gale Wings.')
d('salt', 'Coarse sea salt. Cures fish and keeps food on a long voyage.')
d('fulgurite_shard', 'A shard of glass fused by a lightning strike in sand.')
d('crab_claw', 'A Cliff Crab claw. Not much meat raw; cook it.')
d('gull_egg', 'A speckled Storm Gull egg. Fry it for a decent breakfast.')
d('charged_scale', 'A scale shed by a storm-charged Sparkskink. Still tingles.')
d('cloud_ambergris', 'A waxy lump left behind by a Cloud Whale. A key ingredient of Skylight Lanterns.')
d('glowgel', 'Luminous jelly from a Storm Jelly. Packs into Glowgel Blocks and lights Skylight Lanterns.')
d('gale_essence', 'A wisp of living wind, given by a Gale Sprite in thanks for Thistle Down.')
d('pearlescent_shell', 'A pearly shell left by a Tidewrack Hermit. Makes Nacre and the Tide Conch.')
d('thunderbird_plume', 'A lightning-blue flight feather from a Thunderbird. The sail of a Stormkite.')
d('thistle_down', 'Fluff shed by Thistlehops and Sparkfern. Three make a string; Gale Sprites love it.')
d('glimmer_antler', 'A shed antler that glows faintly. The Doppler Radar needs one.')
d('raw_voltite', 'Raw Voltite from deep in the Tempest Realm. Smelt it into an ingot.')
d('voltite_ingot', 'A charged metal from the Tempest Realm. Voltite gear arcs to nearby enemies.')
d('aurorite', 'A gem that holds the colours of the aurora. Makes the strongest realm gear.')
d('stormhawk_feather', 'A feather from a Stormhawk, the realm\'s sky hunter.')
d('ion_shard', 'A crystal of pure static from a Static Wisp. Crunch it to heal instantly.')
d('serpent_scale', 'An iridescent scale from a Squall Serpent. Repairs the Gale Blade and builds Serpentscale Blocks.')
d('squall_heart', 'The still eye of a Squall Serpent, still turning. The heart of a Gale Blade.')
d('glacial_core', 'The never-melting heart of a Hailstone Golem. Makes Glacial Lamps.')
d('aurora_dust', 'Shimmering dust shed by Aurora Moths at night. Colours glass with the aurora.')
d('tempest_eye', 'An Eye torn from the Tempest Sovereign\'s crown. Sixteen of them open a Stormgate.')
d('tempest_sigil', 'A summoning sigil. Offered at a Sovereign Altar, it calls the Tempest Sovereign down.')
d('maelstrom_core', 'What was left of Cyrrus when the storm let go. Builds the Maelstrom Backpack.')
d('hollow_remnant', 'A cold, humming relic left where the Stranger stood.')
d('heart_of_the_labyrinth', 'A heart, still beating, from the deepest room of the Labyrinth.')

# ---------------------------------------------------------------------------------------------- food and drink
d('cooked_crab_claw', 'A roasted Cliff Crab claw. Filling, and easy to find along the coast.')
d('crab_cakes', 'Crab meat pressed into cakes and fried. One of the coast\'s best meals.')
d('fried_gull_egg', 'A fried Storm Gull egg. Simple and filling.')
d('stormberries', 'Tart, fizzy berries from the Stormberry Shrub. A quick bite with a burst of speed. Plant them to grow a shrub.')
d('stormberry_pie', 'A thick pie of Stormberries. Filling, with a longer burst of speed.')
d('stormberry_jam', 'Stormberries boiled down with sugar. A strong, long burst of speed.')
d('stormberry_juice', 'Pressed Stormberries. Springy: it gives Jump Boost.')
d('storm_chowder', 'A hearty coastal chowder. Very filling, and it toughens you up (Resistance).')
d('sailors_stew', 'The coast\'s most filling meal, with a little Regeneration.')
d('seaweed_salad', 'Kelp and sea greens. Lets you breathe underwater for a while.')
d('salted_cod', 'Cod cured in sea salt. Very filling and keeps for ever.')
d('thrift_tea', 'Tea brewed from Sea Thrift. Soothing: a little Regeneration, and you can always drink it.')
d('stormfin', 'A raw Stormfin, caught in storm-churned water. Cook it.')
d('cooked_stormfin', 'A grilled Stormfin. Filling, and it quickens your hands (Haste).')
d('voltaic_eel', 'A raw Voltaic Eel, still faintly charged. Cook it first.')
d('cooked_voltaic_eel', 'A grilled Voltaic Eel. Good, solid food.')
d('tempest_tonic', 'A fizzing storm brew: Strength, Speed II and a Static Charge for a minute.')
d('rock_candy', 'Salt-crystal candy. Always edible, with a sugar rush of Haste II.')
d('stormfruit', 'A glowing fruit from the Elderstorm trees. Speed, and a crackle of Static Charge.')
d('gilded_stormfruit', 'A Stormfruit dipped in gold: strong Regeneration and Absorption. Something old would like it.')
d('stormfruit_preserve', 'Stormfruit preserved in a jar. A long burst of Speed II.')
d('cloudberries', 'Light, airy berries from the Tempest Realm. Eat them to drift down slowly.')
d('cloudberry_tart', 'A tart of Cloudberries. Springy: Jump Boost II for half a minute.')
d('raw_skyray', 'Raw Skyray meat from the Tempest Realm. Cook it.')
d('cooked_skyray', 'Roasted Skyray. One of the realm\'s best meals.')
d('storm_nectar', 'Sweet nectar from the Tempest Realm\'s flowers, in a bottle.')
d('gale_bread', 'Bread baked from realm grain. Plain and filling.')
d('chasers_coffee', 'A strong thermos-brew for long chase days: Speed, Haste and Night Vision for two minutes.')
d('snow_cone', 'Shaved ice and berry syrup. Cools you right down: Fire Resistance for 30 seconds.')

# ---------------------------------------------------------------------------------------------- weather bottles and events
d('bottled_rain', 'A storm, corked. Uncork it to start a Gale.')
d('bottled_sunshine', 'A clear sky, corked. Uncork it to clear the weather.')
d('bottled_thunder', 'A thunderclap, corked. Uncork it to start a short Thunderhead.')
d('squall_in_a_bottle', 'A captured Squall. Throw it to burst into a gust; throw it at your feet to launch yourself.')
d('bottled_gale', 'Gale Essence in a bottle. Drink it to jump high and fall slowly.')
d('bottled_stormfly', 'A Stormfly caught in a bottle, glowing softly. Put it in a jar for a lamp.')
d('bottled_lightning', 'A real lightning bolt in a bottle. Throw it to call the strike down where it lands.')
d('cloud_seeder', 'A rocket that sows the clouds. Fire it outdoors to bring rain.')
d('supercell_seeder', 'A heavy seeding rocket. Fire it outdoors to start a full thunderstorm.')
d('weather_event_ball_lightning', 'Creative tool: release a swarm of ball lightning.')
d('weather_event_dispel', 'Creative tool: clears every tornado and weather event in the world.')
d('weather_event_downburst', 'Creative tool: drop a downburst, a blast of wind straight down that flattens everything.')
d('weather_event_hailstorm', 'Creative tool: start a hailstorm.')
d('weather_event_supercell', 'Creative tool: start a supercell thunderstorm that can drop tornadoes.')
d('thunderhead_horn', 'A horn carved from a storm. Blow it to summon a Thunderhead over the world. Single use.')
D['tornado_spawner_natural'] = 'Creative tool: spawn a tornado as nature would, its shape and strength picked by the climatology.'
for k, what in (('cone', 'a classic cone tornado'), ('drill_bit', 'a thin, fast drill-bit tornado'), ('landspout', 'a weak, rope-thin landspout'),
                ('large_wedge', 'a huge, violent wedge tornado'), ('multi_vortex', 'a tornado with several suction vortices'),
                ('needle', 'a needle-thin tornado'), ('rope', 'a dying rope tornado'), ('satellite', 'a tornado with a satellite twister'),
                ('companion', 'a tornado with a companion funnel'), ('stovepipe', 'a stovepipe tornado'), ('waterspout', 'a waterspout (over water)'),
                ('wedge', 'a wide wedge tornado')):
    D[f'tornado_spawner_{k}'] = f'Creative tool: spawn {what}.'
d('tornado_director', 'Creative tool: steer the nearest tornado to where you point.')
d('realm_pass', 'Creative tool: travel to the Tempest Realm and back.')

# ---------------------------------------------------------------------------------------------- gear
d('stormshard_sword', 'A blade of storm shards. In a thunderstorm, hits may call lightning on the target.')
d('stormshard_pickaxe stormshard_axe stormshard_shovel', 'A tool of storm shards, a step past iron.')
d('stormshard_helmet stormshard_chestplate stormshard_leggings stormshard_boots', 'Armour of storm shards, a step past iron.')
d('voltite_sword', 'A Voltite blade. Every hit arcs to one more nearby enemy.')
d('voltite_pickaxe voltite_axe voltite_shovel voltite_hoe', 'A tool of charged Voltite from the Tempest Realm.')
d('voltite_helmet voltite_chestplate voltite_leggings voltite_boots', 'Voltite armour from the Tempest Realm. A full set has a set bonus.')
d('aurorite_sword', 'An Aurorite blade, the realm\'s finest. Every hit arcs to one more nearby enemy.')
d('aurorite_pickaxe aurorite_axe aurorite_shovel aurorite_hoe', 'An Aurorite tool, the strongest the Tempest Realm makes.')
d('aurorite_helmet aurorite_chestplate aurorite_leggings aurorite_boots', 'Aurorite armour, the realm\'s strongest. A full set has a set bonus.')
d('cloudstep_boots', 'Boots stitched from cloud. No fall damage, and you can glide.')
d('gale_wings', 'Wings of gull feathers and wind. Wear them to glide, like an elytra.')
d('stormbreaker', 'A great hammer forged around a Tempest Core. Use it to unleash a thunderclap shockwave.')
d('weatherbreaker', 'A storm-forged weapon. Hits hurl the target with wind, and one in four calls lightning.')
d('tempest_staff', 'A staff that calls lightning where you aim. Free to use during a thunderstorm.')
d('weathervane_scepter', 'A sceptre that commands the weather.')
d('gale_blade', 'A sword forged around a Squall Heart. Every hit throws the target into the air.')
d('stormcrown', 'The crown of the storm. Its wearer can\'t be hurt by lightning.')
d('storm_chaser_cap', 'A well-worn chaser\'s cap with a radio in the brim. Warns you of tornadoes nearby.')
d('chasers_badge', 'Proof you\'ve stood in front of an EF5. Carried, it halves tornado damage and warns you of tornadoes.')
d('lightning_charm', 'A charm of twisted fulgurite. In your offhand it grounds you against lightning and storm winds.')
d('stormchaser_goggles', 'Chaser\'s goggles with a heads-up display of the nearest tornado.')
d('stormchaser_jacket', 'A storm-orange chase jacket with reflective tape, braced against the wind.')
d('stormchaser_trousers', 'Rain-proof chase trousers with knee pads.')
d('stormchaser_boots', 'Rubber-soled chase boots for running through the rain.')
d('stormkite', 'A kite of Thunderbird Plumes. Hold it while falling to glide.')
d('tide_conch', 'A conch with a golden mouthpiece. Blow it for Water Breathing and Dolphin\'s Grace for everyone nearby.')

# ---------------------------------------------------------------------------------------------- backpacks
for tier, word in (('obsidian', 'Obsidian'), ('diamond', 'Diamond'), ('netherite', 'Netherite'), ('voltite', 'Voltite'), ('aurorite', 'Aurorite'),
                   ('storm_crystal', 'Storm Crystal'), ('maelstrom', 'Maelstrom')):
    D[f'{tier}_backpack'] = f'A {word}-tier backpack: wear it or place it to carry far more. Upgrade it with the next Upgrade Kit.'
    D[f'{tier}_backpack_upgrade'] = f'Upgrades a backpack to the {word} tier in place, keeping everything inside.'
d('storm_chasers_pack', 'A chaser\'s 45-slot pack with built-in Magnetism. Earned, never crafted.')

# ---------------------------------------------------------------------------------------------- instruments
d('anemometer', 'Measures the wind: sustained speed, gusts and Beaufort force. Gives a redstone signal for the gusts.')
d('thermometer', 'Reads the temperature, dew point and humidity.')
d('wind_vane', 'Shows which way the wind is blowing.')
d('storm_barometer', 'Reads air pressure, which drops sharply near a tornado. Gives redstone for the weather.')
d('storm_detector', 'Senses rain, thunderstorms and tornadoes within 512 blocks and gives a redstone signal.')
d('storm_collector', 'Soaks up a storm under open sky. When full, take a Storm Cell from it.')
d('weather_station', 'All the instruments in one: wind, pressure, temperature and a forecast.')
d('weather_monitor', 'A Weather Station readout plus the severe-weather outlook and full radar.')
d('weather_radio', 'A portable weather radio: the forecast, plus alerts for tornadoes and storms while you carry it.')
d('storm_radar', 'A short-range radar sweep in your hand, with tornado warnings while you carry it.')
d('tornado_radar', 'A Doppler radar dish that finds rotation up to 1024 blocks away and reads out every tornado.')
d('tornado_siren', 'A siren that wails while it has a redstone signal. Wire it to a radar or detector.')
d('rain_gauge', 'A glass tube that fills with rain and slowly dries out. Comparators read how full it is.')
d('tornado_probe', 'A weighted instrument barrel no tornado can move. Set it in a tornado\'s path to record the winds.')
d('weather_balloon', 'A radiosonde balloon. Launch it to get a sounding of the upper air.')
d('intercept_vehicle', 'An armoured chase truck with a tornado dashboard. Park it in the path and ride the tornado out.')
d('chasers_logbook', 'A chaser\'s log. It records every tornado you witness: rating, distance, and your records.')
d('wind_turbine', 'A small turbine that turns in the wind and gives redstone power for how hard it blows.')
d('stormreach_field_guide', 'The complete guide to Stormreach: weather, tornadoes, the Labyrinth, the realm and drop rates.')
d('storm_crafters_handbook', 'Every Stormreach recipe, with the real items.')

# ---------------------------------------------------------------------------------------------- the story and the Labyrinth
for n, num in (('1', 'I'), ('2', 'II'), ('3', 'III'), ('4', 'IV'), ('5', 'V')):
    D[f'journal_page_{n}'] = f'Page {num} of a stranger\'s journal, torn and weathered. Collect all five.'
d('weathered_journal', 'The stranger\'s five journal pages, bound together again.')
d('labyrinth_key', 'An old key from before the Vault Door was rebuilt.')
d('labyrinth_key_gale labyrinth_key_thunder labyrinth_key_hollow', 'One of the three keys to the Vault Door at the heart of the Labyrinth.')
d('labyrinth_keyhole', 'Where the three Labyrinth Keys go, beside the sealed treasure chamber.')
d('labyrinth_seal', 'Unbreakable stone that seals the Labyrinth\'s treasure chamber.')
d('vault_door', 'The great door at the heart of the Labyrinth.')
d('vault_wall', 'Heavy plating from the Labyrinth\'s vaults.')
d('rusted_locker', 'An old steel locker. Step inside to hide from what hunts the Labyrinth.')
d('hollow_lamp', 'A cold lamp from the Labyrinth\'s halls.')
d('hollow_altar', 'An altar in the Hollow Shrine. Lay the Weathered Journal on it.')
d('relic_pedestal', 'A shrine pedestal. Set the right relic on it and it glows.')
d('storm_conduit', 'A shrine conduit. Strike it with lightning once every pedestal glows.')
d('sovereign_altar', 'The altar atop a Tempest Spire, where the Tempest Sovereign is called down.')
d('stormgate_frame', 'One of sixteen frames that ring a Stormgate, the way to the Tempest Realm.')
d('sovereign_trophy', 'A trophy of the Tempest Sovereign, for those who brought it down.')
d('cyrrus_trophy', 'A trophy of Cyrrus, the storm that wore your face.')
d('keeper_trophy', 'A trophy of the Hollow Keeper, for escaping the Labyrinth\'s hunter.')
d('tornado_trophy', 'A trophy for witnessing a violent tornado, rated EF4 or stronger.')

# ---------------------------------------------------------------------------------------------- blocks
d('stormstone', 'Dark, rain-slick stone from the Stormreach Coast.')
d('stormstone_slab stormstone_stairs stormstone_wall', 'Stormstone, shaped for building.')
d('polished_stormstone', 'Stormstone ground smooth.')
d('polished_stormstone_slab polished_stormstone_stairs polished_stormstone_wall', 'Polished Stormstone, shaped for building.')
d('stormstone_bricks', 'Bricks of Stormstone, the coast\'s favourite building stone.')
d('stormstone_brick_slab stormstone_brick_stairs stormstone_brick_wall', 'Stormstone Bricks, shaped for building.')
d('cracked_stormstone_bricks', 'Stormstone Bricks cracked by heat.')
d('chiseled_stormstone', 'Stormstone carved with a lightning mark.')
d('charged_stormstone', 'Stormstone that held on to a lightning strike. It glows.')
d('skyshale', 'The pale stone the Tempest Realm\'s islands are made of.')
d('cobbled_skyshale', 'Broken Skyshale.')
d('cobbled_skyshale_slab cobbled_skyshale_stairs cobbled_skyshale_wall', 'Cobbled Skyshale, shaped for building.')
d('polished_skyshale', 'Skyshale ground smooth.')
d('chiseled_skyshale', 'Skyshale carved with a cloud motif.')
d('skyshale_bricks', 'Bricks of pale Skyshale.')
d('skyshale_brick_slab skyshale_brick_stairs skyshale_brick_wall', 'Skyshale Bricks, shaped for building.')
d('tempest_bricks', 'Violet bricks from the Tempest Spires.')
d('tempest_brick_slab tempest_brick_stairs tempest_brick_wall', 'Tempest Bricks, shaped for building.')
d('cracked_tempest_bricks', 'Tempest Bricks, cracked with age.')
d('chiseled_tempest_bricks', 'Tempest Bricks carved with the Sovereign\'s crown.')
d('storm_crystal_ore', 'Ore studded with storm crystal.')
d('storm_crystal_block', 'A block of storm crystal.')
d('voltite_ore', 'Ore threaded with charged Voltite, deep in the Tempest Realm.')
d('voltite_block', 'A block of Voltite.')
d('aurorite_ore', 'Ore glinting with Aurorite, in the Tempest Realm.')
d('aurorite_block', 'A block of Aurorite.')
d('ion_ore', 'Ore crackling with static.')
d('ion_lamp', 'A lamp of static crystal that lights up the realm.')
d('fulgurite', 'Glass fused by lightning in sand, in branching tubes.')
d('fulgurite_lantern', 'A lantern made of fulgurite.')
d('storm_lantern', 'A lamp that holds a little storm.')
d('stormglass', 'Glass made in a storm, tinted storm-blue.')
d('stormglass_pane', 'A thin pane of Stormglass.')
d('salt_block', 'Packed sea salt.')
d('salt_crystal', 'A cluster of salt crystals from the shore. Gives off a faint light.')
d('nacre_block', 'Mother-of-pearl, pressed into a block.')
d('nacre_bricks', 'Bricks of shimmering Nacre.')
d('glowgel_block', 'A block of glowing Glowgel. Bouncy, and it lights up the dark.')
d('skylight_lantern', 'The brightest lantern there is, lit with Cloud Ambergris and Glowgel.')
d('serpentscale_block', 'Squall Serpent scales, overlapped into a gleaming block.')
d('glacial_lamp', 'A lamp of packed ice around a Glacial Core. Bright and cold.')
d('aurora_glass', 'Glass coloured with Aurora Dust. The aurora drifts across it.')
d('storm_shelter_hatch', 'A heavy steel hatch for a storm cellar. Blast-proof; open it by hand or with redstone.')
d('stormfly_jar', 'A jar of glowing Stormflies. A soft lamp to stand on a shelf or hang from a chain.')
d('stormheart', 'A seed of the Stormblight. Wake it and it turns the land and mobs around it into storm versions.')
d('hail_layer', 'A layer of fallen hail. Melts in the sun.')
d('stormwater_bucket', 'A bucket of Stormwater, the charged water of the Stormreach coast.')
d('stormlava_bucket', 'A bucket of Stormlava. Water hardens it into stone.')
for wood, desc in (('driftwood', 'Weathered, salt-bleached wood from the shore.'), ('elderstorm', 'Wood from the ancient Elderstorm trees.'),
                   ('galewood', 'Light, wind-bent wood from the Tempest Realm.'), ('stormpine', 'Dark, resinous pine from the storm-swept hills.')):
    D[f'{wood}_log'] = desc
    D[f'{wood}_planks'] = f'Planks of {wood.capitalize()}.'
    for part in ('button', 'door', 'fence', 'fence_gate', 'pressure_plate', 'slab', 'stairs', 'trapdoor'):
        D[f'{wood}_{part}'] = f'{wood.capitalize()} wood, made into a {part.replace("_", " ")}.'
    D[f'{wood}_leaves'] = f'Leaves of the {wood.capitalize()} tree.'
    D[f'{wood}_sapling'] = f'Plant it to grow a {wood.capitalize()} tree.'
    D[f'stripped_{wood}_log'] = f'A {wood.capitalize()} log with the bark taken off.'
D['driftwood_planks'] = 'Planks of weathered Driftwood.'
d('stormscar_turf', 'Scorched turf where lightning has struck again and again.')
d('static_grass', 'Grass that crackles with static in the Static Fields.')
d('skysoil', 'Light, airy soil from the Tempest Realm\'s islands.')
d('nimbus_cloud', 'A soft cloud. You sink slowly through it and take no fall damage.')
d('gale_cloud', 'A windy cloud that carries you along with the wind.')
d('thundercloud', 'A charged cloud that flings you high into the air.')

# ---------------------------------------------------------------------------------------------- plants
d('sea_thrift', 'A tough pink flower of the sea cliffs. Brews into Thrift Tea.')
d('sea_rocket', 'A shore plant with pale flowers.')
d('saltgrass', 'Salt-tolerant grass from the shore.')
d('sparkfern', 'A fern that sparks in a storm. Shear it to keep it; it sometimes drops Thistle Down.')
d('thunderbloom', 'A yellow storm flower. Makes a suspicious stew that gives Haste.')
d('skybloom', 'A glowing sky-blue flower from the Tempest Realm. Makes a stew of Slow Falling.')
d('stormcap', 'A glowing purple mushroom-flower from the realm. Makes a stew of Night Vision.')
d('gale_grass', 'Tall, wind-combed grass from the Tempest Realm.')

# ---------------------------------------------------------------------------------------------- spawn eggs
MOBS = {
    'cliff_crab': 'a Cliff Crab, a scuttling crab of the sea cliffs', 'storm_gull': 'a Storm Gull, the coast\'s wheeling gull',
    'squall': 'a Squall, a living gust that rams you', 'thunder_crab': 'a Thunder Crab, a crab that crackles with charge',
    'harbor_seal': 'a Harbor Seal, a friendly seal of the shore', 'brine_wraith': 'a Brine Wraith, a drowned spirit of the storm surge',
    'ball_lightning': 'a Ball Lightning orb, drifting and dangerous', 'stormwarden': 'a Stormwarden, a guardian of the storm',
    'skyray': 'a Skyray, a gentle flying ray you can saddle and ride', 'puffling': 'a Puffling, a fluffy cloud-sheep of the realm',
    'static_wisp': 'a Static Wisp, a crackling realm spirit', 'cyclone': 'a Cyclone, a whirling realm monster',
    'stormhawk': 'a Stormhawk, the realm\'s sky hunter', 'tempest_sovereign': 'the Tempest Sovereign, ruler of the Tempest Realm',
    'stranger': 'the Stranger, who follows you', 'cyrrus': 'Cyrrus, the storm that wears your face',
    'hollow_keeper': 'the Hollow Keeper, hunter of the Labyrinth', 'glimmerstag': 'a Glimmerstag, a glowing stag of the Elderstorm forest',
    'thistlehop': 'a Thistlehop, a hopping puffball', 'cloud_whale': 'a Cloud Whale, a gentle giant of the Cloudsea',
    'shock_eel': 'a Shock Eel, an electric eel of the marsh', 'sparkskink': 'a Sparkskink, a lizard that charges up in storms',
    'storm_jelly': 'a Storm Jelly, a glowing jellyfish that swims through the air', 'gale_sprite': 'a Gale Sprite, a tiny wind spirit',
    'tidewrack_hermit': 'a Tidewrack Hermit, a shy hermit crab', 'thunderbird': 'a Thunderbird, a lightning-calling raptor',
    'squall_serpent': 'a Squall Serpent, a sky-dragon of the realm', 'hailstone_golem': 'a Hailstone Golem, a giant of packed hail',
    'aurora_moth': 'an Aurora Moth, drawn to lantern light', 'stormfly': 'a Stormfly, a firefly that glows with stored lightning',
}
for m, what in MOBS.items(): D[f'{m}_spawn_egg'] = f'Spawns {what}.'

# ---------------------------------------------------------------------------------------------- Shift details for items that had none
DETAILS = {
    'item.stormreach.stormberries': ['Eat: Speed (6 s).'], 'item.stormreach.stormberry_pie': ['Eat: Speed (20 s).'],
    'item.stormreach.stormberry_jam': ['Drink: Speed II (30 s).'], 'item.stormreach.stormberry_juice': ['Drink: Jump Boost (30 s).'],
    'item.stormreach.storm_chowder': ['Eat: Resistance (30 s).'], 'item.stormreach.sailors_stew': ['Eat: Regeneration (10 s).'],
    'item.stormreach.seaweed_salad': ['Eat: Water Breathing (45 s).'], 'item.stormreach.thrift_tea': ['Drink: Regeneration (8 s). Always drinkable.'],
    'item.stormreach.cooked_stormfin': ['Eat: Haste (30 s).'], 'item.stormreach.tempest_tonic': ['Drink: Strength, Speed II and Static Charge (60 s).'],
    'item.stormreach.rock_candy': ['Eat: Haste II (15 s). Always edible.'], 'item.stormreach.stormfruit': ['Eat: Speed (20 s) and Static Charge (10 s).'],
    'item.stormreach.stormfruit_preserve': ['Drink: Speed II (90 s).'], 'item.stormreach.cloudberries': ['Eat: Slow Falling (5 s).'],
    'item.stormreach.cloudberry_tart': ['Eat: Jump Boost II (30 s).'], 'item.stormreach.chasers_coffee': ['Drink: Speed, Haste and Night Vision (2 min).'],
    'item.stormreach.snow_cone': ['Eat: Fire Resistance (30 s). Always edible.'],
    'item.stormreach.bottled_rain': ['Use: start a Gale (rain and wind).'], 'item.stormreach.bottled_sunshine': ['Use: clear the weather.'],
    'item.stormreach.bottled_thunder': ['Use: start a short Thunderhead.'],
    'item.stormreach.squall_in_a_bottle': ['Throw: a gust that knocks things back.', 'Throw at your feet: launch yourself.'],
    'item.stormreach.bottled_gale': ['Drink: Jump Boost III and Slow Falling (45 s).'],
    'item.stormreach.stormkite': ['Hold while falling: glide the way you look.', 'No fall damage while gliding.'],
    'item.stormreach.tide_conch': ['Blow: Water Breathing (60 s) and Dolphin\'s Grace (30 s) for everyone within 8 blocks.', 'Calms Shock Eels nearby. 1-minute cooldown.'],
    'item.stormreach.gale_wings': ['Wear in the chest slot and jump while falling to glide.', 'Repair with Gull Feathers.'],
    'item.stormreach.anemometer': ['Use: read the wind.', 'Redstone: gust speed ÷ 10 (0–15).'],
    'item.stormreach.thermometer': ['Use: read temperature, dew point and humidity.'],
    'block.stormreach.storm_barometer': ['Redstone: 0 clear, 7 rain, 15 thunder.'],
    'block.stormreach.storm_detector': ['Redstone: 4 in rain, 15 in a thunderstorm or with a tornado nearby.'],
    'block.stormreach.storm_collector': ['Charges slowly in rain and quickly in a thunderstorm.', 'Full: use with an empty hand to take a Storm Cell.', 'Comparators read the charge.'],
    'block.stormreach.tornado_siren': ['Wails while powered by redstone.'],
    'block.stormreach.rain_gauge': ['Fills faster in a thunderstorm.', 'Use: read it. Sneak + use: empty it.'],
    'block.stormreach.tornado_probe': ['Place it in a tornado\'s path; it records on its own.', 'Use it after the tornado passes for the reading.'],
    'block.stormreach.wind_turbine': ['Must stand under open sky.', 'Redstone: about 1 per 4 mph of wind (a gale gives 15).', 'Use: read the wind.'],
    'block.stormreach.storm_shelter_hatch': ['Blast resistance like obsidian. Opens by hand or redstone.'],
    'block.stormreach.stormfly_jar': ['Light 12. Place it on the ground or under a block to hang it.'],
    'block.stormreach.glacial_lamp': ['Light 15.'], 'block.stormreach.aurora_glass': ['Light 7. Needs Silk Touch to pick back up.'],
    'block.stormreach.stormheart': ['Wake it with a Storm Cell, lightning or redstone.', 'The radius is set in the config.'],
    'block.stormreach.skylight_lantern': ['Light 15.'],
    'item.stormreach.weather_balloon': ['Use outdoors to launch it. The sounding comes back as it climbs.'],
    'item.stormreach.intercept_vehicle': ['Right-click to drive: W/S throttle, A/D steer, sneak to get out.', 'Stop for two seconds and the anchors drop: no tornado can move it.',
                                          'Sneak + right-click from outside to pack it up.'],
}

# ---------------------------------------------------------------------------------------------- 5.4 Thunderfront
d('umbrella', 'A storm-blue umbrella. Keeps hail off you and lets you float down from a height.')
d('omen_feather', 'A Storm Petrel feather that stirs before bad weather. Hold it up to read the sky.')
d('bottled_dust_devil', 'A whirlwind in a bottle. Throw it to sweep up every dropped item nearby.')
d('tesla_coil', 'A copper coil that stores lightning and zaps hostile mobs that come near.')
d('wind_chimes', 'Copper chimes that ring in the wind: gently in a breeze, wildly in a gale.')
MOBS.update({'rainfrog': 'a Rainfrog, whose croaking chorus says rain is coming', 'storm_petrel': 'a Storm Petrel, a seabird that flies ahead of storms',
             'dust_devil': 'a Dust Devil, a whirlwind of hot clear days'})
for m in ('rainfrog', 'storm_petrel', 'dust_devil'): D[f'{m}_spawn_egg'] = f'Spawns {MOBS[m]}.'
DETAILS.update({
    'item.stormreach.umbrella': ['Hold it: hailstones hit the umbrella, not you (each one wears it).', 'Hold it while falling: float down with no fall damage.',
                                 'Careful in thunderstorms: it can draw lightning onto you.'],
    'item.stormreach.omen_feather': ['Use: when rain comes or stops, and whether thunder is due.'],
    'item.stormreach.bottled_dust_devil': ['Throw: a dust devil stays where it lands for 10 s and pulls in dropped items within 8 blocks.',
                                           'Catch one with a glass bottle in a desert, badlands or savanna on a clear day.'],
    'block.stormreach.tesla_coil': ['Charges from lightning within 24 blocks, a Storm Cell, or slowly in a thunderstorm.',
                                    'Zaps the nearest hostile mob in sight within 10 blocks (6 damage), once a second.',
                                    'A redstone signal holds its fire. Comparators read the charge.'],
    'block.stormreach.wind_chimes': ['Hang it under a block, fence, chain or leaves.', 'Rings more the harder the wind blows; barely at all indoors.'],
})
# Enchantment descriptions (shown by mods such as Enchantment Descriptions)
ENCH = {
    'capacity': 'Backpacks only. Adds 9 slots per level.',
    'magnetism': 'Backpacks only. Pulls nearby dropped items into the pack.',
    'organization': 'Backpacks only. Sorts the pack every time you close it.',
    'resilience': 'Backpacks only. A dropped pack survives fire, lava, explosions and cactus, and floats back up out of the void.',
    'soulbound': 'Backpacks only. You keep the pack when you die.',
    'conductive': 'Every hit also shocks the target with lightning damage.',
    'galeforce': 'Boots. You move faster in the rain under open sky.',
    'grounded': 'Armour. Reduces damage from lightning.',
}

with open(LANG) as f: lang = json.load(f)
for k, v in ENCH.items(): lang[f'enchantment.stormreach.{k}.desc'] = v
for k, v in D.items(): lang[f'desc.stormreach.{k}'] = v
for key, lines in DETAILS.items():
    if key + '.tooltip' in lang: continue          # the item already has its own usage lines
    for i, line in enumerate(lines): lang[key + '.tooltip' + ('' if i == 0 else str(i + 1))] = line
lang['desc.stormreach.storm_block'] = 'A storm-touched %s, changed by the Stormblight. Builds just like the original.'
lang['desc.stormreach.storm_egg'] = 'Spawns a Storm %s: the Stormblight\'s version, with storm powers.'
lang['tooltip.stormreach.more'] = 'Hold ⇧ Shift for details'
with open(LANG, 'w') as f: json.dump(lang, f, indent=2, ensure_ascii=False); f.write('\n')
print(f'descriptions: {len(D)} item descriptions, {len(DETAILS)} detail sets')
