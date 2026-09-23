package com.grim3212.assorted.lib.spawn;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CustomSpawner;

import java.util.Map;

/**
 * One level's copy, ticked while spawn_mobs is on, driving every loaded {@link SpawnHabit}. A tick between
 * firings costs one subtraction per habit. Countdowns are kept by id, so a reload that changes the set
 * keeps the rest where they were.
 */
public final class CreatureSpawner implements CustomSpawner {

    private final Object2IntMap<Identifier> countdown = new Object2IntOpenHashMap<>();

    public CreatureSpawner(ServerLevel level) {
    }

    @Override
    public void tick(ServerLevel level, boolean spawnEnemies) {
        Map<Identifier, SpawnHabit> habits = SpawnHabits.all();
        if (habits.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Map.Entry<Identifier, SpawnHabit> entry : habits.entrySet()) {
            SpawnHabit habit = entry.getValue();
            // A new habit starts somewhere in its first interval, so a world does not open on every habit firing at once.
            int left = this.countdown.getOrDefault(entry.getKey(), 0);
            if (left <= 0) {
                left = 1 + random.nextInt(habit.interval());
            }
            if (--left > 0) {
                this.countdown.put(entry.getKey(), left);
                continue;
            }
            this.countdown.put(entry.getKey(), habit.interval());
            if (habit.isEnabled() && (spawnEnemies || habit.isFriendly())) {
                habit.spawnNearPlayers(level);
            }
        }
        if (this.countdown.size() > habits.size()) {
            this.countdown.keySet().removeIf(id -> !habits.containsKey(id));
        }
    }
}
