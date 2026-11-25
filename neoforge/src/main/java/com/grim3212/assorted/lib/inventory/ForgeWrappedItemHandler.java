package com.grim3212.assorted.lib.inventory;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Presents a foreign {@link IItemHandler} as an {@link IItemStorageHandler}, the inverse of
 * {@link ForgeItemStorageHandler}.
 */
public class ForgeWrappedItemHandler implements IItemStorageHandler {

    private final IItemHandler storage;
    private final BlockEntity entity;

    public ForgeWrappedItemHandler(@Nullable BlockEntity entity, @NotNull IItemHandler storage) {
        this.storage = storage;
        this.entity = entity;
    }

    @Override
    public int getSlots() {
        return this.storage.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return this.storage.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return this.storage.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.storage.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.storage.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return this.storage.isItemValid(slot, stack);
    }

    /**
     * Writes straight through when the handler allows it. Otherwise the slot is emptied and the new
     * contents inserted, the nearest a plain {@link IItemHandler} offers; whatever does not fit
     * stays out.
     */
    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (this.storage instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
            return;
        }

        ItemStack current = this.storage.getStackInSlot(slot);
        if (!current.isEmpty()) {
            this.storage.extractItem(slot, current.getCount(), false);
        }
        if (!stack.isEmpty()) {
            this.storage.insertItem(slot, stack.copy(), false);
        }
    }

    @Override
    public void onContentsChanged(int slot) {
        if (this.entity != null)
            this.entity.setChanged();
    }
}
