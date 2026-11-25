package com.grim3212.assorted.lib.fluid;

import com.grim3212.assorted.lib.core.fluid.FluidInformation;
import com.grim3212.assorted.lib.core.fluid.IFluidVariantHandler;
import com.grim3212.assorted.lib.platform.ForgeFluidManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Optional;

public class ForgeFluidVariantHandlerDelegate implements IFluidVariantHandler {
    private final FluidType delegate;

    public ForgeFluidVariantHandlerDelegate(final FluidType delegate) {
        this.delegate = delegate;
    }

    @Override
    public Component getName(final FluidInformation fluidInformation) {
        return delegate.getDescription(ForgeFluidManager.buildFluidStack(fluidInformation));
    }

    @Override
    public Optional<SoundEvent> getFillSound(final FluidInformation variant) {
        return Optional.ofNullable(delegate.getSound(ForgeFluidManager.buildFluidStack(variant), SoundActions.BUCKET_FILL));
    }

    @Override
    public Optional<SoundEvent> getEmptySound(final FluidInformation variant) {
        return Optional.ofNullable(delegate.getSound(ForgeFluidManager.buildFluidStack(variant), SoundActions.BUCKET_EMPTY));
    }

    @Override
    public int getLuminance(final FluidInformation variant) {
        return delegate.getLightLevel(ForgeFluidManager.buildFluidStack(variant));
    }

    @Override
    public int getTemperature(final FluidInformation variant) {
        return delegate.getTemperature(ForgeFluidManager.buildFluidStack(variant));
    }

    @Override
    public int getViscosity(final FluidInformation variant) {
        return delegate.getViscosity(ForgeFluidManager.buildFluidStack(variant));
    }

    @Override
    public int getDensity(final FluidInformation variant) {
        return delegate.getDensity(ForgeFluidManager.buildFluidStack(variant));
    }

    @Override
    public int getTintColor(final FluidInformation variant) {
        return client().getTintColor(ForgeFluidManager.buildFluidStack(variant));
    }

    @Override
    public Optional<ResourceLocation> getStillTexture(final FluidInformation variant) {
        return Optional.ofNullable(client().getStillTexture(ForgeFluidManager.buildFluidStack(variant)));
    }

    @Override
    public Optional<ResourceLocation> getFlowingTexture(final FluidInformation variant) {
        return Optional.ofNullable(client().getFlowingTexture(ForgeFluidManager.buildFluidStack(variant)));
    }

    // A fluid's textures and tint are client-side extensions of its FluidType, so this is client
    // only. Fabric reads its FluidRenderHandler, the same per-fluid answer.
    private IClientFluidTypeExtensions client() {
        return IClientFluidTypeExtensions.of(this.delegate);
    }
}
