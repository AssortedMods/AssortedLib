package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.util.ItemUtil;
import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** What {@code creative.hideUncraftableItems} hides: items whose material tag has nothing in it. */
final class CreativeTabItemsTests {

    private CreativeTabItemsTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("empty_material_tags_count_as_uncraftable", CreativeTabItemsTests::emptyMaterialTags);
    }

    private static void emptyMaterialTags(GameTestHelper helper) {
        TagKey<Item> undefined = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("assortedlib", "test/never_defined"));
        helper.assertTrue(BuiltInRegistries.ITEM.get(undefined).isEmpty(), "the undefined test tag is defined");
        helper.assertTrue(ItemUtil.isTagEmpty(undefined), "a tag no pack defines counts as having a material");
        helper.assertFalse(ItemUtil.isTagEmpty(LibCommonTags.Items.INGOTS_IRON), "iron ingots count as missing");
        helper.succeed();
    }
}
