package com.grim3212.assorted.lib.manual;

/**
 * How the manual should draw a {@link net.minecraft.world.item.crafting.Recipe} that is none of
 * vanilla's shaped, shapeless, cooking or single item recipes, such as a machine recipe. Those are
 * read from their own ingredients and result instead.
 */
public interface IManualRecipeProvider {

    ManualRecipeView manualView();
}
