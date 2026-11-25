package com.grim3212.assorted.lib.mixin.block;

import com.grim3212.assorted.lib.core.block.IPlantSustainable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets an {@link IPlantSustainable} soil block decide whether a plant may be placed on it.
 * {@code mayPlaceOn} lives on {@link BushBlock}, which every plant extends.
 */
@Mixin(BushBlock.class)
public class BushBlockMixin {
    @Inject(method = "mayPlaceOn(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    public void assortedlib_mayPlaceOn(BlockState state, BlockGetter blockGetter, BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (state.getBlock() instanceof IPlantSustainable planter) {
            callbackInfo.setReturnValue(planter.canSustainPlant(state, blockGetter, pos, Direction.UP, (BushBlock) (Object) this));
        }
    }
}
