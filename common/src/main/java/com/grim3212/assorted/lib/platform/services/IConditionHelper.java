package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.core.conditions.LibCondition;
import net.minecraft.data.worldgen.BootstrapContext;
import com.grim3212.assorted.lib.core.conditions.LibConditionProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IConditionHelper {
    void init();

    /**
     * Gives every entry registered through a datagen context its load conditions, as recipes, loot and advancements
     * are registry entries in 26.3. {@code conditions} is asked on each register, so it may be filled in later.
     */
    <T> BootstrapContext<T> conditionalContext(BootstrapContext<T> context, Function<ResourceKey<T>, List<LibConditionProvider>> conditions);

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
