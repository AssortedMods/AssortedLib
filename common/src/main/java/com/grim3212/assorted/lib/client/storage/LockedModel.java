package com.grim3212.assorted.lib.client.storage;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.grim3212.assorted.lib.client.model.EmptyModel;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.model.loaders.context.IModelBakingContext;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A block model that swaps between a locked and an unlocked child, given as model ids since inline json children
 * cannot be parsed from here: {@code {"unlocked": "ns:block/foo_unlocked", "locked": "ns:block/foo_locked"}}.
 */
public class LockedModel implements IModelSpecification<LockedModel> {

    private final ResourceLocation unlockedModel;
    private final ResourceLocation lockedModel;

    private LockedModel(ResourceLocation unlockedModel, ResourceLocation lockedModel) {
        this.unlockedModel = unlockedModel;
        this.lockedModel = lockedModel;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of(this.unlockedModel, this.lockedModel);
    }

    @SuppressWarnings("deprecation") // NeoForge deprecates ModelBaker#bake(ResourceLocation, ModelState) for an overload of its own extension, which common cannot see
    @Override
    public BakedModel bake(IModelBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        BakedModel unlocked = Objects.requireNonNullElse(baker.bake(this.unlockedModel, modelState), EmptyModel.BAKED);
        BakedModel locked = Objects.requireNonNullElse(baker.bake(this.lockedModel, modelState), EmptyModel.BAKED);
        return new LockedBakedModel(unlocked, locked, overrides);
    }

    public enum Loader implements IModelSpecificationLoader<LockedModel> {
        INSTANCE;

        @Override
        public LockedModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
            ResourceLocation unlocked = ResourceLocation.parse(GsonHelper.getAsString(modelContents, "unlocked"));
            ResourceLocation locked = ResourceLocation.parse(GsonHelper.getAsString(modelContents, "locked"));
            return new LockedModel(unlocked, locked);
        }
    }
}
