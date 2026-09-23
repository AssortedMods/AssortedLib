package com.grim3212.assorted.lib;

import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.spawn.CreatureSpawner;
import com.grim3212.assorted.lib.spawn.SpawnHabits;
import net.minecraft.resources.Identifier;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.grim3212.assorted.lib.manual.ManualInteractions;
import com.grim3212.assorted.lib.manual.LibItems;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;


/** The library's own content, as opposed to its services. Called from both loaders' entry points. */
public class LibCommonSetup {

    public static void init() {
        LibItems.init();

        ManualInteractions.register();
        requireVanillaRecipes();

        // Spawn habits: every mod's data/<ns>/spawn_habit/*.json, driven by one spawner per level.
        Services.PLATFORM.addReloadListener(SpawnHabits.ID, new SpawnHabits());
        Services.WORLD_GEN.addCustomSpawner(Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "creatures"), CreatureSpawner::new);
    }

    /** Recipe pages and JEI need whole recipes on the client, so these are always synced. */
    private static void requireVanillaRecipes() {
        SyncedRecipes.require(() -> RecipeType.CRAFTING, ShapedRecipe.SERIALIZER, ShapelessRecipe.SERIALIZER);
        SyncedRecipes.require(() -> RecipeType.SMELTING, SmeltingRecipe.SERIALIZER);
        SyncedRecipes.require(() -> RecipeType.BLASTING, BlastingRecipe.SERIALIZER);
        SyncedRecipes.require(() -> RecipeType.SMOKING, SmokingRecipe.SERIALIZER);
        SyncedRecipes.require(() -> RecipeType.CAMPFIRE_COOKING, CampfireCookingRecipe.SERIALIZER);
        SyncedRecipes.require(() -> RecipeType.STONECUTTING, StonecutterRecipe.SERIALIZER);
    }
}
