package com.grim3212.assorted.lib.core.conditions;

import com.grim3212.assorted.lib.conditions.PartToggles;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

    protected final Map<ResourceLocation, List<LibConditionProvider>> conditions = new HashMap<>();
    private final String modId;
    private HolderLookup.Provider lookupProvider;
    /** Where {@link #buildRecipes()} saves to. Every recipe saved here, and its unlock advancement, carries the conditions. */
    protected RecipeOutput output;

    public ConditionalRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, String modId) {
        super(packOutput, registries);
        this.modId = modId;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache, HolderLookup.Provider registries) {
        this.lookupProvider = registries;
        return super.run(cache, registries);
    }

    /**
     * Wraps vanilla's output so each recipe is written with its conditions, read as it is saved so
     * {@link #registerConditions()} can still fill them, then hands over to {@link #buildRecipes()}.
     */
    @Override
    public final void buildRecipes(RecipeOutput recipeOutput) {
        this.output = Services.CONDITIONS.conditionalOutput(recipeOutput, id -> withPart(this.conditions.get(id), this.modId));
        this.buildRecipes();
    }

    /** Holders from any registry the recipes name. Only valid while the recipes are being built. */
    protected <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
        return this.lookupProvider.lookupOrThrow(key);
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

    protected ResourceLocation id(Item i) {
        return Services.PLATFORM.getRegistry(Registries.ITEM).getRegistryName(i);
    }

    protected ResourceLocation id(Block b) {
        return Services.PLATFORM.getRegistry(Registries.BLOCK).getRegistryName(b);
    }

    protected ResourceLocation prefix(String name) {
        return ResourceLocation.fromNamespaceAndPath(this.modId, name);
    }

    public void addConditions(LibConditionProvider condition, ResourceLocation... recipes) {
        if (recipes.length == 0)
            return;

        for (ResourceLocation recipe : recipes) {
            this.conditions.computeIfAbsent(recipe, (r) -> new ArrayList<>()).add(condition);
        }
    }

    /**
     * Used to register the conditions for recipes before the recipes are built
     * Makes sure that the conditions are available when building the recipe JSONs
     */
    public abstract void registerConditions();

    /** Builds the recipes into {@link #output}. Overrides call {@code super.buildRecipes()} first. */
    public void buildRecipes() {
        this.registerConditions();
    }
}
