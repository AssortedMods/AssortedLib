package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.core.storage.StorageModelProperties;
import com.grim3212.assorted.lib.platform.ClientServices;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
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
 * Picks the locked or unlocked geometry from the model data the block entity publishes for its
 * position. An item has no block entity, so a locked item picks its child through the json's
 * {@code overrides} on {@link LockedItemProperty} instead, and draws unlocked by default.
 */
public class LockedBakedModel implements IDataAwareBakedModel {

    private final BakedModel unlockedModel;
    private final BakedModel lockedModel;
    private final ItemOverrides overrides;

    public LockedBakedModel(BakedModel unlockedModel, BakedModel lockedModel, ItemOverrides overrides) {
        this.unlockedModel = unlockedModel;
        this.lockedModel = lockedModel;
        this.overrides = overrides;
    }

    private BakedModel choose(IBlockModelData extraData) {
        Boolean locked = extraData.getData(StorageModelProperties.IS_LOCKED);
        return Boolean.TRUE.equals(locked) ? this.lockedModel : this.unlockedModel;
    }

    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull IBlockModelData extraData, @Nullable RenderType renderType) {
        BakedModel model = choose(extraData);
        // Each layer the child is drawn in is asked separately, so a child only answers for its own.
        if (renderType != null && state != null && !ClientServices.MODELS.getRenderTypesFor(model, state, rand, extraData).contains(renderType)) {
            return List.of();
        }
        return model.getQuads(state, side, rand);
    }

    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull IBlockModelData extraData) {
        return choose(extraData).getParticleIcon();
    }

    @Override
    public @NotNull Collection<RenderType> getSupportedRenderTypes(BlockState state, RandomSource rand, IBlockModelData data) {
        return ClientServices.MODELS.getRenderTypesFor(choose(data), state, rand, data);
    }

    @Override
    public @NotNull List<RenderType> getSupportedRenderTypes(ItemStack stack, boolean fabulous) {
        return ClientServices.MODELS.getRenderTypesFor(this.unlockedModel, stack, fabulous);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.unlockedModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.unlockedModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.unlockedModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.unlockedModel.getParticleIcon();
    }

    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for applyTransform, which common cannot see
    @Override
    public ItemTransforms getTransforms() {
        return this.unlockedModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }
}
