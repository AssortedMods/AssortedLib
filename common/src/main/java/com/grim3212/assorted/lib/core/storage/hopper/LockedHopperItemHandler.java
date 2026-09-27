package com.grim3212.assorted.lib.core.storage.hopper;

import com.grim3212.assorted.lib.core.storage.StorageItemStackStorageHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** A hopper's inventory, which starts the hopper's cooldown when an item arrives in it empty, as vanilla's does. */
public class LockedHopperItemHandler extends StorageItemStackStorageHandler {

    private final LockedHopperBlockEntity hopper;

    public LockedHopperItemHandler(LockedHopperBlockEntity hopper) {
        super(hopper, hopper.getStorageMaterial() != null ? hopper.getStorageMaterial().hopperSize() : 5);
        this.hopper = hopper;
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (simulate) {
            return super.insertItem(slot, stack, simulate);
        }

        boolean wasEmpty = this.hopper.getItemStackStorageHandler().isEmpty();

        int originalStackSize = stack.getCount();
        stack = super.insertItem(slot, stack, simulate);

        if (wasEmpty && originalStackSize > stack.getCount() && !this.hopper.isOnCustomCooldown()) {
            this.hopper.setCooldown(LockedHopperBlockEntity.getHopperCooldown(this.hopper.getStorageMaterial()));
        }

        return stack;
    }
}
