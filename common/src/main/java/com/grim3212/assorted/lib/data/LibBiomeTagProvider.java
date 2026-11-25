package com.grim3212.assorted.lib.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class LibBiomeTagProvider extends TagsProvider<Biome> {

    // NeoForge deprecates this constructor in favour of one that also takes a mod id; that overload does
    // not exist in vanilla, which this module builds against.
    @SuppressWarnings("deprecation")
    public LibBiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BIOME, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        throw new NotImplementedException();
    }

    @Override
    protected TagAppender<Biome> tag(TagKey<Biome> tag) {
        throw new NotImplementedException();
    }

    protected void tagAll(Function<TagKey<Biome>, TagAppender<Biome>> tagger, ResourceKey<Biome> biome, TagKey<Biome>... tags) {
        for (TagKey<Biome> key : tags) {
            tagger.apply(key).add(biome);
        }
    }

    public abstract void addCommonTags(Function<TagKey<Biome>, TagAppender<Biome>> tagger);
}
