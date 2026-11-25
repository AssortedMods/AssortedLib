package com.grim3212.assorted.lib.client.model.loaders;

import com.grim3212.assorted.lib.client.model.loaders.context.IModelBakingContext;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * The geometry a model json's {@code loader} produces, baked by each loader's own model loading
 * hook (NeoForge's {@code IUnbakedGeometry}, Fabric's model loading plugin). A superset of vanilla
 * {@link UnbakedModel}: the baked model it returns is the whole model, drawn with the level and
 * position at hand, so an {@link com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel}
 * can be returned directly.
 */
public interface IModelSpecification<T extends IModelSpecification<T>> {
    /**
     * Bakes this specification into a model.
     *
     * @param spriteGetter Resolves a {@link Material} to its stitched sprite.
     * @param overrides    The item overrides the json declared, to hand to the baked model unless it
     *                     picks its item form itself.
     */
    BakedModel bake(IModelBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides);

    /**
     * Every model this specification looks up through {@link ModelBaker#getModel(ResourceLocation)}
     * or bakes through {@link ModelBaker#bake(ResourceLocation, ModelState)}. The loader resolves
     * their parents before baking, which vanilla only does for the models it knows of.
     */
    default Collection<ResourceLocation> getDependencies() {
        return List.of();
    }
}
