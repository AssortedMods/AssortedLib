package com.grim3212.assorted.lib;

import com.grim3212.assorted.lib.client.manual.ManualClient;
import com.grim3212.assorted.lib.manual.ManualIngredientTags;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.grim3212.assorted.lib.platform.FabricNetworkHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class AssortedLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            FabricNetworkHelper.initializeClientHandlers();
        });

        ManualClient.init();

        // The recipes themselves are mirrored into SyncedRecipes by ClientPacketListenerRecipesMixin.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SyncedRecipes.clear();
            ManualIngredientTags.clear();
        });
    }
}
