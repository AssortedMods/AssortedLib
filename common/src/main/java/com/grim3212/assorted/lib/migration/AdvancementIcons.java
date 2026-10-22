package com.grim3212.assorted.lib.migration;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;

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
    private static final Map<Identifier, Supplier<List<Identifier>>> ICONS = new ConcurrentHashMap<>();

    private AdvancementIcons() {
    }

    /** Draws {@code advancement} with the first of {@code icons} that is registered; with none, its file's own icon. */
    public static void register(Identifier advancement, List<Identifier> icons) {
        List<Identifier> fixed = List.copyOf(icons);
        register(advancement, () -> fixed);
    }

    /** As above, with the icons asked for each time advancements load, such as {@code Families#icons}. */
    public static void register(Identifier advancement, Supplier<List<Identifier>> icons) {
        ICONS.put(advancement, icons);
    }

    /** The advancement as loaded, with its registered icon swapped in when it has one and that icon exists. */
    public static Advancement apply(Identifier id, Advancement advancement) {
        Supplier<List<Identifier>> icons = ICONS.get(id);
        if (icons == null || advancement.display().isEmpty()) {
            return advancement;
        }

        Optional<ItemStackTemplate> icon = icons.get().stream()
                .flatMap(item -> BuiltInRegistries.ITEM.getOptional(item).stream())
                .findFirst()
                .map(ItemStackTemplate::new);
        if (icon.isEmpty()) {
            return advancement;
        }

        DisplayInfo display = advancement.display().get();
        DisplayInfo drawn = new DisplayInfo(icon.get(), display.title(), display.description(), display.background(),
                display.type(), display.showToast(), display.announceToChat(), display.hidden());
        return new Advancement(advancement.parent(), Optional.of(drawn), advancement.rewards(), advancement.criteria(),
                advancement.requirements(), advancement.sendsTelemetryEvent(), advancement.name());
    }
}
