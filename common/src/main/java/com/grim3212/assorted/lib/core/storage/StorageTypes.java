package com.grim3212.assorted.lib.core.storage;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * The block entity and menu types a mod registered for its storage blocks. Lib's storage blocks create and open
 * these, so every mod that uses the same block class keeps its own types.
 */
public record StorageTypes<E extends BlockEntity, M extends AbstractContainerMenu>(Supplier<BlockEntityType<E>> blockEntity,
                                                                                     Supplier<MenuType<M>> menu) {
}
