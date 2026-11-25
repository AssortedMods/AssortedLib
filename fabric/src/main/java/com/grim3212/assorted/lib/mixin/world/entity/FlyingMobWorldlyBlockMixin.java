package com.grim3212.assorted.lib.mixin.world.entity;

import com.grim3212.assorted.lib.core.block.IBlockExtraProperties;
import com.grim3212.assorted.lib.mixin.entity.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Feeds the position dependent {@link IBlockExtraProperties} friction into {@link FlyingMob#travel},
 * which reads {@code Block#getFriction()} twice for the block below that affects movement - once for
 * the acceleration, once for the drag - and does not go through {@code LivingEntity#travel}.
 */
@Mixin(FlyingMob.class)
public abstract class FlyingMobWorldlyBlockMixin extends Mob {

    protected FlyingMobWorldlyBlockMixin(final EntityType<? extends Mob> entityType, final Level level) {
        super(entityType, level);
    }

    @Redirect(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F")
    )
    private float assortedlib_rewriteFrictionValueForWorldlyBlocks(final Block block) {
        if (!((Object) this instanceof EntityAccessor entityAccessor)) {
            return block.getFriction();
        }

        final BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
        final BlockState blockState = entityAccessor.getLevel().getBlockState(pos);
        if (blockState.getBlock() instanceof IBlockExtraProperties extraProperties) {
            return extraProperties.getFriction(blockState, entityAccessor.getLevel(), pos, this);
        }

        return block.getFriction();
    }
}
