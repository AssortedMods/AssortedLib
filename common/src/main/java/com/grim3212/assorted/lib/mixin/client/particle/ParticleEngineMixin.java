package com.grim3212.assorted.lib.mixin.client.particle;

import com.grim3212.assorted.lib.core.block.effects.IBlockEffectSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets an {@link IBlockEffectSupplier} block draw its own hit particles. Both loaders reach
 * {@link ParticleEngine#crack} while a block is being mined; the hit sound is played by the caller.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Shadow
    protected ClientLevel level;

    @Inject(method = "crack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V", at = @At("HEAD"), cancellable = true)
    private void assortedlib_customHitEffects(BlockPos pos, Direction dir, CallbackInfo ci) {
        BlockState blockState = this.level.getBlockState(pos);
        if (blockState.getBlock() instanceof IBlockEffectSupplier extraProperties) {
            if (extraProperties.getClientEffects().get().addHitEffects(blockState, this.level, pos, dir, (ParticleEngine) (Object) this)) {
                ci.cancel();
            }
        }
    }
}
