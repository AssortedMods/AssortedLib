package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.conditions.ConditionalRecipeProvider;
import com.grim3212.assorted.lib.manual.LibItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** The library ships one recipe, for the one item it has. */
public class LibRecipes extends ConditionalRecipeProvider {

    public LibRecipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements, LibConstants.MOD_ID);
    }

    @Override
    public void registerConditions() {
    }

    @Override
    public void buildRecipes() {
        super.buildRecipes();

        ShapelessRecipeBuilder.shapeless(this.items, RecipeCategory.TOOLS, LibItems.INSTRUCTION_MANUAL.get())
                .requires(Items.BOOK)
                .requires(Items.LEATHER)
                .requires(this.tag(ItemTags.DYES))
                .unlockedBy("has_book", this.has(Items.BOOK))
                .save(this.output);
    }
}
