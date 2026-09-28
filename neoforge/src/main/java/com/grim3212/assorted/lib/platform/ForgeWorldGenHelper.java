package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.platform.services.IWorldGenHelper;
import com.grim3212.assorted.lib.worldgen.BiomeModification;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ForgeWorldGenHelper implements IWorldGenHelper {
    // Concurrent because mods are constructed in parallel, and each one sorts after it adds.
    private static final List<BiomeModification> biomeModifications = new CopyOnWriteArrayList<>();
    private static final List<BiomeModification> biomeRemovals = new CopyOnWriteArrayList<>();

    /**
     * Kept in the order Fabric applies its own modifications in, which is by the placed feature's id
     * - {@code Identifier} sorts on path first, then namespace. A feature's position in its step
     * decides the seed it generates from, so adding the same features in a different order gives the
     * same seed two different worlds: the terrain matches and every scattered feature has moved.
     * Registration order cannot be used, because on Fabric it is the mod loading order that would
     * have to match and that is not ours to set.
     */
    private static final Comparator<BiomeModification> FABRIC_ORDER =
            Comparator.comparing(modification -> modification.getConfiguredFeatureKey().identifier());

    @Override
    public void addFeatureToBiomes(BiomePredicate biomePredicate, GenerationStep.Decoration step, Identifier placedFeatureIdentifier) {
        ResourceKey<PlacedFeature> resourceKey = ResourceKey.create(Registries.PLACED_FEATURE, placedFeatureIdentifier);
        biomeModifications.add(new BiomeModification(biomePredicate, step, resourceKey));
        biomeModifications.sort(FABRIC_ORDER);
    }

    @Override
    public void removeFeatureFromBiomes(BiomePredicate biomePredicate, GenerationStep.Decoration step, Identifier placedFeatureIdentifier) {
        ResourceKey<PlacedFeature> resourceKey = ResourceKey.create(Registries.PLACED_FEATURE, placedFeatureIdentifier);
        biomeRemovals.add(new BiomeModification(biomePredicate, step, resourceKey));
        biomeRemovals.sort(FABRIC_ORDER);
    }

    private static final List<SpawnAddition> spawnAdditions = new CopyOnWriteArrayList<>();

    /**
     * Fabric names each spawn addition after its entity type and applies them in that order, which
     * is the order a biome's spawn list is drawn from; the same world has to draw the same mobs.
     * The ids are only known once the types are registered, so this sorts when the biomes load.
     */
    private static final Comparator<SpawnAddition> FABRIC_SPAWN_ORDER =
            Comparator.comparing(addition -> BuiltInRegistries.ENTITY_TYPE.getKey(addition.type().get()));

    @Override
    public void addSpawnToBiomes(BiomePredicate biomePredicate, Supplier<? extends EntityType<?>> type, IntSupplier weight, int minCount, int maxCount) {
        spawnAdditions.add(new SpawnAddition(biomePredicate, type, weight, minCount, maxCount));
    }

    private record SpawnAddition(BiomePredicate biomePredicate, Supplier<? extends EntityType<?>> type, IntSupplier weight, int minCount, int maxCount) {
    }

    public static void modifyBiome(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == BiomeModifier.Phase.REMOVE) {
            Identifier location = biome.unwrapKey().map(ResourceKey::identifier).orElse(null);
            for (var removal : biomeRemovals) {
                if (location != null && removal.getBiomePredicate().test(location, biome)) {
                    builder.getGenerationSettings().getFeatures(removal.getStep()).removeIf(feature -> feature.is(removal.getConfiguredFeatureKey()));
                }
            }
        }

        if (phase == BiomeModifier.Phase.ADD) {
            Identifier biomeId = biome.unwrapKey().map(ResourceKey::identifier).orElse(null);
            if (biomeId != null) {
                spawnAdditions.stream().sorted(FABRIC_SPAWN_ORDER).filter(addition -> addition.biomePredicate().test(biomeId, biome)).forEach(addition -> {
                    EntityType<?> type = addition.type().get();
                    builder.getMobSpawnSettings().addSpawn(type.getCategory(), addition.weight().getAsInt(), new MobSpawnSettings.SpawnerData(type, addition.minCount(), addition.maxCount()));
                });
            }

            for (var biomeModification : biomeModifications) {
                Identifier location = biome.unwrapKey().map(ResourceKey::identifier).orElse(null);
                if (location != null && biomeModification.getBiomePredicate().test(location, biome)) {
                    Registry<PlacedFeature> placedFeatures = ServerLifecycleHooks.getCurrentServer()
                            .registryAccess()
                            .lookupOrThrow(Registries.PLACED_FEATURE);
                    placedFeatures.get(biomeModification.getConfiguredFeatureKey())
                            .ifPresent(placedFeature -> builder.getGenerationSettings().addFeature(biomeModification.getStep(), placedFeature));
                }
            }
        }
    }
}
