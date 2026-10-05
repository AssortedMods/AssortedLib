package com.grim3212.assorted.lib.core.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The block an upgrade puts in place, and what it carries over from the old one.
 *
 * @param lockCode null for a vanilla block, which has no lock to keep
 */
public record PreparedLevelUpgrade(BlockState state, BlockEntity blockEntity, @Nullable String lockCode, @Nullable Component customName, NonNullList<ItemStack> items) {
}
