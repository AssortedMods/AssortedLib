package com.grim3212.assorted.lib.core.item;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Lets a dispenser hatch every mod's spawn eggs. 1.21.1 gives spawn eggs their dispenser behaviour once,
 * while the game bootstraps, so only the vanilla eggs get it; a mod's egg is registered later and a
 * dispenser just drops it as an item. (26.x looks the behaviour up per item type, so the 26.x line never
 * needed this.) Run on server start, once every mod's items exist, on both loaders.
 */
public final class SpawnEggDispensing {

    private SpawnEggDispensing() {
    }

    /** Hands vanilla's own spawn egg behaviour to every egg without one, leaving any a mod set itself. */
    public static void fillIn() {
        DispenseItemBehavior eggBehaviour = DispenserBlock.DISPENSER_REGISTRY.get(Items.PIG_SPAWN_EGG);
        for (SpawnEggItem egg : SpawnEggItem.eggs()) {
            if (!DispenserBlock.DISPENSER_REGISTRY.containsKey(egg)) {
                DispenserBlock.registerBehavior(egg, eggBehaviour);
            }
        }
    }
}
