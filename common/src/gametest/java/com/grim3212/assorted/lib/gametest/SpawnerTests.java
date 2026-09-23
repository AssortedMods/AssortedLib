package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.spawn.SpawnHabit;
import com.grim3212.assorted.lib.spawn.SpawnHabitBuilder;
import com.grim3212.assorted.lib.spawn.SpawnHabits;
import com.grim3212.assorted.lib.spawn.ColumnSite;
import com.grim3212.assorted.lib.spawn.Span;
import com.grim3212.assorted.lib.spawn.SpawnSites;
import com.grim3212.assorted.lib.spawn.StructureSite;
import com.grim3212.assorted.lib.spawn.EntrySet;
import net.minecraft.tags.StructureTags;
import com.grim3212.assorted.lib.spawn.WaterSite;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.grim3212.assorted.lib.test.TestSupport;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * IWorldGenHelper#addCustomSpawner is ticked by every level on both loaders, and a spawn_habit json loads and
 * sets down whole packs. {@link #install()} runs from the loader entry points, before any level exists.
 */
final class SpawnerTests {

    private SpawnerTests() {
    }

    /** {@code data/assortedlib/spawn_habit/test_sheep.json}, at a chance of 0 so the live spawner never acts on it. */
    private static final Identifier TEST_SHEEP = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "test_sheep");
    /** Ticks seen, by level, from the spawner {@link #install()} adds. */
    private static final Map<ResourceKey<Level>, AtomicInteger> TICKS = new ConcurrentHashMap<>();

    static void install() {
        Services.WORLD_GEN.addCustomSpawner(Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "test_counter"), level -> {
            AtomicInteger ticks = TICKS.computeIfAbsent(level.dimension(), key -> new AtomicInteger());
            return (serverLevel, spawnEnemies) -> ticks.incrementAndGet();
        });
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("custom_spawner_is_ticked_by_its_level", SpawnerTests::customSpawnerIsTickedByItsLevel);
        out.accept("spawn_habit_loads_from_json", SpawnerTests::spawnHabitLoadsFromJson);
        out.accept("spawn_habit_sets_down_a_whole_flock", SpawnerTests::spawnHabitSetsDownAWholeFlock);
        out.accept("spawn_sites_find_water_under_ice_and_the_column", SpawnerTests::spawnSitesFindWaterUnderIceAndTheColumn);
        out.accept("spawn_habit_defers_to_the_mob_cap", SpawnerTests::spawnHabitDefersToTheMobCap);
        out.accept("spawn_habit_seeds_a_generating_chunk", SpawnerTests::spawnHabitSeedsAGeneratingChunk);
        out.accept("spawn_habit_keeps_out_of_partial_blocks", SpawnerTests::spawnHabitKeepsOutOfPartialBlocks);
        out.accept("spawn_habit_keeps_away_from_players", SpawnerTests::spawnHabitKeepsAwayFromPlayers);
    }

    /** Vanilla's GameTestServer sets spawn_mobs off, which gates custom spawners; Fabric's runner does not. */
    private static void customSpawnerIsTickedByItsLevel(GameTestHelper helper) {
        AtomicInteger ticks = TICKS.get(helper.getLevel().dimension());
        helper.assertTrue(ticks != null, "no spawner was made for " + helper.getLevel().dimension().identifier());
        GameRules rules = helper.getLevel().getGameRules();
        boolean spawning = rules.get(GameRules.SPAWN_MOBS);
        rules.set(GameRules.SPAWN_MOBS, true, helper.getLevel().getServer());
        helper.runBeforeTestEnd(() -> rules.set(GameRules.SPAWN_MOBS, spawning, helper.getLevel().getServer()));
        int before = ticks.get();
        helper.succeedWhen(() -> helper.assertTrue(ticks.get() > before, "the level has not ticked the added spawner"));
    }

    /** Every field the json sets, and the defaults for those it leaves out. */
    private static void spawnHabitLoadsFromJson(GameTestHelper helper) {
        SpawnHabit habit = SpawnHabits.get(TEST_SHEEP);
        helper.assertTrue(habit != null, "the test sheep habit did not load; loaded: " + SpawnHabits.all().keySet());
        helper.assertValueEqual(habit.entity(), EntityTypes.SHEEP, "entity");
        helper.assertValueEqual(habit.interval(), 40, "interval");
        helper.assertValueEqual(habit.chance(), 0.0D, "chance");
        helper.assertValueEqual(habit.site().type(), SpawnSites.LAND, "site type");
        helper.assertValueEqual(habit.group().min(), 2, "group min");
        helper.assertValueEqual(habit.group().max(), 3, "group max");
        helper.assertValueEqual(habit.spread(), 2, "spread");
        helper.assertTrue(habit.cap().isPresent() && habit.cap().get().max() == 2 && habit.cap().get().range() == 16, "cap");
        helper.assertValueEqual(habit.tries(), 4, "default tries");
        helper.assertValueEqual(habit.distance().min(), 0, "distance min");
        helper.assertValueEqual(habit.distance().max(), 48, "distance max");
        helper.assertTrue(habit.part().isEmpty() && habit.isEnabled(), "a habit with no part is enabled");
        helper.succeed();
    }

    /**
     * A pool three deep under a lid of ice. The water site reads through the ice only when told to, and takes its depth
     * from the water's top, not the ice; the column site keeps to its bounds and, below_surface, under the lid.
     */
    private static void spawnSitesFindWaterUnderIceAndTheColumn(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 7, 3, 7)) {
            boolean wall = pos.getX() == 1 || pos.getX() == 7 || pos.getZ() == 1 || pos.getZ() == 7;
            helper.setBlock(pos, wall ? Blocks.GLASS : Blocks.WATER);
        }
        for (BlockPos pos : BlockPos.betweenClosed(2, 4, 2, 6, 4, 6)) {
            helper.setBlock(pos, Blocks.ICE);
        }
        ServerLevel level = helper.getLevel();
        BlockPos column = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertTrue(WaterSite.SURFACE.find(level, column, level.getRandom()) == null, "the surface water site found water under ice");
        helper.assertValueEqual(new WaterSite(new Span(0, 0), true).find(level, column, level.getRandom()), helper.absolutePos(new BlockPos(4, 3, 4)), "the top of the water under the ice");
        helper.assertValueEqual(new WaterSite(new Span(2, 2), true).find(level, column, level.getRandom()), helper.absolutePos(new BlockPos(4, 1, 4)), "two under the top of the water");
        helper.assertTrue(new WaterSite(new Span(3, 3), true).find(level, column, level.getRandom()) == null, "the water site found water under the pool's floor");

        int floor = helper.absolutePos(new BlockPos(4, 1, 4)).getY();
        BlockPos bounded = new ColumnSite(java.util.Optional.of(floor), java.util.Optional.of(floor + 1), false).find(level, column, level.getRandom());
        helper.assertTrue(bounded != null && bounded.getY() >= floor && bounded.getY() <= floor + 1, "the column site left its bounds: " + bounded);
        helper.assertFalse(new StructureSite(EntrySet.of(StructureTags.VILLAGE)).seedsAtGeneration(), "a structure site seeds while a chunk generates, where its starts are out of reach");
        BlockPos under = ColumnSite.UNDERGROUND.find(level, column, level.getRandom());
        helper.assertTrue(under != null && under.getY() <= floor + 2, "the column site, below_surface, chose the ice lid or above it: " + under);
        helper.succeed();
    }

    /**
     * The test server has no real players, so vanilla counts no spawnable chunks and every category's cap is zero: a
     * habit with mob_cap is refused where the same habit without it sets a flock down.
     */
    private static void spawnHabitDefersToTheMobCap(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 8, 1, 8)) {
            helper.setBlock(pos, Blocks.GRASS_BLOCK);
        }
        ServerLevel level = helper.getLevel();
        BlockPos column = helper.absolutePos(new BlockPos(4, 2, 4));
        SpawnHabit capped = SpawnHabitBuilder.of(() -> EntityTypes.SHEEP).onLand().distance(0, 48).group(2, 2).mobCap().build();
        SpawnHabit free = SpawnHabitBuilder.of(() -> EntityTypes.SHEEP).onLand().distance(0, 48).group(2, 2).build();
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getChunkSource().getLastSpawnState() != null, "the level has not computed a spawn state yet");
            helper.assertValueEqual(capped.spawnPackAt(level, column, level.getRandom()), 0, "sheep set down against a full creature cap");
            int spawned = free.spawnPackAt(level, column, level.getRandom());
            helper.assertValueEqual(spawned, 2, "sheep set down by the habit that ignores the cap");
            level.getEntitiesOfClass(Sheep.class, AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(8, 4, 8)))).forEach(Sheep::discard);
        });
    }

    /** The seed roll, asked of the box's chunk as if it were generating: at odds of 1 a sheep lands somewhere in it, at 0 none does. */
    private static void spawnHabitSeedsAGeneratingChunk(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 8, 1, 8)) {
            helper.setBlock(pos, Blocks.GRASS_BLOCK);
        }
        ServerLevel level = helper.getLevel();
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(new BlockPos(4, 2, 4)));
        AABB whole = new AABB(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX() + 1, level.getMaxY(), chunk.getMaxBlockZ() + 1);
        SpawnHabit never = SpawnHabitBuilder.of(() -> EntityTypes.SHEEP).onLand().distance(0, 48).tries(64).seed(0.0D).build();
        SpawnHabit always = SpawnHabitBuilder.of(() -> EntityTypes.SHEEP).onLand().distance(0, 48).tries(64).seed(1.0D).build();
        helper.succeedWhen(() -> {
            helper.assertValueEqual(never.seedChunk(level, chunk, level.getRandom()), 0, "sheep seeded at odds of 0");
            int seeded = always.seedChunk(level, chunk, level.getRandom());
            helper.assertTrue(seeded >= 1, "no sheep was seeded into the chunk at odds of 1");
            List<Sheep> found = level.getEntitiesOfClass(Sheep.class, whole);
            helper.assertTrue(found.size() >= seeded, "seeded sheep are not in the chunk");
            found.forEach(Sheep::discard);
        });
    }

    /**
     * A stair is not a full cube, so the spawn placement's empty-block check lets it through; the creature's own box
     * against the blocks is what keeps a mob out of one. Two columns, so a retry asks the same thing again.
     */
    private static void spawnHabitKeepsOutOfPartialBlocks(GameTestHelper helper) {
        BlockPos blocked = new BlockPos(2, 1, 2);
        BlockPos clear = new BlockPos(6, 1, 6);
        helper.setBlock(blocked, Blocks.GRASS_BLOCK);
        helper.setBlock(blocked.above(), Blocks.STONE_STAIRS);
        helper.setBlock(clear, Blocks.GRASS_BLOCK);

        ServerLevel level = helper.getLevel();
        BlockPos blockedColumn = helper.absolutePos(blocked.above());
        BlockPos clearColumn = helper.absolutePos(clear.above());
        AABB around = AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(8, 4, 8)));
        helper.succeedWhen(() -> {
            helper.assertValueEqual(atExactly(blockedColumn).spawnPackAt(level, blockedColumn, level.getRandom()), 0, "a sheep was set down inside a stair");
            int spawned = atExactly(clearColumn).spawnPackAt(level, clearColumn, level.getRandom());
            level.getEntitiesOfClass(Sheep.class, around).forEach(Sheep::discard);
            helper.assertValueEqual(spawned, 1, "no sheep was set down on the clear floor beside it");
        });
    }

    /**
     * Vanilla refuses a spawn whose nearest player is inside 24 blocks. The player here is not the one the habit was
     * trying for, which is the case a server hits: a spot far from one player and underfoot of another.
     */
    private static void spawnHabitKeepsAwayFromPlayers(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(2, 1, 2, 6, 1, 6)) {
            helper.setBlock(pos, Blocks.GRASS_BLOCK);
        }
        ServerLevel level = helper.getLevel();
        BlockPos column = helper.absolutePos(new BlockPos(4, 2, 4));
        // Beside the column, not on it: a player blocks building, so one standing in the spot would refuse both habits.
        BlockPos stand = column.offset(2, 0, 2);
        ServerPlayer player = TestSupport.survivalPlayer(helper);
        player.snapTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, 0.0F, 0.0F);

        SpawnHabit guarded = SpawnHabitBuilder.of(() -> EntityTypes.SHEEP).distance(24, 48)
                .at(new ColumnSite(Optional.of(column.getY()), Optional.of(column.getY()), false)).tries(1).build();
        helper.succeedWhen(() -> {
            helper.assertValueEqual(guarded.spawnPackAt(level, column, level.getRandom()), 0, "a sheep was set down on top of a player");
            int spawned = atExactly(column).spawnPackAt(level, column, level.getRandom());
            level.getEntitiesOfClass(Sheep.class, new AABB(column).inflate(4.0D)).forEach(Sheep::discard);
            helper.assertValueEqual(spawned, 1, "no sheep was set down by the habit that keeps no distance");
        });
    }

    /** A habit pinned to one block, so the only spot it may use is the one the test built. */
    private static SpawnHabit atExactly(BlockPos pos) {
        return SpawnHabitBuilder.of(() -> EntityTypes.SHEEP).distance(0, 48)
                .at(new ColumnSite(Optional.of(pos.getY()), Optional.of(pos.getY()), false)).tries(1).build();
    }

    /**
     * Grass across the box, in daylight: the habit hands the column to the land site, the sheep's own placement
     * takes it, and two or three arrive together; with two about, the cap stops the next. Through succeedWhen:
     * PathfinderMob#checkSpawnRules reads the light, which lags the blocks just placed.
     */
    private static void spawnHabitSetsDownAWholeFlock(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 8, 1, 8)) {
            helper.setBlock(pos, Blocks.GRASS_BLOCK);
        }
        SpawnHabit habit = SpawnHabits.get(TEST_SHEEP);
        helper.assertTrue(habit != null, "the test sheep habit did not load");
        ServerLevel level = helper.getLevel();
        BlockPos column = helper.absolutePos(new BlockPos(4, 2, 4));
        AABB box = AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(8, 4, 8)));
        helper.succeedWhen(() -> {
            int spawned = habit.spawnPackAt(level, column, level.getRandom());
            helper.assertTrue(spawned >= 2 && spawned <= 3, "the habit set down " + spawned + " sheep, not a flock of two or three");
            List<Sheep> flock = level.getEntitiesOfClass(Sheep.class, box);
            helper.assertValueEqual(flock.size(), spawned, "sheep in the box");
            helper.assertValueEqual(habit.spawnPackAt(level, column, level.getRandom()), 0, "sheep set down beside a flock at the cap");
            flock.forEach(Sheep::discard);
        });
    }
}
