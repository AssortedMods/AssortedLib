package com.grim3212.assorted.lib.crafting;

import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/**
 * Makes an item burn in a furnace. Fabric registers fuel at runtime; on NeoForge burn times are the
 * {@code neoforge:furnace_fuels} data map, written at datagen from what was registered here.
 */
public final class BurnTimes {

    private BurnTimes() {
    }

    /** Makes {@code item} burn for {@code ticks} in a furnace, half that in a smoker or blast furnace. Call from mod construction. */
    public static void add(Supplier<? extends ItemLike> item, int ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("A burn time must be positive, was " + ticks);
        }
        Services.PLATFORM.registerFuel(item, ticks);
    }
}
