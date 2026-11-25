package com.grim3212.assorted.lib.conditions;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.conditions.LibConditionProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * An opaque handle on one {@link ICondition}. Conditions are attached by wrapping the
 * {@code RecipeOutput} ({@code IConditionHelper#conditionalOutput}) and serialised by their
 * registered codec, so this only carries the condition and names it.
 */
public class ForgeConditionProvider implements LibConditionProvider {

    private static final ResourceLocation UNREGISTERED = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "unregistered_condition");

    public final ICondition condition;

    public ForgeConditionProvider(ICondition condition) {
        this.condition = condition;
    }

    public ICondition getCondition() {
        return this.condition;
    }

    @Override
    public ResourceLocation getName() {
        final ResourceLocation name = NeoForgeRegistries.CONDITION_SERIALIZERS.getKey(this.condition.codec());
        return name != null ? name : UNREGISTERED;
    }
}
