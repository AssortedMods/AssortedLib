package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.inventory.IInventoryBlockEntity;
import com.grim3212.assorted.lib.core.inventory.IInventoryItem;
import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.IPlatformInventoryStorageHandler;
import com.grim3212.assorted.lib.inventory.ForgePlatformInventoryStorageHandlerSided;
import com.grim3212.assorted.lib.inventory.ForgePlatformInventoryStorageHandlerUnsided;
import com.grim3212.assorted.lib.inventory.ForgeWrappedItemHandler;
import com.grim3212.assorted.lib.platform.services.IInventoryHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class ForgeInventoryHelper implements IInventoryHelper {
    @Override
    public boolean canItemStacksStack(@NotNull ItemStack a, @NotNull ItemStack b) {
        // ItemHandlerHelper's stack helpers are gone; both of these are plain vanilla now that item
        // data lives in components instead of an opaque tag.
        return !a.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }

    @Override
    public ItemStack copyStackWithSize(@NotNull ItemStack itemStack, int size) {
        return size <= 0 ? ItemStack.EMPTY : itemStack.copyWithCount(size);
    }

    @Override
    public Optional<IItemStorageHandler> getItemStorageHandler(ItemStack stack) {
        // Our own handler first, matching FabricInventoryHelper. Callers type-check the result -
        // BagContainer wants a BagItemHandler, StorageAccessUtil a KeyRingItemHandler - and the
        // capability hands back a ForgeWrappedItemHandler view of the very same storage, which
        // satisfies none of them. Answering with the wrapper left every container item's GUI without
        // slots.
        if (stack.getItem() instanceof IInventoryItem itemStackStorage) {
            IPlatformInventoryStorageHandler storageHandler = itemStackStorage.getStorageHandler(stack);
            if (storageHandler != null) {
                return Optional.of(storageHandler.getItemStorageHandler(null));
            }
        }

        // Anything else is a foreign inventory, reachable only through the capability. ForgeCapabilities
        // and LazyOptional are gone: the lookup is a plain nullable value resolved against an ItemAccess
        // describing which stack is being operated on, and oneByOne() scopes it to a single item.
        ResourceHandler<ItemResource> capability = ItemAccess.forStack(stack).oneByOne().getCapability(Capabilities.Item.ITEM);
        if (capability != null) {
            return Optional.of(new ForgeWrappedItemHandler(null, capability));
        }

        return Optional.empty();
    }

    @Override
    public Optional<IItemStorageHandler> getItemStorageHandler(BlockEntity blockEntity, @Nullable Direction direction) {
        // Our own handler first, for the same reason as the stack overload above: LockedHopperBlockEntity
        // type-checks the result for a LockedStorageHandler.
        if (blockEntity instanceof IInventoryBlockEntity inventoryBlockEntity) {
            IPlatformInventoryStorageHandler storageHandler = inventoryBlockEntity.getStorageHandler();
            if (storageHandler != null) {
                return Optional.of(storageHandler.getItemStorageHandler(direction));
            }
        }

        // Block capabilities are resolved through the level rather than off the block entity itself.
        Level level = blockEntity.getLevel();
        if (level != null) {
            ResourceHandler<ItemResource> capability = level.getCapability(Capabilities.Item.BLOCK, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (capability != null) {
                return Optional.of(new ForgeWrappedItemHandler(blockEntity, capability));
            }
        }

        return Optional.empty();
    }

    @Override
    public IPlatformInventoryStorageHandler createStorageInventoryHandler(IItemStorageHandler handler) {
        return new ForgePlatformInventoryStorageHandlerUnsided(handler);
    }

    @Override
    public IPlatformInventoryStorageHandler createSidedStorageInventoryHandler(Function<Direction, IItemStorageHandler> handler) {
        return new ForgePlatformInventoryStorageHandlerSided(handler);
    }
}
