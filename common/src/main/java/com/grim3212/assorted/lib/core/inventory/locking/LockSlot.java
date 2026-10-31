package com.grim3212.assorted.lib.core.inventory.locking;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.impl.StorageHandlerContainer;
import com.grim3212.assorted.lib.core.inventory.slot.SlotStorageHandler;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that holds one lock with a code on it, which locks whatever the slot belongs to. It has a
 * container of its own so sorting mods never move the lock with the items.
 */
public class LockSlot extends SlotStorageHandler {
    public LockSlot(IItemStorageHandler itemHandler, int index, int xPosition, int yPosition) {
        super(StorageHandlerContainer.separate(itemHandler), itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.isEmpty() || LockItems.isCodedLock(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
