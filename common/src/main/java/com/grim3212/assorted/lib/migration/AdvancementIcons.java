package com.grim3212.assorted.lib.migration;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An advancement's icon picked when advancements load: the first of a list that is registered. A family of mods ships
 * one root advancement between them, and its file cannot name an item only some of them add.
 */
public final class AdvancementIcons {

    // Concurrent: NeoForge constructs mods in parallel.
    private static final Map<Identifier, List<Identifier>> ICONS = new ConcurrentHashMap<>();

    private AdvancementIcons() {
    }

    /** Draws {@code advancement} with the first of {@code icons} that is registered; with none, its file's own icon. */
    public static void register(Identifier advancement, List<Identifier> icons) {
        ICONS.put(advancement, List.copyOf(icons));
    }

    /** The advancements as loaded, with every registered icon swapped in. */
    public static Map<Identifier, Advancement> apply(Map<Identifier, Advancement> advancements) {
        if (ICONS.isEmpty()) {
            return advancements;
        }

        Map<Identifier, Advancement> changed = new HashMap<>(advancements);
        ICONS.forEach((id, icons) -> {
            Advancement advancement = advancements.get(id);
            Optional<ItemStackTemplate> icon = icons.stream()
                    .flatMap(item -> BuiltInRegistries.ITEM.getOptional(item).stream())
                    .findFirst()
                    .map(ItemStackTemplate::new);
            if (advancement != null && advancement.display().isPresent() && icon.isPresent()) {
                DisplayInfo display = advancement.display().get();
                DisplayInfo drawn = new DisplayInfo(icon.get(), display.getTitle(), display.getDescription(), display.getBackground(),
                        display.getType(), display.shouldShowToast(), display.shouldAnnounceChat(), display.isHidden());
                changed.put(id, new Advancement(advancement.parent(), Optional.of(drawn), advancement.rewards(), advancement.criteria(),
                        advancement.requirements(), advancement.sendsTelemetryEvent(), advancement.name()));
            }
        });
        return changed;
    }
}
