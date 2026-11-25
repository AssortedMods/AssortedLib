package com.grim3212.assorted.lib.mixin.world.entity;

import com.grim3212.assorted.lib.core.block.IBlockSoundType;
import com.grim3212.assorted.lib.core.block.effects.IBlockRunningEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Entity.class)
public abstract class EntityWorldlyBlockMixin {

    @Shadow
    private Level level;

    @Shadow
    public abstract void playSound(final SoundEvent sound, final float volume, final float pitch);

    private Entity getThis() {
        return (Entity) (Object) this;
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(
            method = "playStepSound",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true
    )
    public void assortedlib_redirectGetOffsetBlockStateSoundType(BlockPos pos, BlockState state, CallbackInfo ci) {
        // Vanilla plays the state it is handed: its callers have already picked between the block
        // walked on and a step-sound block (snow, carpet) above it.
        if (state.getBlock() instanceof IBlockSoundType extraProperties) {
            SoundType soundType = extraProperties.getSoundType(state, this.level, pos, this.getThis());
            this.playSound(soundType.getStepSound(), soundType.getVolume() * 0.15F, soundType.getPitch());
            ci.cancel();
        }
    }

    @Inject(
            method = "spawnSprintParticle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    public void assortedlib_spawnSprintParticle(CallbackInfo ci, BlockPos blockPos) {
        BlockState state = this.level.getBlockState(blockPos);
        if (state.getBlock() instanceof IBlockRunningEffects extraProperties && extraProperties.addRunningEffects(state, level, blockPos, (Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
