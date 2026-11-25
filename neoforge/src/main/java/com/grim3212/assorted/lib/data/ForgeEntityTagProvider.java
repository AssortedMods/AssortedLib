package com.grim3212.assorted.lib.data;

import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ForgeEntityTagProvider extends EntityTypeTagsProvider {

    private final LibEntityTagProvider commonEntityTags;

    public ForgeEntityTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, String modId, ExistingFileHelper existingFileHelper, LibEntityTagProvider commonEntityTags) {
        super(output, lookup, modId, existingFileHelper);
        this.commonEntityTags = commonEntityTags;
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.commonEntityTags.addCommonTags(this::tag);
    }

}
