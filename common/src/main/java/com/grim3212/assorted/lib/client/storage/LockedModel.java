package com.grim3212.assorted.lib.client.storage;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.model.loaders.context.IModelBakingContext;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

/**
 * A block model that swaps between a locked and an unlocked child, given as model ids since inline json children
 * cannot be parsed from here: {@code {"unlocked": "ns:block/foo_unlocked", "locked": "ns:block/foo_locked"}}.
 */
public class LockedModel implements IModelSpecification<LockedModel> {

    private final Identifier unlockedModel;
    private final Identifier lockedModel;

    private LockedModel(Identifier unlockedModel, Identifier lockedModel) {
        this.unlockedModel = unlockedModel;
        this.lockedModel = lockedModel;
    }

    // ModelBaker#getModel only resolves ids marked during discovery; discovery does not walk the
    // resource pack, so an unmarked child bakes to the missing model however correct its json is.
    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(this.unlockedModel);
        resolver.markDependency(this.lockedModel);
    }

    @Override
    public BlockStateModel bake(IModelBakingContext context, ModelBaker baker, ModelState modelState, Identifier modelLocation) {
        BlockStateModelPart unlocked = SimpleModelWrapper.bake(baker, this.unlockedModel, modelState);
        BlockStateModelPart locked = SimpleModelWrapper.bake(baker, this.lockedModel, modelState);
        return new LockedBakedModel(unlocked, locked);
    }

    public enum Loader implements IModelSpecificationLoader<LockedModel> {
        INSTANCE;

        @Override
        public LockedModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
            Identifier unlocked = Identifier.parse(GsonHelper.getAsString(modelContents, "unlocked"));
            Identifier locked = Identifier.parse(GsonHelper.getAsString(modelContents, "locked"));
            return new LockedModel(unlocked, locked);
        }
    }
}
