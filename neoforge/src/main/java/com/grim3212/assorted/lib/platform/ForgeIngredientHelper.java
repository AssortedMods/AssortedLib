package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.crafting.ingredient.ForgeFluidIngredient;
import com.grim3212.assorted.lib.platform.services.IIngredientHelper;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class ForgeIngredientHelper implements IIngredientHelper {

    /**
     * An {@link net.neoforged.neoforge.common.crafting.IngredientType} lives in its own registry. This is
     * called from the {@code RegisterEvent} for that registry, while it is still open.
     */
    @Override
    public void register() {
        Registry.register(NeoForgeRegistries.INGREDIENT_TYPES, ForgeFluidIngredient.NAME, ForgeFluidIngredient.TYPE);
    }

    @Override
    public Ingredient and(Ingredient... ingredients) {
        requireNotEmpty(ingredients);
        return ingredients.length == 1 ? ingredients[0] : IntersectionIngredient.of(ingredients);
    }

    @Override
    public Ingredient or(Ingredient... ingredients) {
        requireNotEmpty(ingredients);
        return ingredients.length == 1 ? ingredients[0] : CompoundIngredient.of(ingredients);
    }

    @Override
    public Ingredient difference(Ingredient base, Ingredient subtracted) {
        return DifferenceIngredient.of(base, subtracted);
    }

    @Override
    public Ingredient nbt(ItemStack item) {
        // Takes the whole stack and, with strict set, requires an exact component match.
        return DataComponentIngredient.of(true, item.copy());
    }

    @Override
    public Ingredient fluid(@Nullable TagKey<Item> itemTagKey, TagKey<Fluid> fluidTagKey, long amount) {
        return ForgeFluidIngredient.of(itemTagKey, fluidTagKey, amount).toVanilla();
    }

    // NeoForge's intersection refuses an empty list, and an empty or() would quietly be
    // Ingredient.EMPTY, which matches nothing; a zero length and()/or() is a mistake either way.
    private static void requireNotEmpty(Ingredient... ingredients) {
        if (ingredients.length == 0) {
            throw new IllegalArgumentException("Cannot combine zero ingredients");
        }
    }
}
