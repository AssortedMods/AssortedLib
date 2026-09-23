package com.grim3212.assorted.lib.worldgen;

import com.grim3212.assorted.lib.platform.services.IWorldGenHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What {@link IWorldGenHelper#addCustomSpawner} registered. NeoForge adds them from {@code ModifyCustomSpawnersEvent},
 * Fabric from {@code ServerLevelMixin}; one instance per level, so a spawner's countdowns are its level's alone.
 */
public final class CustomSpawners {

    private CustomSpawners() {
    }

    private static final List<Registration> REGISTRATIONS = new CopyOnWriteArrayList<>();
    /** By name, so the spawners tick in the same order on both loaders whatever order the mods loaded in. */
    private static final Comparator<Registration> ORDER = Comparator.comparing(Registration::name);

    public static void add(Identifier name, CustomSpawnerFactory factory) {
        REGISTRATIONS.add(new Registration(name, factory));
    }

    /** What to tick on {@code level}, in order: one spawner from every factory that wants the level. */
    public static List<CustomSpawner> createFor(ServerLevel level) {
        if (REGISTRATIONS.isEmpty()) {
            return List.of();
        }
        List<CustomSpawner> created = new ArrayList<>();
        for (Registration registration : REGISTRATIONS.stream().sorted(ORDER).toList()) {
            CustomSpawner spawner = registration.factory().create(level);
            if (spawner != null) {
                created.add(spawner);
            }
        }
        return created;
    }

    private record Registration(Identifier name, CustomSpawnerFactory factory) {
    }
}
