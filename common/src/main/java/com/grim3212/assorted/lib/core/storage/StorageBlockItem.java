package com.grim3212.assorted.lib.core.storage;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * A storage block's item, whose lock and level lines come from the {@link StorageInfo} component it sets. The
 * vanilla {@code CONTAINER} component shows the contents.
 */
public class StorageBlockItem extends BlockItem {

    public StorageBlockItem(Block block, Properties props, Supplier<DataComponentType<StorageInfo>> storageInfo) {
        super(block, props.component(storageInfo.get(), new StorageInfo(StorageInfo.LockLine.CODE, levelOf(block))));
    }

    private static int levelOf(Block block) {
        if (block instanceof IStorageMaterial storageBlock) {
            StorageMaterial material = storageBlock.getStorageMaterial();
            return material == null ? 0 : material.getStorageLevel();
        }
        return -1;
    }
}
