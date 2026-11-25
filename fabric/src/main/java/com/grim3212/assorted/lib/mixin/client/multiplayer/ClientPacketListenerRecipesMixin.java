package com.grim3212.assorted.lib.mixin.client.multiplayer;

import com.grim3212.assorted.lib.client.manual.ManualClient;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mirrors the recipes the server sends into {@link SyncedRecipes}, so common code reads them the same
 * way on both loaders. Fabric API has no event for the recipe packet; NeoForge fires
 * {@code RecipesReceivedEvent} from the same place. Runs on the client thread, after vanilla has
 * filled its own recipe manager.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerRecipesMixin {

    @Inject(method = "handleUpdateRecipes", at = @At("TAIL"))
    private void assortedlib_mirrorSyncedRecipes(final ClientboundUpdateRecipesPacket packet, final CallbackInfo ci) {
        SyncedRecipes.set(packet.getRecipes());
        // Sent on join and again on every /reload, which is where a condition can start reading
        // differently.
        ManualClient.refreshConditions();
    }
}
