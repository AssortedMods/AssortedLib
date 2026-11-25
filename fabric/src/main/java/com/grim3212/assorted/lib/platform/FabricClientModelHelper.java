package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.model.data.*;
import com.grim3212.assorted.lib.client.model.loader.FabricBakedModelDelegate;
import com.grim3212.assorted.lib.platform.services.IClientModelHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collection;
import java.util.List;

public class FabricClientModelHelper implements IClientModelHelper {

    @Override
    public void requestModelDataRefresh(BlockEntity blockEntity) {
        // NO-OP
    }

    private static final IBlockModelData EMPTY = new ModelDataBuilder().build();

    @Override
    public IBlockModelData empty() {
        return EMPTY;
    }

    @Override
    public @NotNull IModelDataBuilder createNewModelDataBuilder() {
        return new ModelDataBuilder();
    }

    @Override
    public @NotNull <T> IModelDataKey<T> createNewModelDataKey() {
        return new ModelDataKey<>();
    }

    @Override
    public BakedModel adaptToPlatform(BakedModel model) {
        // Only a data aware model needs the delegate, which feeds it the model data of the position
        // it is drawn at; any other model already draws through Fabric's vanilla adapter.
        if (model instanceof FabricBakedModelDelegate || !(model instanceof IDataAwareBakedModel)) {
            return model;
        }

        return new FabricBakedModelDelegate(model);
    }

    /** Fabric has no per-model layers: a block draws in the chunk layer registered for it. */
    @Override
    public @NotNull Collection<RenderType> getRenderTypesFor(BakedModel model, BlockState state, RandomSource rand, IBlockModelData data) {
        return List.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    @Override
    public @NotNull List<RenderType> getRenderTypesFor(BakedModel model, ItemStack stack, boolean fabulous) {
        return List.of(ItemBlockRenderTypes.getRenderType(stack, fabulous));
    }
}
