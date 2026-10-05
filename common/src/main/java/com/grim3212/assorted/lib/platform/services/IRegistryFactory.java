package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.registry.RegistryProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface IRegistryFactory {

    <T> RegistryProvider<T> create(ResourceKey<? extends Registry<T>> resourceKey, String modId);

    default <T> RegistryProvider<T> create(Registry<T> registry, String modId) {
        return create(registry.key(), modId);
    }

    /**
     * Makes {@code from} read as {@code to} in {@code registry}, so a world saved before an entry
     * moved to a new id still loads it. {@code from} must not be registered itself.
     */
    <T> void alias(ResourceKey<? extends Registry<T>> registry, Identifier from, Identifier to);
}
