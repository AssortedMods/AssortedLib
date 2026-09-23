package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.spawn.SpawnHabit;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes {@code data/<modid>/spawn_habit/<name>.json} for each habit {@link #addHabits} gives, on both loaders alike. */
public abstract class LibSpawnHabitProvider implements DataProvider {

    private final PackOutput.PathProvider paths;
    private final String modId;

    protected LibSpawnHabitProvider(PackOutput output, String modId) {
        this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "spawn_habit");
        this.modId = modId;
    }

    protected abstract void addHabits(BiConsumer<String, SpawnHabit> out);

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Map<String, SpawnHabit> habits = new LinkedHashMap<>();
        this.addHabits(habits::put);
        return CompletableFuture.allOf(habits.entrySet().stream()
                .map(entry -> DataProvider.saveStable(cache, SpawnHabit.CODEC, entry.getValue(), this.paths.json(Identifier.fromNamespaceAndPath(this.modId, entry.getKey()))))
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Spawn habits: " + this.modId;
    }
}
