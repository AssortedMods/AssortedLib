package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.client.model.data.IModelDataBuilder;
import com.grim3212.assorted.lib.client.model.data.IModelDataKey;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

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
     * Adapts a vanilla {@link BakedModel} to this loader's own model implementation, unlocking
     * loader-specific functionality where there is any. An
     * {@link com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel} comes back wrapped in
     * the loader's delegate, which feeds it the model data of the position it is drawn at.
     */
    BakedModel adaptToPlatform(final BakedModel model);

    /**
     * The chunk layers {@code model} draws {@code state} in, asked the way the loader asks: on
     * NeoForge through the model ({@code render_type} in its json, or the model's own answer), on
     * Fabric through the block's registered layer. Meant for a model one of ours stands in for, such
     * as a colorizer's stored block; a data aware model asking about itself would be asked back.
     */
    @NotNull
    Collection<RenderType> getRenderTypesFor(BakedModel model, BlockState state, RandomSource rand, IBlockModelData data);

    /**
     * The render types {@code model} draws {@code stack} with as an item, asked the way the loader
     * asks. The same rule applies as for the block form: for a foreign model only.
     */
    @NotNull
    List<RenderType> getRenderTypesFor(BakedModel model, ItemStack stack, boolean fabulous);
}
