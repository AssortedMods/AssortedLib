package com.grim3212.assorted.lib.client.screen.storage;

import com.grim3212.assorted.lib.LibConstants;
import net.minecraft.resources.Identifier;

/** The container backgrounds every storage mod shares, nine rows tall from three to fourteen columns wide. */
public final class StorageScreenTextures {

    private StorageScreenTextures() {
    }

    public static Identifier generic(int columns) {
        return Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "textures/gui/container/generic_9x" + columns + ".png");
    }
}
