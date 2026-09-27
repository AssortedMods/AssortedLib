package com.grim3212.assorted.lib.core.storage;

import com.grim3212.assorted.lib.core.inventory.impl.LockedItemStackStorageHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StorageItemStackStorageHandler extends LockedItemStackStorageHandler {
    protected final BaseStorageBlockEntity lockable;

    public StorageItemStackStorageHandler(BaseStorageBlockEntity lockable, int size) {
        super(lockable, size);
        this.lockable = lockable;
    }

    public StorageItemStackStorageHandler(BaseStorageBlockEntity lockable, NonNullList<ItemStack> stacks) {
        super(lockable, stacks);
        this.lockable = lockable;
    }

    @Override
    public void onContentsChanged(int slot) {
        this.lockable.setChanged();
    }

    @Override
    public void startOpen(Player player) {
        if (!player.isSpectator()) {
            if (lockable.numPlayersUsing < 0) {
                lockable.numPlayersUsing = 0;
            }

            ++lockable.numPlayersUsing;
            lockable.onOpenOrClose();
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!player.isSpectator()) {
            --lockable.numPlayersUsing;
            lockable.onOpenOrClose();
        }
    }
}
