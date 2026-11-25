package com.grim3212.assorted.lib.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One generated tree serves both loaders. NeoForge datagen writes it, and this adds Fabric's spelling
 * beside NeoForge's wherever they differ: load conditions and custom ingredient types. Each loader
 * reads its own keys and ignores the other's. Anything with no Fabric equivalent fails datagen rather
 * than loading unconditionally on Fabric.
 */
public final class CrossLoaderData {

    public static final String NEOFORGE_CONDITIONS = "neoforge:conditions";
    public static final String FABRIC_CONDITIONS = "fabric:load_conditions";
    /**
     * NeoForge 21.1 names a custom ingredient's type under the plain {@code "type"} key, which recipes
     * and loot entries use too, so an object is only taken for an ingredient when its type is one of
     * {@link #INGREDIENT_TYPES}.
     */
    public static final String NEOFORGE_INGREDIENT = "type";
    public static final String FABRIC_INGREDIENT = "fabric:type";

    /** The library's own custom ingredient, which reads the same fields on both loaders. */
    public static final String STORED_FLUID_INGREDIENT = "assortedlib:stored_fluid_ingredient";

    /** Every custom ingredient type NeoForge 21.1 registers, and the library's own. */
    private static final List<String> INGREDIENT_TYPES = List.of("neoforge:compound", "neoforge:components", "neoforge:difference",
            "neoforge:intersection", "neoforge:block_tag", STORED_FLUID_INGREDIENT);

    /** The directories whose files can carry either key; tags and assets cannot. */
    private static final List<String> CONDITIONAL_DIRECTORIES = List.of("recipe", "advancement", "loot_table", "enchantment", "worldgen");

    private CrossLoaderData() {
    }

    /**
     * Writes every json file with the Fabric keys added. {@code RecipeProvider.Runner#run} is final,
     * so providers hand this to their delegate instead of overriding the write.
     */
    public static CachedOutput wrap(CachedOutput output) {
        return (path, data, hash) -> {
            if (!path.toString().endsWith(".json")) {
                output.writeIfNeeded(path, data, hash);
                return;
            }

            JsonElement original = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            JsonElement combined;
            try {
                combined = withFabricKeys(original);
            } catch (IllegalArgumentException e) {
                // Unchecked on purpose: DataProvider#saveStable only logs an IOException.
                throw new IllegalStateException("Cannot write " + path + " for both loaders: " + e.getMessage(), e);
            }

            if (combined.equals(original)) {
                output.writeIfNeeded(path, data, hash);
            } else {
                writeStable(output, combined, path);
            }
        };
    }

    /** A copy of {@code json} carrying the Fabric spelling of every NeoForge condition and ingredient. */
    public static JsonElement withFabricKeys(JsonElement json) {
        JsonElement copy = withoutFabricKeys(json);
        addFabricKeys(copy);
        return copy;
    }

    /** A copy of {@code json} with every Fabric key {@link #withFabricKeys} adds taken out again. */
    public static JsonElement withoutFabricKeys(JsonElement json) {
        JsonElement copy = json.deepCopy();
        stripFabricKeys(copy);
        return copy;
    }

    /**
     * Every file in {@code namespace} whose Fabric keys are not exactly what {@link #withFabricKeys}
     * derives from its NeoForge ones. Reads the raw resources, so a file whose conditions fail is
     * still checked. For each mod's gametests.
     */
    public static List<String> mismatches(ResourceManager manager, String namespace) {
        List<String> problems = new ArrayList<>();
        for (String directory : CONDITIONAL_DIRECTORIES) {
            Map<ResourceLocation, Resource> resources = manager.listResources(directory, id -> id.getNamespace().equals(namespace) && id.getPath().endsWith(".json"));
            resources.forEach((id, resource) -> {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    JsonElement expected = withFabricKeys(json);
                    if (!expected.equals(json)) {
                        problems.add(id + " differs from its cross-loader form " + expected);
                    }
                } catch (IOException | RuntimeException e) {
                    problems.add(id + " could not be checked: " + e.getMessage());
                }
            });
        }
        return problems;
    }

    private static void addFabricKeys(JsonElement json) {
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(CrossLoaderData::addFabricKeys);
            return;
        }
        if (!json.isJsonObject()) {
            return;
        }

        JsonObject object = json.getAsJsonObject();
        if (object.has("neoforge:value")) {
            throw new IllegalArgumentException("Fabric has no equivalent of neoforge:value");
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getKey().equals(NEOFORGE_CONDITIONS)) {
                addFabricKeys(entry.getValue());
            }
        }

        if (object.has(NEOFORGE_CONDITIONS)) {
            object.add(FABRIC_CONDITIONS, conditions(GsonHelper.getAsJsonArray(object, NEOFORGE_CONDITIONS)));
        }
        if (isIngredient(object)) {
            addFabricIngredient(object);
        }
    }

    private static void stripFabricKeys(JsonElement json) {
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(CrossLoaderData::stripFabricKeys);
            return;
        }
        if (!json.isJsonObject()) {
            return;
        }

        JsonObject object = json.getAsJsonObject();
        object.remove(FABRIC_CONDITIONS);
        if (isIngredient(object) && object.has(FABRIC_INGREDIENT)) {
            object.remove(FABRIC_INGREDIENT);
            // The list field Fabric's any/all read, next to NeoForge's "children".
            if (object.has("children")) {
                object.remove("ingredients");
            }
        }
        object.entrySet().forEach(entry -> stripFabricKeys(entry.getValue()));
    }

    private static boolean isIngredient(JsonObject object) {
        JsonElement type = object.get(NEOFORGE_INGREDIENT);
        return type != null && type.isJsonPrimitive() && INGREDIENT_TYPES.contains(type.getAsString());
    }

    private static JsonArray conditions(JsonArray neoforge) {
        JsonArray fabric = new JsonArray();
        neoforge.forEach(condition -> fabric.add(condition(GsonHelper.convertToJsonObject(condition, "condition"))));
        return fabric;
    }

    private static JsonObject condition(JsonObject neoforge) {
        String type = GsonHelper.getAsString(neoforge, "type");
        JsonObject fabric = new JsonObject();
        switch (type) {
            case "neoforge:true" -> fabric.addProperty("condition", "fabric:true");
            case "neoforge:false" -> fabric.addProperty("condition", "fabric:false");
            case "neoforge:and", "neoforge:or" -> {
                fabric.addProperty("condition", "fabric:" + ResourceLocation.parse(type).getPath());
                fabric.add("values", conditions(GsonHelper.getAsJsonArray(neoforge, "values")));
            }
            case "neoforge:not" -> {
                fabric.addProperty("condition", "fabric:not");
                fabric.add("value", condition(GsonHelper.getAsJsonObject(neoforge, "value")));
            }
            case "neoforge:mod_loaded" -> {
                fabric.addProperty("condition", "fabric:all_mods_loaded");
                fabric.add("values", single(neoforge.get("modid")));
            }
            // Fabric's registry defaults to minecraft:item, which is the only registry NeoForge's asks.
            case "neoforge:item_exists" -> {
                fabric.addProperty("condition", "fabric:registry_contains");
                fabric.add("values", single(neoforge.get("item")));
            }
            // An item tag on NeoForge 21.1, and Fabric's registry defaults to minecraft:item.
            case "neoforge:tag_empty" -> {
                JsonObject populated = new JsonObject();
                populated.addProperty("condition", "fabric:tags_populated");
                populated.add("values", single(neoforge.get("tag")));
                fabric.addProperty("condition", "fabric:not");
                fabric.add("value", populated);
            }
            case "assortedlib:item_tag_populated" -> tagsPopulated(fabric, "minecraft:item", neoforge.get("tag"));
            case "assortedlib:block_tag_populated" -> tagsPopulated(fabric, "minecraft:block", neoforge.get("tag"));
            case "assortedlib:block_exists" -> {
                fabric.addProperty("condition", "fabric:registry_contains");
                fabric.addProperty("registry", "minecraft:block");
                fabric.add("values", single(neoforge.get("block")));
            }
            default -> {
                if (type.startsWith("neoforge:")) {
                    throw new IllegalArgumentException("No Fabric equivalent for condition " + type);
                }
                // The lib's own conditions - part_enabled and IConditionHelper#register - have the
                // same id and fields on Fabric.
                fabric.addProperty("condition", type);
                neoforge.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals("type"))
                        .forEach(entry -> fabric.add(entry.getKey(), entry.getValue().deepCopy()));
            }
        }
        return fabric;
    }

    private static void tagsPopulated(JsonObject fabric, String registry, JsonElement tag) {
        fabric.addProperty("condition", "fabric:tags_populated");
        fabric.addProperty("registry", registry);
        fabric.add("values", single(tag));
    }

    private static JsonArray single(JsonElement value) {
        if (value == null) {
            throw new IllegalArgumentException("Condition is missing the field Fabric needs");
        }
        JsonArray array = new JsonArray();
        array.add(value.deepCopy());
        return array;
    }

    /** Called after the object's children were given their own Fabric keys. */
    private static void addFabricIngredient(JsonObject ingredient) {
        String type = GsonHelper.getAsString(ingredient, NEOFORGE_INGREDIENT);
        switch (type) {
            case "neoforge:difference" -> ingredient.addProperty(FABRIC_INGREDIENT, "fabric:difference");
            case "neoforge:compound", "neoforge:intersection" -> {
                ingredient.addProperty(FABRIC_INGREDIENT, type.equals("neoforge:compound") ? "fabric:any" : "fabric:all");
                ingredient.add("ingredients", GsonHelper.getAsJsonArray(ingredient, "children").deepCopy());
            }
            default -> {
                if (type.startsWith("neoforge:")) {
                    throw new IllegalArgumentException("No Fabric equivalent for ingredient " + type);
                }
                // The lib's stored fluid ingredient reads the same fields on both loaders.
                ingredient.addProperty(FABRIC_INGREDIENT, type);
            }
        }
    }

    /** {@link DataProvider#saveStable}'s encoding, run synchronously on the caller's thread. */
    private static void writeStable(CachedOutput output, JsonElement json, Path path) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        @SuppressWarnings("deprecation") // sha1 is what the data generator's cache compares against
        HashingOutputStream hashed = new HashingOutputStream(Hashing.sha1(), bytes);
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(hashed, StandardCharsets.UTF_8))) {
            writer.setSerializeNulls(false);
            writer.setIndent("  ");
            GsonHelper.writeValue(writer, json, DataProvider.KEY_COMPARATOR);
        }
        output.writeIfNeeded(path, bytes.toByteArray(), hashed.hash());
    }
}
