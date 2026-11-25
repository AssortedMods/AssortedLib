package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.crafting.ingredient.FabricFluidIngredient;
import com.grim3212.assorted.lib.platform.services.IIngredientHelper;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public class FabricIngredientHelper implements IIngredientHelper {
    @Override
    public void register() {
        CustomIngredientSerializer.register(FabricFluidIngredient.SERIALIZER);
    }

    @Override
    public Ingredient and(Ingredient... ingredients) {
        // Same shape as or() and as NeoForge: an Ingredient may not be empty, and a single branch
        // is handed back as is rather than wrapped.
        if (ingredients.length == 0)
            throw new IllegalArgumentException("You must supply at least 1 ingredient for an AND!");

        return ingredients.length == 1 ? ingredients[0] : DefaultCustomIngredients.all(ingredients);
    }

    @Override
    public Ingredient or(Ingredient... ingredients) {
        // An empty OR would silently match nothing, so it is rejected here, as NeoForge does.
        if (ingredients.length == 0)
            throw new IllegalArgumentException("You must supply at least 1 ingredient for an OR!");

        return ingredients.length == 1 ? ingredients[0] : DefaultCustomIngredients.any(ingredients);
    }

    @Override
    public Ingredient difference(Ingredient base, Ingredient subtracted) {
        return DefaultCustomIngredients.difference(base, subtracted);
    }

    @Override
    public Ingredient nbt(ItemStack item) {
        // Stacks carry data components rather than NBT, so the strict match is a component patch match.
        return DefaultCustomIngredients.components(item);
    }

    @Override
    public Ingredient fluid(@Nullable TagKey<Item> itemTagKey, TagKey<Fluid> fluidTagKey, long amount) {
        return FabricFluidIngredient.of(itemTagKey, fluidTagKey, amount).toVanilla();
    }
}
