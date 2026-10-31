package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.IPlatformInventoryStorageHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public interface IInventoryHelper {

    boolean canItemStacksStack(@NotNull ItemStack a, @NotNull ItemStack b);

    ItemStack copyStackWithSize(@NotNull ItemStack itemStack, int size);

    Optional<IItemStorageHandler> getItemStorageHandler(ItemStack stack);

    Optional<IItemStorageHandler> getItemStorageHandler(BlockEntity blockEntity, @Nullable Direction direction);

    IPlatformInventoryStorageHandler createStorageInventoryHandler(IItemStorageHandler handler);

    IPlatformInventoryStorageHandler createSidedStorageInventoryHandler(Function<Direction, IItemStorageHandler> handler);
}
