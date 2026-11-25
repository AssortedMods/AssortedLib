package com.grim3212.assorted.lib.fluid;

import com.grim3212.assorted.lib.core.fluid.IFluidVariantHandler;
import com.grim3212.assorted.lib.platform.FabricFluidManager;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/** Answers Fabric's render questions for a fluid whose handler is one of ours. */
public class FabricFluidVariantRenderHandlerDelegate implements FluidVariantRenderHandler {
    private final IFluidVariantHandler delegate;

    public FabricFluidVariantRenderHandlerDelegate(final IFluidVariantHandler delegate) {
        this.delegate = delegate;
    }

    /** Still and flowing sprite, from the textures the handler names, on the block atlas. */
    @Override
    public @Nullable TextureAtlasSprite[] getSprites(final FluidVariant fluidVariant) {
        final var information = FabricFluidManager.makeInformation(fluidVariant);
        final Optional<ResourceLocation> still = delegate.getStillTexture(information);
        if (still.isEmpty()) {
            return FluidVariantRenderHandler.super.getSprites(fluidVariant);
        }

        final Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        final TextureAtlasSprite stillSprite = atlas.apply(still.get());
        final TextureAtlasSprite flowingSprite = delegate.getFlowingTexture(information).map(atlas).orElse(stillSprite);
        return new TextureAtlasSprite[]{stillSprite, flowingSprite};
    }

    @Override
    public int getColor(final FluidVariant fluidVariant, @Nullable final BlockAndTintGetter view, @Nullable final BlockPos pos) {
        return delegate.getTintColor(FabricFluidManager.makeInformation(fluidVariant));
    }

    public IFluidVariantHandler getDelegate() {
        return delegate;
    }
}
