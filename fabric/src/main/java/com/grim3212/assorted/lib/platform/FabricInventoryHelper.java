package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.inventory.IInventoryBlockEntity;
import com.grim3212.assorted.lib.core.inventory.IInventoryItem;
import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.IPlatformInventoryStorageHandler;
import com.grim3212.assorted.lib.inventory.FabricPlatformInventoryStorageHandlerSided;
import com.grim3212.assorted.lib.inventory.FabricPlatformInventoryStorageHandlerUnsided;
import com.grim3212.assorted.lib.inventory.FabricWrappedItemHandler;
import com.grim3212.assorted.lib.platform.services.IInventoryHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class FabricInventoryHelper implements IInventoryHelper {
    @Override
    public boolean canItemStacksStack(@NotNull ItemStack a, @NotNull ItemStack b) {
        // Stack NBT became data components, and vanilla already compares the whole component map.
        return !a.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }

    @Override
    public ItemStack copyStackWithSize(@NotNull ItemStack itemStack, int size) {
        if (size == 0)
            return ItemStack.EMPTY;
        ItemStack copy = itemStack.copy();
        copy.setCount(size);
        return copy;
    }

    @Override
    public Optional<IItemStorageHandler> getItemStorageHandler(ItemStack stack) {
        if (stack.isEmpty())
            return Optional.empty();

        if (stack.getItem() instanceof IInventoryItem itemStackStorage) {
            IPlatformInventoryStorageHandler storageHandler = itemStackStorage.getStorageHandler(stack);
            if (storageHandler != null) {
                return Optional.of(storageHandler.getItemStorageHandler(null));
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<IItemStorageHandler> getItemStorageHandler(BlockEntity blockEntity, @Nullable Direction direction) {
        if (blockEntity == null || blockEntity.isRemoved())
            return Optional.empty();

        if (blockEntity instanceof IInventoryBlockEntity inventoryBlockEntity) {
            IPlatformInventoryStorageHandler storageHandler = inventoryBlockEntity.getStorageHandler();
            if (storageHandler != null) {
                return Optional.of(storageHandler.getItemStorageHandler(direction));
            }
        }

        // ItemStorage.SIDED already falls back to WorldlyContainerHolder blocks and to block entities
        // that are plain Containers, so those need no special case of their own. A storage that is not
        // slotted has nothing to map the slot indexed IItemStorageHandler onto, so it is left alone.
        if (ItemStorage.SIDED.find(blockEntity.getLevel(), blockEntity.getBlockPos(), direction) instanceof SlottedStorage<ItemVariant> slottedStorage) {
            return Optional.of(new FabricWrappedItemHandler(blockEntity, slottedStorage));
        }

        return Optional.empty();
    }

    @Override
    public IPlatformInventoryStorageHandler createStorageInventoryHandler(IItemStorageHandler handler) {
        return new FabricPlatformInventoryStorageHandlerUnsided(handler);
    }

    @Override
    public IPlatformInventoryStorageHandler createSidedStorageInventoryHandler(Function<Direction, IItemStorageHandler> handler) {
        return new FabricPlatformInventoryStorageHandlerSided(handler);
    }
}
