package com.grim3212.assorted.lib.core.conditions;

import com.grim3212.assorted.lib.conditions.PartToggles;
import com.grim3212.assorted.lib.data.CrossLoaderData;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class ConditionalRecipeProvider extends RecipeProvider {

    protected final Map<Identifier, List<LibConditionProvider>> conditions;
    private final String modId;

    public ConditionalRecipeProvider(HolderLookup.Provider registries, RecipeOutput output, String modId) {
        this(registries, output, modId, new HashMap<>());
    }

    // The map has to exist before the super call so the wrapped output can close over it, which is
    // why this goes through a private constructor. The wrapper reads it lazily on each accept, so
    // registerConditions() populating it later still works.
    private ConditionalRecipeProvider(HolderLookup.Provider registries, RecipeOutput output, String modId, Map<Identifier, List<LibConditionProvider>> conditions) {
        super(registries, Services.CONDITIONS.conditionalOutput(output, recipe -> withPart(conditions.get(recipe), modId)));
        this.modId = modId;
        this.conditions = conditions;
    }

    /** A mod with a {@link PartToggles} switch has every recipe turned off with it. */
    private static List<LibConditionProvider> withPart(@Nullable List<LibConditionProvider> conditions, String modId) {
        if (!PartToggles.has(modId)) {
            return conditions;
        }

        List<LibConditionProvider> all = conditions == null ? new ArrayList<>() : new ArrayList<>(conditions);
        all.add(Services.CONDITIONS.partEnabled(modId));
        return all;
    }

    public LibConditionProvider and(LibConditionProvider... providers) {
        return Services.CONDITIONS.and(providers);
    }

    public LibConditionProvider or(LibConditionProvider... providers) {
        return Services.CONDITIONS.or(providers);
    }

    public LibConditionProvider not(LibConditionProvider provider) {
        return Services.CONDITIONS.not(provider);
    }

    public LibConditionProvider itemTagExists(TagKey<Item> itemTag) {
        return Services.CONDITIONS.itemTagExists(itemTag);
    }

    public LibConditionProvider modLoaded(String modId) {
        return Services.CONDITIONS.modLoaded(modId);
    }

    public LibConditionProvider partEnabled(String part) {
        return Services.CONDITIONS.partEnabled(part);
    }

    public Ingredient and(Ingredient... ingredients) {
        return Services.INGREDIENTS.and(ingredients);
    }

    public Ingredient or(Ingredient... ingredients) {
        return Services.INGREDIENTS.or(ingredients);
    }

    public Ingredient difference(Ingredient base, Ingredient subtracted) {
        return Services.INGREDIENTS.difference(base, subtracted);
    }

    public Ingredient nbt(ItemStack item) {
        return Services.INGREDIENTS.nbt(item);
    }

    public Ingredient fluid(@Nullable TagKey<Item> itemTagKey, TagKey<Fluid> fluidTagKey, long amount) {
        return Services.INGREDIENTS.fluid(itemTagKey, fluidTagKey, amount);
    }

    public Ingredient fluid(TagKey<Fluid> fluidTagKey) {
        return Services.INGREDIENTS.fluid(null, fluidTagKey, Services.FLUIDS.getBucketAmount());
    }

    protected String name(Item i) {
        return id(i).getPath();
    }

    protected String name(Block b) {
        return id(b).getPath();
    }

    protected Identifier id(Item i) {
        return Services.PLATFORM.getRegistry(Registries.ITEM).getRegistryName(i);
    }

    protected Identifier id(Block b) {
        return Services.PLATFORM.getRegistry(Registries.BLOCK).getRegistryName(b);
    }

    protected Identifier prefix(String name) {
        return Identifier.fromNamespaceAndPath(this.modId, name);
    }

    public void addConditions(LibConditionProvider condition, Identifier... recipes) {
        if (recipes.length == 0)
            return;

        for (Identifier recipe : recipes) {
            this.conditions.computeIfAbsent(recipe, (r) -> new ArrayList<>()).add(condition);
        }
    }

    /**
     * Used to register the conditions for recipes before the recipes are built
     * Makes sure that the conditions are available when building the recipe JSONs
     */
    public abstract void registerConditions();

    // public, not protected: RecipeProvider.buildRecipes() is public abstract on the vanilla jar
    // (what Fabric compiles against) and protected on NeoForge's patched jar. Widening satisfies
    // both.
    @Override
    public void buildRecipes() {
        this.registerConditions();
    }

    /**
     * Writes a mod's recipes for both loaders. Recipe providers are no longer data providers
     * themselves in 26.2 - a {@link RecipeProvider.Runner} owns the file writing - and its
     * {@code run} is final, so this wraps one and hands it a {@link CrossLoaderData} output.
     */
    public abstract static class Runner implements DataProvider {
        protected final String modId;
        private final RecipeProvider.Runner delegate;

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String modId) {
            this.modId = modId;
            this.delegate = new RecipeProvider.Runner(output, registries) {
                @Override
                protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookup, RecipeOutput recipeOutput) {
                    return Runner.this.createRecipeProvider(lookup, recipeOutput);
                }

                @Override
                public String getName() {
                    return Runner.this.getName();
                }
            };
        }

        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return this.delegate.run(CrossLoaderData.wrap(output));
        }

        protected abstract RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output);
    }
}
