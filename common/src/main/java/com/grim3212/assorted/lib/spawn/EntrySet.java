package com.grim3212.assorted.lib.spawn;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Set;

/**
 * A tag ({@code "#ns:path"}), one id, or a list of ids, matched against a holder at run time. Not a
 * HolderSet, which needs registry ops to decode: this loads with plain JsonOps, and biomes and
 * structures resolve when asked, against whatever registries the level has.
 */
public record EntrySet<T>(Either<TagKey<T>, Set<ResourceKey<T>>> entries) {

    public static <T> Codec<EntrySet<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        Codec<Set<ResourceKey<T>>> keys = ExtraCodecs.compactListCodec(ResourceKey.codec(registry)).xmap(Set::copyOf, List::copyOf);
        return Codec.either(TagKey.hashedCodec(registry), keys).xmap(EntrySet::new, EntrySet::entries);
    }

    public static <T> EntrySet<T> of(TagKey<T> tag) {
        return new EntrySet<>(Either.left(tag));
    }

    public boolean contains(Holder<T> holder) {
        return this.entries.map(holder::is, keys -> holder.unwrapKey().map(keys::contains).orElse(false));
    }
}
