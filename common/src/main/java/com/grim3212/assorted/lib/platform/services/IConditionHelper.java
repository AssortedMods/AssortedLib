package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.core.conditions.LibCondition;
import net.minecraft.data.recipes.RecipeOutput;
import com.grim3212.assorted.lib.core.conditions.LibConditionProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IConditionHelper {
    void init();

    /**
     * Wraps a datagen recipe output so every recipe saved through it, and its unlock advancement, is written with
     * the load conditions {@code conditions} names for the recipe's id (null or empty for none). It is asked on
     * each save, so it may be filled in later.
     */
    RecipeOutput conditionalOutput(RecipeOutput output, Function<ResourceLocation, List<LibConditionProvider>> conditions);

    void register(ResourceLocation name, LibCondition condition);

    LibConditionProvider and(LibConditionProvider... values);

    LibConditionProvider not(LibConditionProvider value);

    LibConditionProvider or(LibConditionProvider... values);

    LibConditionProvider blockExists(ResourceLocation block);

    LibConditionProvider itemExists(ResourceLocation item);

    LibConditionProvider blockTagExists(TagKey<Block> tag);

    LibConditionProvider itemTagExists(TagKey<Item> tag);

    LibConditionProvider modLoaded(String modId);

    LibConditionProvider partEnabled(String partId);

    void registerPartCondition(String part, Supplier<Boolean> check);
}
