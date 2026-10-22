package com.grim3212.assorted.lib.crafting;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootPredicates;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConditionalValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.Map;

/**
 * A fuel's burn time is an item component pointing at an int provider in a reloadable registry. Times vanilla
 * already has use its entry; any other is a mod's own, written by {@link #bootstrap} in vanilla's form.
 */
public final class BurnTimes {

    private static final Map<Integer, ResourceKey<ContextIntProvider>> VANILLA = Map.of(
            50, ContextIntProviders.COOKING_TIME_BAMBOO,
            100, ContextIntProviders.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL,
            150, ContextIntProviders.COOKING_TIME_WOOD_SLABS,
            200, ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE,
            300, ContextIntProviders.COOKING_TIME_WOOD_BLOCKS,
            800, ContextIntProviders.COOKING_TIME_HANGING_SIGNS,
            1200, ContextIntProviders.COOKING_TIME_BOATS,
            1600, ContextIntProviders.COOKING_TIME_COAL);

    private BurnTimes() {
    }

    /** The provider an item burning for {@code ticks} names in {@code Item.Properties#cookingFuel}. */
    public static ResourceKey<ContextIntProvider> key(String modId, int ticks) {
        ResourceKey<ContextIntProvider> vanilla = VANILLA.get(ticks);
        return vanilla != null ? vanilla : ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, Identifier.fromNamespaceAndPath(modId, "cooking/time_" + ticks));
    }

    /** Registers the mod's own entries among {@code ticks}, burning twice as fast in a smoker or blast furnace like vanilla's. */
    public static void bootstrap(BootstrapContext<ContextIntProvider> context, String modId, Iterable<Integer> ticks) {
        HolderGetter<LootItemCondition> predicates = context.lookup(Registries.PREDICATE);
        HolderGetter<ContextIntProvider> providers = context.lookup(Registries.CONTEXT_INT_PROVIDER);
        for (int time : ticks) {
            if (VANILLA.containsKey(time)) {
                continue;
            }
            ConditionalValue divisor = new ConditionalValue(predicates.getOrThrow(LootPredicates.FAST_FURNACE),
                    providers.getOrThrow(ContextIntProviders.COOKING_FAST_BURN_TIME_REDUCTION_FACTOR),
                    providers.getOrThrow(ContextIntProviders.COOKING_NORMAL_BURN_TIME_REDUCTION_FACTOR));
            context.register(key(modId, time), ContextIntProviders.div(ContextIntProviders.exactly(time), Holder.direct(divisor)).value());
        }
    }
}
