package com.grim3212.assorted.lib.core.inventory.locking;

import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The lock and key items as a container sees them. A container never names the mod that adds them:
 * it tests the tags, and drops the lock item that mod registered.
 */
public class LockItems {

    private static Supplier<? extends Item> lockItem = null;

    /** Sets the item a lock taken off a block turns back into; the mod that adds locks calls this. */
    public static void registerLockItem(Supplier<? extends Item> item) {
        lockItem = item;
    }

    /** A lock set to {@code code}, or empty when no mod adds locks. */
    public static ItemStack createLock(String code) {
        if (lockItem == null) {
            return ItemStack.EMPTY;
        }

        return StorageUtil.setCodeOnStack(code, new ItemStack(lockItem.get()));
    }

    public static boolean isLock(ItemStack stack) {
        return stack.is(LibCommonTags.Items.LOCKS);
    }

    /** A lock with a code on it, the only kind a block can take. */
    public static boolean isCodedLock(ItemStack stack) {
        return isLock(stack) && StorageUtil.hasCode(stack);
    }

    public static boolean isKey(ItemStack stack) {
        return stack.is(LibCommonTags.Items.KEYS);
    }
}
