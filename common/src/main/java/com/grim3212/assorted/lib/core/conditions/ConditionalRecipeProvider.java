package com.grim3212.assorted.lib.core.conditions;

import com.grim3212.assorted.lib.conditions.PartToggles;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class ConditionalRecipeProvider extends RecipeProvider {

    protected final Map<Identifier, List<LibConditionProvider>> conditions;
    protected final HolderGetter<Item> items;
    private final BootstrapContext<Recipe<?>> recipes;
    private final String modId;

    /**
     * Recipes are registry entries built in a bootstrap now. Both contexts are wrapped so each recipe and its
     * unlock advancement carry the conditions, read lazily so registerConditions() can still fill them.
     */
    public ConditionalRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, String modId) {
        Map<Identifier, List<LibConditionProvider>> conditions = new HashMap<>();
        this.conditions = conditions;
        this.modId = modId;
        this.recipes = recipes;
        this.items = recipes.lookup(Registries.ITEM);
        super(Services.CONDITIONS.conditionalContext(recipes, key -> withPart(conditions.get(key.identifier()), modId)),
                Services.CONDITIONS.conditionalContext(advancements, key -> withPart(conditions.get(unlockedRecipe(conditions, key.identifier())), modId)));
    }

    /** The recipe an unlock advancement belongs to: vanilla's {@code recipes/<folder>/<path>}, or a mod builder's {@code recipes/<path>}. */
    private static Identifier unlockedRecipe(Map<Identifier, List<LibConditionProvider>> conditions, Identifier advancement) {
        String path = advancement.getPath();
        if (!path.startsWith("recipes/")) {
            return advancement;
        }
        Identifier unfiled = advancement.withPath(path.substring("recipes/".length()));
        int folderEnd = path.indexOf('/', "recipes/".length());
        return folderEnd < 0 || conditions.containsKey(unfiled) ? unfiled : advancement.withPath(path.substring(folderEnd + 1));
    }

    /** What {@code RecipeProvider#registries} was for: holders from any registry the recipes name. */
    protected <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
        return this.recipes.lookup(key);
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

    // public, not protected, so the mods' overrides can stay public; widening an override is allowed.
    @Override
    public void buildRecipes() {
        this.registerConditions();
    }
}
