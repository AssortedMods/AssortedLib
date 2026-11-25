package com.grim3212.assorted.lib.conditions;

import com.grim3212.assorted.lib.core.conditions.LibCondition;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Wraps a loader agnostic {@link LibCondition} registered through
 * {@code IConditionHelper#register(ResourceLocation, LibCondition)}. A {@link LibCondition} has no
 * parameters, so each wrapper is a singleton per name with a unit codec of itself.
 */
public class RecipeConditionWrapper implements ICondition {

    private final ResourceLocation name;
    private final LibCondition condition;
    private final MapCodec<RecipeConditionWrapper> codec;

    public RecipeConditionWrapper(ResourceLocation name, LibCondition condition) {
        this.name = name;
        this.condition = condition;
        this.codec = MapCodec.unit(() -> this);
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public LibCondition getCondition() {
        return this.condition;
    }

    @Override
    public boolean test(IContext context) {
        return this.condition.test();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return this.codec;
    }

    @Override
    public String toString() {
        return this.name.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeConditionWrapper that = (RecipeConditionWrapper) o;
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
