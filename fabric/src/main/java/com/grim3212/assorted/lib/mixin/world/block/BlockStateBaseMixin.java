package com.grim3212.assorted.lib.mixin.world.block;

import com.grim3212.assorted.lib.core.block.IBlockMapColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets an {@link IBlockMapColor} block pick its map colour from its position. {@code
 * BlockStateBase} is nested in {@link BlockBehaviour}, not {@code Block}, so it is named through
 * its real owner.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Shadow
    public abstract Block getBlock();

    @Shadow
    public abstract BlockState asState();

    @Shadow
    @Final
    private MapColor mapColor;


    @Inject(method = "getMapColor", at = @At("HEAD"), cancellable = true)
    public void assortedlib_getMapColor(BlockGetter level, BlockPos pos, CallbackInfoReturnable<MapColor> cir) {
        if (getBlock() instanceof IBlockMapColor iBlockMapColor) {
            cir.setReturnValue(iBlockMapColor.getMapColor(asState(), level, pos, mapColor));
        }
    }

}
