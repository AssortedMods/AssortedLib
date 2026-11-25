package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.platform.services.ILevelPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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

public class ForgeLevelPropertyAccessor implements ILevelPropertyAccessor {
    @Override
    public boolean shouldCheckWeakPower(LevelReader levelReader, BlockPos blockPos, Direction direction) {
        return levelReader.getBlockState(blockPos).shouldCheckWeakPower(levelReader, blockPos, direction);
    }

    @Override
    public float getFriction(LevelReader levelReader, BlockPos blockPos, @Nullable Entity entity) {
        return levelReader.getBlockState(blockPos).getFriction(levelReader, blockPos, entity);
    }

    @Override
    public int getLightEmission(BlockGetter getter, BlockPos blockPos) {
        return getter.getBlockState(blockPos).getLightEmission(getter, blockPos);
    }

    @Override
    public boolean canHarvestBlock(BlockGetter blockGetter, BlockPos pos, Player player) {
        return blockGetter.getBlockState(pos).canHarvestBlock(blockGetter, pos, player);
    }

    @Override
    public SoundType getSoundType(LevelReader levelReader, BlockPos pos, Entity entity) {
        return levelReader.getBlockState(pos).getSoundType(levelReader, pos, entity);
    }

    @Override
    public float getExplosionResistance(BlockGetter blockGetter, BlockPos position, Explosion explosion) {
        return blockGetter.getBlockState(position).getExplosionResistance(blockGetter, position, explosion);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, BlockGetter blockGetter, BlockPos pos, Player player) {
        // NeoForge's hook takes the hit it was picked with and a LevelReader; there is no hit here,
        // so it is the top face of the block; the default hook ignores it.
        if (blockGetter instanceof LevelReader levelReader) {
            return state.getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), levelReader, pos, player);
        }

        return new ItemStack(state.getBlock());
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        return state.getBlock().getMapColor(state, level, pos, defaultColor);
    }
}
