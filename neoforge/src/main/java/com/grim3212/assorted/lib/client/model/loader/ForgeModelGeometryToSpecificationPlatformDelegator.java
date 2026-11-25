package com.grim3212.assorted.lib.client.model.loader;

import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.platform.ClientServices;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

/**
 * Adapts an {@link IModelSpecification} onto NeoForge's {@link IUnbakedGeometry}. The baked model is
 * wrapped in a {@link ForgeBakedModelDelegate}, so a data aware specification is handed the model
 * data of the position it is drawn at.
 */
public final class ForgeModelGeometryToSpecificationPlatformDelegator<T extends IModelSpecification<T>>
        implements IUnbakedGeometry<ForgeModelGeometryToSpecificationPlatformDelegator<T>> {

    private final T delegate;

    public ForgeModelGeometryToSpecificationPlatformDelegator(final T delegate) {
        this.delegate = delegate;
    }

    public T getDelegate() {
        return this.delegate;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        final ForgeModelBakingContextDelegate contextDelegate = new ForgeModelBakingContextDelegate(baker::getModel, context);

        return ClientServices.MODELS.adaptToPlatform(this.delegate.bake(contextDelegate, baker, spriteGetter, modelState, overrides));
    }

    /**
     * Resolves the parents of every model the specification reaches for on its own, which the
     * bakery only does for the models it knows of.
     */
    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        for (ResourceLocation dependency : this.delegate.getDependencies()) {
            modelGetter.apply(dependency).resolveParents(modelGetter);
        }
    }
}
