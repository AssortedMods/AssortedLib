package com.grim3212.assorted.lib.client.model.baked;

import net.minecraft.client.resources.model.BakedModel;

/**
 * Defines a model that delegates its operations to another model.
 */
public interface IDelegatingBakedModel extends BakedModel {

    /**
     * The model that this model delegates its operations to.
     *
     * @return The delegate.
     */
    BakedModel getDelegate();
}
