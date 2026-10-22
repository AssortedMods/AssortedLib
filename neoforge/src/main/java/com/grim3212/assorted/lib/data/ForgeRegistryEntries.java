package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.crafting.BurnTimes;
import com.grim3212.assorted.lib.family.Families;
import com.grim3212.assorted.lib.platform.ForgePlatformHelper;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * A mod's datapack registry entries, written for both loaders, and NeoForge's fire odds for its flammable blocks.
 * Since 26.3 recipes, loot tables and advancements are registry entries too, so they come through here.
 */
public final class ForgeRegistryEntries {

    private final String modId;
    private final List<LibDatapackRegistryProvider> world = new ArrayList<>();
    private final List<MultiRegistryBootstrap> recipes = new ArrayList<>();
    private final List<LootTableProvider.SubProviderEntry> loot = new ArrayList<>();
    private final Set<Integer> burnTimes = new TreeSet<>();
    private final List<AdvancementSubProvider.Factory> advancements = new ArrayList<>();

    private ForgeRegistryEntries(String modId) {
        this.modId = modId;
    }

    public static ForgeRegistryEntries of(String modId) {
        return new ForgeRegistryEntries(modId);
    }

    /** Worldgen, enchantments and the other world layer registries, with their load conditions. */
    public ForgeRegistryEntries world(LibDatapackRegistryProvider entries) {
        this.world.add(entries);
        return this;
    }

    public ForgeRegistryEntries recipes(BiFunction<BootstrapContext<Recipe<?>>, BootstrapContext<Advancement>, ? extends RecipeProvider> provider) {
        this.recipes.add(RecipeProvider.asBootstrap(provider));
        return this;
    }

    public ForgeRegistryEntries blockLoot(LootTableSubProvider.Factory provider) {
        return this.loot(provider, LootContextParamSets.BLOCK);
    }

    public ForgeRegistryEntries loot(LootTableSubProvider.Factory provider, ContextKeySet params) {
        this.loot.add(new LootTableProvider.SubProviderEntry(provider, params));
        return this;
    }

    public ForgeRegistryEntries advancements(AdvancementSubProvider.Factory provider) {
        this.advancements.add(provider);
        return this;
    }

    /** The burn times the mod's fuels name that vanilla has no entry for, see {@link BurnTimes}. */
    public ForgeRegistryEntries burnTimes(Collection<Integer> ticks) {
        this.burnTimes.addAll(ticks);
        return this;
    }

    /**
     * Adds the providers, the world layer first: a loot table or recipe can name the mod's own
     * enchantments or features, so the reloadable layer is patched onto the world one.
     */
    public void addTo(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        // A family's parts each write its shared entries, such as the advancement root, under the family's namespace.
        Set<String> modIds = Families.familyOf(this.modId).map(family -> Set.of(this.modId, family)).orElse(Set.of(this.modId));

        CompletableFuture<HolderLookup.Provider> worldLookup = event.getWorldLookupProvider();
        if (!this.world.isEmpty()) {
            RegistrySetBuilder builder = new RegistrySetBuilder();
            this.world.forEach(entries -> entries.addEntries(new ConditionalBuilder(builder, entries)));
            DatapackBuiltinEntriesProvider provider = DatapackBuiltinEntriesProvider.forWorldLayer(output, "World: " + this.modId, worldLookup, builder, modIds);
            event.addProvider(crossLoader(provider));
            worldLookup = provider.getRegistryProvider();
        }

        if (!this.recipes.isEmpty() || !this.loot.isEmpty() || !this.burnTimes.isEmpty() || !this.advancements.isEmpty()) {
            RegistrySetBuilder builder = new RegistrySetBuilder();
            this.recipes.forEach(builder::add);
            if (!this.advancements.isEmpty()) {
                builder.add(Registries.ADVANCEMENT, new AdvancementProvider(List.copyOf(this.advancements)));
            }
            if (!this.burnTimes.isEmpty()) {
                builder.add(Registries.CONTEXT_INT_PROVIDER, context -> BurnTimes.bootstrap(context, this.modId, this.burnTimes));
            }
            if (!this.loot.isEmpty()) {
                builder.add(Registries.LOOT_TABLE, new LootTableProvider(Set.of(), List.copyOf(this.loot)));
            }
            event.addProvider(crossLoader(DatapackBuiltinEntriesProvider.forReloadableLayer(output, "Reloadable: " + this.modId, worldLookup,
                    event.getReloadableLookupProvider(), builder, modIds)));
        }

        List<ForgePlatformHelper.FlammableRegistration> flammables = ForgePlatformHelper.flammablesToRegister.stream()
                .filter(flammable -> modIds.contains(BuiltInRegistries.BLOCK.getKey(flammable.block().get()).getNamespace())).toList();
        if (!flammables.isEmpty()) {
            event.addProvider(new FlammableDataMapProvider(output, event.getWorldLookupProvider(), this.modId, flammables));
        }
    }

    /** The provider, writing through {@link CrossLoaderData} so the files carry Fabric's keys too. */
    private static DataProvider crossLoader(DataProvider provider) {
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput output) {
                return provider.run(CrossLoaderData.wrap(output));
            }

            @Override
            public String getName() {
                return provider.getName();
            }
        };
    }

    /** Hands each bootstrap a context that attaches {@link LibDatapackRegistryProvider#conditions()}. */
    private static final class ConditionalBuilder extends RegistrySetBuilder {
        private final RegistrySetBuilder delegate;
        private final LibDatapackRegistryProvider entries;

        private ConditionalBuilder(RegistrySetBuilder delegate, LibDatapackRegistryProvider entries) {
            this.delegate = delegate;
            this.entries = entries;
        }

        @Override
        public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> key, SingleRegistryBootstrap<T> bootstrap) {
            this.delegate.add(key, context -> bootstrap.run(this.conditional(context)));
            return this;
        }

        @Override
        public RegistrySetBuilder add(MultiRegistryBootstrap bootstrap) {
            this.delegate.add(new MultiRegistryBootstrap() {
                @Override
                public Set<ResourceKey<? extends Registry<?>>> requestedRegistries() {
                    return bootstrap.requestedRegistries();
                }

                @Override
                public void run(MultiRegistryBootstrap.BootstrapGetter registries) {
                    bootstrap.run(new MultiRegistryBootstrap.BootstrapGetter() {
                        @Override
                        public <T> BootstrapContext<T> get(ResourceKey<? extends Registry<T>> key) {
                            return ConditionalBuilder.this.conditional(registries.get(key));
                        }
                    });
                }
            });
            return this;
        }

        private <T> BootstrapContext<T> conditional(BootstrapContext<T> context) {
            return Services.CONDITIONS.conditionalContext(context, key -> this.entries.conditions().get(key));
        }
    }
}
