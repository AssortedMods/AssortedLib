package com.grim3212.assorted.lib.core.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Serialises a storage handler into a block entity's tag. The registry lookup is passed beside the
 * tag because item stacks need it to encode their components, the same pair vanilla hands to
 * {@code BlockEntity#saveAdditional} / {@code loadAdditional}.
 */
public interface IValueSerializable {

    void serialize(CompoundTag tag, HolderLookup.Provider registries);

    void deserialize(CompoundTag tag, HolderLookup.Provider registries);
}
