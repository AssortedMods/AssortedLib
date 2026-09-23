package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.LibConstants;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Registers this mod's test functions on Fabric. The gametest source set is a dev-only second mod,
 * so nothing in {@code main} references it and the tests never ship, here or into downstream mods.
 * Fabric's {@code @GameTest} is not used because it would duplicate the shared {@code
 * test_instance} jsons.
 */
public class LibFabricGameTests implements ModInitializer {

    @Override
    public void onInitialize() {
        SpawnerTests.install();
        LibGameTests.forEach((name, function) ->
                Registry.register(BuiltInRegistries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, name), function));
        // Fabric-only: its test instance json lives in this source set, not the shared one.
        FabricFluidTests.register((name, function) ->
                Registry.register(BuiltInRegistries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, name), function));
    }
}
