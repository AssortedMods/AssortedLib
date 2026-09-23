package com.grim3212.assorted.lib.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Automated in-world checks for AssortedLib: the NeoForge and Fabric implementations of each
 * {@code lib/platform/services} interface answer the same way about the same world. Client-only
 * services are not covered, since a gametest run is a dedicated server. The tests live in the
 * {@code *Tests} classes; this only lists them.
 */
public final class LibGameTests {

    private LibGameTests() {
    }

    /** Every test in this mod, named once, so both loaders register the same set. */
    public static void forEach(BiConsumer<String, Consumer<GameTestHelper>> out) {
        PlatformServiceTests.register(out);
        CommonBiomeTagTests.register(out);
        InventoryTests.register(out);
        FluidTests.register(out);
        IngredientTests.register(out);
        CrossLoaderDataTests.register(out);
        EventTests.register(out);
        MenuTests.register(out);
        ManualTests.register(out);
        SpawnerTests.register(out);
    }
}
