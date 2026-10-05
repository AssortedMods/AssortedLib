package com.grim3212.assorted.lib.registry;

import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.function.Supplier;

public interface RegistryProvider<T> extends ILoaderRegistry<T> {

    static <T> RegistryProvider<T> create(ResourceKey<? extends Registry<T>> resourceKey, String modId) {
        return Services.REGISTRY_FACTORY.create(resourceKey, modId);
    }

    static <T> RegistryProvider<T> create(Registry<T> registry, String modId) {
        return Services.REGISTRY_FACTORY.create(registry, modId);
    }

    <I extends T> IRegistryObject<I> register(String name, Supplier<? extends I> supplier);

    /**
     * Aliases {@code oldNamespace:<name>} to every entry registered from here on, for a mod whose
     * content used to live under another id. Call it before registering anything.
     */
    RegistryProvider<T> aliasFrom(String oldNamespace);

    Collection<IRegistryObject<T>> getEntries();
}
