package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.conditions.BlockExistsCondition;
import com.grim3212.assorted.lib.conditions.ForgeConditionProvider;
import com.grim3212.assorted.lib.conditions.LibConditions;
import com.grim3212.assorted.lib.conditions.PartEnabledCondition;
import com.grim3212.assorted.lib.conditions.TagPopulatedCondition;
import com.grim3212.assorted.lib.core.conditions.LibCondition;
import com.grim3212.assorted.lib.core.conditions.LibConditionProvider;
import com.grim3212.assorted.lib.platform.services.IConditionHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NeoForgeConditions;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ForgeConditionHelper implements IConditionHelper {

    /**
     * Nothing to do at construction: condition codecs can only be registered from a
     * {@code RegisterEvent}, so {@link LibConditions} collects them and flushes them then.
     */
    @Override
    public void init() {
    }

    @Override
    public RecipeOutput conditionalOutput(RecipeOutput output, Function<Identifier, List<LibConditionProvider>> conditions) {
        return new ConditionalOutput(output, conditions);
    }

    @Override
    public void register(Identifier name, LibCondition condition) {
        LibConditions.register(name, condition);
    }

    @Override
    public LibConditionProvider and(LibConditionProvider... values) {
        return wrap(NeoForgeConditions.and(unwrapAll(values)));
    }

    @Override
    public LibConditionProvider not(LibConditionProvider value) {
        return wrap(NeoForgeConditions.not(unwrap(value)));
    }

    @Override
    public LibConditionProvider or(LibConditionProvider... values) {
        return wrap(NeoForgeConditions.or(unwrapAll(values)));
    }

    @Override
    public LibConditionProvider blockExists(Identifier block) {
        return wrap(new BlockExistsCondition(block));
    }

    @Override
    public LibConditionProvider itemExists(Identifier item) {
        // NeoForge ships this one itself now, as a generic "is this key registered" condition.
        return wrap(NeoForgeConditions.itemRegistered(item));
    }

    @Override
    public LibConditionProvider blockTagExists(TagKey<Block> tag) {
        return wrap(new TagPopulatedCondition.BlockTagPopulatedCondition(tag.location()));
    }

    @Override
    public LibConditionProvider itemTagExists(TagKey<Item> tag) {
        return wrap(new TagPopulatedCondition.ItemTagPopulatedCondition(tag.location()));
    }

    @Override
    public LibConditionProvider modLoaded(String modId) {
        return wrap(NeoForgeConditions.modLoaded(modId));
    }

    @Override
    public LibConditionProvider partEnabled(String partId) {
        return wrap(new PartEnabledCondition(partId));
    }

    @Override
    public void registerPartCondition(String part, Supplier<Boolean> check) {
        LibConditions.registerPartCondition(part, check);
    }

    public static ForgeConditionProvider wrap(ICondition condition) {
        return new ForgeConditionProvider(condition);
    }

    public static ICondition unwrap(LibConditionProvider provider) {
        if (!(provider instanceof ForgeConditionProvider forgeConditionProvider))
            throw new IllegalArgumentException("The given condition is not compatible with the forge platform!");

        return forgeConditionProvider.getCondition();
    }

    private static ICondition[] unwrapAll(LibConditionProvider... providers) {
        return Arrays.stream(providers).map(ForgeConditionHelper::unwrap).toArray(ICondition[]::new);
    }

    /**
     * Attaches the conditions registered for a recipe id to that recipe as it is written.
     * {@code RecipeOutput#withConditions} applies one set to everything, so this dispatches on the
     * recipe key. The lookup runs on every accept, so a provider may still fill it after wrapping.
     */
    private record ConditionalOutput(RecipeOutput delegate, Function<Identifier, List<LibConditionProvider>> conditions) implements RecipeOutput {

        @Override
        public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... extraConditions) {
            List<LibConditionProvider> registered = this.conditions.apply(key.identifier());
            if (registered == null || registered.isEmpty()) {
                this.delegate.accept(key, recipe, advancement, extraConditions);
                return;
            }

            ICondition[] all = Stream.concat(Arrays.stream(extraConditions), registered.stream().map(ForgeConditionHelper::unwrap)).toArray(ICondition[]::new);
            this.delegate.accept(key, recipe, advancement, all);
        }

        @Override
        public Advancement.Builder advancement() {
            return this.delegate.advancement();
        }

        @Override
        public void includeRootAdvancement() {
            this.delegate.includeRootAdvancement();
        }
    }
}
