package com.grim3212.assorted.lib.config;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.platform.Services;

import java.util.function.Supplier;

/** Server rules of the library's own. */
public class LibCommonConfig {

    public final Supplier<Boolean> carryOverMovedIds;
    public final Supplier<Boolean> hideUncraftableItems;

    public LibCommonConfig() {
        final IConfigurationBuilder builder = Services.CONFIG.createBuilder(ConfigurationType.NOT_SYNCED, LibConstants.MOD_ID + "-common");

        carryOverMovedIds = builder.defineBoolean("migration.carryOverMovedIds", true,
                "Set this to true to keep a player's recipe book and advancement progress, and a world's structures and unopened chest loot, when a mod moves them to new ids, as one split into several mods does.");

        hideUncraftableItems = builder.defineBoolean("creative.hideUncraftableItems", false,
                "Set this to true to hide items from the creative menu when nothing installed provides their material, like a tin chest without a mod that adds tin.");

        builder.setup();
    }
}
