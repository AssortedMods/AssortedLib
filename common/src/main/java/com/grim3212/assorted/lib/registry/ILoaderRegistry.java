package com.grim3212.assorted.lib.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.stream.Stream;

public interface ILoaderRegistry<T> {
    Stream<T> getValues();

    Optional<T> getValue(ResourceLocation resourceLocation);

    boolean contains(T entry);

    boolean containsKey(ResourceLocation resourceLocation);

    ResourceLocation getRegistryName(T entry);
}
