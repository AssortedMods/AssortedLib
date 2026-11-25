package com.grim3212.assorted.lib.client.model.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The model loaders registered through {@code IClientHelper#registerModelLoader}, by the id a model
 * json names in its {@code "loader"} key - the key NeoForge reads, so one json serves both loaders.
 * Fabric API has no custom model loaders in 1.21.1, so {@code BlockModelDeserializerMixin} asks here
 * as vanilla parses each model json.
 */
public final class FabricModelLoaders {

    private static final Map<ResourceLocation, IModelSpecificationLoader<?>> LOADERS = new ConcurrentHashMap<>();

    private FabricModelLoaders() {
    }

    public static void register(final ResourceLocation name, final IModelSpecificationLoader<?> loader) {
        if (LOADERS.putIfAbsent(name, loader) != null) {
            throw new IllegalStateException("Model loader " + name + " is already registered");
        }
    }

    /** The specification the json's {@code loader} reads, or null when it names none of ours. */
    @Nullable
    public static IModelSpecification<?> read(final JsonDeserializationContext context, final JsonObject json) {
        if (!json.has("loader")) {
            return null;
        }

        final ResourceLocation name = ResourceLocation.tryParse(GsonHelper.getAsString(json, "loader"));
        final IModelSpecificationLoader<?> loader = name == null ? null : LOADERS.get(name);
        if (loader == null) {
            return null;
        }

        final IModelSpecification<?> specification = loader.read(context, json);
        if (specification == null) {
            throw new JsonParseException("Model loader " + name + " read nothing");
        }
        return specification;
    }
}
