package com.grim3212.assorted.lib.mixin.client.render.block;

import com.grim3212.assorted.lib.core.block.IBlockLightEmission;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Gives a block whose light emission is position dependent ambient occlusion where it emits
 * nothing. {@code tesselateBlock} picks AO on {@code blockState.getLightEmission() == 0}, so
 * redirecting that lookup is enough and keeps the rest of vanilla's decision.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererWorldlyBlockMixin {

    @Redirect(
            method = "tesselateBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"
            )
    )
    private int assortedlib_handleWorldlyBlocksWhichDoNotEmitDefaultLightForAO(final BlockState instance, final BlockAndTintGetter level, final BakedModel model, final BlockState blockState, final BlockPos pos, final PoseStack poseStack, final VertexConsumer consumer, final boolean checkSides, final RandomSource random, final long seed, final int packedOverlay) {
        if (instance.getBlock() instanceof IBlockLightEmission extraProperties) {
            return extraProperties.getLightEmission(instance, level, pos);
        }

        return instance.getLightEmission();
    }
}
