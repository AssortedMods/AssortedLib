package com.grim3212.assorted.lib.mixin.server;

import com.grim3212.assorted.lib.migration.MovedIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A loot table asked for by an old id, as by a chest left unopened since before the move, gives the new one's loot; see {@link MovedIds}. */
@Mixin(ReloadableServerRegistries.Holder.class)
public abstract class ReloadableServerRegistriesHolderMixin {

    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Inject(method = "getLootTable", at = @At("HEAD"), cancellable = true)
    private void assortedlib_carryOverMovedLootTables(ResourceKey<LootTable> id, CallbackInfoReturnable<LootTable> cir) {
        if (!MovedIds.enabled()) {
            return;
        }

        this.registries.lookup(Registries.LOOT_TABLE).ifPresent(tables -> MovedIds.resolve(id.identifier(), to -> tables.get(ResourceKey.create(Registries.LOOT_TABLE, to)).isPresent())
                .flatMap(to -> tables.get(ResourceKey.create(Registries.LOOT_TABLE, to)))
                .ifPresent(table -> cir.setReturnValue(table.value())));
    }
}
