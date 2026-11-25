package com.grim3212.assorted.lib.data;

import net.minecraft.world.item.DyeColor;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Comparator;
import java.util.Arrays;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * A mod's en_us.json, generated with its client assets. {@link #addNames()} adds every other key
 * and every name that is not the id in title case; anything registered and still unnamed afterwards
 * gets its id in title case ({@code deepslate_tin_ore} becomes "Deepslate Tin Ore").
 */
public abstract class LibLanguageProvider extends LanguageProvider {

    private final String modId;
    private final Set<String> added = new HashSet<>();

    protected LibLanguageProvider(PackOutput output, String modId) {
        super(output, modId, "en_us");
        this.modId = modId;
    }

    /** The keys that are not plain id names; see the class comment. */
    protected abstract void addNames();

    @Override
    protected final void addTranslations() {
        this.addNames();
        this.addDefaultNames(BuiltInRegistries.BLOCK, block -> block.getDescriptionId());
        this.addDefaultNames(BuiltInRegistries.ITEM, item -> item.getDescriptionId());
        this.addDefaultNames(BuiltInRegistries.ENTITY_TYPE, type -> type.getDescriptionId());
    }

    @Override
    public void add(String key, String value) {
        super.add(key, value);
        this.added.add(key);
    }

    /**
     * Names every block of this mod that is still unnamed and whose id matches {@code pattern}, as
     * {@code name} makes it from the match, e.g. {@code nameBlocks("(.+)_block", m -> "Block of " +
     * titleCase(m.group(1)))}. Explicit names added before it win.
     */
    protected void nameBlocks(String pattern, Function<Matcher, String> name) {
        this.nameMatching(BuiltInRegistries.BLOCK, block -> block.getDescriptionId(), pattern, name);
    }

    /** As {@link #nameBlocks}, for the items of this mod. */
    protected void nameItems(String pattern, Function<Matcher, String> name) {
        this.nameMatching(BuiltInRegistries.ITEM, item -> item.getDescriptionId(), pattern, name);
    }

    /** A regex group matching any dye colour id, longest first so light_blue is not read as blue. */
    protected static String dyeColors() {
        return "(" + Arrays.stream(DyeColor.values()).map(DyeColor::getName)
                .sorted(Comparator.comparingInt(String::length).reversed()).collect(Collectors.joining("|")) + ")";
    }

    private <T> void addDefaultNames(Registry<T> registry, Function<T, String> descriptionId) {
        this.nameMatching(registry, descriptionId, ".+", matcher -> titleCase(matcher.group()));
    }

    private <T> void nameMatching(Registry<T> registry, Function<T, String> descriptionId, String pattern, Function<Matcher, String> name) {
        Pattern compiled = Pattern.compile(pattern);
        for (T entry : registry) {
            ResourceLocation id = registry.getKey(entry);
            String key = descriptionId.apply(entry);
            if (id == null || !this.modId.equals(id.getNamespace()) || this.added.contains(key)) {
                continue;
            }
            Matcher matcher = compiled.matcher(id.getPath());
            if (matcher.matches()) {
                this.add(key, name.apply(matcher));
            }
        }
    }

    /** {@code light_blue_wool} as "Light Blue Wool". */
    protected static String titleCase(String id) {
        StringBuilder name = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }
}
