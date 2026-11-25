package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.model.data.ForgeBlockModelDataPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.ForgeModelDataMapBuilderPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.ForgeModelPropertyPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.client.model.data.IModelDataBuilder;
import com.grim3212.assorted.lib.client.model.data.IModelDataKey;
import com.grim3212.assorted.lib.client.model.loader.ForgeBakedModelDelegate;
import com.grim3212.assorted.lib.platform.services.IClientModelHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ForgeClientModelHelper implements IClientModelHelper {

    @Override
    public void requestModelDataRefresh(BlockEntity blockEntity) {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().getModelDataManager() != null) {
            blockEntity.getLevel().getModelDataManager().requestRefresh(blockEntity);
        }
    }

    @Override
    public IBlockModelData empty() {
        return new ForgeBlockModelDataPlatformDelegate(ModelData.EMPTY);
    }

    @Override
    public @NotNull IModelDataBuilder createNewModelDataBuilder() {
        return new ForgeModelDataMapBuilderPlatformDelegate();
    }

    @Override
    public @NotNull <T> IModelDataKey<T> createNewModelDataKey() {
        return new ForgeModelPropertyPlatformDelegate<>(new ModelProperty<>());
    }

    @Override
    public BakedModel adaptToPlatform(final BakedModel model) {
        if (model instanceof ForgeBakedModelDelegate)
            return model;

        return new ForgeBakedModelDelegate(model);
    }

    @Override
    public @NotNull Collection<RenderType> getRenderTypesFor(BakedModel model, BlockState state, RandomSource rand, IBlockModelData data) {
        final ModelData modelData = data instanceof ForgeBlockModelDataPlatformDelegate platformDelegate ? platformDelegate.getDelegate() : ModelData.EMPTY;
        return model.getRenderTypes(state, rand, modelData).asList();
    }

    @Override
    public @NotNull List<RenderType> getRenderTypesFor(BakedModel model, ItemStack stack, boolean fabulous) {
        return model.getRenderTypes(stack, fabulous);
    }
}
