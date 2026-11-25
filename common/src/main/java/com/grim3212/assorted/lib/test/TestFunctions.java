package com.grim3212.assorted.lib.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.mixin.test.TestHelperAccessor;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Rotation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Turns a mod's test list into 1.21.1 {@link TestFunction}s. 1.21.1 has no {@code test_instance} registry, so
 * each loader's {@code @GameTestGenerator} hands these to vanilla's {@code GameTestRegistry} instead, while every
 * test keeps the same {@code forEach} body and the same {@code data/<ns>/test_instance/<name>.json} the 26.x line
 * reads its structure, ticks and {@code required} flag from. Builds records only and registers nothing, so it can
 * ship in the library jar as the rest of this package does.
 */
public final class TestFunctions {

    private TestFunctions() {
    }

    /**
     * @param modId    the namespace of the tests and of their test instance jsons
     * @param tests    the mod's {@code <Mod>GameTests::forEach}
     * @param resource opens a resource path in the mod's own module, e.g. {@code path -> MyTestSupport.class.getResourceAsStream(path)};
     *                 on NeoForge each mod is its own module, so the library cannot read another mod's jsons itself
     */
    public static Collection<TestFunction> of(String modId, Consumer<BiConsumer<String, Consumer<GameTestHelper>>> tests,
                                              Function<String, InputStream> resource) {
        List<TestFunction> functions = new ArrayList<>();
        tests.accept((name, body) -> functions.add(function(modId, name, body, resource)));
        return functions;
    }

    private static TestFunction function(String modId, String name, Consumer<GameTestHelper> body, Function<String, InputStream> resource) {
        JsonObject json = read(resource, "/data/" + modId + "/test_instance/" + name + ".json", modId + ":" + name);
        String structure = GsonHelper.getAsString(json, "structure");
        int maxTicks = GsonHelper.getAsInt(json, "max_ticks", 100);
        long setupTicks = GsonHelper.getAsLong(json, "setup_ticks", 0L);
        boolean required = GsonHelper.getAsBoolean(json, "required", true);
        // 1.21.1 walls every test in with barriers, roof included, unless it asks for the sky. A test of
        // where things spawn reads the heightmap, which would otherwise find the roof.
        boolean skyAccess = GsonHelper.getAsBoolean(json, "sky_access", false);

        // One batch per mod and environment: 1.21.1 runs a batch's tests side by side, as the 26.x line
        // runs the tests that share an environment. The name keeps the namespace so two mods' tests of
        // the same name cannot collide.
        ResourceLocation environment = ResourceLocation.parse(GsonHelper.getAsString(json, "environment", "minecraft:default"));
        String batch = modId;
        // Positions count from the box, as on the 26.x line, rather than from 1.21.1's structure block below it.
        Consumer<GameTestHelper> run = helper -> body.accept(new BoxOriginTestHelper(((TestHelperAccessor) helper).assortedlib_getTestInfo()));
        if (!environment.equals(ResourceLocation.withDefaultNamespace("default"))) {
            batch = modId + "_" + environment.getNamespace() + "_" + environment.getPath();
            run = environment(environment, resource).andThen(run);
        }
        return new TestFunction(batch, modId + "." + name, structure, Rotation.NONE, maxTicks, setupTicks, required,
                false, 1, 1, skyAccess, run);
    }

    /**
     * What a test environment sets up, done at the start of each of its tests since 1.21.1 has no
     * environment registry. Only the kinds the Assorted mods use are read; any other fails loudly
     * rather than running the test somewhere it was never meant to.
     */
    private static Consumer<GameTestHelper> environment(ResourceLocation id, Function<String, InputStream> resource) {
        JsonObject json = read(resource, "/data/" + id.getNamespace() + "/test_environment/" + id.getPath() + ".json", "environment " + id);
        return environment(id.toString(), json, resource);
    }

    private static Consumer<GameTestHelper> environment(String name, JsonObject json, Function<String, InputStream> resource) {
        String type = GsonHelper.getAsString(json, "type");
        if (type.equals("minecraft:clock_time")) {
            long time = GsonHelper.getAsLong(json, "time");
            return helper -> helper.getLevel().setDayTime(time);
        }
        if (type.equals("minecraft:all_of")) {
            // Each definition by id or inline, set up in order. An empty one sets nothing up and only
            // keeps its tests in a batch of their own, as the 26.x line does with it.
            Consumer<GameTestHelper> all = helper -> {
            };
            for (JsonElement definition : GsonHelper.getAsJsonArray(json, "definitions", new JsonArray())) {
                all = all.andThen(definition.isJsonPrimitive()
                        ? environment(ResourceLocation.parse(definition.getAsString()), resource)
                        : environment(name, GsonHelper.convertToJsonObject(definition, "definition"), resource));
            }
            return all;
        }
        throw new IllegalStateException("Test environment " + name + " is a " + type + ", which the 1.21.1 tests cannot set up");
    }

    private static JsonObject read(Function<String, InputStream> resource, String path, String what) {
        try (InputStream in = resource.apply(path)) {
            if (in == null) {
                throw new IllegalStateException(what + " has no " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }
}
