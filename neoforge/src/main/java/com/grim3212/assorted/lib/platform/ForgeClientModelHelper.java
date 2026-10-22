package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.model.data.ForgeBlockModelDataPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.ForgeModelDataMapBuilderPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.ForgeModelPropertyPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.client.model.data.IModelDataBuilder;
import com.grim3212.assorted.lib.client.model.data.IModelDataKey;
import com.grim3212.assorted.lib.client.model.loader.ForgeBakedModelDelegate;
import com.grim3212.assorted.lib.platform.services.IClientModelHelper;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

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
    public BlockStateModel adaptToPlatform(final BlockStateModel model) {
        if (model instanceof ForgeBakedModelDelegate)
            return model;

        return new ForgeBakedModelDelegate(model);
    }

}
