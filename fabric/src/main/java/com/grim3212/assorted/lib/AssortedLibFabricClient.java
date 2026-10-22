package com.grim3212.assorted.lib;

import com.grim3212.assorted.lib.client.manual.ManualClient;
import com.grim3212.assorted.lib.client.model.FabricUnbakedModelTracker;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.grim3212.assorted.lib.client.model.block.LibBlockStateModels;
import com.grim3212.assorted.lib.client.model.loader.FabricSpecificationBlockStateModel;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import com.grim3212.assorted.lib.platform.FabricNetworkHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.Collection;

public class AssortedLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            FabricNetworkHelper.initializeClientHandlers();
        });

        ManualClient.init();

        FabricUnbakedModelTracker.register();

        // Fabric puts the synced recipes into the client's own RecipeAccess; this mirrors them into
        // the shared cache so common code reads them the same way it does on NeoForge, which has no
        // such access.
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> {
            SyncedRecipes.set(RecipeMap.create(lookupOf(recipes.recipes())));
            // Sent on join and again on every /reload, which is where a condition can start reading
            // differently.
            ManualClient.refreshConditions();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SyncedRecipes.clear());

        // Must match the id ForgeSpecificationBlockStateModel is registered under: the blockstate
        // jsons naming it are generated once and shipped to both loaders.
        CustomUnbakedBlockStateModel.register(LibBlockStateModels.SPECIFICATION, FabricSpecificationBlockStateModel.MAP_CODEC);
    }

    /** RecipeMap is built from a registry lookup since recipes became registry entries; Fabric hands over the holders. */
    private static HolderLookup<Recipe<?>> lookupOf(Collection<RecipeHolder<?>> recipes) {
        MappedRegistry<Recipe<?>> registry = new MappedRegistry<>(Registries.RECIPE, Lifecycle.stable());
        recipes.forEach(recipe -> registry.register(recipe.id(), recipe.value(), RegistrationInfo.BUILT_IN));
        return registry.freeze();
    }
}
