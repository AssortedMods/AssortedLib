package com.grim3212.assorted.lib.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grim3212.assorted.lib.LibCommonSetup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Content a mod took over from another namespace, as each mod split out of one does. Recipe books, advancement progress,
 * structures, chest loot and data-driven entries such as enchantments saved under an old id carry over to the new one.
 */
public final class MovedIds {

    // Concurrent: NeoForge constructs mods in parallel.
    private static final Map<String, Set<String>> HEIRS = new ConcurrentHashMap<>();
    private static final Map<Identifier, Map<String, String>> CRITERIA = new ConcurrentHashMap<>();

    private MovedIds() {
    }

    /** {@code newNamespace} now has what used to be {@code oldNamespace:<path>}, at the same path. */
    public static void inherit(String oldNamespace, String newNamespace) {
        HEIRS.computeIfAbsent(oldNamespace, key -> ConcurrentHashMap.newKeySet()).add(newNamespace);
    }

    /** Criteria of {@code advancement} that were renamed, old name to new, so progress on them carries over. */
    public static void renameCriteria(Identifier advancement, Map<String, String> renames) {
        CRITERIA.computeIfAbsent(advancement, key -> new ConcurrentHashMap<>()).putAll(renames);
    }

    /** Whether any mod has moved anything, cheap enough to ask of every id a codec reads. */
    public static boolean anyMoved() {
        return !HEIRS.isEmpty() || !CRITERIA.isEmpty();
    }

    /** Whether some mod took over {@code namespace}. */
    public static boolean moved(String namespace) {
        return HEIRS.containsKey(namespace);
    }

    /** Whether the hooks should carry anything over; false at once when no mod has moved anything. */
    public static boolean enabled() {
        return anyMoved() && LibCommonSetup.COMMON_CONFIG.carryOverMovedIds.get();
    }

    /** Where {@code id} went: the first heir of its namespace that has its path, or nothing if it did not move. */
    public static Optional<Identifier> resolve(Identifier id, Predicate<Identifier> exists) {
        if (exists.test(id)) {
            return Optional.empty();
        }

        return HEIRS.getOrDefault(id.getNamespace(), Set.of()).stream()
                .map(heir -> Identifier.fromNamespaceAndPath(heir, id.getPath()))
                .filter(exists)
                .sorted()
                .findFirst();
    }

    /** A recipe book's recipes with any that moved under their new ids; the same list if none did. */
    public static List<ResourceKey<Recipe<?>>> recipes(List<ResourceKey<Recipe<?>>> recipes, Predicate<ResourceKey<Recipe<?>>> exists) {
        List<ResourceKey<Recipe<?>>> moved = new ArrayList<>(recipes.size());
        boolean changed = false;
        for (ResourceKey<Recipe<?>> recipe : recipes) {
            Optional<Identifier> to = resolve(recipe.identifier(), id -> exists.test(ResourceKey.create(Registries.RECIPE, id)));
            moved.add(to.map(id -> ResourceKey.create(Registries.RECIPE, id)).orElse(recipe));
            changed |= to.isPresent();
        }

        return changed ? moved : recipes;
    }

    /** A chunk's saved structure starts or references, keyed by structure, with any that moved renamed in place. */
    public static void structures(CompoundTag byStructure, Predicate<Identifier> exists) {
        for (String key : List.copyOf(byStructure.keySet())) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) {
                continue;
            }

            resolve(id, exists).ifPresent(to -> {
                Tag saved = byStructure.remove(key);
                // A start names its structure again inside itself.
                if (saved instanceof CompoundTag start && key.equals(start.getStringOr("id", ""))) {
                    start.putString("id", to.toString());
                }
                if (!byStructure.contains(to.toString())) {
                    byStructure.put(to.toString(), saved);
                }
            });
        }
    }

    /**
     * A player's advancement progress file with every advancement that moved under its new id, and renamed criteria
     * under their new names. A criterion done under two old names keeps the earlier date.
     */
    public static JsonElement advancements(JsonElement saved, Predicate<Identifier> exists) {
        if (!saved.isJsonObject()) {
            return saved;
        }

        JsonObject moved = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : saved.getAsJsonObject().entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
                moved.add(entry.getKey(), entry.getValue());
                continue;
            }

            Identifier to = resolve(id, exists).orElse(id);
            moved.add(to.toString(), renamed(to, entry.getValue()));
        }

        return moved;
    }

    private static JsonElement renamed(Identifier advancement, JsonElement progress) {
        Map<String, String> renames = CRITERIA.get(advancement);
        if (renames == null || !progress.isJsonObject() || !progress.getAsJsonObject().has("criteria")) {
            return progress;
        }

        JsonObject criteria = new JsonObject();
        for (Map.Entry<String, JsonElement> criterion : progress.getAsJsonObject().getAsJsonObject("criteria").entrySet()) {
            String name = renames.getOrDefault(criterion.getKey(), criterion.getKey());
            JsonElement earlier = criteria.get(name);
            // Dates are ISO strings, so the lesser is the earlier.
            if (earlier == null || criterion.getValue().getAsString().compareTo(earlier.getAsString()) < 0) {
                criteria.add(name, criterion.getValue());
            }
        }

        JsonObject copy = progress.getAsJsonObject().deepCopy();
        copy.add("criteria", criteria);
        return copy;
    }
}
