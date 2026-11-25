package com.grim3212.assorted.lib.core.inventory.impl;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.IValueSerializable;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStackStorageHandler implements IItemStorageHandler, IValueSerializable {
    protected NonNullList<ItemStack> stacks;

    public ItemStackStorageHandler() {
        this(1);
    }

    public ItemStackStorageHandler(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStackStorageHandler(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public void setSize(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        validateSlotIndex(slot);
        this.stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return this.stacks.get(slot);
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        if (!isItemValid(slot, stack))
            return stack;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        int limit = getStackLimit(slot, stack);

        if (!existing.isEmpty()) {
            if (!Services.INVENTORY.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate) {
            if (existing.isEmpty()) {
                this.setStackInSlot(slot, reachedLimit ? Services.INVENTORY.copyStackWithSize(stack, limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
                onContentsChanged(slot);
            }
        }

        return reachedLimit ? Services.INVENTORY.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0)
            return ItemStack.EMPTY;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        if (existing.isEmpty())
            return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getMaxStackSize());

        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                this.setStackInSlot(slot, ItemStack.EMPTY);
                return existing;
            } else {
                return existing.copy();
            }
        } else {
            if (!simulate) {
                this.setStackInSlot(slot, Services.INVENTORY.copyStackWithSize(existing, existing.getCount() - toExtract));
            }

            return Services.INVENTORY.copyStackWithSize(existing, toExtract);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void serialize(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag items = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            // ItemStack#save refuses an empty stack, so empty slots are left out.
            if (!stacks.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                items.add(stacks.get(i).save(registries, itemTag));
            }
        }
        tag.put("Items", items);
        tag.putInt("Size", stacks.size());
    }

    @Override
    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        setSize(tag.contains("Size", Tag.TAG_INT) ? tag.getInt("Size") : stacks.size());
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.contains("Slot", Tag.TAG_INT) ? itemTag.getInt("Slot") : -1;
            if (slot >= 0 && slot < stacks.size()) {
                ItemStack.parse(registries, itemTag).ifPresent(stack -> stacks.set(slot, stack));
            }
        }
        onLoad();
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size())
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
    }

    protected void onLoad() {
    }

    @Override
    public void onContentsChanged(int slot) {
    }

    @Override
    public boolean isEmpty() {
        return this.stacks.isEmpty() || this.stacks.stream().allMatch(x -> x.isEmpty());
    }


    public void setStacks(NonNullList<ItemStack> stacks) {
        if (stacks.size() < this.stacks.size()) {
            NonNullList<ItemStack> newStacks = NonNullList.withSize(this.stacks.size(), ItemStack.EMPTY);
            for (int i = 0; i < stacks.size(); i++) {
                if (i < stacks.size()) {
                    newStacks.set(i, stacks.get(i));
                } else {
                    newStacks.set(i, ItemStack.EMPTY);
                }
            }
            this.stacks = newStacks;
        } else if (stacks.size() > this.stacks.size()) {
            LibConstants.LOG.warn("Can't try to set more stacks than slots");
        } else {
            this.stacks = stacks;
        }
    }

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }
}
