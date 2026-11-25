package com.grim3212.assorted.lib.mixin.client.model;

import com.grim3212.assorted.lib.client.model.loader.FabricModelBakingContextDelegate;
import com.grim3212.assorted.lib.client.model.loader.ISpecificationBlockModel;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.platform.ClientServices;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.function.Function;

/**
 * Bakes a {@link BlockModel} whose json, or a parent's, named one of our loaders through its
 * {@link IModelSpecification}, the way NeoForge bakes custom geometry: an item model whose parent is
 * a loader model bakes through the parent's specification, with its own textures and transforms.
 * The models a specification reaches through the baker are added to the model's dependencies, so the
 * bakery loads them, and their parents are resolved with the model's own; vanilla only resolves the
 * top level models' parents.
 */
@Mixin(BlockModel.class)
public abstract class BlockModelSpecificationMixin implements ISpecificationBlockModel {

    @Shadow
    @Nullable
    protected BlockModel parent;

    @Unique
    @Nullable
    private IModelSpecification<?> assortedlib$specification;

    @Shadow
    protected abstract ItemOverrides getItemOverrides(ModelBaker baker, BlockModel model);

    @Override
    public @Nullable IModelSpecification<?> assortedlib$getSpecification() {
        return this.assortedlib$specification;
    }

    @Override
    public void assortedlib$setSpecification(@Nullable final IModelSpecification<?> specification) {
        this.assortedlib$specification = specification;
    }

    @Unique
    @Nullable
    private static BlockModel assortedlib$parentOf(final BlockModel model) {
        return ((BlockModelSpecificationMixin) (Object) model).parent;
    }

    /** The nearest specification up the parent chain, this model's own first. */
    @Unique
    @Nullable
    private IModelSpecification<?> assortedlib$inheritedSpecification() {
        for (BlockModel model = (BlockModel) (Object) this; model != null; model = assortedlib$parentOf(model)) {
            final IModelSpecification<?> specification = ((ISpecificationBlockModel) model).assortedlib$getSpecification();
            if (specification != null) {
                return specification;
            }
        }
        return null;
    }

    @Inject(method = "getDependencies", at = @At("RETURN"))
    private void assortedlib_addSpecificationDependencies(final CallbackInfoReturnable<Collection<ResourceLocation>> cir) {
        if (this.assortedlib$specification != null) {
            cir.getReturnValue().addAll(this.assortedlib$specification.getDependencies());
        }
    }

    @Inject(method = "resolveParents", at = @At("TAIL"))
    private void assortedlib_resolveSpecificationDependencies(final Function<ResourceLocation, UnbakedModel> resolver, final CallbackInfo ci) {
        for (BlockModel model = (BlockModel) (Object) this; model != null; model = assortedlib$parentOf(model)) {
            final IModelSpecification<?> specification = ((ISpecificationBlockModel) model).assortedlib$getSpecification();
            if (specification == null) {
                continue;
            }

            for (final ResourceLocation dependency : specification.getDependencies()) {
                final UnbakedModel unbaked = resolver.apply(dependency);
                if (unbaked != null && unbaked != (Object) this) {
                    unbaked.resolveParents(resolver);
                }
            }
        }
    }

    @Inject(
            method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void assortedlib_bakeSpecification(final ModelBaker baker, final Function<Material, TextureAtlasSprite> spriteGetter, final ModelState modelState, final CallbackInfoReturnable<BakedModel> cir) {
        final IModelSpecification<?> specification = this.assortedlib$inheritedSpecification();
        if (specification == null) {
            return;
        }

        final BlockModel self = (BlockModel) (Object) this;
        final FabricModelBakingContextDelegate context = new FabricModelBakingContextDelegate(self, baker);
        final BakedModel baked = specification.bake(context, baker, spriteGetter, modelState, this.getItemOverrides(baker, self));
        cir.setReturnValue(ClientServices.MODELS.adaptToPlatform(baked));
    }
}
