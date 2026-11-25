package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Whether a storage item's stack carries a lock, 1 or 0, for an item model's {@code overrides} to
 * branch on. Register it per item through {@code IClientHelper#registerItemProperty}.
 */
public final class LockedItemProperty implements ClampedItemPropertyFunction {

    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed) {
        return StorageUtil.hasCode(stack) ? 1.0F : 0.0F;
    }
}
