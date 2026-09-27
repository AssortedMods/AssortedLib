package com.grim3212.assorted.lib.core.inventory.locking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Whether a player holds the key to a lock. A key is anything in {@code c:keys} with the lock's code,
 * or a key in {@code c:keys} holding such a key, the way a key ring does.
 */
public class StorageAccessUtil {

    private static final List<IKeySource> KEY_SOURCES = new CopyOnWriteArrayList<>();

    /** Adds somewhere else a player's key can be, checked after their inventory. */
    public static void registerKeySource(IKeySource source) {
        KEY_SOURCES.add(source);
    }

    public static boolean canAccess(BlockGetter worldIn, BlockPos pos, Player entityplayer) {
        ILockable lockeable = (ILockable) worldIn.getBlockEntity(pos);

        if (lockeable.isLocked()) {
            return hasKey(entityplayer, lockeable.getLockCode());
        }

        return true;
    }

    public static boolean canAccess(ItemStack stack, Player entityplayer) {
        String currentLock = StorageUtil.getCode(stack);
        if (!currentLock.isEmpty()) {
            return hasKey(entityplayer, currentLock);
        }

        return true;
    }

    private static boolean hasKey(Player entityplayer, String lockCode) {
        for (int slot = 0; slot < entityplayer.getInventory().getContainerSize(); slot++) {
            if (canStackAccess(entityplayer.getInventory().getItem(slot), lockCode)) {
                return true;
            }
        }

        return hasKeyElsewhere(entityplayer, lockCode);
    }

    private static boolean hasKeyElsewhere(@Nullable LivingEntity entity, String lockCode) {
        if (lockCode == null || lockCode.isEmpty()) {
            return true;
        }

        for (IKeySource source : KEY_SOURCES) {
            if (source.hasKey(entity, lockCode)) {
                return true;
            }
        }

        return false;
    }

    public static boolean canStackAccess(ItemStack stack, String lockCode) {
        if (lockCode == null || lockCode.isEmpty()) {
            return true;
        }

        if (stack.isEmpty() || !LockItems.isKey(stack)) {
            return false;
        }

        if (StorageUtil.hasCodeWithMatch(stack, lockCode)) {
            return true;
        }

        return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream()
                .anyMatch(heldKey -> LockItems.isKey(heldKey) && StorageUtil.hasCodeWithMatch(heldKey, lockCode));
    }
}
