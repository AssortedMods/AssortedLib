package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.test.TestFunctions;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Collection;

/**
 * Registers this mod's test functions on NeoForge. It lives in the gametest source set, so nothing in
 * {@code main} references it and the tests never ship, here or into downstream mods. NeoForge finds
 * the {@link GameTestHolder} by its annotation and asks the generator for the tests, which come from
 * the shared list and the shared test instance jsons.
 */
@GameTestHolder(LibConstants.MOD_ID)
@EventBusSubscriber(modid = LibConstants.MOD_ID)
public final class LibNeoForgeGameTests {

    private LibNeoForgeGameTests() {
    }

    /** What the tests rely on being there before the registries freeze; once, on the first registry. */
    @SubscribeEvent
    public static void install(final RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            SpawnerTests.install();
            RegistryAliasTests.install();
            MovedIdsTests.install();
        }
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        return TestFunctions.of(LibConstants.MOD_ID, LibGameTests::forEach, LibNeoForgeGameTests.class::getResourceAsStream);
    }
}
