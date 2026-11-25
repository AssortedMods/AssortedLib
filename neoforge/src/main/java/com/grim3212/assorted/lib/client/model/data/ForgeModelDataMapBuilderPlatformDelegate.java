package com.grim3212.assorted.lib.client.model.data;

import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class ForgeModelDataMapBuilderPlatformDelegate implements IModelDataBuilder {

    private final ModelData.Builder delegate;

    public ForgeModelDataMapBuilderPlatformDelegate() {
        delegate = ModelData.builder();
    }

    @Override
    public IBlockModelData build() {
        return new ForgeBlockModelDataPlatformDelegate(delegate.build());
    }

    @Override
    public <T> IModelDataBuilder withInitial(final IModelDataKey<T> key, final T value) {
        if (!(key instanceof ForgeModelPropertyPlatformDelegate))
            throw new IllegalArgumentException("The given key is not a Forge platform compatible model data key.");

        final ModelProperty<T> property = ((ForgeModelPropertyPlatformDelegate<T>) key).getProperty();
        delegate.with(property, value);

        return this;
    }
}
