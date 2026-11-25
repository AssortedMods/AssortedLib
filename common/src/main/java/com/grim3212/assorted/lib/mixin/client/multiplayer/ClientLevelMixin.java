package com.grim3212.assorted.lib.mixin.client.multiplayer;

import com.grim3212.assorted.lib.core.block.effects.IBlockEffectSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets an {@link IBlockEffectSupplier} block draw its own break particles, through
 * {@link ClientLevel#addDestroyBlockEffect}. Hit particles are {@link com.grim3212.assorted.lib.mixin.client.particle.ParticleEngineMixin}.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "addDestroyBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private void assortedlib_customDestroyEffects(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (blockState.getBlock() instanceof IBlockEffectSupplier extraProperties) {
            if (extraProperties.getClientEffects().get().addDestroyEffects(blockState, (ClientLevel) (Object) this, blockPos, Minecraft.getInstance().particleEngine)) {
                ci.cancel();
            }
        }
    }
}
