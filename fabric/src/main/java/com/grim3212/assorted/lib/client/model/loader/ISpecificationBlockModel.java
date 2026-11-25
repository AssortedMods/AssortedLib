package com.grim3212.assorted.lib.client.model.loader;

import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented on vanilla's {@code BlockModel} by {@code BlockModelSpecificationMixin}: the
 * specification its json's {@code loader} read, if any. A model inherits the nearest one up its
 * parent chain, as NeoForge's custom geometry is inherited.
 */
public interface ISpecificationBlockModel {

    @Nullable
    IModelSpecification<?> assortedlib$getSpecification();

    void assortedlib$setSpecification(@Nullable IModelSpecification<?> specification);
}
