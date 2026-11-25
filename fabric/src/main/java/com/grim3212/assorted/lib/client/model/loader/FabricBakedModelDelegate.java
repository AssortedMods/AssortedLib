package com.grim3212.assorted.lib.client.model.loader;

import com.grim3212.assorted.lib.client.model.baked.IDataAwareBakedModel;
import com.grim3212.assorted.lib.client.model.baked.IDelegatingBakedModel;
import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.core.block.IBlockEntityWithModelData;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Wraps an {@link IDataAwareBakedModel} so Fabric's renderer draws it with the model data of the
 * position being drawn. The data is the block entity's render data, which Fabric snapshots on the
 * main thread as it builds a chunk section ({@code BlockEntityRenderDataMixin} answers it from
 * {@link IBlockEntityWithModelData}); the block entity itself is the fallback for a view that has no
 * snapshot. Each quad is emitted with a material for the chunk layer the model asked for.
 */
public class FabricBakedModelDelegate implements BakedModel, IDelegatingBakedModel {

    /** Materials are interned by the renderer, and chunk building runs on several threads. */
    private static final Map<RenderType, RenderMaterial> MATERIALS = new ConcurrentHashMap<>();

    private final BakedModel delegate;

    public FabricBakedModelDelegate(final BakedModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction direction, final @NotNull RandomSource random) {
        return delegate.getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return delegate.getOverrides();
    }

    @Override
    public BakedModel getDelegate() {
        return delegate;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(final BlockAndTintGetter blockView, final BlockState state, final BlockPos pos, final Supplier<RandomSource> randomSupplier, final RenderContext context) {
        if (!(delegate instanceof final IDataAwareBakedModel dataAware)) {
            delegate.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
        }

        final IBlockModelData data = modelData(blockView, pos);
        final QuadEmitter emitter = context.getEmitter();
        for (final RenderType renderType : dataAware.getSupportedRenderTypes(state, randomSupplier.get(), data)) {
            final RenderMaterial material = material(renderType);
            for (final Direction direction : Direction.values()) {
                if (context.isFaceCulled(direction)) {
                    continue;
                }
                emit(emitter, dataAware.getQuads(state, direction, randomSupplier.get(), data, renderType), material, direction);
            }
            emit(emitter, dataAware.getQuads(state, null, randomSupplier.get(), data, renderType), material, null);
        }
    }

    /**
     * An item draws through the model its {@link ItemOverrides} resolve the stack to, which for a data
     * aware model already carries the stack's data, so this only has to draw what it is handed.
     */
    @Override
    public void emitItemQuads(final ItemStack stack, final Supplier<RandomSource> randomSupplier, final RenderContext context) {
        if (!(delegate instanceof final IDataAwareBakedModel dataAware)) {
            delegate.emitItemQuads(stack, randomSupplier, context);
            return;
        }

        final QuadEmitter emitter = context.getEmitter();
        final IBlockModelData data = IBlockModelData.empty();
        for (final RenderType renderType : dataAware.getSupportedRenderTypes(stack, false)) {
            final RenderMaterial material = material(renderType);
            for (final Direction direction : Direction.values()) {
                emit(emitter, dataAware.getQuads(null, direction, randomSupplier.get(), data, renderType), material, direction);
            }
            emit(emitter, dataAware.getQuads(null, null, randomSupplier.get(), data, renderType), material, null);
        }
    }

    private static void emit(final QuadEmitter emitter, final List<BakedQuad> quads, final RenderMaterial material, @Nullable final Direction cullFace) {
        for (final BakedQuad quad : quads) {
            emitter.fromVanilla(quad, material, cullFace);
            emitter.emit();
        }
    }

    private static RenderMaterial material(final RenderType renderType) {
        return MATERIALS.computeIfAbsent(renderType, type -> {
            final Renderer renderer = Objects.requireNonNull(RendererAccess.INSTANCE.getRenderer(), "No Fabric renderer is present");
            return renderer.materialFinder().blendMode(BlendMode.fromRenderLayer(type)).find();
        });
    }

    private static IBlockModelData modelData(final BlockAndTintGetter blockView, final BlockPos pos) {
        if (blockView.getBlockEntityRenderData(pos) instanceof IBlockModelData data) {
            return data;
        }

        final BlockEntity blockEntity = blockView.getBlockEntity(pos);
        if (blockEntity instanceof IBlockEntityWithModelData withData) {
            return withData.getBlockModelData();
        }

        return IBlockModelData.empty();
    }
}
