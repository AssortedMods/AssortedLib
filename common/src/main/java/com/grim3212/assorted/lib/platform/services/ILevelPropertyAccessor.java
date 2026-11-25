package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.core.block.IBlockLightDampening;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public interface ILevelPropertyAccessor {

    boolean shouldCheckWeakPower(LevelReader levelReader, BlockPos blockPos, Direction direction);

    float getFriction(LevelReader levelReader, BlockPos blockPos, @Nullable Entity entity);

    int getLightEmission(BlockGetter getter, BlockPos blockPos);

    // A block that stands in for another one - a colorizer - answers for where it is through
    // IBlockLightDampening; everything else answers through vanilla's own position-aware queries.
    default int getLightBlock(BlockGetter blockGetter, BlockPos blockPos) {
        final BlockState blockState = blockGetter.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockLightDampening dampening) {
            return dampening.getLightDampening(blockState, blockGetter, blockPos);
        }

        return blockState.getLightBlock(blockGetter, blockPos);
    }

    default boolean propagatesSkylightDown(BlockGetter blockGetter, BlockPos blockPos) {
        final BlockState blockState = blockGetter.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockLightDampening dampening) {
            return dampening.skylightPassesDown(blockState, blockGetter, blockPos);
        }

        return blockState.propagatesSkylightDown(blockGetter, blockPos);
    }

    boolean canHarvestBlock(BlockGetter blockGetter, BlockPos pos, Player player);

    SoundType getSoundType(final LevelReader levelReader, BlockPos pos, Entity entity);

    float getExplosionResistance(BlockGetter blockGetter, BlockPos position, Explosion explosion);

    ItemStack getCloneItemStack(BlockState state, BlockGetter blockGetter, BlockPos pos, Player player);

    MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor);
}
