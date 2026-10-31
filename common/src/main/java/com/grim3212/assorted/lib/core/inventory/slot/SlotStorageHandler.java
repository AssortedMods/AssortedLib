package com.grim3212.assorted.lib.core.inventory.slot;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.impl.StorageHandlerContainer;
import com.grim3212.assorted.lib.core.inventory.locking.LockedStorageHandler;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotStorageHandler extends Slot {
    private final IItemStorageHandler itemHandler;

    public SlotStorageHandler(IItemStorageHandler itemHandler, int slotIndex, int xPosition, int yPosition) {
        this(StorageHandlerContainer.of(itemHandler), itemHandler, slotIndex, xPosition, yPosition);
    }

    /** A slot over a given view of the handler, such as {@link StorageHandlerContainer#separate}. */
    protected SlotStorageHandler(Container container, IItemStorageHandler itemHandler, int slotIndex, int xPosition, int yPosition) {
        super(container, slotIndex, xPosition, yPosition);
        this.itemHandler = itemHandler;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        // Like a vanilla slot, emptying it is always allowed; sorting mods check that before each move.
        if (stack.isEmpty())
            return true;
        return itemHandler.isItemValid(getContainerSlot(), stack);
    }

    @Override
    @NotNull
    public ItemStack getItem() {
        return this.getItemHandler().getStackInSlot(getContainerSlot());
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        this.getItemHandler().setStackInSlot(getContainerSlot(), stack);
        this.setChanged();
    }

    @Override
    public void setByPlayer(ItemStack stack) {
        this.getItemHandler().setStackInSlot(getContainerSlot(), stack);
        this.setChanged();
    }

    @Override
    public void onQuickCraft(@NotNull ItemStack oldStackIn, @NotNull ItemStack newStackIn) {
    }

    @Override
    public int getMaxStackSize() {
        return this.itemHandler.getSlotLimit(this.getContainerSlot());
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        ItemStack maxAdd = stack.copy();
        int maxInput = stack.getMaxStackSize();
        maxAdd.setCount(maxInput);

        IItemStorageHandler handler = this.getItemHandler();
        ItemStack currentStack = handler.getStackInSlot(getContainerSlot());
        ItemStack remainder = this.insert(maxAdd, true);

        int current = currentStack.getCount();
        int added = maxInput - remainder.getCount();
        return Math.min(this.getMaxStackSize(), current + added);
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return !this.extract(1, true).isEmpty();
    }

    @Override
    @NotNull
    public ItemStack remove(int amount) {
        return this.extract(amount, false);
    }

    public IItemStorageHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void setChanged() {
        this.itemHandler.onContentsChanged(this.getContainerSlot());
    }

    protected ItemStack insert(ItemStack test, boolean simulate) {
        if (this.getItemHandler() instanceof LockedStorageHandler lockedStorageHandler) {
            return lockedStorageHandler.insertItem(getContainerSlot(), test, simulate, "", true);
        }
        return this.getItemHandler().insertItem(getContainerSlot(), test, simulate);
    }

    protected ItemStack extract(int amount, boolean simulate) {
        if (this.getItemHandler() instanceof LockedStorageHandler lockedStorageHandler) {
            return lockedStorageHandler.extractItem(getContainerSlot(), amount, simulate, "", true);
        }
        return this.getItemHandler().extractItem(getContainerSlot(), amount, simulate);
    }
}
