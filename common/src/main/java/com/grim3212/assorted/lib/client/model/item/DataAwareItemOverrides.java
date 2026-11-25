package com.grim3212.assorted.lib.client.model.item;

import com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel;
import com.grim3212.assorted.lib.client.model.baked.IDelegatingBakedModel;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.platform.ClientServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The item form of a block whose look comes from model data (a colorizer's stored block, a bridge's
 * projected one), drawn from what the stack carries rather than the block's empty state. A data
 * aware model hands one of these out as its {@link BakedModel#getOverrides() overrides}; vanilla
 * resolves every stack through it, on both loaders, and gets back the same model bound to the data
 * the stack stands for.
 */
public class DataAwareItemOverrides extends ItemOverrides {

    private final IDataAwareBakedModel model;
    private final Function<ItemStack, IBlockModelData> modelData;
    private final Function<ItemStack, ?> identity;
    private final Map<Object, BakedModel> resolved = new ConcurrentHashMap<>();

    /**
     * @param model     The block's own model, which this resolves the stack against.
     * @param modelData The model data a stack stands for, as its block entity would supply it.
     * @param identity  What a stack's resolved model is cached under, so stacks that draw
     *                  differently must answer differently. Never null.
     */
    // The public constructor is the only one vanilla has; NeoForge deprecates it in favour of an
    // overload taking a sprite getter that vanilla, which this module builds against, does not have.
    // With no overrides to bake, neither reads the model or the baker beyond that.
    @SuppressWarnings("deprecation")
    public DataAwareItemOverrides(ModelBaker baker, IDataAwareBakedModel model, Function<ItemStack, IBlockModelData> modelData, Function<ItemStack, ?> identity) {
        super(baker, null, List.of());
        this.model = unwrap(model);
        this.modelData = modelData;
        this.identity = identity;
    }

    /**
     * A data aware model may come wrapped in a loader delegate. The stack is resolved against the
     * model underneath, and the result is wrapped again by the loader.
     */
    private static IDataAwareBakedModel unwrap(IDataAwareBakedModel model) {
        BakedModel inner = model;
        while (inner instanceof IDelegatingBakedModel delegating) {
            inner = delegating.getDelegate();
        }
        return inner instanceof IDataAwareBakedModel dataAware ? dataAware : model;
    }

    @Override
    public @Nullable BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return this.resolved.computeIfAbsent(this.identity.apply(stack),
                key -> ClientServices.MODELS.adaptToPlatform(new StackModel(this.model, this.modelData.apply(stack))));
    }

    /**
     * The block model with the data of one stack fixed, as the item renderer only ever calls the
     * data-less {@code getQuads}.
     */
    private record StackModel(IDataAwareBakedModel model, IBlockModelData data) implements IDataAwareBakedModel {

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull IBlockModelData extraData, @Nullable RenderType renderType) {
            return this.model.getQuads(state, side, rand, this.data, renderType);
        }

        @Override
        public TextureAtlasSprite getParticleIcon(@NotNull IBlockModelData extraData) {
            return this.model.getParticleIcon(this.data);
        }

        @Override
        public @NotNull Collection<RenderType> getSupportedRenderTypes(BlockState state, RandomSource rand, IBlockModelData data) {
            return this.model.getSupportedRenderTypes(state, rand, this.data);
        }

        @Override
        public @NotNull List<RenderType> getSupportedRenderTypes(ItemStack stack, boolean fabulous) {
            return this.model.getSupportedRenderTypes(stack, fabulous);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.model.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.model.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return this.model.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return this.model.isCustomRenderer();
        }

        @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for its own ModelData overload, which common cannot see
        @Override
        public TextureAtlasSprite getParticleIcon() {
            return this.model.getParticleIcon(this.data);
        }

        @SuppressWarnings("deprecation") // NeoForge deprecates this vanilla method for applyTransform, which common cannot see
        @Override
        public ItemTransforms getTransforms() {
            return this.model.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
