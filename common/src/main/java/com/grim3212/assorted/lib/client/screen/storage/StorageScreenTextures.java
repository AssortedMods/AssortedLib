package com.grim3212.assorted.lib.client.screen.storage;

import com.grim3212.assorted.lib.LibConstants;
import net.minecraft.resources.ResourceLocation;

/** The container backgrounds every storage mod shares, nine rows tall from three to fourteen columns wide. */
public final class StorageScreenTextures {

    private StorageScreenTextures() {
    }

    public static ResourceLocation generic(int columns) {
        return ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "textures/gui/container/generic_9x" + columns + ".png");
    }
}
