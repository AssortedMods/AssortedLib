package com.grim3212.assorted.lib.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block whose light dampening depends on where it is, not only on its state - one that keeps what
 * it stands in for in its block entity, such as a colorizer. This is what
 * {@code ILevelPropertyAccessor} asks, beside {@link IBlockLightEmission}; it does not reach the
 * light engines by itself. Those ask vanilla's position-aware {@code BlockBehaviour#getLightBlock}
 * and {@code #propagatesSkylightDown}, which a block state only stops baking when its block has a
 * dynamic shape, so a block whose dampening must really darken the world overrides those too.
 */
public interface IBlockLightDampening {

    /**
     * How much light this block takes out of what passes through it, 0-15, as vanilla's
     * {@code BlockState#getLightBlock} would answer for the block it stands in for.
     */
    int getLightDampening(BlockState state, BlockGetter blockGetter, BlockPos pos);

    /**
     * Whether skylight passes straight down through this block, as vanilla's
     * {@code BlockState#propagatesSkylightDown} would answer for the block it stands in for. Not named
     * after that method: on 1.21.1 vanilla's protected {@code BlockBehaviour#propagatesSkylightDown}
     * takes these same arguments, so a block implementing this would override it by accident.
     */
    default boolean skylightPassesDown(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return state.propagatesSkylightDown(blockGetter, pos);
    }
}
