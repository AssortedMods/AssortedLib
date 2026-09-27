package com.grim3212.assorted.lib.core.storage;

/**
 * An item a crate takes in its upgrade slots. Here rather than in the crates so another mod's item,
 * a level upgrade say, can be one without depending on them.
 */
public interface ICrateUpgrade {

    default int getStorageModifier() {
        return 0;
    }
}
