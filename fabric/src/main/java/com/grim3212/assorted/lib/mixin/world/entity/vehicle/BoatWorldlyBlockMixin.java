package com.grim3212.assorted.lib.mixin.world.entity.vehicle;

import com.grim3212.assorted.lib.core.block.IBlockExtraProperties;
import com.grim3212.assorted.lib.mixin.entity.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Feeds the position dependent {@link IBlockExtraProperties} friction into
 * {@code getGroundFriction} on {@link Boat}, which covers boats, chest boats and rafts.
 */
@Mixin(value = Boat.class, priority = Integer.MIN_VALUE)
public abstract class BoatWorldlyBlockMixin extends Entity {

    @Unique
    private BlockState workingState;
    @Unique
    private BlockPos workingPos;

    public BoatWorldlyBlockMixin(final EntityType<?> entityType, final Level level) {
        super(entityType, level);
    }

    @ModifyArg(
            method = "getGroundFriction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            index = 0
    )
    private BlockPos assortedlib_injectGetFrictionAdaptorForPosition(final BlockPos pos) {
        this.workingPos = pos;
        return pos;
    }

    @ModifyVariable(
            method = "getGroundFriction",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            ordinal = 0
    )
    private BlockState assortedlib_injectGetFrictionAdaptorForState(final BlockState current) {
        this.workingState = current;
        return current;
    }

    /** The friction is summed straight off the call, so the call itself is what gets replaced. */
    @Redirect(
            method = "getGroundFriction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;getFriction()F"
            )
    )
    private float assortedlib_injectGetFrictionAdaptorForBlock(final Block block) {
        if ((Object) this instanceof EntityAccessor entityAccessor && block instanceof IBlockExtraProperties extraProperties) {
            return extraProperties.getFriction(this.workingState, entityAccessor.getLevel(), this.workingPos, this);
        }

        return block.getFriction();
    }
}
