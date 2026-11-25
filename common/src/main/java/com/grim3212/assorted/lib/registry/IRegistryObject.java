package com.grim3212.assorted.lib.registry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public interface IRegistryObject<T> extends Supplier<T> {

    ResourceKey<T> getResourceKey();

    ResourceLocation getId();

    @Override
    T get();

    Holder<T> asHolder();
}
