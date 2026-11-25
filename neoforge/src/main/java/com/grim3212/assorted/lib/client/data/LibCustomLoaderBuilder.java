package com.grim3212.assorted.lib.client.data;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The base of every custom loader model an Assorted mod generates. It writes NeoForge's
 * {@code "loader"} key only: in 1.21.1 the Fabric side reads that same key itself, so one json serves
 * both loaders.
 */
public abstract class LibCustomLoaderBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {

    protected LibCustomLoaderBuilder(ResourceLocation loaderId, T parent, ExistingFileHelper existingFileHelper, boolean allowInlineElements) {
        super(loaderId, parent, existingFileHelper, allowInlineElements);
    }
}
