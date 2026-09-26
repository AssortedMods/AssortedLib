package com.grim3212.assorted.lib.manual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * One mod's entry in the manual's index, from {@code assets/<modId>/manual/section.json} or from
 * {@link ManualRegistry}. The file wins, so a resource pack can override either.
 *
 * @param modId     the namespace whose manual folder supplies this section's chapters
 * @param sortOrder lower sorts first in the index; ties fall back to the mod id
 * @param icon      drawn beside the section in the index
 */
public record ManualSection(String modId, int sortOrder, Supplier<ItemStack> icon) {

    /** Reserved for Assorted Lib's own section, which sorts first. */
    public static final int LIB_SORT_ORDER = -1000;

    public static final int DEFAULT_SORT_ORDER = 0;

    public static ManualSection of(String modId, Supplier<ItemStack> icon) {
        return new ManualSection(modId, DEFAULT_SORT_ORDER, icon);
    }

    public Component title() {
        return Component.translatable(titleKey());
    }

    public String titleKey() {
        return "manual." + this.modId + ".title";
    }

    /** Shown on the section's chapter list, above the chapters themselves. */
    public Component description() {
        return Component.translatable("manual." + this.modId + ".description");
    }

    /**
     * A section file, before the path has told us which mod it belongs to. {@code icon} is one item
     * or a list, the first one registered drawn: mods sharing a section each name the others' items.
     */
    public record Definition(int sortOrder, List<Identifier> icons) {

        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("sort_order", DEFAULT_SORT_ORDER).forGetter(Definition::sortOrder),
                ExtraCodecs.compactListCodec(Identifier.CODEC).optionalFieldOf("icon", List.of()).forGetter(Definition::icons)
        ).apply(instance, Definition::new));

        /**
         * The icon is looked up when the index is actually drawn. Sections are read during the
         * first resource reload, which happens before item components are bound, and building a
         * stack then fails.
         */
        public ManualSection bind(String modId) {
            List<Identifier> icons = this.icons;
            return new ManualSection(modId, this.sortOrder, () -> icons.stream()
                    .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id).stream())
                    .findFirst()
                    .map(ItemStack::new)
                    .orElseGet(() -> new ItemStack(Items.BOOK)));
        }
    }
}
