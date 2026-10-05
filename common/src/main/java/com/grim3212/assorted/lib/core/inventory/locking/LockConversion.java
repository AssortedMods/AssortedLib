package com.grim3212.assorted.lib.core.inventory.locking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Turns one block into its locked version, see {@link LockConversions}. */
@FunctionalInterface
public interface LockConversion {

    /** Swaps the block at {@code pos} for its locked version, carrying over what it held; false if it cannot. */
    boolean lock(Level level, BlockPos pos, BlockState state, String code);
}
