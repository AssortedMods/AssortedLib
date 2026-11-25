package com.grim3212.assorted.lib.crafting;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The recipes the server sent this client. Every recipe is sent on login and after {@code /reload}; each
 * loader's client hands them over here. Cleared on disconnect: they belong to the server that sent them.
 */
public final class SyncedRecipes {

    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile Map<ResourceLocation, RecipeHolder<?>> recipes = Map.of();

    private SyncedRecipes() {
    }

    /** Called by each loader's client whenever the server's recipes arrive. */
    public static void set(Collection<RecipeHolder<?>> received) {
        Map<ResourceLocation, RecipeHolder<?>> byId = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : received) {
            byId.put(holder.id(), holder);
        }
        recipes = Collections.unmodifiableMap(byId);

        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    public static void clear() {
        set(List.of());
    }

    public static Collection<RecipeHolder<?>> recipes() {
        return recipes.values();
    }

    @SuppressWarnings("unchecked")
    public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> type) {
        // The holder's recipe is of type T whenever its type is, which is what RecipeManager relies on too.
        return recipes.values().stream().filter(holder -> holder.value().getType() == type).map(holder -> (RecipeHolder<T>) holder).toList();
    }

    @Nullable
    public static RecipeHolder<?> byKey(ResourceLocation id) {
        return recipes.get(id);
    }

    @Nullable
    public static RecipeHolder<?> byKey(ResourceKey<Recipe<?>> id) {
        return byKey(id.location());
    }

    /** Runs on the client thread whenever a new set arrives, including the empty set on disconnect. */
    public static void addUpdateListener(Runnable listener) {
        LISTENERS.add(listener);
    }
}
