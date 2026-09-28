package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.conditions.PartToggles;
import com.grim3212.assorted.lib.platform.services.IWorldGenHelper;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class FabricWorldGenHelper implements IWorldGenHelper {

    /** Fabric names every modification; this keeps two removals of the same feature apart. */
    private static final AtomicInteger REMOVALS = new AtomicInteger();

    @Override
    public void addFeatureToBiomes(BiomePredicate biomePredicate, GenerationStep.Decoration step, Identifier placedFeatureIdentifier) {
        BiomeModifications.addFeature(it -> PartToggles.isEnabled(placedFeatureIdentifier) && biomePredicate.test(it.getBiomeKey().identifier(), it.getBiomeHolder()), step, ResourceKey.create(Registries.PLACED_FEATURE, placedFeatureIdentifier));
    }

    @Override
    public void removeFeatureFromBiomes(BiomePredicate biomePredicate, GenerationStep.Decoration step, Identifier placedFeatureIdentifier) {
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, placedFeatureIdentifier);
        Identifier name = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "remove_" + REMOVALS.getAndIncrement() + "_" + placedFeatureIdentifier.getNamespace() + "_" + placedFeatureIdentifier.getPath().replace('/', '_'));
        BiomeModifications.create(name).add(ModificationPhase.REMOVALS, it -> biomePredicate.test(it.getBiomeKey().identifier(), it.getBiomeHolder()), context -> context.getGenerationSettings().removeFeature(step, key));
    }

    /**
     * {@code BiomeModifications#addSpawn} takes the weight as it is registered, before any config has
     * loaded, so this is the same modification with the weight read as the biomes load. It keeps the
     * name addSpawn gives it, the entity type's id, which is also the order it is applied in.
     */
    @Override
    public void addSpawnToBiomes(BiomePredicate biomePredicate, Supplier<? extends EntityType<?>> type, IntSupplier weight, int minCount, int maxCount) {
        EntityType<?> entityType = type.get();
        BiomeModifications.create(BuiltInRegistries.ENTITY_TYPE.getKey(entityType)).add(ModificationPhase.ADDITIONS,
                it -> PartToggles.isEnabled(BuiltInRegistries.ENTITY_TYPE.getKey(entityType)) && biomePredicate.test(it.getBiomeKey().identifier(), it.getBiomeHolder()),
                context -> context.getMobSpawnSettings().addSpawn(entityType.getCategory(), new MobSpawnSettings.SpawnerData(entityType, minCount, maxCount), weight.getAsInt()));
    }
}
