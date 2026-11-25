package com.grim3212.assorted.lib.client.model.loaders.context;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * The context in which the model is baked: what the model json declared around its
 * {@code loader} object, answered by each loader from its own baking context.
 */
public interface IModelBakingContext {

    /**
     * The unbaked model named {@code unbakedModel}, or the missing model if there is none; can be
     * used to get a parent. List it in
     * {@link com.grim3212.assorted.lib.client.model.loaders.IModelSpecification#getDependencies()}
     * so its own parents are resolved.
     */
    UnbakedModel getUnbakedModel(final ResourceLocation unbakedModel);

    /**
     * A texture from the json or its parents, or empty if not found. {@code name} may be given with
     * or without its leading {@code #}.
     */
    Optional<Material> getMaterial(final String name);

    /**
     * Indicates if this context is baking a model that is rendered using 3D Light.
     *
     * @return {@code true} if the model is rendered using 3D Light, {@code false} otherwise.
     */
    boolean isGui3d();

    /**
     * Indicates if this context is baking a model that is rendered using a blocks lighting layout.
     *
     * @return {@code true} if the model is rendered using a blocks lighting layout, {@code false} otherwise.
     */
    boolean useBlockLight();

    /**
     * Indicates if this context is using ambient occlusion.
     *
     * @return {@code true} if ambient occlusion is used, {@code false} otherwise.
     */
    boolean useAmbientOcclusion();

    /**
     * The item transforms which should be applied to the model.
     *
     * @return The item transforms.
     */
    ItemTransforms getTransforms();
}
