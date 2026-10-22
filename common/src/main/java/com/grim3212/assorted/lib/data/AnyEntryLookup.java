package com.grim3212.assorted.lib.data;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;

/** A registry where every entry and tag exists, for checking json's shape without the content it names. */
final class AnyEntryLookup<T> implements HolderGetter<T> {

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
        return Optional.of(Holder.Reference.createStandAlone(this, id));
    }

    @Override
    public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
        return Optional.of(HolderSet.emptyNamed(this, id));
    }
}
