package com.grim3212.assorted.lib.conditions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.LibConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * An on/off switch per mod, checked by the namespace of what it owns. Off hides the mod's items from creative tabs and
 * turns off its recipes, manual chapters, placed features and spawns. {@code Families.join} gives one to each part.
 */
public final class PartToggles {

    // Apart from LibParts' free-form names, so a part named after another mod never switches that mod off.
    private static final Map<String, Supplier<Boolean>> TOGGLES = new ConcurrentHashMap<>();
    private static final String PART_ENABLED = LibConstants.MOD_ID + ":part_enabled";

    private PartToggles() {
    }

    /** Gives {@code modId} a switch read from its own config. Recipe and manual conditions name it as a part. */
    public static void register(String modId, Supplier<Boolean> enabled) {
        if (TOGGLES.putIfAbsent(modId, enabled) != null) {
            throw new IllegalArgumentException(modId + " already has a switch");
        }

        LibParts.register(modId, enabled);
    }

    /** Whether {@code modId} was given a switch. */
    public static boolean has(String modId) {
        return TOGGLES.containsKey(modId);
    }

    /** Whether {@code modId} is on. A mod without a switch is always on. */
    public static boolean isEnabled(String modId) {
        Supplier<Boolean> enabled = TOGGLES.get(modId);
        return enabled == null || enabled.get();
    }

    /** Whether the mod that owns {@code id}'s namespace is on. */
    public static boolean isEnabled(Identifier id) {
        return isEnabled(id.getNamespace());
    }

    /** {@code stacks} without the items of parts that are off. */
    public static List<ItemStack> visible(List<ItemStack> stacks) {
        return stacks.stream().filter(stack -> isEnabled(BuiltInRegistries.ITEM.getKey(stack.getItem()))).toList();
    }

    /** Why {@code modId} could not be switched off: no switch, or a recipe written without it. Empty when it can be. */
    public static List<String> problems(ResourceManager manager, String modId) {
        List<String> problems = new ArrayList<>();
        if (!TOGGLES.containsKey(modId)) {
            problems.add(modId + " has no switch; join its family or call PartToggles.register while it is constructed");
        }

        manager.listResources("recipe", id -> id.getNamespace().equals(modId) && id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray conditions = json.has("neoforge:conditions") ? json.getAsJsonArray("neoforge:conditions") : new JsonArray();
                boolean switched = conditions.asList().stream().map(JsonElement::getAsJsonObject)
                        .anyMatch(condition -> PART_ENABLED.equals(GsonHelper.getAsString(condition, "type", "")) && modId.equals(GsonHelper.getAsString(condition, "part", "")));
                if (!switched) {
                    problems.add(id + " is not turned off with " + modId + "; rerun datagen");
                }
            } catch (IOException | RuntimeException e) {
                problems.add(id + " could not be checked: " + e.getMessage());
            }
        });
        return problems;
    }
}
