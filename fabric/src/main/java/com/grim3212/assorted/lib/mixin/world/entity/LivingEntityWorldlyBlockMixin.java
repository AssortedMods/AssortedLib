package com.grim3212.assorted.lib.mixin.world.entity;

import com.grim3212.assorted.lib.core.block.IBlockExtraProperties;
import com.grim3212.assorted.lib.core.block.IBlockSoundType;
import com.grim3212.assorted.lib.core.block.effects.IBlockLandingEffects;
import com.grim3212.assorted.lib.mixin.entity.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("InvalidInjectorMethodSignature")
@Mixin(LivingEntity.class)
public abstract class LivingEntityWorldlyBlockMixin extends Entity {
    public LivingEntityWorldlyBlockMixin(final EntityType<?> entityType, final Level level) {
        super(entityType, level);
    }

    // The ground friction of a walk on land; travel reads Block#getFriction() once, for the block
    // below that affects movement.
    @Redirect(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F")
    )
    private float assortedlib_rewriteFrictionValueForWorldlyBlocks(Block block) {
        if (!(this instanceof EntityAccessor entityAccessor))
            return block.getFriction();

        final BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
        final BlockState blockState = entityAccessor.getLevel().getBlockState(pos);
        if (blockState.getBlock() instanceof IBlockExtraProperties extraProperties) {
            return extraProperties.getFriction(blockState, entityAccessor.getLevel(), pos, this);
        }

        return block.getFriction();
    }


    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
            method = "playBlockFallSound",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            ),
            ordinal = 0
    )
    private SoundType assortedlib_injectGetBlockStateSoundType(final SoundType current) {
        if (!(this instanceof EntityAccessor entityAccessor))
            return current;

        int i = Mth.floor(this.getX());
        int j = Mth.floor(this.getY() - (double) 0.2F);
        int k = Mth.floor(this.getZ());
        final BlockPos pos = new BlockPos(i, j, k);
        BlockState blockState = entityAccessor.getLevel().getBlockState(pos);

        if (blockState.getBlock() instanceof IBlockSoundType extraProperties) {
            return extraProperties.getSoundType(blockState, entityAccessor.getLevel(), pos, this);
        }
        return current;
    }

    /**
     * Lets an {@link IBlockLandingEffects} block draw its own landing particles in place of vanilla's
     * burst; the rest of the fall is handled as vanilla handles it.
     */
    @Redirect(
            method = "checkFallDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
            )
    )
    private <T extends ParticleOptions> int assortedlib_checkFallEffects(final ServerLevel level, final T particle, final double x, final double y, final double z, final int count,
                                                                         final double xOffset, final double yOffset, final double zOffset, final double speed,
                                                                         final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
        if (onState.getBlock() instanceof IBlockLandingEffects extraProps && extraProps.addLandingEffects(onState, level, pos, onState, (LivingEntity) (Object) this, count)) {
            return 0;
        }

        return level.sendParticles(particle, x, y, z, count, xOffset, yOffset, zOffset, speed);
    }
}
