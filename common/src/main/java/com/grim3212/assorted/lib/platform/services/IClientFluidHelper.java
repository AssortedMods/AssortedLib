package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.core.fluid.FluidInformation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/**
 * <b>Render time only.</b> Fluid models are baked after every other model, so calling this during
 * model baking throws {@code Fluid models not yet initialized}. A model that needs a fluid sprite
 * must be an {@code ItemModel} resolved at render time (AssortedTools'
 * {@code FluidContainerItemModel}).
 */
public interface IClientFluidHelper {

    int getFluidColor(final FluidInformation fluid);

    TextureAtlasSprite getSprite(final FluidInformation fluid);

    /** The texture used to render the flowing fluid. */
    ResourceLocation getFlowingFluidTexture(final FluidInformation fluidInformation);

    /** The texture used to render the flowing fluid. */
    ResourceLocation getFlowingFluidTexture(final Fluid fluid);

    /** The texture used to render the still fluid. */
    ResourceLocation getStillFluidTexture(final FluidInformation fluidInformation);

    /** The texture used to render the still fluid. */
    ResourceLocation getStillFluidTexture(final Fluid fluid);
}
