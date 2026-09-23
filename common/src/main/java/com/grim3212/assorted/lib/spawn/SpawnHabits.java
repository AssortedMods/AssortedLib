package com.grim3212.assorted.lib.spawn;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.conditions.LibParts;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Loads every {@code data/<ns>/spawn_habit/*.json} into a {@link SpawnHabit}, on reload as on start.
 * Plain JsonOps: nothing in a habit needs registry ops, see {@link EntrySet}. The map is swapped whole,
 * so the spawner reads a consistent set and a datapack reload takes effect at the next tick.
 */
public final class SpawnHabits extends SimpleJsonResourceReloadListener<SpawnHabit> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "spawn_habits");

    private static volatile Map<Identifier, SpawnHabit> habits = Map.of();

    public SpawnHabits() {
        super(SpawnHabit.CODEC, FileToIdConverter.json("spawn_habit"));
    }

    @Override
    protected void apply(Map<Identifier, SpawnHabit> loaded, ResourceManager manager, ProfilerFiller profiler) {
        loaded.forEach((id, habit) -> habit.part().filter(part -> !LibParts.isRegistered(part))
                .ifPresent(part -> LibConstants.LOG.warn("Spawn habit {} names part '{}', which no mod registered; it will never spawn", id, part)));
        loaded.forEach((id, habit) -> {
            if (habit.seed() > 0.0D && !habit.site().seedsAtGeneration()) {
                LibConstants.LOG.warn("Spawn habit {} has a seed, but its {} site cannot run while a chunk generates; it will only come from the spawner", id, habit.site().type());
            }
        });
        habits = Map.copyOf(loaded);
        LibConstants.LOG.info("Loaded {} spawn habits", habits.size());
    }

    /** Every habit's seed roll for a chunk that is generating; from NaturalSpawnerSeedMixin, on a worldgen thread. */
    public static void seedChunk(ServerLevelAccessor level, ChunkPos chunk, RandomSource random) {
        Map<Identifier, SpawnHabit> loaded = habits;
        if (loaded.isEmpty() || !level.getLevel().getGameRules().get(GameRules.SPAWN_MOBS)) {
            return;
        }
        for (SpawnHabit habit : loaded.values()) {
            habit.seedChunk(level, chunk, random);
        }
    }

    public static Map<Identifier, SpawnHabit> all() {
        return habits;
    }

    @Nullable
    public static SpawnHabit get(Identifier id) {
        return habits.get(id);
    }
}
