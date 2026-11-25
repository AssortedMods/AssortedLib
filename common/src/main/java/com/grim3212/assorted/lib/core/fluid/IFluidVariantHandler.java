package com.grim3212.assorted.lib.core.fluid;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.Optional;

public interface IFluidVariantHandler {
    /**
     * Return the name that should be used for the passed fluid variant.
     */
    Component getName(FluidInformation fluidInformation);

    /**
     * Return the sound corresponding to this fluid being filled, or none if no sound is available.
     */
    Optional<SoundEvent> getFillSound(FluidInformation variant);

    /**
     * Return the sound corresponding to this fluid being emptied, or none if no sound is available.
     */
    Optional<SoundEvent> getEmptySound(FluidInformation variant);

    /**
     * Return an integer in [0, 15]: the light level emitted by this fluid, or 0 if it doesn't naturally emit light.
     */
    int getLuminance(FluidInformation variant);

    /**
     * Return a non-negative integer, representing the temperature of this fluid in Kelvin.
     */
    int getTemperature(FluidInformation variant);

    /**
     * Return a positive integer, representing the viscosity of this fluid.
     * Fluids with lower viscosity generally flow faster than fluids with higher viscosity.
     */
    int getViscosity(FluidInformation variant);

    /** The fluid's density; 0 or less means it is lighter than air. */
    int getDensity(FluidInformation variant);

    /** The fluid's tint colour. */
    int getTintColor(FluidInformation variant);

    /**
     * The fluid's still texture, if it has one. 1.21.1 has no vanilla home for a fluid's textures, so
     * each loader answers from the one place it keeps them: NeoForge from the fluid type's
     * {@code IClientFluidTypeExtensions}, Fabric from its {@code FluidRenderHandlerRegistry} (through
     * {@code FluidVariantRendering}). Either way this is client only, and the component data a
     * {@link FluidInformation} carries does not change the answer.
     */
    Optional<ResourceLocation> getStillTexture(FluidInformation variant);

    /** The fluid's flowing texture, if it has one; same source as {@link #getStillTexture}. */
    Optional<ResourceLocation> getFlowingTexture(FluidInformation variant);
}
