package com.grim3212.assorted.lib.core.inventory.locking;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Somewhere a key can be besides the inventory, such as an accessory slot. */
@FunctionalInterface
public interface IKeySource {

    boolean hasKey(@Nullable LivingEntity entity, String lockCode);
}
