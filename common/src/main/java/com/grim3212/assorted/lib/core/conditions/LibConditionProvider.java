package com.grim3212.assorted.lib.core.conditions;

import net.minecraft.resources.ResourceLocation;

/**
 * An opaque, loader-agnostic handle on one recipe condition. Each loader implements it over its own
 * condition type ({@code ICondition} / {@code ResourceCondition}) and unwraps it in
 * {@code IConditionHelper#conditionalOutput}, which attaches it by wrapping the recipe output.
 */
public interface LibConditionProvider {

    ResourceLocation getName();
}
