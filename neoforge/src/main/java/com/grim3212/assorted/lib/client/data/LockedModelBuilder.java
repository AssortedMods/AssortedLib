package com.grim3212.assorted.lib.client.data;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Writes the loader block of a {@code LockedModel} under the loader name a mod registered it as. The children are
 * model ids, {@code {"unlocked": "ns:block/foo_unlocked", "locked": "ns:block/foo_locked"}}. Used as
 * {@code builder.customLoader(LockedModelBuilder.begin(loaderName))}.
 */
public class LockedModelBuilder<T extends ModelBuilder<T>> extends LibCustomLoaderBuilder<T> {

    private @Nullable ResourceLocation unlockedModel;
    private @Nullable ResourceLocation lockedModel;

    protected LockedModelBuilder(ResourceLocation loaderName, T parent, ExistingFileHelper existingFileHelper) {
        super(loaderName, parent, existingFileHelper, false);
    }

    public static <T extends ModelBuilder<T>> BiFunction<T, ExistingFileHelper, LockedModelBuilder<T>> begin(ResourceLocation loaderName) {
        return (parent, existingFileHelper) -> new LockedModelBuilder<>(loaderName, parent, existingFileHelper);
    }

    public LockedModelBuilder<T> unlockedModel(ResourceLocation unlockedModel) {
        this.unlockedModel = unlockedModel;
        return this;
    }

    public LockedModelBuilder<T> lockedModel(ResourceLocation lockedModel) {
        this.lockedModel = lockedModel;
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);

        Preconditions.checkNotNull(unlockedModel, "unlocked model must not be null");
        Preconditions.checkNotNull(lockedModel, "locked model must not be null");

        json.addProperty("unlocked", unlockedModel.toString());
        json.addProperty("locked", lockedModel.toString());

        return json;
    }
}
