package com.grim3212.assorted.lib.client.model.loader;

import com.grim3212.assorted.lib.client.model.loaders.context.IModelBakingContext;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;

import java.util.Optional;
import java.util.function.Function;

/**
 * An {@link IModelBakingContext} answered from NeoForge's {@link IGeometryBakingContext}, which
 * holds what the model json declared around its {@code loader} object.
 */
public class ForgeModelBakingContextDelegate implements IModelBakingContext {

    private final Function<ResourceLocation, UnbakedModel> unbakedModelGetter;
    private final IGeometryBakingContext delegate;

    public ForgeModelBakingContextDelegate(final Function<ResourceLocation, UnbakedModel> unbakedModelGetter, final IGeometryBakingContext delegate) {
        this.unbakedModelGetter = unbakedModelGetter;
        this.delegate = delegate;
    }

    @Override
    public UnbakedModel getUnbakedModel(final ResourceLocation unbakedModel) {
        return this.unbakedModelGetter.apply(unbakedModel);
    }

    @Override
    public Optional<Material> getMaterial(final String name) {
        if (this.delegate.hasMaterial(name)) {
            return Optional.of(this.delegate.getMaterial(name));
        }

        return Optional.empty();
    }

    @Override
    public boolean isGui3d() {
        return this.delegate.isGui3d();
    }

    @Override
    public boolean useBlockLight() {
        return this.delegate.useBlockLight();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.delegate.useAmbientOcclusion();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.delegate.getTransforms();
    }
}
