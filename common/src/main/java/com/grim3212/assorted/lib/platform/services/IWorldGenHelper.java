package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.worldgen.CustomSpawnerFactory;
import com.grim3212.assorted.lib.worldgen.CustomSpawners;
import com.grim3212.assorted.lib.worldgen.StructureSpawns;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public interface IWorldGenHelper {

    void addFeatureToBiomes(BiomePredicate biomePredicate, GenerationStep.Decoration step, Identifier configuredFeatureIdentifier);

    /**
     * Takes a placed feature out of every biome the predicate accepts, through the loader's own
     * biome modifiers rather than by overriding the biome's json. The predicate is asked when a world
     * loads its biomes, so it may read config.
     */
    void removeFeatureFromBiomes(BiomePredicate biomePredicate, GenerationStep.Decoration step, Identifier placedFeatureIdentifier);

    /**
     * Adds a creature to the natural spawns of every biome the predicate accepts, in the category
     * the type was built with. The predicate and the weight are asked when a world loads its biomes,
     * so both may read config. Pair it with {@link IPlatformHelper#registerSpawnPlacement}, which decides where in
     * those biomes the creature may appear. One call per type: Fabric names the addition after it.
     */
    void addSpawnToBiomes(BiomePredicate biomePredicate, Supplier<? extends EntityType<?>> type, IntSupplier weight, int minCount, int maxCount);

    /**
     * Adds a creature to the natural spawns inside every structure in the tag, in the category the
     * type was built with. It can appear wherever one of the structure's pieces is, underground or
     * not, on top of whatever the biome or the structure's own spawn overrides already allow there;
     * nothing is taken away. The weight is asked on every spawn attempt, so it may read config, and
     * 0 turns the spawn off. Pair it with {@link IPlatformHelper#registerSpawnPlacement}.
     */
    default void addSpawnToStructures(TagKey<Structure> structures, Supplier<? extends EntityType<?>> type, IntSupplier weight, int minCount, int maxCount) {
        StructureSpawns.add(structures, type, weight, minCount, maxCount);
    }

    /**
     * A {@code CustomSpawner} on every server level, beside vanilla's cat and patrol spawners; the factory
     * is asked per level and may return null. Ticked while spawn_mobs is on, in name order on both loaders.
     * Neither the biome spawn lists nor the category caps are involved: it is a mod's own pacing.
     */
    default void addCustomSpawner(Identifier name, CustomSpawnerFactory factory) {
        CustomSpawners.add(name, factory);
    }

    @FunctionalInterface
    interface BiomePredicate {
        boolean test(Identifier key, Holder<Biome> biomeHolder);
    }
}
