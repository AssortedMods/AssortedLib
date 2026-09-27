package com.grim3212.assorted.lib.core.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** An item whose mode the switch-modes key cycles; the mod that has one calls {@link ModeSwitching#enable()}. */
public interface ISwitchModes {

    default ItemStack cycleMode(Player player, ItemStack stack) {
        return stack;
    }

    default ItemStack setMode(Player player, ItemStack stack, int mode) {
        return stack;
    }
}
