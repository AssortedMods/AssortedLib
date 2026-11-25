package com.grim3212.assorted.lib.migration;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * An advancement's icon picked when advancements load: the first of a list that is registered. A family of mods ships
 * one root advancement between them, and its file cannot name an item only some of them add.
 */
public final class AdvancementIcons {

    // Concurrent: NeoForge constructs mods in parallel.
    private static final Map<ResourceLocation, Supplier<List<ResourceLocation>>> ICONS = new ConcurrentHashMap<>();

    private AdvancementIcons() {
    }

    /** Draws {@code advancement} with the first of {@code icons} that is registered; with none, its file's own icon. */
    public static void register(ResourceLocation advancement, List<ResourceLocation> icons) {
        List<ResourceLocation> fixed = List.copyOf(icons);
        register(advancement, () -> fixed);
    }

    /** As above, with the icons asked for each time advancements load, such as {@code Families#icons}. */
    public static void register(ResourceLocation advancement, Supplier<List<ResourceLocation>> icons) {
        ICONS.put(advancement, icons);
    }

    /** The advancement as loaded, with its registered icon swapped in when it has one and that icon exists. */
    public static Advancement apply(ResourceLocation id, Advancement advancement) {
        Supplier<List<ResourceLocation>> icons = ICONS.get(id);
        if (icons == null || advancement.display().isEmpty()) {
            return advancement;
        }

        Optional<ItemStack> icon = icons.get().stream()
                .flatMap(item -> BuiltInRegistries.ITEM.getOptional(item).stream())
                .findFirst()
                .map(ItemStack::new);
        if (icon.isEmpty()) {
            return advancement;
        }

        DisplayInfo display = advancement.display().get();
        DisplayInfo drawn = new DisplayInfo(icon.get(), display.getTitle(), display.getDescription(), display.getBackground(),
                display.getType(), display.shouldShowToast(), display.shouldAnnounceChat(), display.isHidden());
        return new Advancement(advancement.parent(), Optional.of(drawn), advancement.rewards(), advancement.criteria(),
                advancement.requirements(), advancement.sendsTelemetryEvent(), advancement.name());
    }
}
