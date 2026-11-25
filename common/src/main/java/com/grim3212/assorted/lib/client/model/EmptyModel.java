package com.grim3212.assorted.lib.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A model with no quads or texture dependencies, as a {@link BakedModel} ({@link #BAKED}).
 */
public class EmptyModel {
    public static final BakedModel BAKED = new Baked();

    private EmptyModel() {
    }

    /**
     * The missing texture on the block atlas. Looked up on each call because there is no sprite to
     * hand out until the atlas has been stitched, and a reload stitches a new one.
     */
    public static TextureAtlasSprite missingSprite() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }

    private static class Baked implements BakedModel {
        @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
        @Override
        public TextureAtlasSprite getParticleIcon() {
            return missingSprite();
        }

        @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for applyTransform, which common cannot see
        @Override
        public ItemTransforms getTransforms() {
            return ItemTransforms.NO_TRANSFORMS;
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
