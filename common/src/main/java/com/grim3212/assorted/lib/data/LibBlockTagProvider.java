package com.grim3212.assorted.lib.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.VanillaBlockTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class LibBlockTagProvider extends VanillaBlockTagsProvider {

    public LibBlockTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookup) {
        super(packOutput, lookup);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        throw new NotImplementedException();
    }

    public abstract void addCommonTags(Function<TagKey<Block>, TagAppender<Block>> tagger);
}
