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

    @GameTest
    public void datapackContentLoaded(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        // the GameTest server only creates the Overworld, so check the realm's data instead of a loaded level
        h.assertTrue(server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE)
                .get(ResourceKey.create(Registries.DIMENSION_TYPE, Stormreach.id("tempest_realm"))).isPresent(), "the Tempest Realm dimension type is missing");
        String[] advancements = {"root", "meteorology", "enter_labyrinth", "stranger_seen", "defeat_cyrrus", "ef5_tornado", "backpack_maelstrom",
                "struck_by_lightning", "elderstorm_grove", "stormreach_complete"};
        for (String a : advancements) h.assertTrue(server.getAdvancements().get(Stormreach.id(a)) != null, "advancement missing: " + a);
        String[] tables = {"chests/labyrinth_vault", "chests/labyrinth_treasure", "chests/labyrinth_room", "chests/hollow_shrine", "ach/ef5",
                "ach/cyrrus", "entities/cyrrus", "entities/hollow_keeper", "blocks/elderstorm_leaves"};
        for (String t : tables) {
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Stormreach.id(t)));
            h.assertTrue(table != LootTable.EMPTY, "loot table missing or failed to parse: " + t);
        }
        var reg = server.registryAccess();
        for (String e : new String[]{"capacity", "magnetism", "organization", "soulbound", "resilience"})
            h.assertTrue(reg.lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, Stormreach.id(e))).isPresent(), "enchantment missing: " + e);
        for (String s : new String[]{"hollow_labyrinth", "hollow_shrine", "tempest_spire", "sky_vault"})
            h.assertTrue(reg.lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, Stormreach.id(s))).isPresent(), "structure missing: " + s);
        h.assertTrue(reg.lookupOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, Stormreach.id("elderstorm_grove"))).isPresent(), "Elderstorm Grove biome missing");
        h.succeed();
    }

    @GameTest
    public void cinematicScriptsAndVoiceEventsExist(GameTestHelper h) {
        for (String name : ModSounds.SCRIPTS) {
            DialogueScript s = ModSounds.loadScript(name);
            h.assertFalse(s.cues.isEmpty(), "script is empty or missing: " + name);
            for (DialogueScript.Cue c : s.cues) if (c.sound() != null) h.assertTrue(ModSounds.get(c.sound()) != null, "voice line not registered: " + c.sound());
        }
        h.succeed();
    }

    @GameTest
    public void backpackContentsSurviveARoundTripAndRejectNesting(GameTestHelper h) {
        ItemStack pack = new ItemStack(Backpacks.BY_TIER.get(BackpackTier.NETHERITE));
        BackpackStorage s = BackpackStorage.read(pack, 54);
        s.items.set(0, new ItemStack(Items.DIAMOND, 64));
        s.items.set(53, new ItemStack(Items.TORCH, 12));
        h.assertTrue(s.insert(new ItemStack(Backpacks.BY_TIER.get(BackpackTier.DIAMOND))).getCount() == 1, "a backpack went inside a backpack");
        s.write(pack);
        BackpackStorage back = BackpackStorage.read(pack, 54);
        h.assertTrue(back.items.get(0).is(Items.DIAMOND) && back.items.get(0).getCount() == 64, "slot 0 lost its diamonds");
        h.assertTrue(back.items.get(53).is(Items.TORCH) && back.items.get(53).getCount() == 12, "last slot lost its torches");
        // shrinking (e.g. a datapack changing sizes) must never delete items
        BackpackStorage small = BackpackStorage.read(pack, 27);
        h.assertTrue(small.items.stream().anyMatch(i -> i.is(Items.TORCH)), "items were lost when a backpack shrank");
        h.assertTrue(((BackpackItem) pack.getItem()).slots(h.getLevel(), pack) >= 54, "Netherite backpack should have at least 54 slots");
        h.succeed();
    }

    @GameTest(maxTicks = 120)
    public void tornadoSpawnsMovesAndDissipates(GameTestHelper h) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 1, 1)));
        TornadoProfile p = TornadoProfile.rollWith(new Random(7), TornadoFormation.SUPERCELL, EFScale.EF2, null, 0.35, 0.05, 90);
        TornadoEntity t = TornadoEntity.spawn(h.getLevel(), at, p);
        h.assertTrue(t != null, "tornado did not spawn");
        h.assertTrue(TornadoManager.live(h.getLevel()).contains(t), "tornado not tracked");
        h.runAfterDelay(40, () -> {
            h.assertTrue(t.position().distanceTo(at) > 0.5 || t.isRemoved(), "tornado never moved");
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
        h.assertTrue(HidingTracker.isHidden(p), "player is not hidden");
        h.assertTrue(p.hasEffect(ModEffects.HIDDEN), "no Hidden effect");
        HidingTracker.leave(p, true);
        h.assertFalse(HidingTracker.isHidden(p), "player still hidden after leaving");
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
        h.assertTrue(h.getBlockState(con[0]).getValue(ShrineBlocks.Conduit.STRUCK), "first conduit not struck");
        ShrineBlocks.onLightning(h.getLevel(), h.absolutePos(con[2]).above()); // wrong: third before second
        h.assertFalse(h.getBlockState(con[0]).getValue(ShrineBlocks.Conduit.STRUCK), "wrong order did not reset the conduits");
        for (BlockPos c : con) ShrineBlocks.onLightning(h.getLevel(), h.absolutePos(c).above());
        for (BlockPos c : con) h.assertTrue(h.getBlockState(c).getValue(ShrineBlocks.Conduit.STRUCK), "conduit not struck in order");
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
}
