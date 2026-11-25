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

    @GameTest(maxTicks = 60)
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
            for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(x, 1, z));
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
        Vec3 target = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(12, 2, 2)));
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
}
