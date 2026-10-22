package com.grim3212.assorted.lib.crafting;

import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.Map;
import java.util.function.Supplier;

/** A composter's odds as 26.3 has them: a component on the item naming one of vanilla's five chances. */
public final class CompostChances {

    private static final Map<Float, ResourceKey<ContextIntProvider>> CHANCES = Map.of(
            0.3F, ContextIntProviders.COMPOSTABLE_LOW,
            0.5F, ContextIntProviders.COMPOSTABLE_LOW_MEDIUM,
            0.65F, ContextIntProviders.COMPOSTABLE_MEDIUM,
            0.85F, ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH,
            1.0F, ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE);

    private CompostChances() {
    }

    /** Makes {@code item} compostable at {@code chance}, which has to be one of vanilla's: 0.3, 0.5, 0.65, 0.85 or 1. */
    public static void add(Supplier<? extends ItemLike> item, float chance) {
        ResourceKey<ContextIntProvider> layers = CHANCES.get(chance);
        if (layers == null) {
            throw new IllegalArgumentException("No vanilla compost chance of " + chance);
        }
        Services.PLATFORM.modifyDefaultComponents(item, components -> components.set(DataComponents.COMPOSTABLE, new Compostable(layers)));
    }
}
