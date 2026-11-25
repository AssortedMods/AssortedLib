package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.family.Families;
import com.grim3212.assorted.lib.platform.ForgeConditionHelper;
import com.grim3212.assorted.lib.platform.ForgePlatformHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A mod's server data, written for both loaders: its datapack registry entries, recipes, loot tables and
 * advancements, and the NeoForge data maps for the fuels and compostables it registered. Everything a datapack
 * registry entry, a recipe or an advancement says is written through {@link CrossLoaderData}, so the files carry
 * Fabric's keys too.
 */
public final class ForgeRegistryEntries {

    private final String modId;
    private final List<LibDatapackRegistryProvider> world = new ArrayList<>();
    private final List<BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, ? extends RecipeProvider>> recipes = new ArrayList<>();
    private final List<LootTableProvider.SubProviderEntry> loot = new ArrayList<>();
    private final List<AdvancementProvider.AdvancementGenerator> advancements = new ArrayList<>();

    private ForgeRegistryEntries(String modId) {
        this.modId = modId;
    }

    public static ForgeRegistryEntries of(String modId) {
        return new ForgeRegistryEntries(modId);
    }

    /** Worldgen, enchantments and the other datapack registries, with their load conditions. */
    public ForgeRegistryEntries world(LibDatapackRegistryProvider entries) {
        this.world.add(entries);
        return this;
    }

    public ForgeRegistryEntries recipes(BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, ? extends RecipeProvider> provider) {
        this.recipes.add(provider);
        return this;
    }

    public ForgeRegistryEntries blockLoot(Function<HolderLookup.Provider, LootTableSubProvider> provider) {
        return this.loot(provider, LootContextParamSets.BLOCK);
    }

    public ForgeRegistryEntries loot(Function<HolderLookup.Provider, LootTableSubProvider> provider, LootContextParamSet params) {
        this.loot.add(new LootTableProvider.SubProviderEntry(provider, params));
        return this;
    }

    public ForgeRegistryEntries advancements(AdvancementProvider.AdvancementGenerator provider) {
        this.advancements.add(provider);
        return this;
    }

    /**
     * Adds the providers, the datapack registries first: a loot table or recipe can name the mod's own
     * enchantments or features, so the others read the lookup those entries were added to.
     */
    public void addTo(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        // A family's parts each write its shared entries, such as the advancement root, under the family's namespace.
        Set<String> modIds = Families.familyOf(this.modId).map(family -> Set.of(this.modId, family)).orElse(Set.of(this.modId));
        boolean server = event.includeServer();

        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        if (!this.world.isEmpty()) {
            RegistrySetBuilder builder = new RegistrySetBuilder();
            Map<ResourceKey<?>, List<ICondition>> conditions = new HashMap<>();
            for (LibDatapackRegistryProvider entries : this.world) {
                entries.addEntries(builder);
                entries.conditions().forEach((key, providers) -> conditions.computeIfAbsent(key, k -> new ArrayList<>())
                        .addAll(providers.stream().map(ForgeConditionHelper::unwrap).toList()));
            }
            DatapackBuiltinEntriesProvider provider = new DatapackBuiltinEntriesProvider(output, lookup, builder, conditions, modIds);
            event.getGenerator().addProvider(server, crossLoader(provider, "Registries: " + this.modId));
            lookup = provider.getRegistryProvider();
        }

        for (BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, ? extends RecipeProvider> recipes : this.recipes) {
            RecipeProvider provider = recipes.apply(output, lookup);
            event.getGenerator().addProvider(server, crossLoader(provider, provider.getName() + ": " + this.modId));
        }

        if (!this.loot.isEmpty()) {
            event.getGenerator().addProvider(server, crossLoader(new LootTableProvider(output, Set.of(), List.copyOf(this.loot), lookup), "Loot tables: " + this.modId));
        }

        if (!this.advancements.isEmpty()) {
            event.getGenerator().addProvider(server, crossLoader(new AdvancementProvider(output, lookup, event.getExistingFileHelper(), List.copyOf(this.advancements)), "Advancements: " + this.modId));
        }

        List<ForgePlatformHelper.FuelRegistration> fuels = ForgePlatformHelper.fuelsToRegister.stream()
                .filter(fuel -> modIds.contains(BuiltInRegistries.ITEM.getKey(fuel.item().get().asItem()).getNamespace())).toList();
        List<ForgePlatformHelper.CompostableRegistration> compostables = ForgePlatformHelper.compostablesToRegister.stream()
                .filter(compostable -> modIds.contains(BuiltInRegistries.ITEM.getKey(compostable.item().get().asItem()).getNamespace())).toList();
        if (!fuels.isEmpty() || !compostables.isEmpty()) {
            event.getGenerator().addProvider(server, new ForgeDataMapProvider(output, event.getLookupProvider(), this.modId, fuels, compostables));
        }
    }

    /**
     * The provider, writing through {@link CrossLoaderData} so the files carry Fabric's keys too. Named per mod, as
     * a datagen run refuses two providers with one name.
     */
    private static DataProvider crossLoader(DataProvider provider, String name) {
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput output) {
                return provider.run(CrossLoaderData.wrap(output));
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
