package com.grim3212.assorted.lib.manual;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A recipe reduced to what the manual draws: a grid of input slots and a result, left unresolved so
 * each slot keeps the ingredient behind it. Where they land on the page is
 * {@link com.grim3212.assorted.lib.client.manual.ManualRecipeLayout}.
 *
 * @param width     input slots per row
 * @param height    rows of input slots
 * @param inputs    {@code width * height} slots in reading order
 * @param shapeless whether the inputs may go in any arrangement, which the book marks
 */
public record ManualRecipeView(int width, int height, List<ManualSlot> inputs, ManualSlot result, boolean shapeless) {

    /** Packed into rows of three, the width a crafting grid gives them. */
    public static ManualRecipeView shapeless(List<ManualSlot> inputs, ManualSlot result) {
        int width = Math.min(3, Math.max(1, inputs.size()));
        return new ManualRecipeView(width, ceilDiv(inputs.size(), width), padded(inputs, width), result, true);
    }

    public static ManualRecipeView shaped(int width, int height, List<ManualSlot> inputs, ManualSlot result) {
        return new ManualRecipeView(width, height, inputs, result, false);
    }

    /**
     * Empty when the recipe is neither an {@link IManualRecipeProvider} nor one of vanilla's shaped,
     * shapeless, cooking or single item recipes, or is {@link Recipe#isSpecial special}.
     *
     * @param registries what the result is read with
     */
    public static Optional<ManualRecipeView> of(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        Recipe<?> recipe = holder.value();

        if (recipe instanceof IManualRecipeProvider provider) {
            return Optional.of(provider.manualView());
        }

        if (recipe.isSpecial()) {
            return Optional.empty();
        }

        ManualSlot result = ManualSlot.of(recipe.getResultItem(registries));
        return switch (recipe) {
            case ShapedRecipe shaped -> Optional.of(shaped(
                    shaped.getWidth(), shaped.getHeight(), slots(shaped.getIngredients()), result));
            case ShapelessRecipe shapeless -> Optional.of(shapeless(slots(shapeless.getIngredients()), result));
            case AbstractCookingRecipe cooking -> Optional.of(shaped(1, 1, slots(cooking.getIngredients()), result));
            case SingleItemRecipe single -> Optional.of(shaped(1, 1, slots(single.getIngredients()), result));
            default -> Optional.empty();
        };
    }

    private static List<ManualSlot> slots(List<Ingredient> ingredients) {
        return ingredients.stream().map(ManualSlot::of).toList();
    }

    /** Pads the last row so every row is {@code width} slots. */
    private static List<ManualSlot> padded(List<ManualSlot> inputs, int width) {
        List<ManualSlot> padded = new ArrayList<>(inputs);
        while (padded.size() % width != 0) {
            padded.add(ManualSlot.EMPTY);
        }
        return List.copyOf(padded);
    }

    private static int ceilDiv(int value, int divisor) {
        return Math.max(1, (value + divisor - 1) / divisor);
    }

    public ManualSlot slot(int column, int row) {
        int index = row * this.width + column;
        return index >= 0 && index < this.inputs.size() ? this.inputs.get(index) : ManualSlot.EMPTY;
    }

    public boolean isEmpty() {
        return this.result.isEmpty() && this.inputs.stream().allMatch(ManualSlot::isEmpty);
    }
}
