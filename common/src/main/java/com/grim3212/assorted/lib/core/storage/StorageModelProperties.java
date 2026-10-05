package com.grim3212.assorted.lib.core.storage;

import com.grim3212.assorted.lib.client.model.data.IModelDataKey;

/** Model data a storage block entity publishes for the locked models Lib's barrels and hoppers are drawn with. */
public final class StorageModelProperties {

    public static final IModelDataKey<Boolean> IS_LOCKED = IModelDataKey.create();

    private StorageModelProperties() {
    }
}
