package com.grim3212.assorted.lib.mixin.client.model;

import com.grim3212.assorted.lib.client.model.data.ForgeBlockModelDataPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.core.block.IBlockEntityWithModelData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// 26.3 asks the block state, with its level and position, rather than the block entity directly.
@Mixin(value = ModelDataManager.class, remap = false)
public abstract class ModelDataManagerMixin {
    @Redirect(
            method = "refreshAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getModelData(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/model/data/ModelData;"
            )
    )
    private ModelData getModelDataRetrieval(final BlockState state, final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IBlockEntityWithModelData blockEntityWithModelData) {
            final IBlockModelData blockModelData = blockEntityWithModelData.getBlockModelData();
            if (blockModelData == null) {
                return ModelData.EMPTY;
            }

            if (!(blockModelData instanceof ForgeBlockModelDataPlatformDelegate platformDelegate)) {
                throw new IllegalStateException("Block model data is not compatible with the current platform.");
            }

            return platformDelegate.getDelegate();
        }

        return state.getModelData(level, pos);
    }
}
