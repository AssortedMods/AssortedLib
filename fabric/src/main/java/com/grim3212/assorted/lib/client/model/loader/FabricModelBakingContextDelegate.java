package com.grim3212.assorted.lib.client.model.loader;

import com.grim3212.assorted.lib.client.model.loaders.context.IModelBakingContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Answers for the model json being baked, parents included: its textures, display transforms, GUI
 * light and ambient occlusion are read through vanilla's own {@link BlockModel} lookups, which walk
 * the resolved parent chain.
 */
public class FabricModelBakingContextDelegate implements IModelBakingContext {

    private final BlockModel source;
    private final ModelBaker baker;

    public FabricModelBakingContextDelegate(final BlockModel source, final ModelBaker baker) {
        this.source = source;
        this.baker = baker;
    }

    @Override
    public UnbakedModel getUnbakedModel(final ResourceLocation unbakedModel) {
        return this.baker.getModel(unbakedModel);
    }

    @Override
    public Optional<Material> getMaterial(final String name) {
        if (this.source.hasTexture(name)) {
            return Optional.of(this.source.getMaterial(name));
        }

        return Optional.empty();
    }

    @Override
    public boolean isGui3d() {
        return this.source.getGuiLight().lightLikeBlock();
    }

    @Override
    public boolean useBlockLight() {
        return this.source.getGuiLight().lightLikeBlock();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.source.hasAmbientOcclusion();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.source.getTransforms();
    }
}
