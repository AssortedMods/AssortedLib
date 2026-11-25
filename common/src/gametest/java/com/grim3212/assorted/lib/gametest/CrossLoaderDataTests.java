package com.grim3212.assorted.lib.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.conditions.LibParts;
import com.grim3212.assorted.lib.data.CrossLoaderData;
import com.grim3212.assorted.lib.test.TestSupport;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * One generated tree read by both loaders: {@link CrossLoaderData} adds Fabric's condition and
 * ingredient keys beside NeoForge's, and the fixtures under {@code recipe/cross_loader_test} and
 * {@code advancement/cross_loader_test} are files in exactly that form.
 */
final class CrossLoaderDataTests {

    private static final String PART_ON = "assortedlib_test_cross_loader_on";
    private static final String PART_OFF = "assortedlib_test_cross_loader_off";

    private CrossLoaderDataTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        // Registered with the test functions: the fixtures' part conditions are read when the
        // datapack loads, before any test runs.
        registerTestParts();
        out.accept("cross_loader_json_adds_fabric_keys", CrossLoaderDataTests::jsonAddsFabricKeys);
        out.accept("cross_loader_conditions_gate_recipes", CrossLoaderDataTests::conditionsGateRecipes);
        out.accept("cross_loader_conditions_gate_advancements", CrossLoaderDataTests::conditionsGateAdvancements);
        out.accept("cross_loader_ingredients_read_on_every_loader", CrossLoaderDataTests::ingredientsReadOnEveryLoader);
        out.accept("cross_loader_data_matches_translator", CrossLoaderDataTests::dataMatchesTranslator);
    }

    private static synchronized void registerTestParts() {
        if (!LibParts.isRegistered(PART_ON)) {
            LibParts.register(PART_ON, () -> true);
        }
        if (!LibParts.isRegistered(PART_OFF)) {
            LibParts.register(PART_OFF, () -> false);
        }
    }

    /**
     * The translation itself, nested both ways: conditions inside conditions and ingredients inside
     * ingredients. Stripping undoes it, running it twice changes nothing, and anything without a
     * Fabric equivalent is refused rather than written unconditionally.
     */
    private static void jsonAddsFabricKeys(GameTestHelper helper) {
        JsonElement neoforge = JsonParser.parseString("""
                {
                  "neoforge:conditions": [{"type": "neoforge:and", "values": [
                    {"type": "assortedlib:part_enabled", "part": "dairy"},
                    {"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": "create"}}
                  ]}],
                  "type": "minecraft:crafting_shaped",
                  "key": {"A": {"type": "neoforge:compound", "children": [
                    "minecraft:stick",
                    {"type": "neoforge:difference", "base": "#minecraft:planks", "subtracted": "minecraft:oak_planks"}
                  ]}}
                }""");
        JsonElement expected = JsonParser.parseString("""
                {
                  "neoforge:conditions": [{"type": "neoforge:and", "values": [
                    {"type": "assortedlib:part_enabled", "part": "dairy"},
                    {"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": "create"}}
                  ]}],
                  "fabric:load_conditions": [{"condition": "fabric:and", "values": [
                    {"condition": "assortedlib:part_enabled", "part": "dairy"},
                    {"condition": "fabric:not", "value": {"condition": "fabric:all_mods_loaded", "values": ["create"]}}
                  ]}],
                  "type": "minecraft:crafting_shaped",
                  "key": {"A": {"type": "neoforge:compound", "fabric:type": "fabric:any",
                    "children": [
                      "minecraft:stick",
                      {"type": "neoforge:difference", "fabric:type": "fabric:difference", "base": "#minecraft:planks", "subtracted": "minecraft:oak_planks"}
                    ],
                    "ingredients": [
                      "minecraft:stick",
                      {"type": "neoforge:difference", "fabric:type": "fabric:difference", "base": "#minecraft:planks", "subtracted": "minecraft:oak_planks"}
                    ]
                  }}
                }""");

        JsonElement combined = CrossLoaderData.withFabricKeys(neoforge);
        helper.assertTrue(combined.equals(expected), "translated to " + combined);
        helper.assertTrue(CrossLoaderData.withFabricKeys(combined).equals(expected), "translating twice changed the file: " + CrossLoaderData.withFabricKeys(combined));
        helper.assertTrue(CrossLoaderData.withoutFabricKeys(combined).equals(neoforge), "stripping did not give back the NeoForge file: " + CrossLoaderData.withoutFabricKeys(combined));

        JsonElement plain = JsonParser.parseString("{\"type\": \"minecraft:crafting_shapeless\", \"ingredients\": [\"minecraft:stick\"], \"result\": {\"id\": \"minecraft:stone\"}}");
        helper.assertTrue(CrossLoaderData.withFabricKeys(plain).equals(plain), "a file with nothing loader specific was changed");

        assertRefused(helper, "{\"neoforge:conditions\": [{\"type\": \"neoforge:feature_flags_enabled\", \"flags\": [\"minecraft:vanilla\"]}]}");
        assertRefused(helper, "{\"key\": {\"A\": {\"type\": \"neoforge:components\", \"items\": \"minecraft:stick\", \"components\": {}}}}");
        assertRefused(helper, "{\"neoforge:conditions\": [], \"neoforge:value\": 1}");

        helper.succeed();
    }

    private static void assertRefused(GameTestHelper helper, String json) {
        try {
            JsonElement translated = CrossLoaderData.withFabricKeys(JsonParser.parseString(json));
            helper.fail("translated a file Fabric cannot read the same way: " + translated);
        } catch (IllegalArgumentException expected) {
            // Refused, as it should be.
        }
    }

    /**
     * The same files are gated by each loader's own copy of the conditions, and a loader ignores the
     * other's: the two single-loader fixtures load on exactly one loader each.
     */
    private static void conditionsGateRecipes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        assertRecipe(helper, server, lib("always"), true);
        assertRecipe(helper, server, lib("never"), false);
        assertRecipe(helper, server, lib("part_on"), true);
        assertRecipe(helper, server, lib("part_off"), false);
        assertRecipe(helper, server, lib("every_condition"), true);
        assertRecipe(helper, server, lib("every_condition_failing"), false);

        boolean neoforge = TestSupport.onNeoForge();
        assertRecipe(helper, server, ResourceLocation.fromNamespaceAndPath("assortedlib_test", "cross_loader_test/neoforge_passes"), neoforge);
        assertRecipe(helper, server, ResourceLocation.fromNamespaceAndPath("assortedlib_test", "cross_loader_test/fabric_passes"), !neoforge);

        helper.succeed();
    }

    private static void assertRecipe(GameTestHelper helper, MinecraftServer server, ResourceLocation id, boolean loaded) {
        boolean present = server.getRecipeManager().byKey(id).isPresent();
        helper.assertTrue(present == loaded, "recipe " + id + (loaded ? " did not load" : " loaded although its conditions fail"));
    }

    private static void conditionsGateAdvancements(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server.getAdvancements().get(lib("always")) != null, "the advancement whose conditions pass did not load");
        helper.assertTrue(server.getAdvancements().get(lib("never")) == null, "the advancement whose conditions fail loaded");
        helper.succeed();
    }

    /**
     * Each combined ingredient in the fixture decodes on this loader and matches what its NeoForge
     * half says. Fluid amounts are millibuckets in the file on both loaders: two buckets must not be
     * satisfied by one, and the amount writes back out unchanged.
     */
    private static void ingredientsReadOnEveryLoader(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        assertRecipe(helper, server, lib("ingredients"), true);

        JsonObject key = readFixture(helper, server, "recipe/cross_loader_test/ingredients.json").getAsJsonObject("key");
        DynamicOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);

        Ingredient difference = decode(helper, ops, key, "D");
        assertMatches(helper, "difference", difference, List.of(Items.STICK, Items.DIRT), List.of(Items.STONE, Items.OAK_LOG));

        Ingredient any = decode(helper, ops, key, "A");
        assertMatches(helper, "compound", any, List.of(Items.STICK, Items.STONE), List.of(Items.DIRT));

        Ingredient all = decode(helper, ops, key, "I");
        assertMatches(helper, "intersection", all, List.of(Items.STONE), List.of(Items.STICK, Items.DIRT));

        Ingredient nested = decode(helper, ops, key, "N");
        assertMatches(helper, "difference of a compound", nested, List.of(Items.STICK), List.of(Items.STONE, Items.DIRT));

        Ingredient oneBucket = decode(helper, ops, key, "W");
        assertMatches(helper, "one bucket of water", oneBucket, List.of(Items.WATER_BUCKET), List.of(Items.BUCKET, Items.LAVA_BUCKET));

        Ingredient twoBuckets = decode(helper, ops, key, "T");
        helper.assertFalse(twoBuckets.test(new ItemStack(Items.WATER_BUCKET)), "a two bucket fluid ingredient accepted one bucket, so its amount was read in the wrong unit");

        assertAmountWritten(helper, ops, oneBucket, null);
        assertAmountWritten(helper, ops, decode(helper, ops, key, "H"), 500L);
        assertAmountWritten(helper, ops, twoBuckets, 2000L);

        helper.succeed();
    }

    private static Ingredient decode(GameTestHelper helper, DynamicOps<JsonElement> ops, JsonObject key, String symbol) {
        return Ingredient.CODEC.parse(ops, key.get(symbol))
                .getOrThrow(error -> new GameTestAssertException("ingredient " + symbol + " did not decode: " + error));
    }

    private static void assertMatches(GameTestHelper helper, String name, Ingredient ingredient, List<Item> accepted, List<Item> rejected) {
        accepted.forEach(item -> helper.assertTrue(ingredient.test(new ItemStack(item)), "the " + name + " ingredient rejected " + item));
        rejected.forEach(item -> helper.assertFalse(ingredient.test(new ItemStack(item)), "the " + name + " ingredient accepted " + item));
    }

    /** {@code expected} null means the default, one bucket, which is left out of the file. */
    private static void assertAmountWritten(GameTestHelper helper, DynamicOps<JsonElement> ops, Ingredient ingredient, Long expected) {
        JsonObject written = Ingredient.CODEC.encodeStart(ops, ingredient)
                .getOrThrow(error -> new GameTestAssertException("a fluid ingredient did not encode: " + error))
                .getAsJsonObject();
        JsonElement amount = written.get("amount");
        if (expected == null) {
            helper.assertTrue(amount == null, "a one bucket fluid ingredient wrote an amount: " + written);
        } else {
            helper.assertTrue(amount != null && amount.getAsLong() == expected, "a fluid ingredient of " + expected + " mB wrote " + written);
        }
    }

    /**
     * Every fixture, and anything this mod generates, is exactly what the translator writes. Each
     * mod's own gametests run the same check over its data.
     */
    private static void dataMatchesTranslator(GameTestHelper helper) {
        List<String> problems = CrossLoaderData.mismatches(helper.getLevel().getServer().getResourceManager(), LibConstants.MOD_ID);
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    private static JsonObject readFixture(GameTestHelper helper, MinecraftServer server, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, path);
        try (Reader reader = server.getResourceManager().getResourceOrThrow(id).openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new GameTestAssertException("could not read " + id + ": " + e.getMessage());
        }
    }

    private static ResourceLocation lib(String name) {
        return ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "cross_loader_test/" + name);
    }
}
