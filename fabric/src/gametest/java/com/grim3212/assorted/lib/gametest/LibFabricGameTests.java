package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.test.TestFunctions;
import net.fabricmc.api.ModInitializer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Registers this mod's test functions on Fabric. The gametest source set is a dev-only second mod,
 * so nothing in {@code main} references it and the tests never ship, here or into downstream mods.
 * It is both that mod's initializer, for what the tests need set up early, and its
 * {@code fabric-gametest} entrypoint, whose generator Fabric API hands to vanilla's registry.
 */
public class LibFabricGameTests implements ModInitializer {

    @Override
    public void onInitialize() {
        SpawnerTests.install();
        RegistryAliasTests.install();
        MovedIdsTests.install();
    }

    @GameTestGenerator
    public Collection<TestFunction> tests() {
        List<TestFunction> tests = new ArrayList<>(TestFunctions.of(LibConstants.MOD_ID, LibGameTests::forEach, LibFabricGameTests.class::getResourceAsStream));
        // Fabric-only: its test instance json lives in this source set, not the shared one.
        tests.addAll(TestFunctions.of(LibConstants.MOD_ID, FabricFluidTests::register, LibFabricGameTests.class::getResourceAsStream));
        return tests;
    }
}
