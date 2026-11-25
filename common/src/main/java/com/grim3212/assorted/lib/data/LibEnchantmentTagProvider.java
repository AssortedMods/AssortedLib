package com.grim3212.assorted.lib.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Enchantment tags for a mod's own enchantments, which decide whether one is offered at the table,
 * traded or looted. Entries from {@link #obtainable} are optional, so an enchantment disabled by a
 * condition on its definition ({@link LibDatapackRegistryProvider#conditions()}) drops out of the
 * tag instead of breaking it.
 */
public abstract class LibEnchantmentTagProvider extends TagsProvider<Enchantment> {

    private static final List<TagKey<Enchantment>> OBTAINABLE = List.of(EnchantmentTags.IN_ENCHANTING_TABLE, EnchantmentTags.TRADEABLE, EnchantmentTags.ON_RANDOM_LOOT);

    // NeoForge deprecates this constructor in favour of one that also takes a mod id; that overload
    // does not exist in vanilla, which this module builds against.
    @SuppressWarnings("deprecation")
    public LibEnchantmentTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, Registries.ENCHANTMENT, lookup);
    }

    /**
     * Makes the enchantments obtainable the way vanilla's own are: the enchanting table, librarian
     * trades and random loot.
     */
    protected void obtainable(List<ResourceKey<Enchantment>> enchantments) {
        for (TagKey<Enchantment> tag : OBTAINABLE) {
            TagAppender<Enchantment> appender = this.tag(tag);
            enchantments.forEach(enchantment -> appender.addOptional(enchantment.location()));
        }
    }
}
