package com.grim3212.assorted.lib.crafting;

import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/**
 * Makes an item compostable. Fabric registers the chance at runtime; on NeoForge it is the
 * {@code neoforge:compostables} data map, written at datagen from what was registered here.
 */
public final class CompostChances {

    private CompostChances() {
    }

    /** Makes {@code item} compostable, adding a layer with probability {@code chance} (0 to 1; vanilla uses 0.3, 0.5, 0.65, 0.85 and 1). */
    public static void add(Supplier<? extends ItemLike> item, float chance) {
        if (!(chance > 0.0F && chance <= 1.0F)) {
            throw new IllegalArgumentException("A compost chance must be in (0, 1], was " + chance);
        }
        Services.PLATFORM.registerCompostable(item, chance);
    }
}
