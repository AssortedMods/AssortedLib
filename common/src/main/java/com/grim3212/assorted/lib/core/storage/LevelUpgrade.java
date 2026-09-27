package com.grim3212.assorted.lib.core.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** How a mod's blocks take a level upgrade, see {@link LevelUpgrades}. */
@FunctionalInterface
public interface LevelUpgrade {

    /** What replaces the block at {@code pos} once it is {@code material}; null if it is not this mod's or cannot take it now. */
    @Nullable
    PreparedLevelUpgrade prepare(Level level, BlockPos pos, BlockState state, Player player, StorageMaterial material);
}
