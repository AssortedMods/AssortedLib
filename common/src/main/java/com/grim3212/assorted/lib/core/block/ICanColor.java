package com.grim3212.assorted.lib.core.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A block a paint roller can recolor in place. Lives here so a mod's blocks take paint without depending on the mod the roller comes from. */
public interface ICanColor {

    @Nullable
    DyeColor currentColor(BlockState state);

    BlockState stateForColor(BlockState state, DyeColor color);
}
