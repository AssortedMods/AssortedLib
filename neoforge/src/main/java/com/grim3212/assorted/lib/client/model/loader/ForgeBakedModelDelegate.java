package com.grim3212.assorted.lib.client.model.loader;

import com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel;
import com.grim3212.assorted.lib.client.model.baked.IDelegatingBakedModel;
import com.grim3212.assorted.lib.client.model.data.ForgeBlockModelDataPlatformDelegate;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.platform.ClientServices;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bridges {@link IDataAwareBakedModel} onto NeoForge's {@link ModelData} aware {@link BakedModel}
 * extension: NeoForge hands the model data of the position being drawn to
 * {@link #getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)}, which this passes on
 * as {@link IBlockModelData}. Any other model is forwarded untouched.
 */
public final class ForgeBakedModelDelegate implements IDelegatingBakedModel, IDataAwareBakedModel {

    private final BakedModel delegate;

    public ForgeBakedModelDelegate(final BakedModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public BakedModel getDelegate() {
        return this.delegate;
    }

    // Deprecated by NeoForge in favour of the ModelData overload; still abstract on BakedModel, and
    // both this and IDataAwareBakedModel declare it, so it has to be implemented here.
    @Override
    @Deprecated
    public @NotNull List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, final @NotNull RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, @NotNull final RandomSource rand, @NotNull final ModelData data, @Nullable final RenderType renderType) {
        if (this.delegate instanceof IDataAwareBakedModel dataAwareBakedModel) {
            return dataAwareBakedModel.getQuads(state, side, rand, new ForgeBlockModelDataPlatformDelegate(data), renderType);
        }

        return this.delegate.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, @NotNull final RandomSource rand, @NotNull final IBlockModelData extraData, @Nullable final RenderType renderType) {
        if (this.delegate instanceof IDataAwareBakedModel dataAwareBakedModel) {
            return dataAwareBakedModel.getQuads(state, side, rand, extraData, renderType);
        }

        return this.delegate.getQuads(state, side, rand, unwrap(extraData), renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.delegate.useAmbientOcclusion();
    }

    @Override
    public @NotNull TriState useAmbientOcclusion(final @NotNull BlockState state, final @NotNull ModelData data, final @NotNull RenderType renderType) {
        return this.delegate.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public boolean isGui3d() {
        return this.delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.delegate.isCustomRenderer();
    }

    // Deprecated by NeoForge in favour of the ModelData overload; still abstract on BakedModel.
    @Override
    @Deprecated
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull final ModelData data) {
        if (this.delegate instanceof IDataAwareBakedModel dataAwareBakedModel) {
            return dataAwareBakedModel.getParticleIcon(new ForgeBlockModelDataPlatformDelegate(data));
        }

        return this.delegate.getParticleIcon(data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull final IBlockModelData extraData) {
        if (this.delegate instanceof IDataAwareBakedModel dataAwareBakedModel) {
            return dataAwareBakedModel.getParticleIcon(extraData);
        }

        return this.delegate.getParticleIcon(unwrap(extraData));
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    // Deprecated by NeoForge in favour of applyTransform, which is forwarded below; a caller that
    // still reads the transforms directly gets the delegate's.
    @Override
    @Deprecated
    public @NotNull ItemTransforms getTransforms() {
        return this.delegate.getTransforms();
    }

    @Override
    public @NotNull BakedModel applyTransform(final @NotNull ItemDisplayContext transformType, final @NotNull PoseStack poseStack, final boolean applyLeftHandTransform) {
        return ClientServices.MODELS.adaptToPlatform(this.delegate.applyTransform(transformType, poseStack, applyLeftHandTransform));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull final BlockAndTintGetter level, @NotNull final BlockPos pos, @NotNull final BlockState state, @NotNull final ModelData modelData) {
        return this.delegate.getModelData(level, pos, state, modelData);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull final BlockState state, @NotNull final RandomSource rand, @NotNull final ModelData data) {
        return ChunkRenderTypeSet.of(getSupportedRenderTypes(state, rand, new ForgeBlockModelDataPlatformDelegate(data)));
    }

    @Override
    public @NotNull List<RenderType> getRenderTypes(final @NotNull ItemStack itemStack, final boolean fabulous) {
        return getSupportedRenderTypes(itemStack, fabulous);
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(final @NotNull ItemStack itemStack, final boolean fabulous) {
        if (this.delegate instanceof IDataAwareBakedModel) {
            return List.of(this);
        }

        return this.delegate.getRenderPasses(itemStack, fabulous).stream()
                .map(pass -> pass == this.delegate ? this : ClientServices.MODELS.adaptToPlatform(pass))
                .toList();
    }

    @Override
    public @NotNull Collection<RenderType> getSupportedRenderTypes(final BlockState state, final RandomSource rand, final IBlockModelData data) {
        if (this.delegate instanceof IDataAwareBakedModel dataAwareBakedModel) {
            return dataAwareBakedModel.getSupportedRenderTypes(state, rand, data);
        }

        return this.delegate.getRenderTypes(state, rand, unwrap(data)).asList();
    }

    @Override
    public @NotNull List<RenderType> getSupportedRenderTypes(final ItemStack stack, final boolean fabulous) {
        if (this.delegate instanceof IDataAwareBakedModel dataAwareBakedModel) {
            return dataAwareBakedModel.getSupportedRenderTypes(stack, fabulous);
        }

        return new ArrayList<>(this.delegate.getRenderTypes(stack, fabulous));
    }

    private static ModelData unwrap(final IBlockModelData data) {
        return data instanceof ForgeBlockModelDataPlatformDelegate platformDelegate ? platformDelegate.getDelegate() : ModelData.EMPTY;
    }
}
