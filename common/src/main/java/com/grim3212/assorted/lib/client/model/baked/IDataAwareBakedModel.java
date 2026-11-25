package com.grim3212.assorted.lib.client.model.baked;

import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * A {@link BakedModel} whose geometry also depends on the model data a block entity exposes.
 * Vanilla's {@link BakedModel#getQuads(BlockState, Direction, RandomSource)} gets no data, so the
 * loader delegates ({@code FabricBakedModelDelegate}, {@code ForgeBakedModelDelegate}) route the
 * data of the position being drawn in here; vanilla call sites get
 * {@linkplain IBlockModelData#empty() empty} data. Wrap a model in its delegate with
 * {@code ClientServices.MODELS.adaptToPlatform}.
 * <p>
 * The item form of such a model reads its data from the stack instead: give the model
 * {@link com.grim3212.assorted.lib.client.model.item.DataAwareItemOverrides} as its
 * {@link #getOverrides() overrides}.
 */
public interface IDataAwareBakedModel extends BakedModel {

    /**
     * The quads of this model for {@code extraData}.
     *
     * @param renderType The chunk layer being drawn, or null when the caller wants every layer.
     */
    @NotNull
    List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull IBlockModelData extraData, @Nullable RenderType renderType);

    /**
     * The particle sprite for {@code extraData}. Break and hit particles read this rather than the
     * geometry, so a model textured from a block entity must answer it from the data too.
     */
    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
    default TextureAtlasSprite getParticleIcon(@NotNull IBlockModelData extraData) {
        return getParticleIcon();
    }

    /**
     * The chunk layers this model draws {@code state} in for {@code data}. Each is then asked for
     * its quads through {@link #getQuads(BlockState, Direction, RandomSource, IBlockModelData, RenderType)}.
     */
    @NotNull
    Collection<RenderType> getSupportedRenderTypes(BlockState state, RandomSource rand, IBlockModelData data);

    /**
     * The render types this model draws {@code stack} with as an item.
     */
    @NotNull
    List<RenderType> getSupportedRenderTypes(ItemStack stack, boolean fabulous);

    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
    @Override
    default List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, IBlockModelData.empty(), null);
    }
}
