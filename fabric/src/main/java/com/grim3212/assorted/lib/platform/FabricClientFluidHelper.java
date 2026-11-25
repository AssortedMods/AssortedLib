package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.fluid.FluidInformation;
import com.grim3212.assorted.lib.platform.services.IClientFluidHelper;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/**
 * Every answer comes from Fabric's {@link FluidVariantRendering}, which asks a fluid's registered
 * {@code FluidVariantRenderHandler} and falls back to its {@code FluidRenderHandler}, so our fluids
 * and foreign ones read the same source. The still sprite is index 0, the flowing one index 1.
 */
public class FabricClientFluidHelper implements IClientFluidHelper {

    @Override
    public int getFluidColor(FluidInformation fluid) {
        return FluidVariantRendering.getColor(FabricFluidManager.makeVariant(fluid));
    }

    @Override
    public TextureAtlasSprite getSprite(FluidInformation fluid) {
        return sprite(fluid.fluid(), 0);
    }

    @Override
    public ResourceLocation getFlowingFluidTexture(final FluidInformation fluidInformation) {
        return getFlowingFluidTexture(fluidInformation.fluid());
    }

    @Override
    public ResourceLocation getFlowingFluidTexture(final Fluid fluid) {
        return sprite(fluid, 1).contents().name();
    }

    @Override
    public ResourceLocation getStillFluidTexture(final FluidInformation fluidInformation) {
        return getStillFluidTexture(fluidInformation.fluid());
    }

    @Override
    public ResourceLocation getStillFluidTexture(final Fluid fluid) {
        return sprite(fluid, 0).contents().name();
    }

    private static TextureAtlasSprite sprite(final Fluid fluid, final int index) {
        final TextureAtlasSprite[] sprites = FluidVariantRendering.getSprites(FabricFluidManager.makeVariant(new FluidInformation(fluid, 1)));
        if (sprites == null || sprites.length <= index || sprites[index] == null) {
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
        }
        return sprites[index];
    }
}
