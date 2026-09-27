package com.grim3212.assorted.lib.core.storage;

import org.jetbrains.annotations.Nullable;

/** A storage block made of a {@link StorageMaterial}, or null for the locked stand-in of a vanilla one. */
public interface IStorageMaterial {

    @Nullable
    StorageMaterial getStorageMaterial();
}
