package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.grim3212.assorted.lib.gametest.LibTestSupport.*;

/**
 * Composed ingredients - OR, AND and DIFFERENCE - accepting the same items on both loaders.
 */
final class IngredientTests {

    private IngredientTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("ingredients_combine", IngredientTests::ingredientsCombine);
        out.accept("fluid_ingredient_shows_filled_containers", IngredientTests::fluidIngredientShowsFilledContainers);
    }

    /**
     * Composed ingredients accept and reject the same stacks on both loaders. Each loader builds
     * them from its own types, so nothing here is shared code.
     */
    private static void ingredientsCombine(GameTestHelper helper) {
        Ingredient either = Services.INGREDIENTS.or(Ingredient.of(Items.STICK), Ingredient.of(Items.STONE));
        helper.assertTrue(either.test(new ItemStack(Items.STICK)), "an OR ingredient rejected its first branch");
        helper.assertTrue(either.test(new ItemStack(Items.STONE)), "an OR ingredient rejected its second branch");
        helper.assertFalse(either.test(new ItemStack(Items.DIRT)), "an OR ingredient accepted an item in neither branch");

        // A single element OR has to stay usable: both helpers special-case the one-argument call
        // rather than wrapping it.
        Ingredient single = Services.INGREDIENTS.or(Ingredient.of(Items.STICK));
        helper.assertTrue(single.test(new ItemStack(Items.STICK)), "a single branch OR rejected its own item");
        helper.assertFalse(single.test(new ItemStack(Items.STONE)), "a single branch OR accepted a foreign item");

        Ingredient allButStone = Services.INGREDIENTS.difference(
                Ingredient.of(Items.STICK, Items.STONE, Items.DIRT), Ingredient.of(Items.STONE));
        helper.assertTrue(allButStone.test(new ItemStack(Items.STICK)), "a DIFFERENCE ingredient dropped an item it should keep");
        helper.assertTrue(allButStone.test(new ItemStack(Items.DIRT)), "a DIFFERENCE ingredient dropped an item it should keep");
        helper.assertFalse(allButStone.test(new ItemStack(Items.STONE)), "a DIFFERENCE ingredient kept the item it subtracts");

        Ingredient both = Services.INGREDIENTS.and(Ingredient.of(Items.STICK, Items.STONE), Ingredient.of(Items.STONE, Items.DIRT));
        helper.assertTrue(both.test(new ItemStack(Items.STONE)), "an AND ingredient rejected the item both branches accept");
        helper.assertFalse(both.test(new ItemStack(Items.STICK)), "an AND ingredient accepted an item only its first branch accepts");
        helper.assertFalse(both.test(new ItemStack(Items.DIRT)), "an AND ingredient accepted an item only its second branch accepts");

        Ingredient singleAnd = Services.INGREDIENTS.and(Ingredient.of(Items.STICK));
        helper.assertTrue(singleAnd.test(new ItemStack(Items.STICK)), "a single branch AND rejected its own item");
        helper.assertFalse(singleAnd.test(new ItemStack(Items.STONE)), "a single branch AND accepted a foreign item");

        helper.succeed();
    }

    /**
     * A fluid ingredient matches a container by what is inside it, and draws the containers holding
     * that fluid rather than the bare items it matches. Both loaders ask the custom ingredient for
     * its own items ({@code Ingredient#getItems()}), so the same statement holds on each. AssortedTools covers the half
     * that only a modded container shows: one whose fluid lives in components.
     */
    private static void fluidIngredientShowsFilledContainers(GameTestHelper helper) {
        Ingredient water = Services.INGREDIENTS.fluid(null, FluidTags.WATER, Services.FLUIDS.getBucketAmount());

        helper.assertTrue(water.test(new ItemStack(Items.WATER_BUCKET)), "a water fluid ingredient rejected a water bucket");
        helper.assertFalse(water.test(new ItemStack(Items.BUCKET)), "a water fluid ingredient accepted an empty bucket");
        helper.assertFalse(water.test(new ItemStack(Items.LAVA_BUCKET)), "a water fluid ingredient accepted a lava bucket");

        List<ItemStack> shown = List.of(water.getItems());
        helper.assertFalse(shown.isEmpty(), "a fluid ingredient draws nothing at all");
        helper.assertTrue(shown.stream().anyMatch(stack -> stack.is(Items.WATER_BUCKET)),
                "a water fluid ingredient draws " + shown + ", with no water bucket among them");
        helper.assertFalse(shown.stream().anyMatch(stack -> stack.is(Items.BUCKET)),
                "a water fluid ingredient draws an empty bucket, which it does not accept");

        helper.succeed();
    }
}
