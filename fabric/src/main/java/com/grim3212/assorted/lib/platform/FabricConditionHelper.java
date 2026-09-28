package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.conditions.LibParts;
import com.grim3212.assorted.lib.core.conditions.LibCondition;
import com.grim3212.assorted.lib.core.conditions.LibConditionProvider;
import com.grim3212.assorted.lib.platform.services.IConditionHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric conditions are codec based in 26.2: a {@link ResourceCondition} is deserialised by the
 * {@link ResourceConditionType} registered for its id, and datagen attaches conditions to the recipe
 * object itself rather than writing them into the recipe json.
 */
public class FabricConditionHelper implements IConditionHelper {
    public static final Identifier PART_ENABLED = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "part_enabled");

    @Override
    public void init() {
        // "item exists" / "block exists" are vanilla Fabric conditions now (registry_contains), so only
        // the part condition still needs a type of its own.
        ResourceConditions.register(PartEnabledCondition.TYPE);
    }

    /**
     * Fabric's datagen keeps the conditions for a written object in a side table keyed by the object
     * itself - the same mechanism {@code FabricRecipeProvider#withConditions} uses - and its data
     * provider mixin writes them out alongside the serialised recipe.
     */
    @Override
    public RecipeOutput conditionalOutput(RecipeOutput output, Function<Identifier, List<LibConditionProvider>> conditions) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, AdvancementHolder advancement) {
                final List<LibConditionProvider> recipeConditions = conditions.apply(key.identifier());
                if (recipeConditions != null && !recipeConditions.isEmpty()) {
                    FabricDataGenHelper.addConditions(recipe, recipeConditions.stream().map(FabricConditionHelper::unwrap).toArray(ResourceCondition[]::new));
                }

                output.accept(key, recipe, advancement);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                output.includeRootAdvancement();
            }

            @Override
            public Identifier getRecipeIdentifier(Identifier identifier) {
                return output.getRecipeIdentifier(identifier);
            }
        };
    }

    @Override
    public void register(Identifier name, LibCondition condition) {
        ResourceConditions.register(new LibResourceCondition(name, condition).getType());
    }

    @Override
    public LibConditionProvider and(LibConditionProvider... values) {
        return wrap(ResourceConditions.and(Arrays.stream(values).map(FabricConditionHelper::unwrap).toArray(ResourceCondition[]::new)));
    }

    @Override
    public LibConditionProvider not(LibConditionProvider value) {
        return wrap(ResourceConditions.not(unwrap(value)));
    }

    @Override
    public LibConditionProvider or(LibConditionProvider... values) {
        return wrap(ResourceConditions.or(Arrays.stream(values).map(FabricConditionHelper::unwrap).toArray(ResourceCondition[]::new)));
    }

    @Override
    public LibConditionProvider blockExists(Identifier block) {
        return wrap(ResourceConditions.registryContains(Registries.BLOCK, block));
    }

    @Override
    public LibConditionProvider itemExists(Identifier item) {
        return wrap(ResourceConditions.registryContains(Registries.ITEM, item));
    }

    @Override
    @SuppressWarnings("unchecked")
    public LibConditionProvider blockTagExists(TagKey<Block> tag) {
        return wrap(ResourceConditions.tagsPopulated(Registries.BLOCK, tag));
    }

    @Override
    @SuppressWarnings("unchecked")
    public LibConditionProvider itemTagExists(TagKey<Item> tag) {
        return wrap(ResourceConditions.tagsPopulated(Registries.ITEM, tag));
    }

    @Override
    public LibConditionProvider modLoaded(String modId) {
        return wrap(ResourceConditions.allModsLoaded(modId));
    }

    @Override
    public LibConditionProvider partEnabled(String partId) {
        return wrap(new PartEnabledCondition(partId));
    }

    @Override
    public void registerPartCondition(String part, Supplier<Boolean> check) {
        LibParts.register(part, check);
    }

    public static LibConditionProvider wrap(ResourceCondition condition) {
        return new LibConditionWrapper(condition);
    }

    public static ResourceCondition unwrap(LibConditionProvider condition) {
        if (condition instanceof LibConditionWrapper wrapper) {
            return wrapper.condition();
        }

        throw new IllegalArgumentException("Condition " + condition.getName() + " was not created by this platform");
    }

    /**
     * A condition whose whole state is its name, so its codec is a unit codec over the single instance
     * created next to the {@link LibCondition} it delegates to.
     */
    private static class LibResourceCondition implements ResourceCondition {
        private final ResourceConditionType<LibResourceCondition> type;
        private final LibCondition condition;

        private LibResourceCondition(Identifier id, LibCondition condition) {
            this.condition = condition;
            this.type = ResourceConditionType.create(id, MapCodec.unit(() -> this));
        }

        @Override
        public ResourceConditionType<?> getType() {
            return this.type;
        }

        @Override
        public boolean test(RegistryOps.RegistryInfoLookup registryInfoLookup) {
            return this.condition.test();
        }
    }

    public record PartEnabledCondition(String part) implements ResourceCondition {
        public static final MapCodec<PartEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("part").forGetter(PartEnabledCondition::part)
        ).apply(instance, PartEnabledCondition::new));
        public static final ResourceConditionType<PartEnabledCondition> TYPE = ResourceConditionType.create(PART_ENABLED, CODEC);

        @Override
        public ResourceConditionType<?> getType() {
            return TYPE;
        }

        @Override
        public boolean test(RegistryOps.RegistryInfoLookup registryInfoLookup) {
            if (!LibParts.isRegistered(this.part)) {
                throw new IllegalArgumentException("Can't check part that doesn't exist!");
            }

            return LibParts.isEnabled(this.part);
        }
    }

    private record LibConditionWrapper(ResourceCondition condition) implements LibConditionProvider {

        @Override
        public Identifier getName() {
            return this.condition.getType().id();
        }
    }
}
