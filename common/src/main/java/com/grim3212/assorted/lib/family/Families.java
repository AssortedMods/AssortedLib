package com.grim3212.assorted.lib.family;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.conditions.PartToggles;
import com.grim3212.assorted.lib.core.creative.SharedCreativeTabs;
import com.grim3212.assorted.lib.manual.ManualRegistry;
import com.grim3212.assorted.lib.manual.ManualSection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groups of mods that act as one: separately installable parts sharing a creative tab and a manual section, each
 * switchable in one config file. See {@link #join} for everything joining does.
 */
public final class Families {

    // Concurrent: NeoForge constructs mods in parallel.
    private static final Map<String, Map<String, FamilyMember>> FAMILIES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> MANUAL_ORDERS = new ConcurrentHashMap<>();

    private Families() {
    }

    /**
     * Puts {@code modId} in {@code familyId} while the mod is constructed: a switch in {@code config/<familyId>-parts.toml}
     * and the family's manual section. It makes no tab and aliases nothing; the README's Families section lists it all.
     */
    public static FamilyMember join(String modId, String familyId) {
        FamilyMember member = new FamilyMember(modId, familyId);
        if (FAMILIES.computeIfAbsent(familyId, family -> new ConcurrentHashMap<>()).putIfAbsent(modId, member) != null) {
            throw new IllegalArgumentException(modId + " already joined " + familyId);
        }

        FamilySwitches.add(modId, familyId);
        PartToggles.register(modId, () -> FamilySwitches.isEnabled(familyId, modId));
        registerSection(familyId);
        return member;
    }

    /** The family {@code modId} joined, if any. */
    public static Optional<String> familyOf(String modId) {
        return FAMILIES.entrySet().stream().filter(family -> family.getValue().containsKey(modId)).map(Map.Entry::getKey).findFirst();
    }

    /** The mods that joined {@code familyId}, in mod id order. */
    public static List<String> members(String familyId) {
        return FAMILIES.getOrDefault(familyId, Map.of()).keySet().stream().sorted().toList();
    }

    /** The family's icon candidates, best first: offered by members that are on, by weight and then mod id. */
    public static List<Identifier> icons(String familyId) {
        return FAMILIES.getOrDefault(familyId, Map.of()).values().stream()
                .filter(member -> member.icon() != null && PartToggles.isEnabled(member.modId()))
                .sorted(Comparator.comparingInt(FamilyMember::iconWeight).reversed().thenComparing(FamilyMember::modId))
                .map(FamilyMember::icon)
                .toList();
    }

    /** The first registered of {@link #icons}, or a book. Build it when drawing, not while mods load. */
    public static ItemStack icon(String familyId) {
        return icons(familyId).stream()
                .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id).stream())
                .findFirst()
                .map(ItemStack::new)
                .orElseGet(() -> new ItemStack(Items.BOOK));
    }

    /** The family's creative tab, {@code <familyId>:tab}, drawn with its icon. Each member fills in its own items. */
    public static ResourceKey<CreativeModeTab> tab(String familyId) {
        return SharedCreativeTabs.tab(Identifier.fromNamespaceAndPath(familyId, "tab"), () -> icons(familyId));
    }

    static void manualOrder(FamilyMember member, int order) {
        Integer previous = MANUAL_ORDERS.putIfAbsent(member.familyId(), order);
        if (previous != null && previous != order) {
            LibConstants.LOG.warn("{} gives {}'s manual order as {}, but another member gave {}; keeping {}", member.modId(), member.familyId(), order, previous, previous);
        }
        registerSection(member.familyId());
    }

    private static void registerSection(String familyId) {
        int order = MANUAL_ORDERS.getOrDefault(familyId, ManualSection.DEFAULT_SORT_ORDER);
        ManualRegistry.register(new ManualSection(familyId, order, () -> icon(familyId)));
    }
}
