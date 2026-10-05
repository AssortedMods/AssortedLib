package com.grim3212.assorted.lib.core.storage.shulker;

import com.grim3212.assorted.lib.core.storage.StorageBlockItem;
import com.grim3212.assorted.lib.core.storage.StorageInfo;
import com.grim3212.assorted.lib.util.NBTHelper;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** A shulker box's item, named for the color it was dyed, which cannot go inside another container item. */
public class ShulkerBoxBlockItem extends StorageBlockItem {

    public ShulkerBoxBlockItem(Block block, Properties props, Supplier<DataComponentType<StorageInfo>> storageInfo) {
        super(block, props, storageInfo);
    }

    /** The dyed name is {@code <description id>_<color>}, read from the color in the stack's {@code custom_data}. */
    @Override
    public Component getName(ItemStack stack) {
        int color = NBTHelper.getInt(stack, "Color", -1);
        if (color == -1) {
            return super.getName(stack);
        }

        return Component.translatable(this.getDescriptionId() + "_" + DyeColor.byId(color).getName());
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
