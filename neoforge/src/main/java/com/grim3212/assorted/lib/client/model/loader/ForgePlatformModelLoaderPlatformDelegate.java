package com.grim3212.assorted.lib.client.model.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link IGeometryLoader} reading a model json's {@code loader} object through the common
 * specification loader. The standard top level fields (textures, transforms, ambient occlusion)
 * are NeoForge's to read, and reach the specification through its baking context.
 */
public final class ForgePlatformModelLoaderPlatformDelegate<L extends IModelSpecificationLoader<T>, T extends IModelSpecification<T>>
        implements IGeometryLoader<ForgeModelGeometryToSpecificationPlatformDelegator<T>>, ResourceManagerReloadListener {

    private final L delegate;

    public ForgePlatformModelLoaderPlatformDelegate(final L delegate) {
        this.delegate = delegate;
    }

    @Override
    public ForgeModelGeometryToSpecificationPlatformDelegator<T> read(final JsonObject jsonObject, final JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new ForgeModelGeometryToSpecificationPlatformDelegator<>(this.delegate.read(deserializationContext, jsonObject));
    }

    @Override
    public void onResourceManagerReload(final @NotNull ResourceManager resourceManager) {
        this.delegate.onResourceManagerReload(resourceManager);
    }
}
