package com.grim3212.assorted.lib.client.screen.storage;

import com.grim3212.assorted.lib.core.storage.StorageContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GenericStorageScreen extends BaseStorageScreen<StorageContainer> {

    public GenericStorageScreen(StorageContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
    }
}
