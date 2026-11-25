package com.grim3212.assorted.lib.data;

import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ForgeBiomeTagProvider extends BiomeTagsProvider {

    private final LibBiomeTagProvider commonBiomes;

    public ForgeBiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, String modId, ExistingFileHelper existingFileHelper, LibBiomeTagProvider commonBiomes) {
        super(output, lookup, modId, existingFileHelper);
        this.commonBiomes = commonBiomes;
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.commonBiomes.addCommonTags(this::tag);
    }

}
