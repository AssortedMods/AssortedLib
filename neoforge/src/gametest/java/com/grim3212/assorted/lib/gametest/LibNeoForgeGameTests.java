package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.LibConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers this mod's test functions on NeoForge. It is an {@link EventBusSubscriber} in the
 * gametest source set, so nothing in {@code main} references it and the tests never ship, here or
 * into downstream mods.
 */
@EventBusSubscriber(modid = LibConstants.MOD_ID)
public final class LibNeoForgeGameTests {

    private LibNeoForgeGameTests() {
    }

    @SubscribeEvent
    public static void registerGameTests(final RegisterEvent event) {
        SpawnerTests.install();
        event.register(Registries.TEST_FUNCTION, helper -> LibGameTests.forEach(
                (name, function) -> helper.register(Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, name), function)));
    }
}
