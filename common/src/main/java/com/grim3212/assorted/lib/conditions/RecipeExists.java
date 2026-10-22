package com.grim3212.assorted.lib.conditions;

import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/** For a page whose recipes another mod's ingredient turns on, so it is left out rather than drawn as missing. True when any of them reached the client. */
public record RecipeExists(List<ResourceKey<Recipe<?>>> recipes) implements DisplayCondition {

    public static final MapCodec<RecipeExists> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(ResourceKey.codec(Registries.RECIPE).listOf().fieldOf("recipes").forGetter(RecipeExists::recipes))
            .apply(instance, RecipeExists::new));

    @Override
    public boolean test() {
        return this.recipes.stream().anyMatch(recipe -> SyncedRecipes.byKey(recipe) != null);
    }

    @Override
    public MapCodec<? extends DisplayCondition> codec() {
        return CODEC;
    }
}
