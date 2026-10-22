package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.client.model.data.IModelDataBuilder;
import com.grim3212.assorted.lib.client.model.data.IModelDataKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public interface IClientModelHelper {
    /**
     * Refreshes the model data for the block entity.
     *
     * @param blockEntity The block entity to refresh the model data for.
     */
    void requestModelDataRefresh(BlockEntity blockEntity);

    /**
     * Returns an empty block model data to use in cases where it is not available.
     *
     * @return The block model data.
     */
    IBlockModelData empty();

    /**
     * Creates a new model data builder instance.
     *
     * @return The model data instance.
     */
    @NotNull
    IModelDataBuilder createNewModelDataBuilder();

    /** Creates a new model data key; each call returns a unique key. */
    @NotNull <T> IModelDataKey<T> createNewModelDataKey();

    /**
     * Adapts a vanilla {@link BlockStateModel} to this loader's own model implementation, unlocking
     * loader-specific functionality where there is any.
     */
    BlockStateModel adaptToPlatform(final BlockStateModel model);

    // No model lookup or render type queries here any more, because 26.2 has no answer either loader
    // can give honestly:
    //  - an UnbakedModel can only be resolved through a ModelBaker, mid-bake, so a model that needs
    //    one takes an IModelBakingContext instead of asking this service;
    //  - a block model does not declare its render types (a terrain layer comes per quad from
    //    BakedQuad.MaterialInfo) and a fluid has only the ChunkSectionLayer on its baked FluidModel,
    //    so a caller that must know reads the layer itself.
}
