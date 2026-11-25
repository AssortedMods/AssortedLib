package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.stream.Stream;

public class FabricRegistryWrapper<T> implements ILoaderRegistry<T> {

    private final Registry<T> registry;

    private FabricRegistryWrapper(Registry<T> registry) {
        this.registry = registry;
    }

    public static <T> ILoaderRegistry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
        Registry<? extends Registry<?>> rootRegistry = BuiltInRegistries.REGISTRY;
        Registry<?> registry = rootRegistry.get(key.location());
        if (registry == null) {
            throw new NullPointerException("Could not find registry for key: " + key);
        }
        ILoaderRegistry<?> registryWrapper = new FabricRegistryWrapper<>(registry);
        @SuppressWarnings("unchecked")
        ILoaderRegistry<T> castPlatformRegistry = (ILoaderRegistry<T>) registryWrapper;
        return castPlatformRegistry;
    }

    @Override
    public Stream<T> getValues() {
        return this.registry.stream();
    }

    @Override
    public Optional<T> getValue(ResourceLocation resourceLocation) {
        return this.registry.getOptional(resourceLocation);
    }

    @Override
    public boolean containsKey(ResourceLocation resourceLocation) {
        return getValue(resourceLocation).isPresent();
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
