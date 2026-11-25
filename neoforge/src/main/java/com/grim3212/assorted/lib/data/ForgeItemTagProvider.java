package com.grim3212.assorted.lib.data;

import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.data.tags.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ForgeItemTagProvider extends ItemTagsProvider {

    private final LibItemTagProvider commonItems;

    public ForgeItemTagProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTagsProvider, String modId, ExistingFileHelper existingFileHelper, LibItemTagProvider commonItems) {
        super(pOutput, pLookupProvider, blockTagsProvider, modId, existingFileHelper);
        this.commonItems = commonItems;
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.commonItems.addCommonTags(this::tag, this::copy);
    }

}
