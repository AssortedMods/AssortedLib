package com.grim3212.assorted.lib.mixin.client.multiplayer;

import com.grim3212.assorted.lib.core.block.IBlockOnPlayerBreak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The client half of {@link IBlockOnPlayerBreak}: NeoForge raises its break event on the server only, so the
 * client's own prediction of the break is vetoed here, before {@code playerWillDestroy} spawns the particles.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void assortedlib$canPlayerBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.minecraft.level == null || this.minecraft.player == null) {
            return;
        }

        BlockState state = this.minecraft.level.getBlockState(pos);
        if (state.getBlock() instanceof IBlockOnPlayerBreak onPlayerBreak && !onPlayerBreak.canPlayerBreak(state, this.minecraft.level, pos, this.minecraft.player)) {
            cir.setReturnValue(false);
        }
    }
}
