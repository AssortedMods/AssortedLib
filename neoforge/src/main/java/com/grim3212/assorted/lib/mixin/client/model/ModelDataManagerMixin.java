package com.grim3212.assorted.lib.mixin.client.model;

import com.grim3212.assorted.lib.client.model.data.ForgeBlockModelDataPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.core.block.IBlockEntityWithModelData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// A block entity of ours keeps its model data as common IBlockModelData; hand NeoForge the
// ModelData that carries it.
@Mixin(value = ModelDataManager.class, remap = false)
public abstract class ModelDataManagerMixin {
    @Redirect(
            method = "refreshAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getModelData()Lnet/neoforged/neoforge/client/model/data/ModelData;"
            )
    )
    private ModelData getModelDataRetrieval(final BlockEntity blockEntity) {
        if (blockEntity instanceof IBlockEntityWithModelData blockEntityWithModelData) {
            final IBlockModelData blockModelData = blockEntityWithModelData.getBlockModelData();
            if (blockModelData == null) {
                return ModelData.EMPTY;
            }

            if (!(blockModelData instanceof ForgeBlockModelDataPlatformDelegate platformDelegate)) {
                throw new IllegalStateException("Block model data is not compatible with the current platform.");
            }

            return platformDelegate.getDelegate();
        }

        return blockEntity.getModelData();
    }
}
