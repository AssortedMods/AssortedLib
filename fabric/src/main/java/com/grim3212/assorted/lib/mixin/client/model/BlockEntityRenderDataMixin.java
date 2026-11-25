package com.grim3212.assorted.lib.mixin.client.model;

import com.grim3212.assorted.lib.core.block.IBlockEntityWithModelData;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands an {@link IBlockEntityWithModelData}'s model data to Fabric as the block entity's render
 * data. Fabric reads it on the main thread while it builds a chunk section and keeps the snapshot,
 * so {@code FabricBakedModelDelegate} draws from it without touching the block entity from a chunk
 * building thread. {@code getRenderData} is added to {@link BlockEntity} by Fabric's own mixin, which
 * applies first at the default priority, so this injects into the method it merged.
 */
@Mixin(value = BlockEntity.class, priority = 1100)
public abstract class BlockEntityRenderDataMixin {

    @Inject(method = "getRenderData", at = @At("HEAD"), cancellable = true, remap = false)
    private void assortedlib_modelDataAsRenderData(final CallbackInfoReturnable<Object> cir) {
        if (this instanceof IBlockEntityWithModelData withData) {
            cir.setReturnValue(withData.getBlockModelData());
        }
    }
}
