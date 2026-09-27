package com.grim3212.assorted.lib.core.storage;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.impl.ItemStackStorageHandler;
import com.grim3212.assorted.lib.core.inventory.slot.SlotStorageHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** The menu of a material container, sized by its {@link StorageMaterial}, or a chest's 3 by 9 when it has none. */
public class LockedMaterialContainer extends AbstractContainerMenu {

    private final IItemStorageHandler inventory;
    private final StorageMaterial storageMaterial;

    /** The client's copy of the menu, sized from the material the server sent as menu data. */
    public static LockedMaterialContainer createClient(MenuType<LockedMaterialContainer> menuType, int windowId, Inventory playerInventory, Optional<StorageMaterial> material, boolean useShulkerSlots) {
        StorageMaterial storageMaterial = material.orElse(null);
        return new LockedMaterialContainer(menuType, windowId, playerInventory, new ItemStackStorageHandler(storageMaterial == null ? 27 : storageMaterial.totalItems()), storageMaterial, useShulkerSlots);
    }

    public LockedMaterialContainer(MenuType<LockedMaterialContainer> menuType, int windowId, Inventory playerInventory, IItemStorageHandler inventory, StorageMaterial storageMaterial, boolean useShulkerSlots) {
        super(menuType, windowId);
        this.inventory = inventory;
        this.storageMaterial = storageMaterial;

        int xRows = 3;
        int yCols = 9;

        if (this.storageMaterial != null) {
            xRows = storageMaterial.getXRows();
            yCols = storageMaterial.getYCols();
        }

        inventory.startOpen(playerInventory.player);

        for (int chestRow = 0; chestRow < xRows; chestRow++) {
            for (int chestCol = 0; chestCol < yCols; chestCol++) {
                if (useShulkerSlots) {
                    this.addSlot(new ShulkerSlotStorageHandler(inventory, chestCol + chestRow * yCols, 8 + chestCol * 18, 18 + chestRow * 18));
                } else {
                    this.addSlot(new SlotStorageHandler(inventory, chestCol + chestRow * yCols, 8 + chestCol * 18, 18 + chestRow * 18));
                }

            }
        }

        int leftOffset = Math.max(yCols - 9, 0) * 9;
        int leftCol = ((184 - 168) / 2) + leftOffset;
        int heighOffset = 113 + xRows * 18;

        for (int playerInvRow = 0; playerInvRow < 3; playerInvRow++) {
            for (int playerInvCol = 0; playerInvCol < 9; playerInvCol++) {
                this.addSlot(new Slot(playerInventory, playerInvCol + playerInvRow * 9 + 9, leftCol + playerInvCol * 18, heighOffset - (4 - playerInvRow) * 18 - 10));
            }

        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, leftCol + hotbarSlot * 18, heighOffset - 24));
        }
    }

    public StorageMaterial getStorageMaterial() {
        return storageMaterial;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return this.inventory.stillValid(playerIn);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int maxSlot = this.inventory.getSlots();

            if (index < maxSlot) {
                if (!this.moveItemStackTo(itemstack1, maxSlot, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, maxSlot, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        this.inventory.stopOpen(playerIn);
    }

}
