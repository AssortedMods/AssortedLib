package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.stream.Stream;

/** A thin wrapper over a vanilla {@link Registry}, where NeoForge keeps modded registries too. */
public class ForgeRegistryWrapper<T> implements ILoaderRegistry<T> {

    private final Registry<T> registry;

    private ForgeRegistryWrapper(Registry<T> registry) {
        this.registry = registry;
    }

    public static <T> ILoaderRegistry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
        Registry<? extends Registry<?>> rootRegistry = BuiltInRegistries.REGISTRY;
        Registry<?> registry = rootRegistry.get(key.location());
        if (registry == null) {
            throw new NullPointerException("Could not find registry for key: " + key);
        }

        ILoaderRegistry<?> registryWrapper = new ForgeRegistryWrapper<>(registry);
        @SuppressWarnings("unchecked")
        ILoaderRegistry<T> castRegistry = (ILoaderRegistry<T>) registryWrapper;
        return castRegistry;
    }

    @Override
    public Stream<T> getValues() {
        return this.registry.stream();
    }

    @Override
    public Optional<T> getValue(ResourceLocation resourceLocation) {
        // Registry#getValue answers a defaulted registry's default (air, for blocks and items) for an
        // unknown id; getOptional answers empty, which is what Fabric returns and what callers expect.
        return this.registry.getOptional(resourceLocation);
    }

    @Override
    public boolean containsKey(ResourceLocation resourceLocation) {
        return this.registry.containsKey(resourceLocation);
    }

    @Override
    public boolean contains(T entry) {
        return this.registry.getKey(entry) != null;
    }

    @Override
    public ResourceLocation getRegistryName(T entry) {
        return this.registry.getKey(entry);
    }
}
