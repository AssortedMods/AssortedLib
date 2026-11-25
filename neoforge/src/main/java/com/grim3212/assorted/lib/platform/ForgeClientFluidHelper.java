package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.fluid.FluidInformation;
import com.grim3212.assorted.lib.platform.services.IClientFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * A fluid's textures and tint are the client extensions of its {@code FluidType}. The sprite is
 * looked up on the block atlas, where fluid textures are stitched.
 */
public class ForgeClientFluidHelper implements IClientFluidHelper {

    @Override
    public int getFluidColor(FluidInformation fluid) {
        return IClientFluidTypeExtensions.of(fluid.fluid()).getTintColor(ForgeFluidManager.buildFluidStack(fluid));
    }

    @Override
    public TextureAtlasSprite getSprite(FluidInformation fluid) {
        final ResourceLocation still = IClientFluidTypeExtensions.of(fluid.fluid()).getStillTexture(ForgeFluidManager.buildFluidStack(fluid));
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
    }

    @Override
    public ResourceLocation getFlowingFluidTexture(final FluidInformation fluidInformation) {
        return IClientFluidTypeExtensions.of(fluidInformation.fluid()).getFlowingTexture(ForgeFluidManager.buildFluidStack(fluidInformation));
    }

    @Override
    public ResourceLocation getFlowingFluidTexture(final Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getFlowingTexture();
    }

    @Override
    public ResourceLocation getStillFluidTexture(final FluidInformation fluidInformation) {
        return IClientFluidTypeExtensions.of(fluidInformation.fluid()).getStillTexture(ForgeFluidManager.buildFluidStack(fluidInformation));
    }

    @Override
    public ResourceLocation getStillFluidTexture(final Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getStillTexture();
    }
}
