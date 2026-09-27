package com.grim3212.assorted.lib.core.inventory.locking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A block entity that holds nothing but a lock, for a block such as a door that has no inventory. */
public class BaseLockedBlockEntity extends BlockEntity implements ILockable {

    private String lockCode = "";

    public BaseLockedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isLocked() {
        return this.lockCode != null && !this.lockCode.isEmpty();
    }

    @Override
    public String getLockCode() {
        return this.lockCode;
    }

    @Override
    public void setLockCode(String s) {
        if (s == null || s.isEmpty())
            this.lockCode = "";
        else
            this.lockCode = s;

        this.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.lockCode = StorageUtil.readLock(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        StorageUtil.writeLock(output, this.lockCode);
    }

    /**
     * The two halves of a door share one lock, so only the upper half drops it. This used to live in
     * the block's {@code onRemove}, which no longer sees the block entity.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);

        boolean lowerHalf = state.getValueOrElse(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER) == DoubleBlockHalf.LOWER;
        if (this.level != null && this.isLocked() && !lowerHalf) {
            Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), LockItems.createLock(this.lockCode));
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

}
