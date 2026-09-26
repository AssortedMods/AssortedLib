package com.grim3212.assorted.lib.core.creative;

import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * One creative tab for a family of mods, however many of them are installed: whichever asks first
 * registers it, and each fills in its own items.
 */
public final class SharedCreativeTabs {

    // Concurrent: NeoForge constructs mods in parallel.
    private static final Map<ResourceKey<CreativeModeTab>, List<Map.Entry<Integer, Supplier<List<ItemStack>>>>> CONTENTS = new ConcurrentHashMap<>();

    private SharedCreativeTabs() {
    }

    /**
     * The tab {@code id}, titled by {@code itemGroup.<namespace>}. Its icon is the first of {@code icons}
     * that is registered, so every mod of the family passes the same list.
     */
    public static ResourceKey<CreativeModeTab> tab(Identifier id, List<Identifier> icons) {
        ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
        CONTENTS.computeIfAbsent(key, created -> {
            Services.PLATFORM.registerCreativeTab(id, () -> build(id, List.copyOf(icons)));
            Services.PLATFORM.modifyCreativeTab(created, () -> stacks(created));
            return new CopyOnWriteArrayList<>();
        });
        return key;
    }

    /** Adds to a tab from {@link #tab}; lower {@code order} comes first, whatever order the mods load in. */
    public static void add(ResourceKey<CreativeModeTab> tab, int order, Supplier<List<ItemStack>> items) {
        List<Map.Entry<Integer, Supplier<List<ItemStack>>>> contents = CONTENTS.get(tab);
        if (contents == null) {
            throw new IllegalArgumentException("No shared creative tab " + tab.identifier() + "; ask for it with SharedCreativeTabs.tab first");
        }

        contents.add(Map.entry(order, items));
    }

    private static List<ItemStack> stacks(ResourceKey<CreativeModeTab> tab) {
        List<ItemStack> stacks = new ArrayList<>();
        CONTENTS.get(tab).stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> stacks.addAll(entry.getValue().get()));
        return stacks;
    }

    // Built empty and filled through modifyCreativeTab; the builder is deprecated only by NeoForge's patches.
    @SuppressWarnings("deprecation")
    private static CreativeModeTab build(Identifier id, List<Identifier> icons) {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup." + id.getNamespace()))
                .icon(() -> icons.stream()
                        .flatMap(icon -> BuiltInRegistries.ITEM.getOptional(icon).stream())
                        .findFirst()
                        .map(ItemStack::new)
                        .orElseGet(() -> new ItemStack(Items.BOOK)))
                .build();
    }
}
