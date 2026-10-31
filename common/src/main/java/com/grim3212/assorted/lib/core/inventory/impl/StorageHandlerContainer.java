package com.grim3212.assorted.lib.core.inventory.impl;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.locking.LockedStorageHandler;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A vanilla {@link Container} view of a storage handler, shared by every slot of that handler so
 * sorting mods that group slots by {@code slot.container} see the whole inventory as one.
 */
public class StorageHandlerContainer implements Container {

    private static final Map<IItemStorageHandler, Container> WRAPPED = Collections.synchronizedMap(new WeakHashMap<>());

    private final IItemStorageHandler handler;

    private StorageHandlerContainer(IItemStorageHandler handler) {
        this.handler = handler;
    }

    /** The one container for this handler, made on first use. */
    public static Container of(IItemStorageHandler handler) {
        return WRAPPED.computeIfAbsent(handler, StorageHandlerContainer::new);
    }

    /**
     * A container of the handler's own, apart from {@link #of}, for a slot sorting mods must leave
     * alone, such as a lock or an upgrade kept in the same handler as the items.
     */
    public static Container separate(IItemStorageHandler handler) {
        return new StorageHandlerContainer(handler);
    }

    @Override
    public int getContainerSize() {
        return this.handler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        return this.handler.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.extract(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.extract(slot, this.handler.getSlotLimit(slot), false);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.handler.setStackInSlot(slot, stack);
        this.setSlotChanged(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.handler.isItemValid(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return this.handler.getSlotLimit(0);
    }

    @Override
    public void setChanged() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setSlotChanged(i);
        }
    }

    private void setSlotChanged(int slot) {
        this.handler.onContentsChanged(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    protected ItemStack insert(int slot, ItemStack test, boolean simulate) {
        if (this.handler instanceof LockedStorageHandler lockedStorageHandler) {
            return lockedStorageHandler.insertItem(slot, test, simulate, "", true);
        }
        return this.handler.insertItem(slot, test, simulate);
    }

    protected ItemStack extract(int slot, int amount, boolean simulate) {
        if (this.handler instanceof LockedStorageHandler lockedStorageHandler) {
            return lockedStorageHandler.extractItem(slot, amount, simulate, "", true);
        }
        return this.handler.extractItem(slot, amount, simulate);
    }
}
