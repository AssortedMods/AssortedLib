package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.conditions.LibParts;
import com.grim3212.assorted.lib.conditions.PartToggles;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** A mod's switch is looked up by the namespace of what it owns, and only a mod's own switch counts. */
final class PartToggleTests {

    private static final String PART = "assortedlibtestpart";
    private static final AtomicBoolean ON = new AtomicBoolean(true);
    // A free-form part that shares a mod's namespace, off only while the test runs.
    private static final AtomicBoolean NAMESAKE_ON = new AtomicBoolean(true);

    private PartToggleTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("part_toggles_follow_the_owning_namespace", PartToggleTests::followOwningNamespace);
    }

    private static void followOwningNamespace(GameTestHelper helper) {
        synchronized (PartToggleTests.class) {
            if (!PartToggles.has(PART)) {
                PartToggles.register(PART, ON::get);
            }
            if (!LibParts.isRegistered("minecraft")) {
                LibParts.register("minecraft", NAMESAKE_ON::get);
            }
        }

        Identifier owned = Identifier.fromNamespaceAndPath(PART, "thing");
        try {
            helper.assertTrue(PartToggles.isEnabled(owned), "a part that is on hides what it owns");
            ON.set(false);
            helper.assertFalse(PartToggles.isEnabled(owned), "a part that is off still shows what it owns");
            helper.assertTrue(PartToggles.isEnabled(Identifier.withDefaultNamespace("stone")), "a namespace that is not a part reads as off");
            NAMESAKE_ON.set(false);
            helper.assertTrue(PartToggles.isEnabled(Identifier.withDefaultNamespace("stone")), "a part named after a mod switched that mod off");

            List<ItemStack> shown = PartToggles.visible(List.of(new ItemStack(Items.STONE)));
            helper.assertTrue(shown.size() == 1, "another mod's item was taken out of a creative tab");
        } finally {
            ON.set(true);
            NAMESAKE_ON.set(true);
        }

        helper.succeed();
    }
}
