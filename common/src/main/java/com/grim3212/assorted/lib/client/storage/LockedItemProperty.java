package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Whether a storage item's stack carries a lock, for an item model to branch on. Each mod registers its own
 * instance, as an id can only be bound to one codec.
 */
public final class LockedItemProperty implements ConditionalItemModelProperty {

    private final MapCodec<LockedItemProperty> codec = MapCodec.unit(this);

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
        return StorageUtil.hasCode(stack);
    }

    @Override
    public MapCodec<LockedItemProperty> type() {
        return this.codec;
    }
}
