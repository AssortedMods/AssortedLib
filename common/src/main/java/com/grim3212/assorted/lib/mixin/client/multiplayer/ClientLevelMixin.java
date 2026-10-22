package com.grim3212.assorted.lib.mixin.client.multiplayer;

import com.grim3212.assorted.lib.core.block.effects.IBlockEffectSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets an {@link IBlockEffectSupplier} block draw its own hit and break particles, through
 * {@link ClientLevel}'s {@code addBreakingParticles} and {@code addDestroyBlockEffect}.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    // The particles alone: 26.3 plays the hit sound from the same caller, and that should still play.
    @Inject(method = "addBreakingParticles(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private void assortedlib_customHitEffects(BlockPos pos, Direction dir, BlockState blockState, CallbackInfo ci) {
        final ClientLevel level = (ClientLevel) (Object) this;
        if (blockState.getBlock() instanceof IBlockEffectSupplier extraProperties) {
            if (extraProperties.getClientEffects().get().addHitEffects(blockState, level, pos, dir, Minecraft.getInstance().particleEngine)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "addDestroyBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private void assortedlib_customDestroyEffects(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (blockState.getBlock() instanceof IBlockEffectSupplier extraProperties) {
            if (extraProperties.getClientEffects().get().addDestroyEffects(blockState, (ClientLevel) (Object) this, blockPos, Minecraft.getInstance().particleEngine)) {
                ci.cancel();
            }
        }
    }
}
