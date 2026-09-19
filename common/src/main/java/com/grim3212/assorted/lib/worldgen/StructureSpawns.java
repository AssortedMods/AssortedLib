package com.grim3212.assorted.lib.worldgen;

import com.grim3212.assorted.lib.platform.services.IWorldGenHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The creatures {@link IWorldGenHelper#addSpawnToStructures} adds, and the lookup both loaders make
 * when vanilla asks what may spawn at a position: NeoForge from {@code LevelEvent.PotentialSpawns},
 * Fabric from a mixin on {@code NaturalSpawner#mobsAt}. Vanilla asks twice for every spawn, once to
 * pick the creature and again at the exact spot it lands, so both answers come from here.
 */
public final class StructureSpawns {

    private StructureSpawns() {
    }

    private static final List<Addition> ADDITIONS = new CopyOnWriteArrayList<>();

    /**
     * By entity id, as biome spawns are, so the list is drawn from in the same order on both loaders.
     * The ids are only known once the types are registered, so this sorts on the first lookup.
     */
    private static final Comparator<Addition> ORDER = Comparator.comparing(addition -> BuiltInRegistries.ENTITY_TYPE.getKey(addition.type().get()));
    private static volatile List<Addition> sorted;

    public static void add(TagKey<Structure> structures, Supplier<? extends EntityType<?>> type, IntSupplier weight, int minCount, int maxCount) {
        ADDITIONS.add(new Addition(structures, type, weight, minCount, maxCount));
        sorted = null;
    }

    /**
     * What to add to the spawns at {@code pos} for {@code category}: every addition whose structure
     * has a piece there. Empty, and cheap, when nothing was added for that category.
     */
    public static List<Weighted<MobSpawnSettings.SpawnerData>> at(StructureManager structureManager, MobCategory category, BlockPos pos) {
        if (ADDITIONS.isEmpty()) {
            return List.of();
        }

        List<Addition> additions = sorted;
        if (additions == null) {
            additions = ADDITIONS.stream().sorted(ORDER).toList();
            sorted = additions;
        }

        List<Weighted<MobSpawnSettings.SpawnerData>> found = null;
        for (Addition addition : additions) {
            EntityType<?> type = addition.type().get();
            int weight = addition.weight().getAsInt();
            if (type.getCategory() != category || weight <= 0 || !structureManager.getStructureWithPieceAt(pos, addition.structures()).isValid()) {
                continue;
            }

            if (found == null) {
                found = new ArrayList<>();
            }
            found.add(new Weighted<>(new MobSpawnSettings.SpawnerData(type, addition.minCount(), addition.maxCount()), weight));
        }
        return found == null ? List.of() : found;
    }

    private record Addition(TagKey<Structure> structures, Supplier<? extends EntityType<?>> type, IntSupplier weight, int minCount, int maxCount) {
    }
}
