package com.grim3212.assorted.lib.core.creative;

import com.grim3212.assorted.lib.LibCommonSetup;
import com.grim3212.assorted.lib.util.ItemUtil;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreativeTabItems {
    List<ItemStack> items = new ArrayList<>();

    public void add(ItemLike item) {
        items.add(new ItemStack(item));
    }

    public void add(ItemStack item) {
        items.add(item);
    }

    /** Adds {@code item} unless {@code creative.hideUncraftableItems} is on and one of its {@code materials} tags is empty. */
    @SafeVarargs
    public final void addIfObtainable(ItemLike item, TagKey<Item>... materials) {
        addIfObtainable(new ItemStack(item), materials);
    }

    @SafeVarargs
    public final void addIfObtainable(ItemStack item, TagKey<Item>... materials) {
        if (!LibCommonSetup.COMMON_CONFIG.hideUncraftableItems.get() || Arrays.stream(materials).noneMatch(ItemUtil::isTagEmpty)) {
            add(item);
        }
    }

    public List<ItemStack> getItems() {
        return this.items;
    }
}
