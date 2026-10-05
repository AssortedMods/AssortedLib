package com.grim3212.assorted.lib.mixin.resources;

import com.grim3212.assorted.lib.migration.MovedIds;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * A data-driven entry saved under an old id, such as an enchantment on an item, is read as its new one, where
 * vanilla would drop it as unknown; see {@link MovedIds}. Registry aliases only reach built-in registries.
 */
@Mixin(RegistryFixedCodec.class)
public abstract class RegistryFixedCodecMixin<E> {

    @Shadow
    @Final
    private ResourceKey<? extends Registry<E>> registryKey;

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private <T> void assortedlib_carryOverMovedIds(DynamicOps<T> ops, T input, CallbackInfoReturnable<DataResult<Pair<Holder<E>, T>>> cir) {
        // Every holder read passes through here, so leave at once unless a mod has moved something.
        if (!MovedIds.anyMoved() || !(ops instanceof RegistryOps<?> registryOps)) {
            return;
        }

        Optional<Pair<Identifier, T>> decoded = Identifier.CODEC.decode(ops, input).result();
        if (decoded.isEmpty() || !MovedIds.moved(decoded.get().getFirst().getNamespace()) || !MovedIds.enabled()) {
            return;
        }

        Optional<HolderGetter<E>> lookup = registryOps.getter(this.registryKey);
        if (lookup.isEmpty()) {
            return;
        }

        HolderGetter<E> getter = lookup.get();
        MovedIds.resolve(decoded.get().getFirst(), id -> getter.get(ResourceKey.create(this.registryKey, id)).isPresent())
                .flatMap(to -> getter.get(ResourceKey.create(this.registryKey, to)))
                .ifPresent(holder -> cir.setReturnValue(DataResult.success(Pair.of(holder, decoded.get().getSecond()))));
    }
}
