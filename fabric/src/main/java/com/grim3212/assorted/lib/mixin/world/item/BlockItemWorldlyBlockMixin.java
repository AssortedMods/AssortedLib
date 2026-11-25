package com.grim3212.assorted.lib.mixin.world.item;

import com.grim3212.assorted.lib.core.block.IBlockSoundType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemWorldlyBlockMixin extends Item {

    @Unique
    private BlockState soundState;
    @Unique
    private SoundType soundType;

    public BlockItemWorldlyBlockMixin(final Properties properties) {
        super(properties);
    }

    @ModifyVariable(
            method = "place",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            // The placed state: the first BlockState local is the placement state, asked for before it.
            ordinal = 1
    )
    private BlockState assortedlib_injectGetSoundTypeAdaptorForInitialState(final BlockState current) {
        this.soundState = current;
        return current;
    }

    @ModifyVariable(
            method = "place",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/item/BlockItem;updateBlockStateFromTag(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            ordinal = 1
    )
    private BlockState assortedlib_injectGetSoundTypeAdaptorForTagUpdate(final BlockState current) {
        this.soundState = current;
        return current;
    }


    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
            method = "place",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            ),
            ordinal = 0
    )
    private SoundType assortedlib_injectGetSoundTypeAdaptor(final SoundType current, BlockPlaceContext blockPlaceContext) {
        if (soundState.getBlock() instanceof IBlockSoundType extraProperties) {
            this.soundType = extraProperties.getSoundType(soundState, blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), blockPlaceContext.getPlayer());
            return this.soundType;
        }
        this.soundType = null;
        return current;
    }

    @Inject(
            method = "getPlaceSound",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true)
    public void assortedlib_redirectGetBlockStateSoundTypePlace(final BlockState state, final CallbackInfoReturnable<SoundEvent> cir) {
        if (this.soundType != null) {
            cir.setReturnValue(this.soundType.getPlaceSound());
        }
    }
}
