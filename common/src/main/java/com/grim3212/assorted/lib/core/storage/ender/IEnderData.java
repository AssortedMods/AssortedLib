package com.grim3212.assorted.lib.core.storage.ender;

/** Where the ender inventories live, one per lock code. */
public interface IEnderData {
    void markDirty();

    LockedEnderChestInventory getInventory(String code);
}
