package com.grim3212.assorted.lib.mixin.client.multiplayer;

import com.grim3212.assorted.lib.core.block.IBlockOnPlayerBreak;
import com.grim3212.assorted.lib.events.EntityInteractEvent;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeWorldlyBlockMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "interact",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    // interact gained an EntityHitResult in 26.2. An @Inject handler must mirror the target's
    // full parameter list, so it is taken here even though the event does not use it.
    private void assortedlib_entityInteract(Player player, Entity entity, EntityHitResult hitResult, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        final EntityInteractEvent event = new EntityInteractEvent(player, interactionHand, entity);
        Services.EVENTS.handleEvents(event);

        if (event.getInteractionResult() == InteractionResult.SUCCESS) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    /**
     * The client half of {@link IBlockOnPlayerBreak}: refusing here is what keeps the client from
     * playing a break the server is going to refuse anyway.
     */
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void assortedlib_canPlayerBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        final Level level = this.minecraft.level;
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof IBlockOnPlayerBreak onPlayerBreak && !onPlayerBreak.canPlayerBreak(state, level, pos, this.minecraft.player)) {
            cir.setReturnValue(false);
        }
    }
}
