package com.grim3212.assorted.lib.client.model.baked.simple;

import com.grim3212.assorted.lib.client.model.RenderTypeGroup;
import com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.platform.ClientServices;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A plain model built out of loose quads, with optional fixed render types. It is written out
 * rather than extending vanilla's {@code SimpleBakedModel}, whose constructor NeoForge deprecates in
 * favour of one taking its own render type group that vanilla does not have.
 */
public class WrappedSimpleBakedModel implements IDataAwareBakedModel {

    protected final List<BakedQuad> unculledFaces;
    protected final Map<Direction, List<BakedQuad>> culledFaces;
    protected final boolean hasAmbientOcclusion;
    protected final boolean usesBlockLight;
    protected final boolean isGui3d;
    protected final TextureAtlasSprite particleIcon;
    protected final ItemTransforms transforms;
    protected final ItemOverrides overrides;
    @Nullable
    protected final List<RenderType> blockRenderTypes;
    @Nullable
    protected final List<RenderType> itemRenderTypes;
    @Nullable
    protected final List<RenderType> fabulousItemRenderTypes;

    public WrappedSimpleBakedModel(List<BakedQuad> unculledFaces, Map<Direction, List<BakedQuad>> culledFaces, boolean hasAmbientOcclusion, boolean usesBlockLight,
                                   boolean isGui3d, TextureAtlasSprite particleIcon, ItemTransforms transforms, ItemOverrides overrides, RenderTypeGroup renderTypeGroup) {
        this.unculledFaces = unculledFaces;
        this.culledFaces = culledFaces;
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.usesBlockLight = usesBlockLight;
        this.isGui3d = isGui3d;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;
        this.blockRenderTypes = !renderTypeGroup.isEmpty() ? List.of(renderTypeGroup.block()) : null;
        this.itemRenderTypes = !renderTypeGroup.isEmpty() ? List.of(renderTypeGroup.entity()) : null;
        this.fabulousItemRenderTypes = !renderTypeGroup.isEmpty() ? List.of(renderTypeGroup.entityFabulous()) : null;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull IBlockModelData extraData, @Nullable RenderType renderType) {
        return side == null ? this.unculledFaces : this.culledFaces.get(side);
    }

    @Override
    public @NotNull Collection<RenderType> getSupportedRenderTypes(BlockState state, RandomSource rand, IBlockModelData data) {
        if (this.blockRenderTypes != null)
            return this.blockRenderTypes;
        return ClientServices.MODELS.getRenderTypesFor(this, state, rand, data);
    }

    @Override
    public @NotNull List<RenderType> getSupportedRenderTypes(ItemStack stack, boolean fabulous) {
        List<RenderType> fixed = fabulous ? this.fabulousItemRenderTypes : this.itemRenderTypes;
        if (fixed != null)
            return fixed;
        return ClientServices.MODELS.getRenderTypesFor(this, stack, fabulous);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return this.usesBlockLight;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.particleIcon;
    }

    @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for applyTransform; it is still abstract on vanilla's BakedModel
    @Override
    public ItemTransforms getTransforms() {
        return this.transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    public static class Builder {

        private final List<BakedQuad> unculledFaces = new ArrayList<>();
        private final Map<Direction, List<BakedQuad>> culledFaces = new EnumMap<>(Direction.class);
        private final ItemOverrides overrides;
        private final boolean hasAmbientOcclusion;
        private final boolean usesBlockLight;
        private final boolean isGui3d;
        private final ItemTransforms transforms;
        private TextureAtlasSprite particleIcon;

        public Builder(boolean hasAmbientOcclusion, boolean usesBlockLight, boolean isGui3d, ItemTransforms transforms, ItemOverrides overrides) {
            for (Direction direction : Direction.values()) {
                this.culledFaces.put(direction, new ArrayList<>());
            }

            this.overrides = overrides;
            this.hasAmbientOcclusion = hasAmbientOcclusion;
            this.usesBlockLight = usesBlockLight;
            this.isGui3d = isGui3d;
            this.transforms = transforms;
        }

        public WrappedSimpleBakedModel.Builder addCulledFace(Direction direction, BakedQuad quad) {
            this.culledFaces.get(direction).add(quad);
            return this;
        }

        public WrappedSimpleBakedModel.Builder addUnculledFace(BakedQuad quad) {
            this.unculledFaces.add(quad);
            return this;
        }

        public WrappedSimpleBakedModel.Builder particle(TextureAtlasSprite particleIcon) {
            this.particleIcon = particleIcon;
            return this;
        }

        public WrappedSimpleBakedModel build() {
            return build(RenderTypeGroup.EMPTY);
        }

        public WrappedSimpleBakedModel build(RenderTypeGroup renderTypes) {
            if (this.particleIcon == null) {
                throw new RuntimeException("Missing particle!");
            }

            return new WrappedSimpleBakedModel(this.unculledFaces, this.culledFaces, this.hasAmbientOcclusion, this.usesBlockLight, this.isGui3d, this.particleIcon,
                    this.transforms, this.overrides, renderTypes);
        }
    }
}
