package com.grim3212.assorted.lib.mixin.world.entity.animal.horse;

import com.grim3212.assorted.lib.core.block.IBlockSoundType;
import com.grim3212.assorted.lib.mixin.entity.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseWorldlyBlockMixin extends Entity {

    public AbstractHorseWorldlyBlockMixin(final EntityType<?> entityType, final Level level) {
        super(entityType, level);
    }

    @ModifyVariable(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;",
                    // The first call reads the block walked on; the second, snow above it, is left alone.
                    ordinal = 0
            ),
            ordinal = 0
    )
    private SoundType assortedlib_injectGetBlockStateSoundType(final SoundType current, BlockPos pPos, BlockState pBlock) {
        if (!(this instanceof EntityAccessor entityAccessor))
            return current;

        if (pBlock.getBlock() instanceof IBlockSoundType extraProperties) {
            return extraProperties.getSoundType(pBlock, entityAccessor.getLevel(), pPos, this);
        }
        return current;
    }
}
