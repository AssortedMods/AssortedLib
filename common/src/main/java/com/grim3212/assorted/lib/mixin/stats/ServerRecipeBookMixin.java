package com.grim3212.assorted.lib.mixin.stats;

import com.grim3212.assorted.lib.migration.MovedIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/** A recipe saved under an old id is loaded under its new one, where vanilla would drop it as unrecognized; see {@link MovedIds}. */
@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {

    // Both the known and the highlighted recipes are read through loadRecipes, one id at a time.
    @Redirect(method = "loadRecipes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;byKey(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"))
    private Optional<RecipeHolder<?>> assortedlib_carryOverMovedRecipes(RecipeManager recipes, ResourceLocation id) {
        Optional<RecipeHolder<?>> found = recipes.byKey(id);
        if (found.isPresent() || !MovedIds.enabled()) {
            return found;
        }

        return MovedIds.resolve(id, moved -> recipes.byKey(moved).isPresent()).flatMap(recipes::byKey);
    }
}
