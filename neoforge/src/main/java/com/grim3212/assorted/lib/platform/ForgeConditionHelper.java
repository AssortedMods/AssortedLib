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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.AndCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ItemExistsCondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
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
    public RecipeOutput conditionalOutput(RecipeOutput output, Function<ResourceLocation, List<LibConditionProvider>> conditions) {
        return new ConditionalOutput(output, conditions);
    }

    @Override
    public void register(ResourceLocation name, LibCondition condition) {
        LibConditions.register(name, condition);
    }

    @Override
    public LibConditionProvider and(LibConditionProvider... values) {
        return wrap(new AndCondition(List.of(unwrapAll(values))));
    }

    @Override
    public LibConditionProvider not(LibConditionProvider value) {
        return wrap(new NotCondition(unwrap(value)));
    }

    @Override
    public LibConditionProvider or(LibConditionProvider... values) {
        return wrap(new OrCondition(List.of(unwrapAll(values))));
    }

    @Override
    public LibConditionProvider blockExists(ResourceLocation block) {
        return wrap(new BlockExistsCondition(block));
    }

    @Override
    public LibConditionProvider itemExists(ResourceLocation item) {
        return wrap(new ItemExistsCondition(item));
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
        return wrap(new ModLoadedCondition(modId));
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
     * Adds each recipe's conditions to any the caller passes. NeoForge writes the recipe's unlock
     * advancement with the same conditions, so both come and go together.
     */
    private record ConditionalOutput(RecipeOutput delegate, Function<ResourceLocation, List<LibConditionProvider>> conditions) implements RecipeOutput {

        @Override
        public Advancement.Builder advancement() {
            return this.delegate.advancement();
        }

        @Override
        public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... extraConditions) {
            List<LibConditionProvider> registered = this.conditions.apply(id);
            if (registered == null || registered.isEmpty()) {
                this.delegate.accept(id, recipe, advancement, extraConditions);
                return;
            }

            ICondition[] all = Stream.concat(Arrays.stream(extraConditions), registered.stream().map(ForgeConditionHelper::unwrap)).toArray(ICondition[]::new);
            this.delegate.accept(id, recipe, advancement, all);
        }
    }
}
