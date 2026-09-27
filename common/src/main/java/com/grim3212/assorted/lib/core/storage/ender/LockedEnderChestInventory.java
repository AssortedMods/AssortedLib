package com.grim3212.assorted.lib.core.storage.ender;

import com.google.common.collect.Lists;
import com.grim3212.assorted.lib.core.inventory.impl.LockedItemStackStorageHandler;
import com.grim3212.assorted.lib.core.inventory.locking.ILockable;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

/** One lock code's shared ender inventory, which opens and dirties every block entity showing it. */
public class LockedEnderChestInventory extends LockedItemStackStorageHandler {

    private final IEnderData enderData;
    private final List<Reference<? extends BaseStorageBlockEntity>> lockedEnderChests = Lists.newArrayList();

    public LockedEnderChestInventory(IEnderData enderData, String lockCode, int numSlots) {
        super(ILockable.CONSTANT(lockCode), numSlots);
        this.enderData = enderData;
    }

    public void addWeakListener(BaseStorageBlockEntity e) {
        lockedEnderChests.add(new WeakReference<>(e));
    }

    public void removeWeakListener(BaseStorageBlockEntity e) {
        for (Iterator<Reference<? extends BaseStorageBlockEntity>> itr = lockedEnderChests.iterator(); itr.hasNext(); ) {
            BaseStorageBlockEntity tileentity = itr.next().get();
            if (tileentity == null || tileentity.isRemoved() || tileentity == e) {
                itr.remove();
            }
        }
    }

    @Override
    public void onContentsChanged(int slot) {
        // HA!
        List<BaseStorageBlockEntity> dirtyChests = Lists.newArrayList();
        for (Iterator<Reference<? extends BaseStorageBlockEntity>> itr = lockedEnderChests.iterator(); itr.hasNext(); ) {
            BaseStorageBlockEntity tileentity = itr.next().get();
            if (tileentity == null || tileentity.isRemoved()) {
                itr.remove();
            } else {
                dirtyChests.add(tileentity);
            }
        }

        dirtyChests.forEach(BlockEntity::setChanged);

        enderData.markDirty();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void startOpen(Player player) {
        if (!player.isSpectator()) {
            lockedEnderChests.forEach(x -> {
                BaseStorageBlockEntity linked = x.get();
                if (linked.numPlayersUsing < 0) {
                    linked.numPlayersUsing = 0;
                }

                ++linked.numPlayersUsing;
                linked.onOpenOrClose();
            });
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!player.isSpectator()) {
            lockedEnderChests.forEach(x -> {
                BaseStorageBlockEntity linked = x.get();
                --linked.numPlayersUsing;
                linked.onOpenOrClose();
            });
        }
    }
}
