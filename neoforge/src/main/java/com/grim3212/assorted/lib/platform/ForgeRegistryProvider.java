package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.platform.services.IRegistryFactory;
import com.grim3212.assorted.lib.registry.IRegistryObject;
import com.grim3212.assorted.lib.registry.RegistryProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ForgeRegistryProvider implements IRegistryFactory {

    @Override
    public <T> RegistryProvider<T> create(ResourceKey<? extends Registry<T>> resourceKey, String modId) {
        final var containerOpt = ModList.get().getModContainerById(modId);
        if (containerOpt.isEmpty())
            throw new NullPointerException("Cannot find mod container for id " + modId);

        // Every container exposes its event bus now, so there is no need to check for the java
        // specific FMLModContainer first.
        final ModContainer container = containerOpt.get();
        final var register = DeferredRegister.create(resourceKey, modId);
        register.register(container.getEventBus());
        return new Provider<>(register);
    }

    @Override
    public <T> void alias(ResourceKey<? extends Registry<T>> registry, Identifier from, Identifier to) {
        Registry<?> target = BuiltInRegistries.REGISTRY.getValue(registry.identifier());
        if (target == null) {
            throw new IllegalArgumentException("No registry " + registry.identifier() + " to alias " + from + " in");
        }

        // NeoForge keeps a registry's aliases in a plain map, and mods are constructed in parallel.
        synchronized (target) {
            target.addAlias(from, to);
        }
    }

    private class Provider<T> implements RegistryProvider<T> {
        private final DeferredRegister<T> registry;
        private volatile String aliasNamespace;

        private final Set<IRegistryObject<T>> entries = new HashSet<>();
        private final Set<IRegistryObject<T>> entriesView = Collections.unmodifiableSet(entries);

        private Provider(DeferredRegister<T> registry) {
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends T> IRegistryObject<I> register(String name, Supplier<? extends I> supplier) {
            final var obj = registry.<I>register(name, supplier);
            if (this.aliasNamespace != null) {
                alias(this.registry.getRegistryKey(), Identifier.fromNamespaceAndPath(this.aliasNamespace, name), obj.getId());
            }
            final var ro = new IRegistryObject<I>() {

                @Override
                public ResourceKey<I> getResourceKey() {
                    // A DeferredHolder is keyed by the registry's type rather than the registered
                    // type, so the narrowing this interface promises has to be done here.
                    return (ResourceKey<I>) obj.getKey();
                }

                @Override
                public Identifier getId() {
                    return obj.getId();
                }

                @Override
                public I get() {
                    return obj.get();
                }

                @Override
                public Holder<I> asHolder() {
                    // DeferredHolder is itself the Holder now; getHolder() is gone.
                    return (Holder<I>) obj;
                }
            };
            entries.add((IRegistryObject<T>) ro);
            return ro;
        }

        @Override
        public RegistryProvider<T> aliasFrom(String oldNamespace) {
            this.aliasNamespace = oldNamespace;
            return this;
        }

        @Override
        public Set<IRegistryObject<T>> getEntries() {
            return entriesView;
        }

        @Override
        public Stream<T> getValues() {
            return this.entriesView.stream().map(x -> x.get());
        }

        @Override
        public Optional<T> getValue(Identifier resourceLocation) {
            Optional<T> value = entriesView.stream().filter(x -> x.getId().equals(resourceLocation)).map(x -> x.get()).findFirst();
            return value;
        }

        @Override
        public boolean contains(T entry) {
            return this.getValues().anyMatch(x -> entry.equals(x));
        }

        @Override
        public boolean containsKey(Identifier resourceLocation) {
            return this.getValue(resourceLocation).isPresent();
        }

        @Override
        public Identifier getRegistryName(T entry) {
            return this.registry.getRegistry().get().getKey(entry);
        }
    }
}
