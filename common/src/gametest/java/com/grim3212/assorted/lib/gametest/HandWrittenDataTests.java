package com.grim3212.assorted.lib.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.data.HandWrittenData;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** {@link HandWrittenData} is what stops a hand-written file's renamed key from reaching a server. */
final class HandWrittenDataTests {

    private static final String UNLOCK = """
            {
              "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "some_other_mod" }],
              "parent": "minecraft:recipes/root",
              "criteria": {
                "has_ingredient": { "trigger": "minecraft:inventory_changed", "conditions": { "items": [{ "items": "#some_other_mod:ores" }] } },
                "has_the_recipe": { "trigger": "minecraft:recipe_unlocked", "conditions": { "%s": "some_other_mod:dust" } }
              },
              "requirements": [["has_the_recipe", "has_ingredient"]],
              "rewards": { "recipes": ["some_other_mod:dust"] }
            }
            """;

    private HandWrittenDataTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("hand_written_advancements_are_checked", HandWrittenDataTests::advancementsAreChecked);
    }

    /** 1.21.1 names the unlocked recipe {@code recipe}; a file spelling it {@code recipes}, as 26.3 does, is refused at load. */
    private static void advancementsAreChecked(GameTestHelper helper) {
        HandWrittenData.advancement(unlock("recipe"));

        try {
            HandWrittenData.advancement(unlock("recipes"));
        } catch (IllegalStateException expected) {
            helper.succeed();
            return;
        }
        helper.fail("An unlock advancement with 26.3's recipes key passed the check");
    }

    private static JsonObject unlock(String recipeKey) {
        return JsonParser.parseString(UNLOCK.formatted(recipeKey)).getAsJsonObject();
    }
}
