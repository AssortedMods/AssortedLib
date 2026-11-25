package com.grim3212.assorted.lib.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class LibEntityTagProvider extends TagsProvider<EntityType<?>> {

    // NeoForge deprecates this constructor in favour of one that also takes a mod id; that overload does
    // not exist in vanilla, which this module builds against.
    @SuppressWarnings("deprecation")
    public LibEntityTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ENTITY_TYPE, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        throw new NotImplementedException();
    }

    public abstract void addCommonTags(Function<TagKey<EntityType<?>>, TagAppender<EntityType<?>>> tagger);
}
