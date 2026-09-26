package com.grim3212.assorted.lib.mixin.world.level.chunk;

import com.grim3212.assorted.lib.migration.MovedIds;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** A chunk's structures saved under an old id load under the new one, where vanilla would discard them; see {@link MovedIds}. */
@Mixin(SerializableChunkData.class)
public abstract class SerializableChunkDataMixin {

    // The chunk saves under the new ids from then on.
    @Inject(method = "unpackStructureStart", at = @At("HEAD"))
    private static void assortedlib_carryOverMovedStarts(StructurePieceSerializationContext context, CompoundTag tag, long seed, CallbackInfoReturnable<Map<?, ?>> cir) {
        if (MovedIds.enabled()) {
            tag.getCompound("starts").ifPresent(starts -> MovedIds.structures(starts, context.registryAccess().lookupOrThrow(Registries.STRUCTURE)::containsKey));
        }
    }

    @Inject(method = "unpackStructureReferences", at = @At("HEAD"))
    private static void assortedlib_carryOverMovedReferences(RegistryAccess registryAccess, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<Map<?, ?>> cir) {
        if (MovedIds.enabled()) {
            tag.getCompound("References").ifPresent(references -> MovedIds.structures(references, registryAccess.lookupOrThrow(Registries.STRUCTURE)::containsKey));
        }
    }
}
