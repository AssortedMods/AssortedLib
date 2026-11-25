package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.manual.ManualLinks;
import com.grim3212.assorted.lib.manual.ManualPageRef;
import com.grim3212.assorted.lib.manual.ManualRegistry;
import com.grim3212.assorted.lib.manual.ManualSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The manual as the loaded resource packs describe it. Replaced wholesale on a resource reload. */
public final class ManualContent {

    private static final ManualContent EMPTY =
            new ManualContent(ManualBookStyle.DEFAULT, Map.of(), Map.of(), Map.of(), ManualLinks.Loaded.EMPTY);

    private static volatile ManualContent current = EMPTY;

    private final ManualBookStyle style;
    private final Map<String, ManualSection> sections;
    private final Map<String, List<ManualChapter>> chapters;
    private final Map<ResourceLocation, ManualRecipeLayout> layouts;
    private final ManualLinks.Loaded links;

    /** Conditions already applied; see {@link #resolveConditions()}. */
    private volatile Map<String, List<ManualChapter>> resolved = Map.of();

    ManualContent(ManualBookStyle style, Map<String, ManualSection> sections,
                  Map<String, List<ManualChapter>> chapters, Map<ResourceLocation, ManualRecipeLayout> layouts,
                  ManualLinks.Loaded links) {
        this.style = style;
        this.sections = sections;
        this.chapters = chapters;
        this.layouts = layouts;
        this.links = links;
    }

    public static ManualContent get() {
        return current;
    }

    static void set(ManualContent content) {
        content.resolveConditions();
        current = content;
    }

    /**
     * Applies every chapter and page condition, once. Called on load, on join and on {@code /reload},
     * which is where a condition can start reading differently; the book holds the answer in between
     * rather than testing while it draws.
     */
    public void resolveConditions() {
        Map<String, List<ManualChapter>> out = new LinkedHashMap<>();
        this.chapters.forEach((section, all) -> {
            List<ManualChapter> visible = ManualChapter.resolve(all);
            if (!visible.isEmpty()) {
                out.put(section, visible);
            }
        });

        this.resolved = Map.copyOf(out);
    }

    /** Re-applies the conditions of whatever is loaded now. */
    public static void refresh() {
        ManualRecipeLayout.Extra.clearDecoded();
        current.resolveConditions();
    }

    public ManualBookStyle style() {
        return this.style;
    }

    public ManualLinks.Loaded links() {
        return this.links;
    }

    /**
     * Sections with at least one chapter, in index order. A {@code section.json} overrides a
     * {@link ManualRegistry} entry of the same namespace.
     */
    public List<ManualSection> sections() {
        Map<String, ManualSection> merged = new LinkedHashMap<>();
        ManualRegistry.sections().forEach(section -> merged.put(section.modId(), section));
        merged.putAll(this.sections);

        return merged.values().stream()
                .filter(section -> !this.chaptersOf(section.modId()).isEmpty())
                .sorted(Comparator.comparingInt(ManualSection::sortOrder).thenComparing(ManualSection::modId))
                .toList();
    }

    public Optional<ManualSection> section(String modId) {
        return this.sections.containsKey(modId)
                ? Optional.of(this.sections.get(modId))
                : ManualRegistry.section(modId);
    }

    /** A section with none of these drops out of the index, taking a switched-off mod with it. */
    public List<ManualChapter> chaptersOf(String section) {
        return this.resolved.getOrDefault(section, List.of());
    }

    /** A loop rather than a stream: the HUD asks this every frame while the manual is in hand. */
    public Optional<ManualChapter> chapter(String section, String id) {
        for (ManualChapter chapter : this.chaptersOf(section)) {
            if (chapter.id().equals(id)) {
                return Optional.of(chapter);
            }
        }

        return Optional.empty();
    }

    /** Where a reference lands, or empty when nothing loaded matches it. */
    public Optional<Location> locate(ManualPageRef ref) {
        return this.chapter(ref.section(), ref.chapter()).map(chapter -> new Location(chapter, chapter.indexOfPage(ref.page())));
    }

    /** Null when no layout is registered for the type; see {@link #layoutFor} for the fallback. */
    @Nullable
    public ManualRecipeLayout layout(RecipeType<?> type) {
        return this.layouts.get(typeId(type));
    }

    @Nullable
    public ManualRecipeLayout layoutFor(Recipe<?> recipe) {
        ManualRecipeLayout layout = this.layout(recipe.getType());
        return layout != null ? layout : this.layouts.get(ResourceLocation.withDefaultNamespace("crafting"));
    }

    /** {@code RecipeType} carries no id of its own; the registry is the only place it is written. */
    private static ResourceLocation typeId(RecipeType<?> type) {
        return net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(type);
    }

    public boolean isEmpty() {
        return this.chapters.isEmpty();
    }

    public record Location(ManualChapter chapter, int pageIndex) {
    }

    /** Filled by {@link ManualLoader}, then frozen. */
    static final class Builder {

        ManualBookStyle style = ManualBookStyle.DEFAULT;
        final Map<String, ManualSection> sections = new LinkedHashMap<>();
        final Map<String, List<ManualChapter>> chapters = new LinkedHashMap<>();
        final Map<ResourceLocation, ManualRecipeLayout> layouts = new LinkedHashMap<>();
        final List<ManualLinks.Group> links = new ArrayList<>();

        void addChapter(ManualChapter chapter) {
            this.chapters.computeIfAbsent(chapter.section(), key -> new ArrayList<>()).add(chapter);
        }

        ManualContent build() {
            this.chapters.replaceAll((section, list) -> list.stream()
                    .sorted(Comparator.comparingInt(ManualChapter::sortOrder).thenComparing(ManualChapter::id))
                    .toList());

            return new ManualContent(this.style, Map.copyOf(this.sections), Map.copyOf(this.chapters),
                    Map.copyOf(this.layouts), ManualLinks.Loaded.of(this.links));
        }
    }
}
