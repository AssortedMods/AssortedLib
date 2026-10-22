package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.model.data.*;
import com.grim3212.assorted.lib.client.model.loader.FabricBakedModelDelegate;
import com.grim3212.assorted.lib.platform.services.IClientModelHelper;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

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
    public BlockStateModel adaptToPlatform(BlockStateModel model) {
        if (model instanceof FabricBakedModelDelegate) {
            return model;
        }

        return new FabricBakedModelDelegate(model);
    }
}
