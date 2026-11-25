package com.grim3212.assorted.lib.core.item;

import com.google.common.base.Suppliers;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * One data component type for a family of mods, however many of them are installed: whichever asks
 * first registers it under that id, and every mod gets the same type back.
 */
public final class SharedDataComponents {

    // Concurrent: NeoForge constructs mods in parallel.
    private static final Map<ResourceLocation, Supplier<? extends DataComponentType<?>>> TYPES = new ConcurrentHashMap<>();

    private static final Set<ResourceLocation> TOOLTIPS = ConcurrentHashMap.newKeySet();

    private SharedDataComponents() {
    }

    /** The type {@code id}; ask from common init, before items are registered. Later asks ignore {@code type}. */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<DataComponentType<T>> component(ResourceLocation id, Supplier<DataComponentType<T>> type) {
        return (Supplier<DataComponentType<T>>) TYPES.computeIfAbsent(id, created -> {
            Supplier<DataComponentType<T>> shared = Suppliers.memoize(type::get);
            Services.PLATFORM.registerDataComponentType(id, shared);
            return shared;
        });
    }

    /** As {@link #component}, with the type's tooltip lines shown once however many mods ask. */
    public static <T extends IStackTooltipProvider> Supplier<DataComponentType<T>> tooltipComponent(ResourceLocation id, Supplier<DataComponentType<T>> type) {
        Supplier<DataComponentType<T>> shared = component(id, type);
        if (TOOLTIPS.add(id)) {
            Services.PLATFORM.showComponentTooltip(shared);
        }
        return shared;
    }
}
