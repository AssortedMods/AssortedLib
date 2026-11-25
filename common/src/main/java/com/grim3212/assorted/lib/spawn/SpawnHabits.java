package com.grim3212.assorted.lib.spawn;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.conditions.LibParts;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads every {@code data/<ns>/spawn_habit/*.json} into a {@link SpawnHabit}, on reload as on start.
 * Plain JsonOps: nothing in a habit needs registry ops, see {@link EntrySet}. The map is swapped whole,
 * so the spawner reads a consistent set and a datapack reload takes effect at the next tick.
 */
public final class SpawnHabits extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "spawn_habits");

    private static volatile Map<ResourceLocation, SpawnHabit> habits = Map.of();

    public SpawnHabits() {
        super(new Gson(), "spawn_habit");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, SpawnHabit> loaded = new HashMap<>();
        files.forEach((id, json) -> SpawnHabit.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LibConstants.LOG.error("Couldn't parse spawn habit {}: {}", id, error))
                .ifPresent(habit -> loaded.put(id, habit)));
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
        Map<ResourceLocation, SpawnHabit> loaded = habits;
        if (loaded.isEmpty() || !level.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        for (SpawnHabit habit : loaded.values()) {
            habit.seedChunk(level, chunk, random);
        }
    }

    public static Map<ResourceLocation, SpawnHabit> all() {
        return habits;
    }

    @Nullable
    public static SpawnHabit get(ResourceLocation id) {
        return habits.get(id);
    }
}
