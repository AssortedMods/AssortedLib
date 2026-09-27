package com.grim3212.assorted.lib.core.storage;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.slot.SlotStorageHandler;
import net.minecraft.world.item.ItemStack;

/** A slot that refuses what cannot go inside a container item, such as another shulker box. */
public class ShulkerSlotStorageHandler extends SlotStorageHandler {
    public ShulkerSlotStorageHandler(IItemStorageHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return itemStack.getItem().canFitInsideContainerItems();
    }
}
