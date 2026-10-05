package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.core.item.ModeSwitching;
import com.grim3212.assorted.lib.core.tool.ToolTiers;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** The tool pieces Lib holds for other mods, which must not exist until one of them asks. */
final class SharedToolTests {

    private SharedToolTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("shared_tools_are_not_made_until_asked", SharedToolTests::notMadeUntilAsked);
    }

    /** Lib runs alone here, so no tier config file and no mode-switch packet. */
    private static void notMadeUntilAsked(GameTestHelper helper) {
        helper.assertFalse(ToolTiers.created(), "the shared tool tiers and their config file were made with no mod asking for them");
        helper.assertFalse(ModeSwitching.enabled(), "the mode-switch packet was registered with no mod asking for it");
        helper.succeed();
    }
}
