package com.grim3212.assorted.lib.mixin.stats;

import com.grim3212.assorted.lib.migration.MovedIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

/** A recipe saved under an old id is loaded under its new one, where vanilla would drop it as unrecognized; see {@link MovedIds}. */
@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {

    @Shadow
    public abstract void loadUntrusted(ServerRecipeBook.Packed packed, Predicate<ResourceKey<Recipe<?>>> validator);

    // Loads the corrected book instead; nothing is left to move the second time through.
    @Inject(method = "loadUntrusted", at = @At("HEAD"), cancellable = true)
    private void assortedlib_carryOverMovedRecipes(ServerRecipeBook.Packed packed, Predicate<ResourceKey<Recipe<?>>> validator, CallbackInfo ci) {
        if (!MovedIds.enabled()) {
            return;
        }

        List<ResourceKey<Recipe<?>>> known = MovedIds.recipes(packed.known(), validator);
        List<ResourceKey<Recipe<?>>> highlight = MovedIds.recipes(packed.highlight(), validator);
        if (known != packed.known() || highlight != packed.highlight()) {
            this.loadUntrusted(new ServerRecipeBook.Packed(packed.settings(), known, highlight), validator);
            ci.cancel();
        }
    }
}
