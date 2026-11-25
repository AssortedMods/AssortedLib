package com.grim3212.assorted.lib;

import com.grim3212.assorted.lib.config.LibCommonConfig;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.spawn.CreatureSpawner;
import com.grim3212.assorted.lib.spawn.SpawnHabits;
import net.minecraft.resources.ResourceLocation;
import com.grim3212.assorted.lib.manual.ManualIngredientTags;
import com.grim3212.assorted.lib.manual.ManualInteractions;
import com.grim3212.assorted.lib.manual.LibItems;


/** The library's own content, as opposed to its services. Called from both loaders' entry points. */
public class LibCommonSetup {

    public static final LibCommonConfig COMMON_CONFIG = new LibCommonConfig();

    public static void init() {
        LibItems.init();

        ManualInteractions.register();
        ManualIngredientTags.init();

        // Spawn habits: every mod's data/<ns>/spawn_habit/*.json, driven by one spawner per level.
        Services.PLATFORM.addReloadListener(SpawnHabits.ID, new SpawnHabits());
        Services.WORLD_GEN.addCustomSpawner(ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "creatures"), CreatureSpawner::new);
    }
}
