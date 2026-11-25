package com.grim3212.assorted.lib.mixin.block;

import com.grim3212.assorted.lib.core.block.IBlockEntityWithModelData;
import com.grim3212.assorted.lib.core.block.IBlockLightEmission;
import com.grim3212.assorted.lib.platform.ClientServices;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brings a client up to date whenever it loads new data into a block entity (a sync packet and an
 * update tag both end in {@code loadWithComponents}): an {@link IBlockEntityWithModelData} is
 * re-rendered, and an {@link IBlockLightEmission} block is relit. Neither loader does either - model
 * data is only read at the section's next rebuild, and light is only recomputed where a block state
 * changed - so a block whose look or light lives in its block entity changed only for the player who
 * changed it.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {

    @Inject(method = "loadWithComponents", at = @At("TAIL"))
    private void assortedlib_refreshOnLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }

        if (self instanceof IBlockEntityWithModelData) {
            // The refresh re-reads the model data; the block update re-renders the section with it.
            ClientServices.MODELS.requestModelDataRefresh(self);
            BlockState state = self.getBlockState();
            level.sendBlockUpdated(self.getBlockPos(), state, state, 0);
        }

        if (self.getBlockState().getBlock() instanceof IBlockLightEmission) {
            // A client relights on its own only where a block state changed, and the server sends
            // whole-chunk light only to the players a chunk is on the tracked border for. A block
            // whose emission comes from its block entity changes neither, so a colorizer filled with
            // glowstone stayed dark for everyone but the player who filled it.
            level.getLightEngine().checkBlock(self.getBlockPos());
        }
    }
}
