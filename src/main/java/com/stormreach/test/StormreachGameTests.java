package com.stormreach.test;

import com.stormreach.Stormreach;
import com.stormreach.backpack.BackpackItem;
import com.stormreach.backpack.BackpackStorage;
import com.stormreach.backpack.Backpacks;
import com.stormreach.forest.ForestRegistry;
import com.stormreach.forest.StormfruitPodBlock;
import com.stormreach.labyrinth.HidingTracker;
import com.stormreach.labyrinth.LabyrinthRegistry;
import com.stormreach.registry.ModEffects;
import com.stormreach.registry.ModSounds;
import com.stormreach.sim.BackpackTier;
import com.stormreach.sim.DialogueScript;
import com.stormreach.sim.EFScale;
import com.stormreach.sim.TornadoFormation;
import com.stormreach.sim.TornadoProfile;
import com.stormreach.story.ShrineBlocks;
import com.stormreach.story.StoryRegistry;
import com.stormreach.tornado.TornadoEntity;
import com.stormreach.tornado.TornadoManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * In-game tests, run by a headless GameTest server (./gradlew runGametest, and in CI). Just getting to the first
 * test proves the whole datapack (worldgen, structures, advancements, loot, recipes, enchantments, the realm
 * dimension) parsed and loaded; the tests then exercise the systems that can't be checked outside the game.
 */
public class StormreachGameTests {
    static {
        // tests run side by side, each with its own tornado; the default cap of 2 would refuse some of them
        com.stormreach.StormreachConfig.maxActiveTornadoes = 8;
    }

    @GameTest
    public void datapackContentLoaded(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        // the GameTest server only creates the Overworld, so check the realm's data instead of a loaded level
        Check.isTrue(h, server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE)
                .get(ResourceKey.create(Registries.DIMENSION_TYPE, Stormreach.id("tempest_realm"))).isPresent(), "the Tempest Realm dimension type is missing");
        String[] advancements = {"root", "meteorology", "enter_labyrinth", "stranger_seen", "defeat_cyrrus", "ef5_tornado", "backpack_maelstrom",
                "struck_by_lightning", "elderstorm_grove", "stormreach_complete"};
        for (String a : advancements) Check.isTrue(h, server.getAdvancements().get(Stormreach.id(a)) != null, "advancement missing: " + a);
        String[] tables = {"chests/labyrinth_vault", "chests/labyrinth_treasure", "chests/labyrinth_room", "chests/hollow_shrine", "ach/ef5",
                "ach/cyrrus", "entities/cyrrus", "entities/hollow_keeper", "blocks/elderstorm_leaves"};
        for (String t : tables) {
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Stormreach.id(t)));
            Check.isTrue(h, table != LootTable.EMPTY, "loot table missing or failed to parse: " + t);
        }
        var reg = server.registryAccess();
        for (String e : new String[]{"capacity", "magnetism", "organization", "soulbound", "resilience"})
            Check.isTrue(h, reg.lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, Stormreach.id(e))).isPresent(), "enchantment missing: " + e);
        for (String s : new String[]{"hollow_labyrinth", "hollow_shrine", "tempest_spire", "sky_vault"})
            Check.isTrue(h, reg.lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, Stormreach.id(s))).isPresent(), "structure missing: " + s);
        Check.isTrue(h, reg.lookupOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, Stormreach.id("elderstorm_grove"))).isPresent(), "Elderstorm Grove biome missing");
        h.succeed();
    }

    @GameTest
    public void cinematicScriptsAndVoiceEventsExist(GameTestHelper h) {
        for (String name : ModSounds.SCRIPTS) {
            DialogueScript s = ModSounds.loadScript(name);
            Check.isFalse(h, s.cues.isEmpty(), "script is empty or missing: " + name);
            for (DialogueScript.Cue c : s.cues) if (c.sound() != null) Check.isTrue(h, ModSounds.get(c.sound()) != null, "voice line not registered: " + c.sound());
        }
        h.succeed();
    }

    @GameTest
    public void backpackContentsSurviveARoundTripAndRejectNesting(GameTestHelper h) {
        ItemStack pack = new ItemStack(Backpacks.BY_TIER.get(BackpackTier.NETHERITE));
        BackpackStorage s = BackpackStorage.read(pack, 54);
        s.items.set(0, new ItemStack(Items.DIAMOND, 64));
        s.items.set(53, new ItemStack(Items.TORCH, 12));
        Check.isTrue(h, s.insert(new ItemStack(Backpacks.BY_TIER.get(BackpackTier.DIAMOND))).getCount() == 1, "a backpack went inside a backpack");
        s.write(pack);
        BackpackStorage back = BackpackStorage.read(pack, 54);
        Check.isTrue(h, back.items.get(0).is(Items.DIAMOND) && back.items.get(0).getCount() == 64, "slot 0 lost its diamonds");
        Check.isTrue(h, back.items.get(53).is(Items.TORCH) && back.items.get(53).getCount() == 12, "last slot lost its torches");
        // shrinking (e.g. a datapack changing sizes) must never delete items
        BackpackStorage small = BackpackStorage.read(pack, 27);
        Check.isTrue(h, small.items.stream().anyMatch(i -> i.is(Items.TORCH)), "items were lost when a backpack shrank");
        Check.isTrue(h, ((BackpackItem) pack.getItem()).slots(h.getLevel(), pack) >= 54, "Netherite backpack should have at least 54 slots");
        h.succeed();
    }

    @GameTest(maxTicks = 120)
    public void tornadoSpawnsMovesAndDissipates(GameTestHelper h) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 1, 1)));
        TornadoProfile p = TornadoProfile.rollWith(new Random(7), TornadoFormation.SUPERCELL, EFScale.EF2, null, 0.35, 0.05, 90);
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), at, p);
        Check.isTrue(h, t != null, "tornado did not spawn");
        Check.isTrue(h, TornadoManager.live(h.getLevel()).contains(t), "tornado not tracked");
        h.runAfterDelay(40, () -> {
            Check.isTrue(h, t.position().distanceTo(at) > 0.5 || t.isRemoved(), "tornado never moved");
            t.discard();
            h.succeed();
        });
    }

    @GameTest
    public void hidingInALockerGivesTheHiddenEffect(GameTestHelper h) {
        BlockPos lower = new BlockPos(1, 1, 1);
        BlockState base = LabyrinthRegistry.LOCKER.defaultBlockState();
        h.setBlock(lower, base.setValue(com.stormreach.labyrinth.LockerBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(lower.above(), base.setValue(com.stormreach.labyrinth.LockerBlock.HALF, DoubleBlockHalf.UPPER));
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        HidingTracker.enter(p, h.absolutePos(lower), Direction.NORTH);
        Check.isTrue(h, HidingTracker.isHidden(p), "player is not hidden");
        Check.isTrue(h, p.hasEffect(ModEffects.HIDDEN), "no Hidden effect");
        HidingTracker.leave(p, true);
        Check.isFalse(h, HidingTracker.isHidden(p), "player still hidden after leaving");
        h.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", padding = 4)
    public void shrineConduitsMustBeStruckInOrder(GameTestHelper h) {
        BlockPos[] ped = {new BlockPos(0, 1, 0), new BlockPos(2, 1, 0), new BlockPos(4, 1, 0), new BlockPos(6, 1, 0)};
        for (int i = 0; i < 4; i++) h.setBlock(ped[i], StoryRegistry.RELIC_PEDESTAL.defaultBlockState()
                .setValue(ShrineBlocks.Pedestal.RELIC, i + 1).setValue(ShrineBlocks.Pedestal.LIT, true));
        BlockPos[] con = {new BlockPos(0, 1, 4), new BlockPos(3, 1, 4), new BlockPos(6, 1, 4), new BlockPos(9, 1, 4)};
        for (int i = 0; i < 4; i++) h.setBlock(con[i], StoryRegistry.STORM_CONDUIT.defaultBlockState().setValue(ShrineBlocks.Conduit.ORDER, i + 1));
        ShrineBlocks.onLightning(h.getLevel(), h.absolutePos(con[0]).above());
        Check.isTrue(h, h.getBlockState(con[0]).getValue(ShrineBlocks.Conduit.STRUCK), "first conduit not struck");
        ShrineBlocks.onLightning(h.getLevel(), h.absolutePos(con[2]).above()); // wrong: third before second
        Check.isFalse(h, h.getBlockState(con[0]).getValue(ShrineBlocks.Conduit.STRUCK), "wrong order did not reset the conduits");
        for (BlockPos c : con) ShrineBlocks.onLightning(h.getLevel(), h.absolutePos(c).above());
        for (BlockPos c : con) Check.isTrue(h, h.getBlockState(c).getValue(ShrineBlocks.Conduit.STRUCK), "conduit not struck in order");
        h.succeed();
    }

    @GameTest
    public void stormfruitHangsFromElderstormLeaves(GameTestHelper h) {
        BlockPos leaves = new BlockPos(1, 3, 1);
        h.setBlock(leaves, ForestRegistry.ELDERSTORM_LEAVES);
        h.setBlock(leaves.below(), ForestRegistry.STORMFRUIT_POD.defaultBlockState().setValue(StormfruitPodBlock.AGE, 2));
        h.assertBlockPresent(ForestRegistry.STORMFRUIT_POD, leaves.below());
        h.setBlock(leaves, Blocks.AIR);
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.AIR, leaves.below());
            h.succeed();
        });
    }

    @GameTest
    public void altarRitualAimsAtTheGroundEvenOverTheVoid(GameTestHelper h) {
        // the 5.0 crash: an altar with nothing solid below its ring made the ritual strike a null position
        BlockPos sky = h.absolutePos(new BlockPos(1, 1, 1)).atY(h.getLevel().getMaxY() - 20);
        for (int i = 0; i < 16; i++) {
            BlockPos aim = com.stormreach.realm.SovereignAltarBlock.ritualStrike(h.getLevel(), sky, i * Math.PI / 8, 6);
            Check.isTrue(h, aim != null, "ritual strike had no target over the void");
        }
        Check.isTrue(h, com.stormreach.weather.StormWeather.surfaceOr(h.getLevel(), sky, 24) != null, "surfaceOr returned null");
        h.succeed();
    }

    @GameTest(maxTicks = 60)
    public void lightningStaffBoltsHurtButNeverBurn(GameTestHelper h) {
        BlockPos spot = new BlockPos(2, 1, 2);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
        var cow = h.spawn(net.minecraft.world.entity.EntityTypes.COW, spot);
        float hp = cow.getHealth();
        com.stormreach.weather.StormWeather.strike(h.getLevel(), h.absolutePos(spot), null, true);
        h.runAfterDelay(12, () -> {
            for (int x = -2; x <= 6; x++) for (int y = 0; y <= 3; y++) for (int z = -2; z <= 6; z++)
                Check.isFalse(h, h.getBlockState(new BlockPos(x, y, z)).is(Blocks.FIRE), "the staff's lightning lit a fire");
            if (cow.isAlive()) {
                Check.isFalse(h, cow.isOnFire(), "the staff's lightning set the target on fire");
                Check.isFalse(h, cow.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), "fire resistance was applied");
                Check.isTrue(h, cow.getHealth() < hp, "the staff's lightning did no damage");
            }
            h.succeed();
        });
    }

    // runs in its own batch: dispelling ends every tornado in the level, which would break the tornado tests running beside it
    @GameTest(maxTicks = 60, environment = "stormreach:isolated")
    public void dispelWeatherClearsTheStormAndTornadoes(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        server.setWeatherParameters(0, 6000, true, true);
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 1, 1)));
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), at,
                TornadoProfile.rollWith(new Random(3), TornadoFormation.SUPERCELL, EFScale.EF1, null, 0.3, 0.05, 90));
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { if (t != null) t.discard(); h.succeed(); return; }
        int n = com.stormreach.tornado.WeatherEvents.dispel(h.getLevel(), p);
        Check.isTrue(h, n > 0, "dispel reported nothing cleared");
        Check.isFalse(h, server.getWeatherData().isRaining(), "rain still set after dispel");
        Check.isFalse(h, server.getWeatherData().isThundering(), "thunder still set after dispel");
        Check.isTrue(h, server.getWeatherData().getClearWeatherTime() >= 12000, "no clear-weather window after dispel");
        Check.isTrue(h, t == null || t.isDissipating(), "tornado not dissipating after dispel");
        if (t != null) t.discard();
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void lockerHoldsItsHiderAndAllowsOnlyOne(GameTestHelper h) {
        BlockPos lower = new BlockPos(1, 1, 1);
        BlockState base = LabyrinthRegistry.LOCKER.defaultBlockState();
        h.setBlock(lower, base.setValue(com.stormreach.labyrinth.LockerBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(lower.above(), base.setValue(com.stormreach.labyrinth.LockerBlock.HALF, DoubleBlockHalf.UPPER));
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer a) || !(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer b)) { h.succeed(); return; }
        BlockPos abs = h.absolutePos(lower);
        HidingTracker.enter(a, abs, Direction.NORTH);
        HidingTracker.enter(b, abs, Direction.NORTH);
        Check.isTrue(h, HidingTracker.isHidden(a), "first player not hidden");
        Check.isFalse(h, HidingTracker.isHidden(b), "two players hid in one locker");
        a.teleportTo(a.getX() + 2, a.getY(), a.getZ()); // try to walk out
        h.runAfterDelay(3, () -> {
            // mock players aren't in the level's entity list, so the level tick can't reach them: run its hold step
            if (h.getLevel().getEntity(a.getUUID()) == null) HidingTracker.hold(a);
            Check.isTrue(h, a.position().distanceTo(Vec3.atBottomCenterOf(abs)) < 0.1, "the locker did not hold its hider in place");
            HidingTracker.leave(a, true);
            Check.isFalse(h, HidingTracker.isHidden(a), "still hidden after the exit request");
            Check.isFalse(h, a.hasEffect(ModEffects.HIDDEN), "Hidden effect left behind");
            h.succeed();
        });
    }

    /**
     * The GameTest server encloses each test in barrier blocks, roof included. Tornadoes find the ground with the
     * heightmap, so tests that need open sky take the roof off over their area first.
     */
    private static void openSky(GameTestHelper h, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = 1; y <= 40; y++) {
            BlockPos a = h.absolutePos(new BlockPos(x, y, z));
            if (h.getLevel().getBlockState(a).is(Blocks.BARRIER)) h.getLevel().setBlock(a, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static TornadoProfile stationary(TornadoFormation formation, EFScale rating, double gustMph, double width) {
        return new TornadoProfile(formation, rating, gustMph, com.stormreach.sim.TornadoMorphology.STOVEPIPE,
                com.stormreach.sim.VortexStructure.SINGLE, 0, width, 0.0, 0, 1, 6000, com.stormreach.sim.TornadoRelation.NONE);
    }

    @GameTest(maxTicks = 60)
    public void aNewTornadoComesDownBeforeItDoesDamage(GameTestHelper h) {
        openSky(h, 0, 0, 4, 4);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.OAK_PLANKS);
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 2))),
                stationary(TornadoFormation.SUPERCELL, EFScale.EF5, 230, 20));
        Check.isTrue(h, t != null, "tornado did not spawn");
        t.simulateWithoutPlayers();
        h.runAfterDelay(20, () -> {
            Check.isTrue(h, t.funnelReach() < 0.5f, "the funnel should still be coming down, reach=" + t.funnelReach());
            Check.isTrue(h, t.damagedBlocks() == 0, "the tornado did damage before touching down");
            // (no block-by-block check: debris thrown by the EF5 test next door can land here)
            t.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void anEf5TearsUpBlocksAndCarriesMobsAloft(GameTestHelper h) {
        openSky(h, -4, -4, 12, 12);
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) { h.setBlock(new BlockPos(x, 1, z), Blocks.OAK_PLANKS); h.setBlock(new BlockPos(x, 2, z), Blocks.OAK_PLANKS); }
        var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(8, 3, 4)); // at the core's edge, where the wind peaks
        double startY = zombie.getY();
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 3, 4))),
                stationary(TornadoFormation.SUPERCELL, EFScale.EF5, 240, 8));
        Check.isTrue(h, t != null, "tornado did not spawn");
        t.simulateWithoutPlayers();
        t.skipTo(0.4); // mature and on the ground
        double[] maxY = {startY};
        h.onEachTick(() -> { if (zombie.isAlive()) maxY[0] = Math.max(maxY[0], zombie.getY()); });
        h.runAfterDelay(80, () -> {
            int gone = 0;
            for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) for (int y = 1; y <= 2; y++)
                if (!h.getBlockState(new BlockPos(x, y, z)).is(Blocks.OAK_PLANKS)) gone++;
            Check.isTrue(h, t.damagedBlocks() > 0, "an EF5 did no damage at all");
            Check.isTrue(h, gone >= 40, "an EF5 should tear up most of a plank floor in 4 seconds, removed " + gone + " of 162");
            Check.isTrue(h, maxY[0] > startY + 4 || !zombie.isAlive(), "an EF5 core should lift (or kill) a zombie; it rose " + (maxY[0] - startY));
            t.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void waterspoutsStayOverWater(GameTestHelper h) {
        openSky(h, -2, -2, 14, 14);
        for (int x = 0; x < 13; x++) for (int z = 0; z < 13; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            h.setBlock(new BlockPos(x, 2, z), x >= 2 && x <= 10 && z >= 2 && z <= 10 ? Blocks.WATER : Blocks.GRASS_BLOCK);
        }
        TornadoProfile p = new TornadoProfile(TornadoFormation.WATERSPOUT, EFScale.EF0, 75, com.stormreach.sim.TornadoMorphology.NEEDLE,
                com.stormreach.sim.VortexStructure.SINGLE, 0, 4, 12.0, 0, 1, 6000, com.stormreach.sim.TornadoRelation.NONE);
        BlockPos c = h.absolutePos(new BlockPos(6, 3, 6));
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), Vec3.atBottomCenterOf(c), p);
        Check.isTrue(h, t != null, "waterspout did not spawn over water: overWater=" + TornadoEntity.overWater(h.getLevel(), c.getX() + 0.5, c.getZ() + 0.5)
                + " surface=" + h.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, c.getX(), c.getZ())
                + " pool y=" + h.absolutePos(new BlockPos(6, 2, 6)).getY() + " state=" + h.getBlockState(new BlockPos(6, 2, 6))
                + " active=" + com.stormreach.tornado.TornadoManager.active(h.getLevel()) + " cap=" + com.stormreach.StormreachConfig.maxActiveTornadoes);
        t.simulateWithoutPlayers();
        h.onEachTick(() -> {
            if (!t.isRemoved() && !t.isDissipating())
                Check.isTrue(h, TornadoEntity.overWater(h.getLevel(), t.getX(), t.getZ()), "the waterspout went ashore at " + t.position());
        });
        h.runAfterDelay(100, () -> {
            t.discard();
            // and one can't be made on dry land at all
            for (int x = 0; x < 13; x++) for (int z = 0; z < 13; z++) h.setBlock(new BlockPos(x, 2, z), Blocks.GRASS_BLOCK);
            Check.isTrue(h, TornadoEntity.findWater(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6, 3, 6))), 6) == null,
                    "found water where there is none");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void theTornadoDirectorSteersATornadoToItsTarget(GameTestHelper h) {
        openSky(h, 0, 0, 13, 5);
        for (int x = 0; x < 14; x++) for (int z = 0; z < 6; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 2))),
                stationary(TornadoFormation.SUPERCELL, EFScale.EF0, 70, 4));
        Check.isTrue(h, t != null, "tornado did not spawn");
        Vec3 target = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6, 2, 2))); // stay inside the test's ticking chunks
        t.steerTo(target);
        h.runAfterDelay(80, () -> {
            double d = Math.hypot(t.getX() - target.x, t.getZ() - target.z);
            Check.isTrue(h, d < 2.0, "the directed tornado did not reach its target; still " + d + " blocks away");
            t.release();
            Check.isTrue(h, t.steerTarget() == null, "release did not clear the target");
            t.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void hailBreaksPanesHurtsMobsAndPilesUp(GameTestHelper h) {
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(new BlockPos(1, 2, 1), Blocks.GLASS_PANE);
        var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(5, 2, 5));
        float hp = zombie.getHealth();
        var level = h.getLevel();
        com.stormreach.weather.precip.HailstoneEntity.drop(level, Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 6, 1))), com.stormreach.sim.HailSize.GOLF_BALL, 0, 0);
        com.stormreach.weather.precip.HailstoneEntity.drop(level, new Vec3(zombie.getX(), zombie.getY() + 5, zombie.getZ()), com.stormreach.sim.HailSize.SOFTBALL, 0, 0);
        for (int i = 0; i < 8; i++)
            com.stormreach.weather.precip.HailstoneEntity.drop(level, Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 6, i % 7))), com.stormreach.sim.HailSize.BASEBALL, 0, 0);
        h.runAfterDelay(30, () -> {
            Check.isFalse(h, h.getBlockState(new BlockPos(1, 2, 1)).is(Blocks.GLASS_PANE), "golf-ball hail should smash a glass pane");
            Check.isTrue(h, !zombie.isAlive() || zombie.getHealth() <= hp - com.stormreach.sim.HailSize.SOFTBALL.damage() + 4, "softball hail should badly hurt a zombie");
            int piles = 0;
            for (int z = 0; z < 7; z++) {
                BlockPos at = new BlockPos(3, 2, z);
                if (h.getBlockState(at).is(com.stormreach.weather.precip.PrecipContent.HAIL_LAYER)) {
                    piles++;
                    Check.isTrue(h, level.getBlockTicks().hasScheduledTick(h.absolutePos(at), com.stormreach.weather.precip.PrecipContent.HAIL_LAYER),
                            "hail on the ground must have a melt timer");
                }
            }
            Check.isTrue(h, piles > 0, "baseball hail should leave hail on the ground");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void puddlesFormOnLevelGroundAndGrow(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        var level = h.getLevel();
        BlockPos on = h.absolutePos(new BlockPos(1, 2, 1));
        Check.isTrue(h, com.stormreach.weather.precip.PuddleBlock.grow(level, on, 2), "puddle did not form on stone");
        com.stormreach.weather.precip.PuddleBlock.grow(level, on, 2);
        com.stormreach.weather.precip.PuddleBlock.grow(level, on, 2);
        Check.isTrue(h, level.getBlockState(on).getValue(com.stormreach.weather.precip.PuddleBlock.SIZE) == 2, "puddle should grow to, and stop at, its max size");
        h.setBlock(new BlockPos(3, 1, 1), Blocks.AIR);
        Check.isFalse(h, com.stormreach.weather.precip.PuddleBlock.grow(level, h.absolutePos(new BlockPos(3, 2, 1)), 3), "a puddle formed in mid-air");
        h.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        h.runAfterDelay(2, () -> {
            Check.isFalse(h, level.getBlockState(on).is(com.stormreach.weather.precip.PrecipContent.PUDDLE), "a puddle should vanish when its ground goes");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void downburstsBlowThingsOutward(GameTestHelper h) {
        for (int x = 0; x < 12; x++) for (int z = 0; z < 12; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(9, 2, 6));
        Vec3 centre = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 6)));
        double start = Math.hypot(zombie.getX() - centre.x, zombie.getZ() - centre.z);
        com.stormreach.tornado.WeatherEvents.downburst(h.getLevel(), centre, false);
        double[] far = {start};
        h.onEachTick(() -> far[0] = Math.max(far[0], Math.hypot(zombie.getX() - centre.x, zombie.getZ() - centre.z)));
        h.runAfterDelay(60, () -> {
            Check.isTrue(h, far[0] > start + 1.5, "the downburst's outflow should push a zombie away from its centre (moved " + (far[0] - start) + ")");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 30)
    public void stormCloudsGrowAndCanBeDispelled(GameTestHelper h) {
        var level = h.getLevel();
        var cell = com.stormreach.weather.cloud.StormCells.spawnNear(level, Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 1, 1))), level.getRandom(), true, true);
        Check.isTrue(h, cell != null, "storm cloud did not spawn");
        cell.skipTo(0.5);
        h.runAfterDelay(3, () -> {
            Check.isTrue(h, cell.growth() > 0.45f, "growth not synced: " + cell.growth());
            Check.isTrue(h, cell.getY() + cell.cloudDepth() <= level.getMaxY(), "the cloud top is above the build limit");
            cell.dissipate();
            h.runAfterDelay(4, () -> {
                Check.isTrue(h, cell.growth() >= 0.85f, "a dispelled cloud should be collapsing");
                cell.discard();
                h.succeed();
            });
        });
    }

    @GameTest
    public void theBackpackHasItsOwnEquipmentSlot(GameTestHelper h) {
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        ItemStack pack = new ItemStack(Backpacks.BY_TIER.get(BackpackTier.DIAMOND));
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        p.getInventory().setItem(9, pack);
        var menu = p.inventoryMenu;
        net.minecraft.world.inventory.Slot worn = null;
        for (var s : menu.slots) if (s instanceof com.stormreach.backpack.BackpackEquipment.WornSlot) worn = s;
        Check.isTrue(h, worn != null, "the inventory has no backpack slot");
        int from = -1;
        for (var s : menu.slots) if (s.container == p.getInventory() && s.getContainerSlot() == 9) from = s.index;
        menu.quickMoveStack(p, from);
        Check.isTrue(h, com.stormreach.backpack.BackpackEquipment.worn(p).getItem() instanceof BackpackItem, "shift-click didn't put the backpack on");
        Check.isTrue(h, p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST) == chest, "the chestplate was disturbed: the pack must not use the chest slot");
        Check.isTrue(h, p.getInventory().getItem(9).isEmpty(), "the backpack was duplicated");
        Check.isFalse(h, worn.mayPlace(new ItemStack(Items.DIRT)), "only backpacks fit the backpack slot");
        // its contents are kept while worn
        BackpackStorage st = BackpackStorage.read(com.stormreach.backpack.BackpackEquipment.worn(p), 27);
        st.items.set(0, new ItemStack(Items.EMERALD, 5));
        st.write(com.stormreach.backpack.BackpackEquipment.worn(p));
        menu.quickMoveStack(p, worn.index);
        Check.isTrue(h, com.stormreach.backpack.BackpackEquipment.worn(p).isEmpty(), "shift-click didn't take the backpack off");
        ItemStack back = ItemStack.EMPTY;
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).getItem() instanceof BackpackItem) back = p.getInventory().getItem(i);
        Check.isTrue(h, BackpackStorage.read(back, 27).items.get(0).is(Items.EMERALD), "the backpack lost its contents");
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void weatherScreensMeasureAndTheirDataSurvivesTheNetwork(GameTestHelper h) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 1)));
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), at.add(30, 0, 0), stationary(TornadoFormation.SUPERCELL, EFScale.EF2, 120, 10));
        var r = com.stormreach.tornado.WeatherUi.measure(h.getLevel(), com.stormreach.tornado.InstrumentBlock.Kind.TORNADO_RADAR.ordinal(), BlockPos.containing(at));
        Check.isTrue(h, t == null || !r.contacts().isEmpty(), "the Doppler radar screen didn't see a tornado 30 blocks away");
        Check.isTrue(h, r.range() >= 512, "the Doppler radar's range is too short");
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        com.stormreach.tornado.WeatherUi.ReportPayload.CODEC.encode(buf, new com.stormreach.tornado.WeatherUi.ReportPayload(true, r));
        var back = com.stormreach.tornado.WeatherUi.ReportPayload.CODEC.decode(buf);
        Check.isTrue(h, back.report().contacts().size() == r.contacts().size() && back.report().pos().equals(r.pos())
                && Math.abs(back.report().tempC() - r.tempC()) < 1e-3, "the weather report changed on its way over the network");
        var anemo = com.stormreach.tornado.WeatherUi.measure(h.getLevel(), com.stormreach.tornado.InstrumentBlock.Kind.ANEMOMETER.ordinal(), BlockPos.containing(at));
        Check.isTrue(h, anemo.contacts().isEmpty(), "an anemometer shouldn't report radar contacts");
        if (t != null) t.discard();
        h.succeed();
    }

    @GameTest(maxTicks = 20, skyAccess = true)
    public void theDopplerRadarIsOneStructure(GameTestHelper h) {
        openSky(h, 0, 0, 4, 4);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(2, 2, 2));
        Check.isTrue(h, com.stormreach.tornado.radar.RadarStructure.blocked(level, base).isEmpty(), "the radar should fit in an open space");
        h.setBlock(new BlockPos(3, 6, 2), Blocks.STONE); // in the dish space
        Check.isFalse(h, com.stormreach.tornado.radar.RadarStructure.blocked(level, base).isEmpty(), "a block inside the footprint should stop placement");
        h.setBlock(new BlockPos(3, 6, 2), Blocks.AIR);
        com.stormreach.tornado.radar.RadarStructure.build(level, base);
        BlockPos controller = base.offset(com.stormreach.tornado.radar.RadarLayout.CONTROLLER);
        Check.isTrue(h, com.stormreach.tornado.radar.RadarStructure.complete(level, controller), "the radar structure is incomplete after building");
        Check.isTrue(h, level.getBlockEntity(controller) instanceof com.stormreach.tornado.radar.DopplerRadarBlockEntity, "the dish renderer's block entity is missing");
        // taking the controller away brings the whole thing down
        level.setBlock(controller, Blocks.AIR.defaultBlockState(), 3);
        h.runAfterDelay(2, () -> {
            for (BlockPos p : com.stormreach.tornado.radar.RadarLayout.footprint(base))
                Check.isFalse(h, level.getBlockState(p).is(com.stormreach.tornado.radar.RadarContent.RADAR_PART), "a radar part was left floating at " + p);
            h.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void theVaultDoorNeedsAllThreeRightKeys(GameTestHelper h) {
        var level = h.getLevel();
        for (int x = 1; x <= 5; x++) for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(x, y, 3), LabyrinthRegistry.VAULT_DOOR);
        BlockPos[] locks = {new BlockPos(2, 3, 3), new BlockPos(3, 3, 3), new BlockPos(4, 3, 3)};
        for (int k = 0; k < 3; k++) h.setBlock(locks[k], LabyrinthRegistry.KEYHOLE.defaultBlockState()
                .setValue(com.stormreach.labyrinth.KeyholeBlock.FACING, Direction.NORTH).setValue(com.stormreach.labyrinth.KeyholeBlock.KIND, k));
        Check.isTrue(h, LabyrinthRegistry.VAULT_DOOR.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) < 0, "the Vault Door must be unbreakable");
        Check.isTrue(h, LabyrinthRegistry.VAULT_WALL.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) < 0, "the vault plating must be unbreakable");
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        java.util.function.BiConsumer<BlockPos, net.minecraft.world.item.Item> use = (rel, key) -> {
            BlockPos abs = h.absolutePos(rel);
            ItemStack stack = new ItemStack(key);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            level.getBlockState(abs).useItemOn(stack, level, p, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false));
        };
        use.accept(locks[0], LabyrinthRegistry.KEYS[1]); // the Thunder key in the Gale lock
        Check.isFalse(h, h.getBlockState(locks[0]).getValue(com.stormreach.labyrinth.KeyholeBlock.FILLED), "a lock accepted the wrong key");
        use.accept(locks[0], LabyrinthRegistry.KEYS[0]);
        use.accept(locks[1], LabyrinthRegistry.KEYS[1]);
        Check.isTrue(h, h.getBlockState(new BlockPos(3, 5, 3)).is(LabyrinthRegistry.VAULT_DOOR), "the door opened with only two keys");
        use.accept(locks[2], LabyrinthRegistry.KEYS[2]);
        h.runAfterDelay(70, () -> {
            for (int x = 1; x <= 5; x++) for (int y = 1; y <= 5; y++) {
                var st = h.getBlockState(new BlockPos(x, y, 3));
                Check.isFalse(h, st.is(LabyrinthRegistry.VAULT_DOOR) || st.is(LabyrinthRegistry.KEYHOLE), "the Vault Door didn't open fully");
            }
            h.succeed();
        });
    }

    @GameTest
    public void hollowLampsDriftButStayInRange(GameTestHelper h) {
        var r = h.getLevel().getRandom();
        int light = 7, min = 99, max = -1;
        for (int i = 0; i < 2000; i++) {
            light = com.stormreach.labyrinth.HollowLampBlock.next(light, r);
            min = Math.min(min, light); max = Math.max(max, light);
            Check.isTrue(h, light >= 1 && light <= 12, "lamp light out of range: " + light);
        }
        Check.isTrue(h, min <= 2 && max >= 10, "lamp light should both gutter and flare over time (" + min + ".." + max + ")");
        h.succeed();
    }

    @GameTest
    public void guidebooksAreGivenOnceAndNeverLost(GameTestHelper h) {
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        Check.isTrue(h, com.stormreach.guide.Guidebooks.giveOnce(p), "the first join should give the guidebooks");
        int books = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var it = p.getInventory().getItem(i);
            if (it.is(com.stormreach.guide.Guidebooks.FIELD_GUIDE_ITEM) || it.is(com.stormreach.guide.Guidebooks.CRAFTING_ITEM)) books++;
        }
        Check.isTrue(h, books == 2, "expected both guidebooks, found " + books);
        Check.isFalse(h, com.stormreach.guide.Guidebooks.giveOnce(p), "guidebooks were given twice");
        h.succeed();
    }

    @GameTest
    public void everyModSoundIsRegisteredAndOriginal(GameTestHelper h) {
        int n = 0;
        for (var f : com.stormreach.registry.ModSfx.class.getFields()) {
            if (f.getType() != net.minecraft.sounds.SoundEvent.class) continue;
            try {
                var e = (net.minecraft.sounds.SoundEvent) f.get(null);
                Check.isTrue(h, e.location().getNamespace().equals(Stormreach.MOD_ID), "not a Stormreach sound: " + e.location());
                Check.isTrue(h, net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(e.location()), "sound not registered: " + e.location());
                n++;
            } catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
        }
        Check.isTrue(h, n >= 40, "expected the full set of original sound effects, found " + n);
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void theSovereignArenaIsBuiltSafeAndOptional(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos altar = h.absolutePos(new BlockPos(1, 2, 1));
        BlockPos c = com.stormreach.realm.SovereignArena.centreFor(level, altar);
        Check.isTrue(h, c.getY() <= level.getMaxY() - 24, "the arena is too close to the build limit");
        com.stormreach.realm.SovereignArena.build(level, c);
        Check.isTrue(h, com.stormreach.realm.SovereignArena.safeSpot(level, c.above()), "the arena centre isn't a safe landing spot");
        Check.isTrue(h, com.stormreach.realm.SovereignArena.safeSpot(level, c.offset(12, 1, 0)), "the arrival ring isn't a safe landing spot");
        boolean was = com.stormreach.StormreachConfig.sovereignArena;
        com.stormreach.StormreachConfig.sovereignArena = false;
        Check.isTrue(h, com.stormreach.realm.SovereignArena.begin(level, altar) == null, "with the arena turned off the fight should stay at the altar");
        com.stormreach.StormreachConfig.sovereignArena = true;
        Check.isTrue(h, com.stormreach.realm.SovereignArena.begin(level, altar) != null, "the arena should give the Sovereign a place to appear");
        com.stormreach.StormreachConfig.sovereignArena = was;
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void theHollowKeeperEnragesAndDimsTheLamps(GameTestHelper h) {
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(new BlockPos(3, 5, 3), LabyrinthRegistry.HOLLOW_LAMP.defaultBlockState().setValue(com.stormreach.labyrinth.HollowLampBlock.LIGHT, 10));
        var keeper = h.spawn(LabyrinthRegistry.HOLLOW_KEEPER, new BlockPos(3, 2, 3));
        keeper.setHome(new net.minecraft.world.level.levelgen.structure.BoundingBox(h.absolutePos(BlockPos.ZERO).getX() - 2, h.absolutePos(BlockPos.ZERO).getY(),
                h.absolutePos(BlockPos.ZERO).getZ() - 2, h.absolutePos(BlockPos.ZERO).getX() + 9, h.absolutePos(BlockPos.ZERO).getY() + 8, h.absolutePos(BlockPos.ZERO).getZ() + 9));
        double speed = keeper.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        keeper.setHealth(keeper.getMaxHealth() * 0.4f);
        int[] dimmest = {99};
        h.onEachTick(() -> {
            var st = h.getBlockState(new BlockPos(3, 5, 3));
            if (st.is(LabyrinthRegistry.HOLLOW_LAMP)) dimmest[0] = Math.min(dimmest[0], st.getValue(com.stormreach.labyrinth.HollowLampBlock.LIGHT));
        });
        h.runAfterDelay(20, () -> {
            Check.isTrue(h, keeper.enraged(), "a badly wounded Keeper should enrage");
            Check.isTrue(h, keeper.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > speed, "an enraged Keeper should be faster");
            Check.isTrue(h, dimmest[0] <= 1, "lamps should gutter as the Keeper passes");
            keeper.discard();
            h.succeed();
        });
    }

    @GameTest
    public void stormreachBlocksUseTheirOwnSounds(GameTestHelper h) {
        var vault = LabyrinthRegistry.VAULT_DOOR.defaultBlockState().getSoundType();
        Check.isTrue(h, vault == com.stormreach.registry.ModSoundTypes.METAL, "the vault door should use Stormreach's metal sounds");
        Check.isTrue(h, LabyrinthRegistry.HOLLOW_LAMP.defaultBlockState().getSoundType() == com.stormreach.registry.ModSoundTypes.GLASS, "the lamp should use Stormreach's glass sounds");
        Check.isTrue(h, Blocks.STONE.defaultBlockState().getSoundType() == net.minecraft.world.level.block.SoundType.STONE, "vanilla blocks must keep their own sounds");
        Check.isTrue(h, com.stormreach.registry.ModSoundTypes.familyFor("DEEPSLATE_TILES") == com.stormreach.registry.ModSoundTypes.STONE
                && com.stormreach.registry.ModSoundTypes.familyFor("AZALEA_LEAVES") == com.stormreach.registry.ModSoundTypes.PLANT, "sound families mis-sorted");
        h.succeed();
    }

    // ------------------------------------------------------------------------------------------ 5.2 Stormwild
    @GameTest
    public void stormwildContentLoaded(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        for (String a : new String[]{"cloud_ambergris", "shock_eel", "gale_friend", "stormkite_glide", "thunderbird", "weather_balloon", "chaser_first",
                "chaser_veteran", "tiv_intercept", "probe_hit", "probe_violent"})
            Check.isTrue(h, server.getAdvancements().get(Stormreach.id(a)) != null, "advancement missing: " + a);
        for (String t : new String[]{"entities/cloud_whale", "entities/shock_eel", "entities/thunderbird", "entities/intercept_vehicle", "blocks/tornado_probe",
                "blocks/rain_gauge", "blocks/glowgel_block"}) {
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Stormreach.id(t)));
            Check.isTrue(h, table != LootTable.EMPTY, "loot table missing or failed to parse: " + t);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 80)
    public void stormwildCreaturesSpawnAndLive(GameTestHelper h) {
        openSky(h, 0, 0, 8, 8);
        java.util.List<net.minecraft.world.entity.Mob> mobs = new java.util.ArrayList<>();
        mobs.add(h.spawn(com.stormreach.wild.WildRegistry.SPARKSKINK, new BlockPos(1, 2, 1)));
        mobs.add(h.spawn(com.stormreach.wild.WildRegistry.TIDEWRACK_HERMIT, new BlockPos(3, 2, 1)));
        mobs.add(h.spawn(com.stormreach.wild.WildRegistry.STORM_JELLY, new BlockPos(5, 4, 1)));
        mobs.add(h.spawn(com.stormreach.wild.WildRegistry.GALE_SPRITE, new BlockPos(1, 4, 5)));
        mobs.add(h.spawn(com.stormreach.wild.WildRegistry.SHOCK_EEL, new BlockPos(3, 2, 5)));
        mobs.add(h.spawn(com.stormreach.wild.WildRegistry.CLOUD_WHALE, new BlockPos(4, 8, 4)));
        mobs.add(h.spawn(com.stormreach.wild.WildRegistry.THUNDERBIRD, new BlockPos(4, 14, 4)));
        h.runAfterDelay(60, () -> {
            for (var m : mobs) Check.isTrue(h, m.isAlive() || m.isRemoved() && !m.isDeadOrDying(), m.getType() + " died or vanished while ticking");
            for (var m : mobs) m.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void sparkskinksChargeWhenStruck(GameTestHelper h) {
        var skink = h.spawn(com.stormreach.wild.WildRegistry.SPARKSKINK, new BlockPos(2, 2, 2));
        Check.isFalse(h, skink.isCharged(), "a new Sparkskink should start uncharged");
        var bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        skink.thunderHit(h.getLevel(), bolt);
        Check.isTrue(h, skink.isCharged(), "lightning should charge a Sparkskink");
        Check.isTrue(h, skink.isAlive(), "lightning should not kill a Sparkskink");
        skink.discard();
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void hermitsHideFromAPlayer(GameTestHelper h) {
        var hermit = h.spawn(com.stormreach.wild.WildRegistry.TIDEWRACK_HERMIT, new BlockPos(2, 2, 2));
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 2, 2)));
        p.snapTo(at.x, at.y, at.z, 0, 0);
        h.runAfterDelay(6, () -> {
            Check.isTrue(h, hermit.isHiding(), "a Tidewrack Hermit should pull into its shell with a player beside it");
            hermit.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void weatherBalloonsRise(GameTestHelper h) {
        openSky(h, 0, 0, 4, 4);
        var b = h.spawn(com.stormreach.chase.ChaseRegistry.WEATHER_BALLOON_ENTITY, new BlockPos(2, 2, 2));
        double y0 = b.getY();
        h.runAfterDelay(20, () -> {
            Check.isTrue(h, b.getY() > y0 + 3, "a weather balloon should climb about 5 blocks a second, it rose " + (b.getY() - y0));
            b.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void stormkiteSlowsAFall(GameTestHelper h) {
        openSky(h, 0, 0, 4, 4);
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 12, 2)));
        p.snapTo(at.x, at.y, at.z, 0, 0);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(com.stormreach.wild.WildRegistry.STORMKITE));
        p.setOnGround(false);
        p.setDeltaMovement(0, -0.8, 0);
        p.fallDistance = 10;
        com.stormreach.wild.Stormkite.tick(p);
        Check.isTrue(h, p.getDeltaMovement().y >= -0.15, "holding a Stormkite should slow a fall to a glide, vy=" + p.getDeltaMovement().y);
        Check.isTrue(h, p.fallDistance < 1, "gliding should cancel fall damage");
        Check.isTrue(h, Math.hypot(p.getDeltaMovement().x, p.getDeltaMovement().z) > 0.01, "a glide should carry you forward");
        h.succeed();
    }

    @GameTest
    public void rainGaugeDrivesAComparator(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, com.stormreach.chase.ChaseRegistry.RAIN_GAUGE.defaultBlockState().setValue(com.stormreach.chase.RainGaugeBlock.LEVEL, 6));
        var st = h.getBlockState(pos);
        Check.isTrue(h, st.getAnalogOutputSignal(h.getLevel(), h.absolutePos(pos), net.minecraft.core.Direction.NORTH) == 12,
                "a rain gauge at level 6 should give a comparator signal of 12");
        h.succeed();
    }

    @GameTest(maxTicks = 80)
    public void anAnchoredInterceptVehicleHoldsInATornadoAndTheProbeRecordsIt(GameTestHelper h) {
        openSky(h, -4, -4, 14, 14);
        for (int x = -2; x < 13; x++) for (int z = -2; z < 13; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.BEDROCK);
        var v = h.spawn(com.stormreach.chase.ChaseRegistry.INTERCEPT_VEHICLE_ENTITY, new BlockPos(9, 2, 4));
        v.setAnchored(true);
        Vec3 start = v.position();
        h.setBlock(new BlockPos(4, 2, 9), com.stormreach.chase.ChaseRegistry.TORNADO_PROBE);
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 2, 4))),
                stationary(TornadoFormation.SUPERCELL, EFScale.EF5, 240, 10));
        Check.isTrue(h, t != null, "tornado did not spawn");
        t.simulateWithoutPlayers();
        t.skipTo(0.4);
        h.runAfterDelay(50, () -> {
            Check.isTrue(h, v.isAlive() && v.position().distanceTo(start) < 0.5, "an anchored intercept vehicle moved " + v.position().distanceTo(start) + " blocks");
            var be = h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4, 2, 9)));
            Check.isTrue(h, be instanceof com.stormreach.chase.TornadoProbeBlockEntity, "the tornado probe was destroyed");
            if (be instanceof com.stormreach.chase.TornadoProbeBlockEntity probe)
                Check.isTrue(h, probe.peakMph() > 65, "the probe should have recorded tornado-force wind, got " + probe.peakMph());
            t.discard();
            v.discard();
            h.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------ 5.3 Skyward
    @GameTest(maxTicks = 20)
    public void skywardContentLoaded(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        for (String a : new String[]{"radio_warning", "cloud_seeder", "supercell_seeder", "stormchaser_suit", "bottled_lightning", "stormfly_jar",
                "hailstone_golem", "squall_serpent", "gale_blade"})
            Check.isTrue(h, server.getAdvancements().get(Stormreach.id(a)) != null, "advancement missing: " + a);
        for (String t : new String[]{"entities/squall_serpent", "entities/hailstone_golem", "entities/aurora_moth", "entities/stormfly", "blocks/wind_turbine",
                "blocks/aurora_glass", "blocks/storm_shelter_hatch", "blocks/stormfly_jar", "blocks/glacial_lamp", "blocks/serpentscale_block"}) {
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Stormreach.id(t)));
            Check.isTrue(h, table != LootTable.EMPTY, "loot table missing or failed to parse: " + t);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 60)
    public void skywardCreaturesSpawnAndLive(GameTestHelper h) {
        openSky(h, 0, 0, 8, 8);
        java.util.List<net.minecraft.world.entity.Mob> mobs = new java.util.ArrayList<>();
        mobs.add(h.spawn(com.stormreach.sky.SkyRegistry.SQUALL_SERPENT, new BlockPos(4, 6, 4)));
        mobs.add(h.spawn(com.stormreach.sky.SkyRegistry.HAILSTONE_GOLEM, new BlockPos(1, 2, 1)));
        mobs.add(h.spawn(com.stormreach.sky.SkyRegistry.AURORA_MOTH, new BlockPos(6, 4, 1)));
        mobs.add(h.spawn(com.stormreach.sky.SkyRegistry.STORMFLY, new BlockPos(1, 4, 6)));
        h.runAfterDelay(40, () -> {
            for (var m : mobs) Check.isTrue(h, m.isAlive(), m.getType().toShortString() + " died within two seconds of spawning");
            for (var m : mobs) m.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void windTurbinePowerFollowsTheWind(GameTestHelper h) {
        Check.isTrue(h, com.stormreach.sky.WindTurbineBlock.powerFor(0, true) == 0, "no wind should give no power");
        Check.isTrue(h, com.stormreach.sky.WindTurbineBlock.powerFor(30, true) == 8, "30 mph should give about 8");
        Check.isTrue(h, com.stormreach.sky.WindTurbineBlock.powerFor(90, true) == 15, "a gale should give full power");
        Check.isTrue(h, com.stormreach.sky.WindTurbineBlock.powerFor(90, false) == 0, "a turbine without open sky gives nothing");
        BlockPos pos = new BlockPos(1, 2, 1);
        h.setBlock(pos, com.stormreach.sky.SkyRegistry.WIND_TURBINE.defaultBlockState().setValue(com.stormreach.sky.WindTurbineBlock.POWER, 9));
        Check.isTrue(h, h.getBlockState(pos).getSignal(h.getLevel(), h.absolutePos(pos), net.minecraft.core.Direction.NORTH) == 9,
                "the turbine should output its POWER as a redstone signal");
        Check.isTrue(h, h.getLevel().getBlockEntity(h.absolutePos(pos)) instanceof com.stormreach.sky.WindTurbineBlockEntity, "turbine block entity missing");
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void theStormchaserSuitBracesAndGrounds(GameTestHelper h) {
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        Check.isTrue(h, com.stormreach.sky.ChaserGear.massFactor(p) == 1.0, "no jacket: normal mass");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(com.stormreach.sky.SkyRegistry.CHASER_JACKET));
        Check.isTrue(h, com.stormreach.sky.ChaserGear.massFactor(p) > 2.5, "the jacket should brace you against tornado winds");
        Check.isTrue(h, !com.stormreach.weather.StormWeather.isLightningProof(p), "the jacket alone is not lightning-proof");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(com.stormreach.sky.SkyRegistry.CHASER_GOGGLES));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(com.stormreach.sky.SkyRegistry.CHASER_TROUSERS));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(com.stormreach.sky.SkyRegistry.CHASER_BOOTS));
        Check.isTrue(h, com.stormreach.weather.StormWeather.isLightningProof(p), "the full suit should make you lightning-proof");
        h.succeed();
    }

    // ------------------------------------------------------------------------------------------------ 5.4 Thunderfront
    @GameTest(maxTicks = 20)
    public void thunderfrontContentLoaded(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        for (String a : new String[]{"omen_feather", "bottled_dust_devil", "tesla_coil", "umbrella_hail"})
            Check.isTrue(h, server.getAdvancements().get(Stormreach.id(a)) != null, "advancement missing: " + a);
        for (String t : new String[]{"entities/storm_petrel", "entities/rainfrog", "blocks/tesla_coil", "blocks/wind_chimes"}) {
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Stormreach.id(t)));
            Check.isTrue(h, table != LootTable.EMPTY, "loot table missing or failed to parse: " + t);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 60)
    public void thunderfrontCreaturesSpawnAndLive(GameTestHelper h) {
        openSky(h, 0, 0, 8, 8);
        java.util.List<net.minecraft.world.entity.Mob> mobs = new java.util.ArrayList<>();
        mobs.add(h.spawn(com.stormreach.front.FrontRegistry.RAINFROG, new BlockPos(1, 2, 1)));
        mobs.add(h.spawn(com.stormreach.front.FrontRegistry.STORM_PETREL, new BlockPos(5, 4, 5)));
        var devil = h.spawn(com.stormreach.front.FrontRegistry.DUST_DEVIL, new BlockPos(4, 2, 1));
        devil.anchor(200);
        mobs.add(devil);
        h.runAfterDelay(40, () -> {
            for (var m : mobs) Check.isTrue(h, m.isAlive(), m.getType().toShortString() + " died within two seconds of spawning");
            for (var m : mobs) m.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void aChargedTeslaCoilZapsAMonster(GameTestHelper h) {
        openSky(h, 0, 0, 8, 8);
        BlockPos coil = new BlockPos(1, 2, 1);
        h.setBlock(coil, com.stormreach.front.FrontRegistry.TESLA_COIL);
        var be = (com.stormreach.front.TeslaCoilBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(coil));
        Check.isTrue(h, be != null, "tesla coil block entity missing");
        be.addCharge(h.getLevel(), com.stormreach.front.TeslaCoilBlockEntity.MAX);
        var spider = h.spawn(net.minecraft.world.entity.EntityTypes.SPIDER, new BlockPos(5, 2, 1));
        spider.setNoAi(true);
        h.runAfterDelay(50, () -> {
            Check.isTrue(h, be.charge() < com.stormreach.front.TeslaCoilBlockEntity.MAX, "the coil should have spent charge zapping the spider");
            Check.isTrue(h, !spider.isAlive() || spider.getHealth() < spider.getMaxHealth(), "the spider should have been zapped");
            Check.isTrue(h, h.getBlockState(coil).getValue(com.stormreach.front.TeslaCoilBlock.CHARGE) > 0, "a charged coil should show its charge");
            spider.discard();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void umbrellasFloatAndChimesFollowTheWind(GameTestHelper h) {
        Check.isTrue(h, com.stormreach.front.WindChimesBlock.ringChance(0, true) == 0, "still air: no chimes");
        Check.isTrue(h, com.stormreach.front.WindChimesBlock.ringChance(40, true) == 1, "a gale should ring them every time");
        Check.isTrue(h, com.stormreach.front.WindChimesBlock.ringChance(20, false) < com.stormreach.front.WindChimesBlock.ringChance(20, true), "indoors should be quieter");
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { h.succeed(); return; }
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(com.stormreach.front.FrontRegistry.UMBRELLA));
        p.setOnGround(false);
        p.setDeltaMovement(0, -0.9, 0);
        p.fallDistance = 12;
        com.stormreach.front.UmbrellaItem.tick(p);
        Check.isTrue(h, p.getDeltaMovement().y >= -0.17, "an umbrella should slow a fall, vy=" + p.getDeltaMovement().y);
        Check.isTrue(h, p.fallDistance < 1, "an umbrella should cancel fall damage");
        Check.isTrue(h, com.stormreach.front.UmbrellaItem.shields(p), "an umbrella in hand should shield from hail");
        h.succeed();
    }

    // ------------------------------------------------------------------------------------------------ 5.5 Live Wire
    private <T> T be(GameTestHelper h, BlockPos p, Class<T> c) {
        var e = h.getLevel().getBlockEntity(h.absolutePos(p));
        Check.isTrue(h, c.isInstance(e), "expected a " + c.getSimpleName() + " at " + p + " but found " + e);
        return c.cast(e);
    }

    @GameTest(maxTicks = 60)
    public void aCapacitorPowersALampDownACable(GameTestHelper h) {
        for (int x = 0; x < 6; x++) h.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(1, 2, 1), com.stormreach.power.PowerRegistry.STORM_CAPACITOR);
        h.setBlock(new BlockPos(2, 2, 1), com.stormreach.power.PowerRegistry.COPPER_CABLE);
        h.setBlock(new BlockPos(3, 2, 1), com.stormreach.power.PowerRegistry.COPPER_CABLE);
        h.setBlock(new BlockPos(4, 2, 1), com.stormreach.power.PowerRegistry.ARC_LAMP);
        var cap = be(h, new BlockPos(1, 2, 1), com.stormreach.power.StormCapacitorBlock.Entity.class);
        cap.insert(10_000);
        Check.isTrue(h, h.getBlockState(new BlockPos(2, 2, 1)).getValue(net.minecraft.world.level.block.PipeBlock.EAST), "cable should join the next cable");
        h.runAfterDelay(40, () -> {
            Check.isTrue(h, cap.stored() < 10_000, "the capacitor should have sent power down the cable");
            Check.isTrue(h, h.getBlockState(new BlockPos(4, 2, 1)).getValue(com.stormreach.power.ArcLampBlock.LIT), "the arc lamp should be lit");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void rainBarrelsDrainIntoCisternsAndKeepStormwaterApart(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 2, 1), com.stormreach.power.PowerRegistry.CISTERN);
        h.setBlock(new BlockPos(1, 3, 1), com.stormreach.power.PowerRegistry.RAIN_BARREL);
        var barrel = be(h, new BlockPos(1, 3, 1), com.stormreach.power.TankBlock.Entity.class);
        var cistern = be(h, new BlockPos(1, 2, 1), com.stormreach.power.TankBlock.Entity.class);
        Check.isTrue(h, barrel.waterCapacity() == 4000 && cistern.waterCapacity() == 32000, "tank sizes");
        barrel.fill(1000, true);
        Check.isTrue(h, barrel.isStormwater(), "thunderstorm water in an empty barrel is Stormwater");
        h.runAfterDelay(45, () -> {
            Check.isTrue(h, cistern.water() >= 250, "the barrel should drain into the cistern below, cistern has " + cistern.water());
            Check.isTrue(h, cistern.isStormwater(), "stormwater stays stormwater when it drains");
            cistern.fill(100, false);
            Check.isTrue(h, !cistern.isStormwater(), "plain water mixed in makes it plain water");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void aPumpLiftsWaterIntoATank(GameTestHelper h) {
        for (int x = 0; x < 4; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(new BlockPos(1, 1, 1), Blocks.WATER);
        h.setBlock(new BlockPos(1, 2, 1), com.stormreach.power.PowerRegistry.ELECTRIC_PUMP);
        h.setBlock(new BlockPos(2, 2, 1), com.stormreach.power.PowerRegistry.CISTERN);
        var pump = be(h, new BlockPos(1, 2, 1), com.stormreach.power.ElectricPumpBlock.Entity.class);
        var cistern = be(h, new BlockPos(2, 2, 1), com.stormreach.power.TankBlock.Entity.class);
        pump.insert(2000);
        h.runAfterDelay(45, () -> {
            Check.isTrue(h, cistern.water() >= com.stormreach.power.ElectricPumpBlock.MB, "the pump should have lifted water, cistern has " + cistern.water());
            Check.isTrue(h, pump.stored() < 2000, "pumping should cost power");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void aSprinklerWetsFarmland(GameTestHelper h) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(new BlockPos(1, 2, 1), com.stormreach.power.PowerRegistry.CISTERN);
        h.setBlock(new BlockPos(1, 3, 1), com.stormreach.power.PowerRegistry.SPRINKLER);
        BlockPos farm = new BlockPos(3, 2, 3);
        h.setBlock(farm, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 1));
        var cistern = be(h, new BlockPos(1, 2, 1), com.stormreach.power.TankBlock.Entity.class);
        var spr = be(h, new BlockPos(1, 3, 1), com.stormreach.power.SprinklerBlock.Entity.class);
        cistern.fill(1000, false);
        spr.insert(1000);
        h.runAfterDelay(50, () -> {
            Check.isTrue(h, cistern.water() < 1000, "the sprinkler should draw water from the cistern under it");
            var st = h.getBlockState(farm);
            Check.isTrue(h, st.is(Blocks.FARMLAND) && st.getValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE) == 7, "farmland in range should be wet: " + st);
            h.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void aShockPlateZapsAMonster(GameTestHelper h) {
        for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        BlockPos plate = new BlockPos(2, 2, 2);
        h.setBlock(plate, com.stormreach.power.PowerRegistry.SHOCK_PLATE);
        var pe = be(h, plate, com.stormreach.power.ShockPlateBlock.Plate.class);
        pe.insert(2000);
        var spider = h.spawn(net.minecraft.world.entity.EntityTypes.CAVE_SPIDER, plate);
        spider.setNoAi(true);
        h.runAfterDelay(30, () -> {
            Check.isTrue(h, pe.stored() < 2000, "the plate should have spent power on a shock");
            Check.isTrue(h, !spider.isAlive() || spider.getHealth() < spider.getMaxHealth(), "the spider should have been shocked");
            spider.discard();
            h.succeed();
        });
    }

    /** 5.5.0 crash: a placed feature nested in the Thunderspire tree selector carried a biome filter. Placing the
     *  selector itself (never top-level) used to throw "Tried to biome check an unregistered feature". */
    @GameTest(maxTicks = 20)
    public void thunderspireTreesPlaceWithoutABiomeCheckCrash(GameTestHelper h) {
        var level = h.getLevel();
        var reg = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        var gen = level.getChunkSource().getGenerator();
        for (String id : new String[]{"trees_thunderspire", "trees_galewood", "trees_elderstorm"}) {
            var f = reg.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(Stormreach.MOD_ID, id));
            Check.isTrue(h, f != null, "configured feature stormreach:" + id + " should be registered");
            var rnd = net.minecraft.util.RandomSource.create(1234);
            for (int x = 0; x < 6; x++) h.setBlock(new BlockPos(x, 1, 0), Blocks.GRASS_BLOCK);
            for (int i = 0; i < 200; i++) {   // the driftwood branch is 5%: 200 tries hit it with near certainty
                try { f.place(level, gen, rnd, h.absolutePos(new BlockPos(i % 6, 2, 0))); }
                catch (IllegalStateException e) { throw new AssertionError("stormreach:" + id + " threw during placement: " + e.getMessage(), e); }
            }
        }
        h.succeed();
    }

    /** 6.0 (test list 14): the radar's velocity scan is real Doppler data: a tornado shows an inbound/outbound couplet. */
    @GameTest(maxTicks = 20)
    public void dopplerVelocityShowsATornadoCouplet(GameTestHelper h) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 1)));
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), at.add(0, 0, -200), stationary(TornadoFormation.SUPERCELL, EFScale.EF3, 150, 0));
        if (t == null) { h.succeed(); return; }
        var r = com.stormreach.tornado.WeatherUi.measure(h.getLevel(), com.stormreach.tornado.InstrumentBlock.Kind.TORNADO_RADAR.ordinal(), BlockPos.containing(at));
        Check.isTrue(h, r.scan() != null, "the Doppler radar sent no scan");
        int in = 0, out = 0;
        double range = r.range();
        for (int az = 0; az < com.stormreach.sim.RadarMath.AZIMUTHS; az++) for (int g = 0; g < com.stormreach.sim.RadarMath.GATES; g++) {
            double[] o = com.stormreach.sim.RadarMath.gateOffset(az, g, range);
            if (Math.hypot(o[0] - (t.getX() - at.x), o[1] - (t.getZ() - at.z)) > 120) continue;
            byte v = r.scan().velocity()[com.stormreach.sim.RadarMath.index(az, g)];
            if (v == com.stormreach.sim.RadarMath.NO_DATA) continue;
            if (v < -15) in++;
            if (v > 15) out++;
        }
        t.discard();
        Check.isTrue(h, in > 0 && out > 0, "no velocity couplet around the tornado: inbound gates " + in + ", outbound " + out);
        // the scan survives the network
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        com.stormreach.tornado.WeatherUi.ReportPayload.CODEC.encode(buf, new com.stormreach.tornado.WeatherUi.ReportPayload(true, r));
        var back = com.stormreach.tornado.WeatherUi.ReportPayload.CODEC.decode(buf);
        Check.isTrue(h, back.report().scan() != null && java.util.Arrays.equals(back.report().scan().velocity(), r.scan().velocity()), "the velocity scan changed over the network");
        h.succeed();
    }

    /** 6.0: tall machines build their whole column, refuse to place when blocked, and come down together. */
    @GameTest(maxTicks = 20)
    public void tallMachinesBuildAndComeDownWhole(GameTestHelper h) {
        BlockPos base = new BlockPos(1, 1, 1);
        h.setBlock(base, com.stormreach.sky.SkyRegistry.WIND_TURBINE);
        for (int dy = 1; dy <= com.stormreach.sky.WindTurbineBlock.TOWER; dy++)
            Check.isTrue(h, h.getBlockState(base.above(dy)).is(com.stormreach.structure.StructureRegistry.MACHINE_PART), "turbine tower cell " + dy + " missing");
        h.getLevel().destroyBlock(h.absolutePos(base.above(7)), false);
        Check.isTrue(h, h.getBlockState(base).isAir(), "breaking the tower didn't take the turbine down");
        for (int dy = 1; dy <= com.stormreach.sky.WindTurbineBlock.TOWER; dy++)
            Check.isTrue(h, !h.getBlockState(base.above(dy)).is(com.stormreach.structure.StructureRegistry.MACHINE_PART), "a turbine part was left floating at " + dy);
        BlockPos siren = new BlockPos(3, 1, 1);
        h.setBlock(siren, com.stormreach.tornado.WeatherItems.TORNADO_SIREN);
        Check.isTrue(h, h.getBlockState(siren.above(5)).is(com.stormreach.structure.StructureRegistry.MACHINE_PART), "the siren has no pole");
        Check.isTrue(h, h.getLevel().getBlockEntity(h.absolutePos(siren)) instanceof com.stormreach.tornado.InstrumentBlockEntity, "the siren has no renderer entity");
        h.succeed();
    }

    /** 6.0: the TIV anchors respond to the key immediately (no two-second wait) and lock the truck in place. */
    @GameTest(maxTicks = 20)
    public void tivAnchorsDropOnCommand(GameTestHelper h) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        var v = h.spawn(com.stormreach.chase.ChaseRegistry.INTERCEPT_VEHICLE_ENTITY, new BlockPos(2, 2, 2));
        if (!(h.makeMockPlayer(GameType.SURVIVAL) instanceof ServerPlayer p)) { v.discard(); h.succeed(); return; }
        p.startRiding(v, true, true);
        h.runAfterDelay(3, () -> {
            v.toggleAnchors(p);
            Check.isTrue(h, v.isAnchored(), "the anchors didn't drop on the first press");
            v.toggleAnchors(p);
            Check.isTrue(h, !v.isAnchored(), "the anchors didn't rise on the second press");
            p.stopRiding(); v.discard();
            h.succeed();
        });
    }
}
