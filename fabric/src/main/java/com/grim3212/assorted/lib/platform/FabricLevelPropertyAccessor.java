package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.block.*;
import com.grim3212.assorted.lib.platform.services.ILevelPropertyAccessor;
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

public class FabricLevelPropertyAccessor implements ILevelPropertyAccessor {
    @Override
    public boolean shouldCheckWeakPower(LevelReader levelReader, BlockPos blockPos, Direction direction) {
        final BlockState blockState = levelReader.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockExtraProperties extraProperties) {
            return extraProperties.shouldCheckWeakPower(blockState, levelReader, blockPos, direction);
        }

        return blockState.isRedstoneConductor(levelReader, blockPos);
    }

    @Override
    public float getFriction(LevelReader levelReader, BlockPos blockPos, @Nullable Entity entity) {
        final BlockState blockState = levelReader.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockExtraProperties extraProperties) {
            return extraProperties.getFriction(blockState, levelReader, blockPos, entity);
        }

        return blockState.getBlock().getFriction();
    }

    @Override
    public int getLightEmission(BlockGetter getter, BlockPos blockPos) {
        final BlockState blockState = getter.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockLightEmission extraProperties) {
            return extraProperties.getLightEmission(blockState, getter, blockPos);
        }

        return blockState.getLightEmission();
    }

    @Override
    public boolean canHarvestBlock(BlockGetter blockGetter, BlockPos blockPos, Player player) {
        final BlockState blockState = blockGetter.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockCanHarvest extraProperties) {
            return extraProperties.canHarvestBlock(blockState, blockGetter, blockPos, player);
        }

        return player.hasCorrectToolForDrops(blockState);
    }

    @Override
    public SoundType getSoundType(LevelReader levelReader, BlockPos blockPos, Entity entity) {
        final BlockState blockState = levelReader.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockSoundType extraProperties) {
            return extraProperties.getSoundType(blockState, levelReader, blockPos, entity);
        }

        return blockState.getSoundType();
    }

    @Override
    public float getExplosionResistance(BlockGetter blockGetter, BlockPos blockPos, Explosion explosion) {
        final BlockState blockState = blockGetter.getBlockState(blockPos);
        if (blockState.getBlock() instanceof IBlockExtraProperties extraProperties) {
            return extraProperties.getExplosionResistance(blockState, blockGetter, blockPos, explosion);
        }

        return blockState.getBlock().getExplosionResistance();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, BlockGetter blockGetter, BlockPos pos, Player player) {
        // Block#getCloneItemStack needs a LevelReader, because it may read block entity data; fall
        // back to the bare item when the getter is not a level.
        if (blockGetter instanceof LevelReader levelReader) {
            return state.getBlock().getCloneItemStack(levelReader, pos, state);
        }
        return new ItemStack(state.getBlock());
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        if (state.getBlock() instanceof IBlockMapColor extraProperties) {
            return extraProperties.getMapColor(state, level, pos, defaultColor);
        }
        return state.getMapColor(level, pos);
    }
}
