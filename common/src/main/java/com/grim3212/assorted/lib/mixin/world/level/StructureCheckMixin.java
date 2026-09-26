package com.grim3212.assorted.lib.mixin.world.level;

import com.grim3212.assorted.lib.migration.MovedIds;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla reads a saved chunk's structures here without loading the chunk, so moved ids carry over here too; see {@link MovedIds}. */
@Mixin(StructureCheck.class)
public abstract class StructureCheckMixin {

    @Shadow
    @Final
    private RegistryAccess registryAccess;

    @Inject(method = "loadStructures", at = @At("HEAD"))
    private void assortedlib_carryOverMovedStarts(CompoundTag chunkTag, CallbackInfoReturnable<?> cir) {
        if (MovedIds.enabled()) {
            chunkTag.getCompound("structures").flatMap(tag -> tag.getCompound("starts"))
                    .ifPresent(starts -> MovedIds.structures(starts, this.registryAccess.lookupOrThrow(Registries.STRUCTURE)::containsKey));
        }
    }
}
