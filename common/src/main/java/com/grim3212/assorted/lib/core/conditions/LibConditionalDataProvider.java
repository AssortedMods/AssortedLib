package com.grim3212.assorted.lib.core.conditions;

import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;

public interface LibConditionalDataProvider extends DataProvider {

    void addConditions(ResourceLocation id, LibConditionProvider... providers);
}
