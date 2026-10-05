package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.core.conditions.LibCondition;
import net.minecraft.data.recipes.RecipeOutput;
import com.grim3212.assorted.lib.core.conditions.LibConditionProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IConditionHelper {
    void init();

    /**
     * Wraps a {@link RecipeOutput} so every recipe written through it carries the conditions
     * registered for its id. Recipes are serialised by codec, so the output is the only place to
     * attach them.
     *
     * @param conditions the conditions for a recipe id, or null for none. Asked on each accept, so
     *                   what it reads may still be filled after this call.
     */
    RecipeOutput conditionalOutput(RecipeOutput output, Function<Identifier, List<LibConditionProvider>> conditions);

    void register(Identifier name, LibCondition condition);

    LibConditionProvider and(LibConditionProvider... values);

    LibConditionProvider not(LibConditionProvider value);

    LibConditionProvider or(LibConditionProvider... values);

    LibConditionProvider blockExists(Identifier block);

    LibConditionProvider itemExists(Identifier item);

    LibConditionProvider blockTagExists(TagKey<Block> tag);

    LibConditionProvider itemTagExists(TagKey<Item> tag);

    LibConditionProvider modLoaded(String modId);

    LibConditionProvider partEnabled(String partId);

    void registerPartCondition(String part, Supplier<Boolean> check);
}
