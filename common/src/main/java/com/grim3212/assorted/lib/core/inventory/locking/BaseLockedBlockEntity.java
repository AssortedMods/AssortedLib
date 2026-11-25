package com.grim3212.assorted.lib.core.inventory.locking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.lockCode = StorageUtil.readLock(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        StorageUtil.writeLock(tag, this.lockCode);
    }

    /**
     * Drops the lock as the block is removed. Block entities have no removal hook here, so the
     * block calls this from its {@code onRemove}, before {@code super.onRemove} removes the block
     * entity, and only when the block itself changes. The two halves of a door share one lock, so
     * only the upper half drops it.
     */
    public void onBlockRemoved(Level level, BlockPos pos, BlockState state) {
        boolean lowerHalf = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        if (this.isLocked() && !lowerHalf) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), LockItems.createLock(this.lockCode));
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
