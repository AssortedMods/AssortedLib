package com.grim3212.assorted.lib.mixin.client.render;

import com.grim3212.assorted.lib.core.block.IBlockLightEmission;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Feeds the position dependent {@link IBlockLightEmission} value into the packed light coordinates
 * a block is drawn with. {@link LevelRenderer#getLightColor(BlockAndTintGetter, BlockState, BlockPos)}
 * is where every call site packs block light.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererLightColorMixin {

    @Redirect(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"
            )
    )
    private static int assortedlib_injectGetBlockStateLightEmission(final BlockState instance, final BlockAndTintGetter level, final BlockState state, final BlockPos pos) {
        if (instance.getBlock() instanceof IBlockLightEmission extraProperties) {
            return extraProperties.getLightEmission(instance, level, pos);
        }

        return instance.getLightEmission();
    }
}
