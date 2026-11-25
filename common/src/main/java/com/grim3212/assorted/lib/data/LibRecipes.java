package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.conditions.ConditionalRecipeProvider;
import com.grim3212.assorted.lib.manual.LibItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

/** The library ships one recipe, for the one item it has. */
public class LibRecipes extends ConditionalRecipeProvider {

    public LibRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, LibConstants.MOD_ID);
    }

    @Override
    public void registerConditions() {
    }

    @Override
    public void buildRecipes() {
        super.buildRecipes();

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, LibItems.INSTRUCTION_MANUAL.get())
                .requires(Items.BOOK)
                .requires(Items.LEATHER)
                .requires(Ingredient.of(LibCommonTags.Items.DYES))
                .unlockedBy("has_book", has(Items.BOOK))
                .save(this.output);
    }
}
