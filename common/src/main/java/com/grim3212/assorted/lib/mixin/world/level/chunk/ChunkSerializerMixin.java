package com.grim3212.assorted.lib.mixin.world.level.chunk;

import com.grim3212.assorted.lib.migration.MovedIds;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** A chunk's structures saved under an old id load under the new one, where vanilla would discard them; see {@link MovedIds}. */
@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {

    // The chunk saves under the new ids from then on.
    @Inject(method = "unpackStructureStart", at = @At("HEAD"))
    private static void assortedlib_carryOverMovedStarts(StructurePieceSerializationContext context, CompoundTag tag, long seed, CallbackInfoReturnable<Map<?, ?>> cir) {
        if (MovedIds.enabled() && tag.contains("starts", Tag.TAG_COMPOUND)) {
            Registry<Structure> structures = context.registryAccess().registryOrThrow(Registries.STRUCTURE);
            MovedIds.structures(tag.getCompound("starts"), structures::containsKey);
        }
    }

    @Inject(method = "unpackStructureReferences", at = @At("HEAD"))
    private static void assortedlib_carryOverMovedReferences(RegistryAccess registryAccess, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<Map<?, ?>> cir) {
        if (MovedIds.enabled() && tag.contains("References", Tag.TAG_COMPOUND)) {
            Registry<Structure> structures = registryAccess.registryOrThrow(Registries.STRUCTURE);
            MovedIds.structures(tag.getCompound("References"), structures::containsKey);
        }
    }
}
