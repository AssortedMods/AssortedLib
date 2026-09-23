package com.grim3212.assorted.lib.worldgen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import org.jetbrains.annotations.Nullable;

/** Makes a level's copy of a custom spawner, or null to leave that level alone. See {@link CustomSpawners}. */
@FunctionalInterface
public interface CustomSpawnerFactory {

    @Nullable
    CustomSpawner create(ServerLevel level);
}
