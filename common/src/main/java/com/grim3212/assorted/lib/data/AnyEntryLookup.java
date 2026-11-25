package com.grim3212.assorted.lib.data;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;

/**
 * A registry where every entry and tag exists, for checking json's shape without the content it names.
 * Backed by an empty, never-frozen registry of its own: 1.21.1 only hands out an unbound named tag
 * through a registry ({@code HolderSet.emptyNamed} is deprecated and the constructor is package-private),
 * and the registry's owner is then the one owner every reference and tag here shares.
 */
final class AnyEntryLookup<T> implements HolderGetter<T> {

    private final MappedRegistry<T> registry;

    AnyEntryLookup(ResourceKey<? extends Registry<T>> key) {
        this.registry = new MappedRegistry<>(key, Lifecycle.stable());
    }

    HolderOwner<T> owner() {
        return this.registry.holderOwner();
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
        return Optional.of(Holder.Reference.createStandAlone(this.owner(), id));
    }

    @Override
    public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
        return Optional.of(this.registry.getOrCreateTag(id));
    }
}
