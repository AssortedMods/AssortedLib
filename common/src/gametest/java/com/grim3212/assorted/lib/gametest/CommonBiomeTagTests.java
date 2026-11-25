package com.grim3212.assorted.lib.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The c: biome tags a mod checks hold the same biomes on NeoForge and Fabric. Each loader ships its
 * own copy of these tags, and a mod that decides where things generate from them would otherwise
 * generate differently depending on the loader.
 */
final class CommonBiomeTagTests {

    private CommonBiomeTagTests() {
    }

    /** What every c: biome tag holds, on either loader. Both loaders' runs compare against this one file. */
    private static final String SNAPSHOT = "/assortedlib_test/common_biome_tags.json";

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("common_biome_tags_match_on_every_loader", CommonBiomeTagTests::commonBiomeTagsMatchOnEveryLoader);
    }

    /**
     * Every biome tag in {@link LibCommonTags.Biomes} holds exactly the snapshot's biomes. A failure
     * names the loader and the biomes it adds or lacks. When a loader update moves a biome, check
     * the other loader agrees, add the difference to {@code LibCommonTagProvider.BiomeTagProvider}
     * so both carry it, and update the snapshot - never just the snapshot.
     */
    private static void commonBiomeTagsMatchOnEveryLoader(GameTestHelper helper) {
        HolderLookup.RegistryLookup<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Map<String, Set<String>> expected = readSnapshot(helper);
        List<String> problems = new ArrayList<>();

        for (TagKey<Biome> tag : biomeTags()) {
            String name = tag.location().toString();
            Set<String> want = expected.get(name);
            if (want == null) {
                problems.add(name + " is not in the snapshot");
                continue;
            }

            Set<String> have = new TreeSet<>();
            biomes.get(tag).ifPresent(set -> set.forEach(holder -> holder.unwrapKey().map(ResourceKey::location).ifPresent(id -> have.add(id.toString()))));

            Set<String> extra = new TreeSet<>(have);
            extra.removeAll(want);
            Set<String> missing = new TreeSet<>(want);
            missing.removeAll(have);
            if (!extra.isEmpty() || !missing.isEmpty()) {
                problems.add(name + (extra.isEmpty() ? "" : " also holds " + extra) + (missing.isEmpty() ? "" : " lacks " + missing));
            }
        }

        helper.assertTrue(problems.isEmpty(), Services.PLATFORM.getPlatformName() + " biome tags differ from the other loader's: " + String.join("; ", problems));
        helper.succeed();
    }

    /** Every biome tag constant, so a tag added to {@link LibCommonTags.Biomes} is covered without touching this. */
    @SuppressWarnings("unchecked")
    private static List<TagKey<Biome>> biomeTags() {
        List<TagKey<Biome>> tags = new ArrayList<>();
        for (Field field : LibCommonTags.Biomes.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == TagKey.class) {
                try {
                    tags.add((TagKey<Biome>) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return tags;
    }

    private static Map<String, Set<String>> readSnapshot(GameTestHelper helper) {
        Map<String, Set<String>> snapshot = new TreeMap<>();
        try (InputStream in = CommonBiomeTagTests.class.getResourceAsStream(SNAPSHOT)) {
            helper.assertTrue(in != null, SNAPSHOT + " is not on the classpath");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                Set<String> biomes = new TreeSet<>();
                JsonArray array = entry.getValue().getAsJsonArray();
                array.forEach(element -> biomes.add(element.getAsString()));
                snapshot.put(entry.getKey(), biomes);
            }
        } catch (IOException e) {
            helper.assertTrue(false, "could not read " + SNAPSHOT + ": " + e);
        }
        return snapshot;
    }
}
