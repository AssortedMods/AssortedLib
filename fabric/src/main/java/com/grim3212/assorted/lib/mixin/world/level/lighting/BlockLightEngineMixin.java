package com.grim3212.assorted.lib.mixin.world.level.lighting;

import com.grim3212.assorted.lib.core.block.IBlockLightEmission;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> extends LightEngine<M, S> {

    @Unique
    private static ThreadLocal<BlockLightEngine> INSTANCE = new ThreadLocal<>();

    protected BlockLightEngineMixin(LightChunkGetter lightChunkGetter, S layerLightSectionStorage) {
        super(lightChunkGetter, layerLightSectionStorage);
    }

    @Inject(method = "getEmission", at = @At("HEAD"), cancellable = true)
    public void assortedlib_onGetEmission(long packedPos, BlockState blockState, CallbackInfoReturnable<Integer> cir) {
        if (!(this instanceof LightEngineAccessor<?, ?> lightEngineAccessor)) {
            return;
        }

        if (!(lightEngineAccessor.getStorage() instanceof LayerLightSectionStorageAccessor<?> layerLightSectionStorageAccessor)) {
            return;
        }

        if (!(blockState.getBlock() instanceof IBlockLightEmission blockWithWorldlyProperties)) {
            return;
        }

        final int lightEmission = blockWithWorldlyProperties.getLightEmission(blockState, lightEngineAccessor.getChunkSource().getLevel(), BlockPos.of(packedPos));
        cir.setReturnValue(lightEmission > 0 && layerLightSectionStorageAccessor.callLightOnInSection(SectionPos.blockToSection(packedPos)) ? lightEmission : 0);
    }

    @Inject(method = "propagateLightSources", at = @At("HEAD"))
    private void assortedlib_onPropagateLightSourcesCall(ChunkPos chunkPos, CallbackInfo ci) {
        INSTANCE.set((BlockLightEngine) (Object) this);
    }

    @Inject(method = "propagateLightSources", at = @At("RETURN"))
    private void assortedlib_onPropagateLightSourcesEnd(ChunkPos chunkPos, CallbackInfo ci) {
        INSTANCE.remove();
    }

    /**
     * The block light source callback {@link BlockLightEngine#propagateLightSources(ChunkPos)}
     * hands to {@code LightChunk#findBlockLightSources}. Mojang's mappings do not name lambdas, so it
     * keeps its intermediary name in development and at runtime alike, hence {@code remap = false}.
     */
    @Inject(method = "method_51532(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", remap = false, at = @At("HEAD"), cancellable = true)
    private void assortedlib_onCallPropagateLightSourcesCallback(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (!(blockState.getBlock() instanceof IBlockLightEmission blockWithWorldlyProperties)) {
            return;
        }

        final Object currentInstance = INSTANCE.get();
        if (currentInstance == null) {
            return;
        }

        //noinspection ConstantValue
        if (!(currentInstance instanceof LightEngineAccessor<?, ?> lightEngineAccessor)) {
            return;
        }

        int lightEmission = blockWithWorldlyProperties.getLightEmission(blockState, lightEngineAccessor.getChunkSource().getLevel(), blockPos);
        lightEngineAccessor.callEnqueueIncrease(blockPos.asLong(), LightEngine.QueueEntry.increaseLightFromEmission(lightEmission, isEmptyShape(blockState)));

        ci.cancel();
    }
}
