package com.grim3212.assorted.lib.client.manual;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.manual.ManualLinks;
import com.grim3212.assorted.lib.manual.ManualSection;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.util.Map;

/**
 * Reads the whole manual out of the loaded resource packs, so a pack can change any of it.
 *
 * <pre>
 * assets/assortedlib/manual/book.json                  the book's own look, overriding the defaults
 * assets/&lt;mod&gt;/manual/section.json                 that mod's place in the index
 * assets/&lt;mod&gt;/manual/links.json                   what right clicking its content opens
 * assets/&lt;mod&gt;/manual/chapters/&lt;id&gt;.json      a chapter of it
 * assets/&lt;ns&gt;/manual/recipe_layouts/&lt;path&gt;.json  how &lt;ns&gt;:&lt;path&gt; recipes are drawn
 * </pre>
 */
public class ManualLoader extends SimplePreparableReloadListener<ManualContent> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "manual");

    /** One file, not one per namespace: there is only one book. */
    private static final Identifier BOOK = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "manual/book.json");

    private static final String SECTION = "manual/section.json";
    private static final String LINKS = "manual/links.json";
    private static final String CHAPTERS = "manual/chapters";
    private static final String RECIPE_LAYOUTS = "manual/recipe_layouts";
    private static final String EXTENSION = ".json";

    @Override
    protected ManualContent prepare(ResourceManager manager, ProfilerFiller profiler) {
        ManualContent.Builder builder = new ManualContent.Builder();

        manager.getResource(BOOK).ifPresent(resource ->
                read(BOOK, resource, ManualBookStyle.CODEC).ifPresent(style -> builder.style = style));

        for (String namespace : manager.getNamespaces()) {
            Identifier section = Identifier.fromNamespaceAndPath(namespace, SECTION);
            manager.getResource(section).ifPresent(resource ->
                    read(section, resource, ManualSection.Definition.CODEC)
                            .ifPresent(definition -> builder.sections.put(namespace, definition.bind(namespace))));

            Identifier links = Identifier.fromNamespaceAndPath(namespace, LINKS);
            manager.getResource(links).ifPresent(resource ->
                    read(links, resource, ManualLinks.Group.FILE_CODEC).ifPresent(builder.links::addAll));
        }

        forEach(manager, CHAPTERS, (path, id, resource) ->
                read(path, resource, ManualChapter.Definition.CODEC)
                        .ifPresent(definition -> builder.addChapter(definition.bind(path.getNamespace(), id))));

        forEach(manager, RECIPE_LAYOUTS, (path, id, resource) ->
                read(path, resource, ManualRecipeLayout.CODEC)
                        .ifPresent(layout -> builder.layouts.put(Identifier.fromNamespaceAndPath(path.getNamespace(), id), layout)));

        return builder.build();
    }

    @Override
    protected void apply(ManualContent preparations, ResourceManager manager, ProfilerFiller profiler) {
        ManualContent.set(preparations);
        ManualLinks.setLoaded(preparations.links());
        LibConstants.LOG.debug("Loaded the manual: {} section(s)", preparations.sections().size());
    }

    /** Ids are the path below {@code directory} less the extension, slashes and all. */
    private static void forEach(ResourceManager manager, String directory, Entry out) {
        ResourceManager.Selector isJson = path -> path.getPath().endsWith(EXTENSION);

        for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, isJson).entrySet()) {
            Identifier path = entry.getKey();
            String id = path.getPath().substring(directory.length() + 1, path.getPath().length() - EXTENSION.length());
            out.accept(path, id, entry.getValue());
        }
    }

    /** One bad file costs that file, not the whole book. */
    private static <T> java.util.Optional<T> read(Identifier path, Resource resource, Codec<T> codec) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            return java.util.Optional.of(codec.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalStateException::new));
        } catch (Exception e) {
            LibConstants.LOG.error("Failed to load manual file {}", path, e);
            return java.util.Optional.empty();
        }
    }

    @FunctionalInterface
    private interface Entry {
        void accept(Identifier path, String id, Resource resource);
    }
}
