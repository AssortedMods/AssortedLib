package com.grim3212.assorted.lib.mixin.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.grim3212.assorted.lib.client.model.loader.FabricModelLoaders;
import com.grim3212.assorted.lib.client.model.loader.ISpecificationBlockModel;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

/**
 * Reads a model json's {@code "loader"} key, which vanilla ignores, while the json is at hand. Every
 * model json goes through this deserializer, so the specification travels on the {@link BlockModel}
 * vanilla builds and nothing has to read the file a second time.
 */
@Mixin(BlockModel.Deserializer.class)
public abstract class BlockModelDeserializerMixin {

    @Inject(
            method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockModel;",
            at = @At("RETURN")
    )
    private void assortedlib_readSpecification(final JsonElement element, final Type type, final JsonDeserializationContext context, final CallbackInfoReturnable<BlockModel> cir) {
        final IModelSpecification<?> specification = FabricModelLoaders.read(context, element.getAsJsonObject());
        if (specification != null) {
            ((ISpecificationBlockModel) cir.getReturnValue()).assortedlib$setSpecification(specification);
        }
    }
}
