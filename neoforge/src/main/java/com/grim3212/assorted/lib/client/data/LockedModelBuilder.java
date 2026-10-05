package com.grim3212.assorted.lib.client.data;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the loader block of a {@code LockedModel} under the loader name a mod registered it as. The children are
 * model ids, {@code {"unlocked": "ns:block/foo_unlocked", "locked": "ns:block/foo_locked"}}.
 */
public class LockedModelBuilder extends LibCustomLoaderBuilder {

    private final Identifier loaderName;
    private @Nullable Identifier unlockedModel;
    private @Nullable Identifier lockedModel;

    protected LockedModelBuilder(Identifier loaderName) {
        super(loaderName, false);
        this.loaderName = loaderName;
    }

    public static LockedModelBuilder begin(Identifier loaderName) {
        return new LockedModelBuilder(loaderName);
    }

    public LockedModelBuilder unlockedModel(Identifier unlockedModel) {
        this.unlockedModel = unlockedModel;
        return this;
    }

    public LockedModelBuilder lockedModel(Identifier lockedModel) {
        this.lockedModel = lockedModel;
        return this;
    }

    @Override
    protected CustomLoaderBuilder copyInternal() {
        LockedModelBuilder copy = new LockedModelBuilder(this.loaderName);
        copy.unlockedModel = this.unlockedModel;
        copy.lockedModel = this.lockedModel;
        return copy;
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
